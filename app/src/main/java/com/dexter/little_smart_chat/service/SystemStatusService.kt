package com.dexter.little_smart_chat.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class SystemStatusService : Service() {
    
    companion object {
        private const val TAG = "SystemStatusService"
        private const val TIME_UPDATE_INTERVAL = 60000L // 1分钟
        
        // 广播Action
        const val ACTION_TIME_UPDATED = "com.dexter.little_smart_chat.TIME_UPDATED"
        const val ACTION_WIFI_STATUS_CHANGED = "com.dexter.little_smart_chat.WIFI_STATUS_CHANGED"
        const val ACTION_BATTERY_STATUS_CHANGED = "com.dexter.little_smart_chat.BATTERY_STATUS_CHANGED"
        
        // 广播Extra
        const val EXTRA_CURRENT_TIME = "current_time"
        const val EXTRA_WIFI_CONNECTED = "wifi_connected"
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        const val EXTRA_BATTERY_CHARGING = "battery_charging"
        const val EXTRA_BATTERY_COLOR = "battery_color"
    }
    
    private lateinit var handler: Handler
    private lateinit var timeUpdateRunnable: Runnable
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    
    // 网络状态监听器
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            checkWifiStatus()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            checkWifiStatus()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            checkWifiStatus()
        }
    }
    
    // 电池状态广播接收器
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    handleBatteryChanged(intent)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d(TAG, "Power connected")
                    checkBatteryStatus()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d(TAG, "Power disconnected")
                    checkBatteryStatus()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SystemStatusService created")
        
        handler = Handler(Looper.getMainLooper())
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        setupTimeUpdate()
        setupNetworkMonitoring()
        setupBatteryMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SystemStatusService started")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SystemStatusService destroyed")
        
        // 清理资源
        handler.removeCallbacks(timeUpdateRunnable)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        unregisterReceiver(batteryReceiver)
    }
    
    /**
     * 设置时间更新
     */
    private fun setupTimeUpdate() {
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, TIME_UPDATE_INTERVAL)
            }
        }
        
        // 立即更新一次时间
        updateTime()
        // 启动定时更新
        handler.postDelayed(timeUpdateRunnable, TIME_UPDATE_INTERVAL)
    }
    
    /**
     * 更新时间
     */
    private fun updateTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val intent = Intent(ACTION_TIME_UPDATED).apply {
            putExtra(EXTRA_CURRENT_TIME, currentTime)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Time updated: $currentTime")
    }
    
    /**
     * 设置网络监控
     */
    private fun setupNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // 立即检查一次WiFi状态
        checkWifiStatus()
    }
    
    /**
     * 检查WiFi状态
     */
    private fun checkWifiStatus() {
        val isWifiConnected = isWifiConnected()
        val intent = Intent(ACTION_WIFI_STATUS_CHANGED).apply {
            putExtra(EXTRA_WIFI_CONNECTED, isWifiConnected)
        }
        sendBroadcast(intent)
        Log.d(TAG, "WiFi status changed: connected=$isWifiConnected")
    }
    
    /**
     * 检查WiFi是否连接
     */
    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 设置电池监控
     */
    private fun setupBatteryMonitoring() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        
        // 立即检查一次电池状态
        checkBatteryStatus()
        
        // 设置定时检查电池状态（每30秒检查一次）
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkBatteryStatus()
                handler.postDelayed(this, 30000) // 30秒检查一次
            }
        }, 30000)
    }
    
    /**
     * 检查电池状态
     */
    private fun checkBatteryStatus() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let { handleBatteryChanged(it) }
    }
    
    /**
     * 处理电池状态变化
     */
    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        val batteryColor = when {
            batteryPct > 30 -> "green"
            batteryPct > 15 -> "yellow"
            else -> "red"
        }
        
        val batteryIntent = Intent(ACTION_BATTERY_STATUS_CHANGED).apply {
            putExtra(EXTRA_BATTERY_LEVEL, batteryPct)
            putExtra(EXTRA_BATTERY_CHARGING, isCharging)
            putExtra(EXTRA_BATTERY_COLOR, batteryColor)
        }
        sendBroadcast(batteryIntent)
        
        Log.d(TAG, "Battery status: level=$batteryPct%, charging=$isCharging, color=$batteryColor")
    }
} 