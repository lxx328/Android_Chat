package com.dexter.little_smart_chat.controller

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.data.MqttData
import com.xctech.advertise.mvvm.Iinterface.MQTTCallback

import com.xctech.advertise.mvvm.utils.GeneralUtils
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object MqttController {


    const val TAG: String = "MqttController"

    private var mqttCallback: MQTTCallback? = null
    private var mScheduledExecutorService: ScheduledExecutorService? =
        Executors.newSingleThreadScheduledExecutor()
    private var mMqtt_sub_topic: List<String>? = null
    private var mClient: MqttAsyncClient? = null
    private var mOptions: MqttConnectionOptions? = null
    var brokerA:String =""


    fun initController(mqttCallbackA: MQTTCallback ) {
        if (mClient != null) {
            mClient!!.disconnect()
            Log.d(TAG, "MqttAndroidClient 断开连接成功")
            mClient = null
        }
        this.mqttCallback = mqttCallbackA
    }

    @Throws(MqttException::class)
    fun init(context: Context?, mqData: MqttData) {
        mMqtt_sub_topic = mqData.topicArray
        brokerA = mqData.broker
        //204为连接id冲突注意不要连接冲突
        mClient = MqttAsyncClient(mqData.broker,
            MyApplication.instance?.applicationContext?.let { GeneralUtils.getSN(it) }, MemoryPersistence())

        mClient!!.setCallback(object : MqttCallback {
            override fun disconnected(disconnectResponse: MqttDisconnectResponse) {
                //断开连接
                Log.d(TAG, "mqtt连接断开--connectionLost case：$disconnectResponse")
                // 初始化ScheduledExecutorService（如果尚未初始化）
                if (mScheduledExecutorService == null || mScheduledExecutorService!!.isShutdown) {
                    mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                }
                // 安排首次重连尝试，之后每30秒尝试一次
                scheduleReconnect()
            }

            override fun mqttErrorOccurred(exception: MqttException) {
                Log.d(TAG, "mqttErrorOccurred: " + exception.message)
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                if (mqttCallback != null) {
                    mqttCallback!!.onMQTTMessage(topic, message)
                    Log.d(TAG, "messageArrived:mqttCallback topic:$topic")
                }
                val payload = String(message.payload)
                Log.d(TAG, "messageArrived: $topic : $payload")
            }

            override fun deliveryComplete(token: IMqttToken) {
                //发送消息成功后的回调
                Log.d(TAG, "deliveryComplete:token = $token")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    // 在这里也可以调用resubscribeTopics()来重新订阅
                    Log.d(TAG, "MQTT客户端已重新连接到: $serverURI")
                    resubscribeTopics()
                    stopExecuteService()
                } else {
                    Log.d(TAG, "MQTT客户端首次连接到: $serverURI")
                }
            }

            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties) {
            }
        })
        mOptions = MqttConnectionOptions()
        mOptions!!.isAutomaticReconnect = true
        mOptions!!.isCleanStart = false //设置是否清除缓存
        mOptions!!.userName = mqData.username
        mOptions!!.password = mqData.password.toByteArray()
        mOptions!!.connectionTimeout = 30
        mOptions!!.keepAliveInterval = 60
        // 设置最大重连间隔
        mOptions!!.maxReconnectDelay = 120
        mClient!!.connect(mOptions, null, object : MqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Log.d(TAG, "MQTT连接成功---开始订阅---topic=${mqData.topicArray} ")
                    if (mqData.topicArray != null) {
                        try {
                            addSubscribeTopic(mqData.topicArray)
                            Log.d(
                                TAG,
                                "MQTT连接成功---开始订阅---topic=$mqData.topicArray"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e(TAG, "MQTT连接失败---Msg=" + e.message)
                        }
                    }


//                pubHeartBeatMsg(Config.DEVICE_HEART_TOPIC, msg)
//                //打印
//                Log.d(TAG, "MQTT连接成功---开始发送第一条心跳---Msg=$msg")
            }

            override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                var msg = ""
                if (exception is MqttException) {
                    msg = exception.message.toString()
                    Log.e(TAG, "MQTT连接失败---Msg= $msg")
                } else {
                    Log.e(TAG, "MQTT连接失败---Msg=$msg")
                }
            }
        }
        )
    }

    fun pubHeartBeatMsg(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = 0

            mClient!!.publish(topic, mqttMessage, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "pubHeartBeatMsg发送成功-Msg=$message------topic=$topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    var errorMsg = "pubHeartBeatMsg发送失败"
                    if (exception != null) {
                        errorMsg += "-Msg=" + exception.message
                        Log.e(TAG, "pubHeartBeatMsg发送失败：$errorMsg")
                    } else {
                        errorMsg += "-原因未知"
                        Log.e(TAG, "pubHeartBeatMsg发送失败：$errorMsg")
                    }
                    Log.e(TAG, "pubHeartBeatMsg发送失败：$errorMsg")
                }
            })
