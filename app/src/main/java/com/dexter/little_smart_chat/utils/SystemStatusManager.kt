package com.dexter.little_smart_chat.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.ImageView
import android.widget.TextView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.service.SystemStatusService

class SystemStatusManager(
    private val context: Context,
    private val timeText: TextView,
    private val wifiIcon: ImageView,
    private val batteryIcon: ImageView
) {
    
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SystemStatusService.ACTION_TIME_UPDATED -> {
                    val currentTime = intent.getStringExtra(SystemStatusService.EXTRA_CURRENT_TIME)
                    currentTime?.let { updateTime(it) }
                }
                SystemStatusService.ACTION_WIFI_STATUS_CHANGED -> {
                    val isConnected = intent.getBooleanExtra(SystemStatusService.EXTRA_WIFI_CONNECTED, false)
                    updateWifiStatus(isConnected)
                }
                SystemStatusService.ACTION_BATTERY_STATUS_CHANGED -> {
                    val batteryLevel = intent.getIntExtra(SystemStatusService.EXTRA_BATTERY_LEVEL, -1)
                    val isCharging = intent.getBooleanExtra(SystemStatusService.EXTRA_BATTERY_CHARGING, false)
                    val batteryColor = intent.getStringExtra(SystemStatusService.EXTRA_BATTERY_COLOR) ?: "green"
                    updateBatteryStatus(batteryLevel, isCharging, batteryColor)
                }
            }
        }
    }
    
    private var currentBatteryLevel = -1
    private var currentBatteryCharging = false
    private var currentBatteryColor = "green"
    
    init {
        registerReceiver()
    }
    
    /**
     * 注册广播接收器
     */
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(SystemStatusService.ACTION_TIME_UPDATED)
            addAction(SystemStatusService.ACTION_WIFI_STATUS_CHANGED)
            addAction(SystemStatusService.ACTION_BATTERY_STATUS_CHANGED)
        }
        context.registerReceiver(statusReceiver, filter)
    }
    
    /**
     * 更新时间显示
     */
    private fun updateTime(time: String) {
        timeText.text = time
    }
    
    /**
     * 更新WiFi状态
     */
    private fun updateWifiStatus(isConnected: Boolean) {
        val iconRes = if (isConnected) {
            R.drawable.ic_wifi
        } else {
            R.drawable.ic_wifi_off
        }
        wifiIcon.setImageResource(iconRes)
    }
    
    /**
     * 更新电池状态
     */
    private fun updateBatteryStatus(level: Int, isCharging: Boolean, color: String) {
        currentBatteryLevel = level
        currentBatteryCharging = isCharging
        currentBatteryColor = color
        
        val iconRes = when {
            isCharging -> {
                when (color) {
                    "green" -> R.drawable.ic_battery_charging_green
                    "yellow" -> R.drawable.ic_battery_charging_yellow
                    "red" -> R.drawable.ic_battery_charging_red
                    else -> R.drawable.ic_battery_charging_green
                }
            }
            else -> {
                when (color) {
                    "green" -> R.drawable.ic_battery_green
                    "yellow" -> R.drawable.ic_battery_yellow
                    "red" -> R.drawable.ic_battery_red
                    else -> R.drawable.ic_battery_green
                }
            }
        }
        
        batteryIcon.setImageResource(iconRes)
    }
    
    /**
     * 获取当前电池信息
     */
    fun getCurrentBatteryInfo(): BatteryInfo {
        return BatteryInfo(
            level = currentBatteryLevel,
            isCharging = currentBatteryCharging,
            color = currentBatteryColor
        )
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            context.unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // 忽略重复注销的异常
        }
    }
    
    /**
     * 电池信息数据类
     */
    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val color: String
    )
} 