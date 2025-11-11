package com.dexter.little_smart_chat.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.contants.Constants
import com.dexter.little_smart_chat.controller.MqttController
import com.dexter.little_smart_chat.data.DeviceInfo
import com.dexter.little_smart_chat.data.MqttData
import com.dexter.little_smart_chat.data.MqttReplyData
import com.dexter.little_smart_chat.mvvm.viewmodel.MqServiceViewModel
import com.dexter.little_smart_chat.utils.OPUtils
import com.google.gson.Gson

import com.xctech.advertise.mvvm.Iinterface.MQTTCallback

import com.xctech.advertise.mvvm.utils.GeneralUtils

import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random
import java.util.concurrent.TimeUnit

class AdvertiseService : LifecycleService() {

    companion object {
        const val TAG = "AdvertiseService"
        const val NOTIFICATION_ID: Int = 1235
        const val NOTIFICATION_CHANNEL_ID: String = "advertise_mqtt_service_channel"
        const val NOTIFICATION_NAME = "XC_NOTIFICATION"
        const val NOTIFICATION_SERVICE = "Notifications for MQTT service"


        const val DEVICE_HEART_TOPIC: String = "deskBot/client/heartBeat/"

        //问卷下发 mqtt
        const val DEVICE_QUESTION_TOPIC: String = "/xcheng/bot/survey/"

        //问卷提交 post
        const val DEVICE_QUESTION_REPLY_TOPIC: String = "/bot/out/submit/"

        //日程查询 mqtt
        const val DEVICE_SCHEDULE_TOPIC: String = "/xcheng/bot/daywork/" //+sn

        //通知下发 mqtt
        const val DEVICE_NOTIFICATION_TOPIC: String = "/xcheng/bot/notice/" //+sn

        //修改通知状态 get
        const val DEVICE_UPDATE_NOTIFICATION_STATE_TOPIC: String = "/bot/out/updateNoticeStatus/" //+sn

        //获取通知列表 get
        const val DEVICE_NOTIFICATION_LIST_TOPIC: String = "/bot/out/listPage/" // deviceSN,pageNum,pageSize

        //获取用户信息 get
        const val DEVICE_USER_INFO_TOPIC: String = "/bot/out/getUserInfo/" //+sn

        //MP3日常通知 mqtt
        const val DEVICE_MP3_NOTIFICATION_TOPIC: String = "/xcheng/bot/daywork/"//+sn

    }
    private lateinit var notificationManager : NotificationManager
    private lateinit var wakeLock: WakeLock

    private lateinit var mqServiceViewModel: MqServiceViewModel

    private var mqtt_conneted_state: Boolean = false
    private val mMqttClient = MqttController
    private val mqttCallback = object : MQTTCallback {
        override fun onMQTTMessage(topic: String?, message: MqttMessage?) {
            //打印
            Log.d(TAG, "收到一条mqtt 消息： onMQTTMessage: topic: $topic, message: $message")
            if (topic != null && message != null) {
                executeMqttReply(message, topic)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        mqServiceViewModel = MqServiceViewModel()
        initNotification()
        initObserver()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Define the service type. This is required on Android 12 (API level 31) and higher.
//        int serviceType = NotificationCompat.FLAG_ONGOING_EVENT; // Example type, choose the appropriate one.
        // 显示前台通知并启动服务 增加前台服务类型适配Android14+
        //做一个sdk判断
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
               NOTIFICATION_ID,
                getNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, getNotification())
            Log.d(TAG, "MqttService--Android 13 or higher")
        }
        //todo调用方法
        //执行获取Mqtt数据
        mqServiceViewModel.fetchMQTTData()
        return START_STICKY
    }

    private fun initNotification() {
        // 初始化通知管理器
        notificationManager = (getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager)!!
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null")
            return
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name: CharSequence = "Mqtt Notifications"
            val description = "Notifications for MQTT service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(
               NOTIFICATION_CHANNEL_ID,
                name,
                importance
            )
            channel.description = description
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initObserver() {
        //加载
        mqServiceViewModel.loading.observe(this) {

        }

        //观察错误信息
        mqServiceViewModel.error.observe(this) {
            if (it.isNotEmpty()) {
//                toast(it)
            }
        }
        //观察初始化完成
        mqServiceViewModel.initialized.observe(this) {
            if (it) {
            }
        }

        mqServiceViewModel.mqttData.observe(this) {
            if (it != null) {
                //初始化成功后进行mqtt服务订阅连接
                Log.d(TAG, "MqttService--初始化成功 开始调用mqtt初始化连接业务")
                connectToMqttBroker(it)
            }

        }

    }