//            });
        } catch (e: Exception) {
            Log.e(TAG, "pubHeartBeatMsg发送失败" + e.message, e)
            e.printStackTrace()
        }
    }

    fun setMqttData(mqttData: MqttData?): Boolean {
        if (mqttData == null || TextUtils.isEmpty(mqttData.broker)  || TextUtils.isEmpty(
                mqttData.username
            ) || TextUtils.isEmpty(mqttData.password)
        ) {
            return false
        } else {
            this.brokerA = mqttData.broker
            Log.i(TAG, "--------setMqttData-设置成功")
            return true
        }
    }

    @Throws(MqttException::class)
    fun addSubscribeTopic(topicList: List<String>) {
        if (mClient == null) {
            return
        }
        for (topic in topicList) {

            mClient!!.subscribe(topic, 0, null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "MQTT订阅成功---Msg=$topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e(
                        TAG,
                        "MQTT订阅失败---Msg=" + topic + "---error:" + exception.message
                    )
                }
            })
        }
    }

    // 断开连接
    fun disconnect() {
        try {
            if (mClient != null) {
                mClient!!.disconnect()
                Log.d(TAG, "MqttAndroidClient 断开连接成功")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "MqttAndroidClient 断开连接失败-Msg=" + e.message)
        }
    }

    fun isConnect(): Boolean {
        try {
            if (mClient != null) {
                return mClient!!.isConnected
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "MqttAndroidClient 断开连接失败-Msg=" + e.message)
        }
        return false
    }

    private fun scheduleReconnect() {
        mScheduledExecutorService!!.scheduleAtFixedRate(reconnectTask, 0, 30, TimeUnit.SECONDS)
    }

    private fun resubscribeTopics() {
        try {
            if (mMqtt_sub_topic != null && !mMqtt_sub_topic!!.isEmpty()) {
                for (topic in mMqtt_sub_topic!!) {
                    mClient!!.subscribe(topic, 0, null, object : MqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken) {
                            Log.d(TAG, "重连后订阅成功: $topic")
                        }

                        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                            Log.e(
                                TAG,
                                "重连后订阅失败: " + topic + "Msg:" + exception
                            )
                        }
                    })
                }
            }
        } catch (e: Exception) {
            if (e != null) {
                e.printStackTrace()
                Log.e(TAG, "重连后重新订阅主题失败Msg:" + e.message)
            }
        }
    }

    private fun stopExecuteService() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService!!.shutdown()
            Log.d(TAG, "执行服务停止成功")
        }
    }

    private val reconnectTask = Runnable {
        try {
            if (!mClient!!.isConnected) {
                mClient!!.reconnect()
                Log.d(TAG, "尝试mqtt执行重连操作成功:")
            } else {
                // 如果已经连接成功，取消后续的重连任务
                stopExecuteService()
                Log.d(TAG, "重连成功关闭定时池:")
            }
        } catch (e: MqttException) {
            e.printStackTrace()
            Log.e(TAG, "mqtt执行重连操作失败Msg:" + e.message)
            // 如果重连失败，可以在这里选择继续安排下一次重试或者停止尝试
            scheduleReconnect()
        }
    }

    fun getCommandIdByMqttMsg(msg: MqttMessage): String? {
        try {
            // 将 MQTT 消息的 payload 转换为字符串
            val payload = String(msg.payload)
            // 将 JSON 格式的字符串转换为 JSONObject
            val jsonObject = JSONObject(payload)
            // 从 JSONObject 中获取 commandId 字段的值
            Log.d(TAG, "---------------------getCommandIdByMqttMsg: " + jsonObject.getString("commandId"))
            return jsonObject.getString("commandId")
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e(TAG, "解析Mqtt的消息时出错 $e")
        }
        return null
    }
}