package com.dexter.little_smart_chat.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import com.dexter.little_smart_chat.R
import java.text.SimpleDateFormat
import java.util.*

class StatusBarUpdater(
    private val context: Context,
    private val timeText: TextView,
    private val wifiIcon: ImageView,
    private val batteryIcon: ImageView
) {
    private val handler = Handler(Looper.getMainLooper())
    private var batteryAnimation: AnimationDrawable? = null
    private var isCharging = false
    private var batteryLevel = 100
    
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 60000) // 每分钟更新一次
        }
    }
    
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    
                    updateBatteryIcon()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    updateBatteryIcon()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    updateBatteryIcon()
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION,
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    updateWifiIcon()
                }
            }
        }
    }
    
    init {
        // 注册状态广播接收器
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        context.registerReceiver(statusReceiver, filter)
        
        // 开始时间更新
        handler.post(timeUpdateRunnable)
        
        // 初始化电池动画
        batteryIcon.setImageResource(R.drawable.battery_charging_animation)
        batteryAnimation = batteryIcon.drawable as? AnimationDrawable
        
        // 初始化WiFi状态
        updateWifiIcon()
    }
    
    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = sdf.format(Date())
    }
    
    private fun updateBatteryIcon() {
        if (isCharging) {
            // 使用充电动画
            if (batteryAnimation?.isRunning != true) {
                batteryIcon.setImageResource(R.drawable.battery_charging_animation)
                batteryAnimation = batteryIcon.drawable as? AnimationDrawable
                batteryAnimation?.start()
            }
        } else {
            // 停止动画，显示静态图标
            batteryAnimation?.stop()
            batteryAnimation = null
            
            val iconRes = when {
                batteryLevel > 30 -> R.drawable.ic_battery_green
                batteryLevel > 15 -> R.drawable.ic_battery_yellow
                else -> R.drawable.ic_battery_red
            }
            batteryIcon.setImageResource(iconRes)
        }
    }
    
    private fun updateWifiIcon() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isWifiConnected = networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        
        val iconRes = if (isWifiConnected) {
            R.drawable.ic_wifi
        } else {
            R.drawable.ic_wifi_off
        }
        wifiIcon.setImageResource(iconRes)
    }
    
    fun release() {
        handler.removeCallbacks(timeUpdateRunnable)
        try {
            context.unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // 忽略未注册的异常
        }
        batteryAnimation?.stop()
        batteryAnimation = null
    }
} 