    @Throws(MqttException::class)
    private fun connectToMqttBroker(mqttData2: MqttData?) {
        // 如果当前mqtt的ip与当前的ip不同则重新设置ip和数据
        if (mMqttClient != null && mqttData2?.broker?.equals(mMqttClient.brokerA) == true) {
            Log.d(TAG, "MqttService--MQTT连接地址未改变")
            return
        }
        Log.d(TAG, "MqttService--MQTT连接地址发生改变 mMqttClient.brokerA：${mMqttClient.brokerA}  mqttData2?.broker:${mqttData2?.broker}")

        //清空原链接和订阅
        mMqttClient.disconnect()
        //重新初始化连接
        mMqttClient.initController(mqttCallback)
        if (mqttData2 != null) {
            val isSuccess: Boolean = mqttData2.broker.isNotEmpty() && MqttController.setMqttData(mqttData2)
            if (!isSuccess) {
                Log.e(TAG, "设置MQTT数据失败")
                return
            }
            mMqttClient.init(this, mqttData2)
        } else {
            Log.e(TAG, "MqttService--MQTT数据为空")
        }
    }

    private fun getNotification(): Notification {
        val notificationIntent = Intent(this, AdvertiseService::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            this,NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("ADVERTISE Service")
            .setContentText("Receiving Service messages...")
            .setSmallIcon(R.mipmap.logo_large) // 设置通知图标
            .setContentIntent(pendingIntent) // 点击通知后的行为

        return builder.build()
    }

    private fun executeMqttReply(message: MqttMessage, topic: String) {

        Log.d(TAG, "MqttService--收到一条MQTT消息： topic: $topic, message: $message")
        val gson = Gson()
        val mQttReplyData: MqttReplyData? = MqttController.getCommandIdByMqttMsg(message)?.let {
            MyApplication.instance?.applicationContext?.let { it1 ->
                GeneralUtils.getSN(it1)?.let { it1 ->
                    MqttReplyData(
                        it,
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()),
                        true,
                        it1
                    )
                }
            }
        }

        //        const val DEVICE_QUESTION_REPLY_TOPIC: String = "/bot/out/submit/"
        //
        //        //日程查询 mqtt
        //        const val DEVICE_SCHEDULE_TOPIC: String = "/xcheng/bot/daywork/" //+sn
        //
        //        //通知下发 mqtt
        //        const val DEVICE_NOTIFICATION_TOPIC: String = "/xcheng/bot/notice/" //+sn
        //
        //        //修改通知状态 get
        //        const val DEVICE_UPDATE_NOTIFICATION_STATE_TOPIC: String = "/bot/out/updateNoticeStatus/" //+sn
        //
        //        //获取通知列表 get
        //        const val DEVICE_NOTIFICATION_LIST_TOPIC: String = "/bot/out/listPage/" // deviceSN,pageNum,pageSize
        //
        //        //获取用户信息 get
        //        const val DEVICE_USER_INFO_TOPIC: String = "/bot/out/getUserInfo/" //+sn
        //
        //        //MP3日常通知
        //        const val DEVICE_MP3_NOTIFICATION_TOPIC: String = "/xcheng/bot/daywork/"//+sn
        when (topic) {
            Constants.MQTT_TOPIC_REFRESH_DATE + DeviceInfo.getTenantId() -> {
                Log.d(TAG, "MqttService--收到刷新数据")
                sendLocalBroadcast(Constants.MQTT_TOPIC_REFRESH_DATE_BROADCAST)
            }
            DEVICE_QUESTION_TOPIC + OPUtils.getSN( this)-> {
                Log.d(TAG, "MqttService--收到问卷下发")
            }
            DEVICE_NOTIFICATION_TOPIC + OPUtils.getSN( this) -> {
                Log.d(TAG, "MqttService--收到问题回复")
                mQttReplyData?.let {
//                    sendLocalBroadcast(Constants.MQTT_TOPIC_QUESTION_REPLY_BROADCAST)
                }
            }

            DEVICE_MP3_NOTIFICATION_TOPIC + OPUtils.getSN( this) -> {
                Log.d(TAG, "MqttService--收到定时任务")
                mQttReplyData?.let {
//                    sendLocalBroadcast(Constants.MQTT_TOPIC_QUESTION_REPLY_BROADCAST)
                }
            }

         }



    }

    //订阅一个本地广播用于执行MQTT回复
    private fun sendLocalBroadcast(action: String) {
        val intent = Intent()
        Log.d(TAG, "发送一个广播：sendLocalBroadcast: $action")
        intent.setAction(action)
        intent.setComponent(ComponentName("com.xctech.advertise","com.xctech.advertise.AdvertiseActivity"))
        MyApplication.instance?.applicationContext?.let { LocalBroadcastManager.getInstance(it).sendBroadcast(intent) }
    }



}