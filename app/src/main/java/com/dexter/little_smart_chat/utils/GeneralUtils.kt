package com.xctech.advertise.mvvm.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Random
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object GeneralUtils {


    @SuppressLint("HardwareIds")
    fun getSN(context: Context): String? {
        // 1. 首先检查缓存
        val sp = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)
        sp.getString("device_sn", null)?.let { return it }

        var sn: String? = null

        try {
            when {
                // Android 15+ (根据你的发现可以获取)
                Build.VERSION.SDK_INT >= 34 -> {
                    try {
                        sn = Build.getSerial()
                    } catch (e: Exception) {
                        Log.e("dx","getySN Android 15+获取SN失败: ${e.message}")
                        sn = Build.SERIAL

                    }
                }

                // Android 10-14 (特别是Android 11+需要系统权限)
                Build.VERSION.SDK_INT >= 29 -> {
                    // 这些版本普通应用几乎不可能获取真实序列号
                    // 直接使用替代方案
                    try {
                        sn = Build.getSerial()
                    } catch (e: Exception) {
                        Log.e("dx","getySN Android 10-14 +获取SN失败: ${e.message}")
                        sn = Build.SERIAL

                    }
                }

                // Android 8.0-9.0
                Build.VERSION.SDK_INT >= 26 -> {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            sn = Build.getSerial()
                        } catch (e: Exception) {
                            Log.e("dx","getySN Android 8.0-9.0获取SN失败: ${e.message}")
                            sn = Build.SERIAL
                        }
                    }
                }

                // Android 8.0以下
                else -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            sn = Build.getSerial()
                        }else{
                            sn = Build.SERIAL
                        }
                    } catch (e: Exception) {
                        Log.e("dx","getySN Android 8.0以下获取SN失败: ${e.message}")
                        sn = Build.SERIAL
                    }                }
            }

            // 检查获取的序列号是否有效
            if (sn.isNullOrEmpty() || sn.equals("unknown", ignoreCase = true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sn = getAlternativeDeviceId(context)
                }
            }

        } catch (e: Exception) {
            Log.e("dx","getySN 获取设备SN号异常: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sn = getAlternativeDeviceId(context)
            }
        }

        // 保存有效的SN
        if (!sn.isNullOrEmpty()) {
            sp.edit().putString("device_sn", sn).apply()
        }

        Log.e("dx","getySN 获取设备SN号: $sn")
        return sn
    }
    /**
     * 获取替代的设备标识符
     * 适用于Android 10+或无法获取真实序列号的情况
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @SuppressLint("HardwareIds")
    private fun getAlternativeDeviceId(context: Context): String {

        val RANDOM_SN_PREFIX = "H8T" //随机sn前缀
        val sb = java.lang.StringBuilder()
        val random = Random()
        for (i in 0 until 12) {
            val number = random.nextInt(10)
            sb.append(number)
        }
        return RANDOM_SN_PREFIX+ sb.toString()
//        // 生成基于多个设备特征的唯一标识符
//
//        val deviceId = StringBuilder()
//
//        try {
//            // 1. Android ID (在Android 8.0+对每个应用都是唯一的)
//            val androidId = Settings.Secure.getString(
//                context.contentResolver,
//                Settings.Secure.ANDROID_ID
//            )
//            if (androidId != null && androidId != "9774d56d682e549c") { // 避免模拟器默认值
//                deviceId.append(androidId)
//            }
//
//            // 2. 设备型号和制造商
//            deviceId.append(Build.MODEL).append(Build.MANUFACTURER)
//
//            // 3. 设备指纹的部分信息
//            deviceId.append(Build.FINGERPRINT.hashCode())
//
//            // 4. 应用安装时间戳(首次安装时生成)
//            val pm = context.packageManager
//            val pi = pm.getPackageInfo(context.packageName, 0)
//            deviceId.append(pi.firstInstallTime)
//        } catch (e: Exception) {
//            // 如果以上方法都失败，使用随机生成的ID
//          Log.e(
//                "getAlternativeDeviceId",
//                "生成替代设备ID时异常: " + e.message,
//                null
//            )
//        }
//        val finalDeviceId = hashString(deviceId.toString())
//
//        return finalDeviceId
    }

    /**
     * 生成字符串的SHA-256哈希值
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun hashString(input: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            val hexString = StringBuilder()

            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }

            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            // 如果SHA-256不可用，使用简单的hashCode
            return abs(input.hashCode().toDouble()).toString()
        }
    }
        fun hideSystemUI(activity: Activity) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                Log.d("HideUtils", "Android 11 (API 30) 及以上版本");
                // Android 11 (API 30) 及以上版本
                val controller = activity.window.insetsController
                if (controller != null) {
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                Log.d("HideUtils", "Android 10 (API 30) 及以下版本");

                val decorView = activity.window.decorView;
                // Android 11 以下版本
                val flags = View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                decorView.systemUiVisibility = flags
            }
        }

    //创建一个类用于调整设备媒体音量
    /**
     * 调整设备媒体音量
     * @param context 上下文
     * @param volume 音量值 (范围: 0-100，表示音量百分比)
     * @return 设置是否成功
     */
    fun adjustMediaVolume(context: Context, volume: Int): Boolean {
        return adjustMediaVolume(context, volume, false, false)
    }

    /**
     * 调整设备媒体音量
     * @param context 上下文
     * @param volume 音量值 (范围: 0-100，表示音量百分比) 默认值为0~15
     * @param showUI 是否显示系统音量UI
     * @param ignoreDoNotDisturb 是否忽略勿扰模式
     * @return 设置是否成功
     */
    fun adjustMediaVolume(context: Context, volume: Int, showUI: Boolean, ignoreDoNotDisturb: Boolean): Boolean {
        try {
            // 获取AudioManager实例
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // 获取媒体音量的最大值
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            // 将百分比值转换为实际音量级别
            val volumeLevel = when {
                volume < 0 -> 0
                volume > 100 -> maxVolume
                else -> (volume * maxVolume / 15)
            }

            // 设置音量调整的标志
            var flags = 0
            if (showUI) {
                flags = flags or AudioManager.FLAG_SHOW_UI
            }
            if (ignoreDoNotDisturb) {
                flags = flags or AudioManager.FLAG_ALLOW_RINGER_MODES
            }

            // 设置媒体音量
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volumeLevel,
                flags
            )

            return true
        } catch (e: Exception) {
            Log.e("MediaVolumeAdjuster", "调整媒体音量失败: ${e.message}")
            return false
        }
    }

    /**
     * 获取当前媒体音量百分比
     * @param context 上下文
     * @return 当前媒体音量百分比 (0-100)
     */
    fun getCurrentMediaVolume(context: Context): Int {
        try {
            // 获取AudioManager实例
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // 获取当前媒体音量
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // 获取媒体音量的最大值
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            // 计算并返回百分比
            return (currentVolume * 100) / maxVolume
        } catch (e: Exception) {
            Log.e("MediaVolumeAdjuster", "获取媒体音量失败: ${e.message}")
            return 0
        }
    }

    //創建一個常用的綫程池
    object ThreadPoolManager {
        // 通用线程池
        private val threadPoolExecutor: ThreadPoolExecutor

        // 定时任务线程池
        private val scheduledExecutor: ScheduledThreadPoolExecutor
        // 核心线程池大小
        const val CORE_POOL_SIZE: Int = 10
        // 最大线程池大小
        const val MAX_POOL_SIZE: Int = 20
        // 非核心线程空闲存活时间
        const val KEEP_ALIVE_TIME: Long = 60L
        // 线程池的队列容量
        const val QUEUE_CAPACITY: Int = 100

        // 私有构造函数，确保单例模式
        init {
            threadPoolExecutor = ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                LinkedBlockingQueue(QUEUE_CAPACITY),
                ThreadPoolExecutor.CallerRunsPolicy()
            )

            scheduledExecutor = ScheduledThreadPoolExecutor(CORE_POOL_SIZE)
        }

        // 提交任务到通用线程池
        fun execute(task: Runnable?) {
            threadPoolExecutor.execute(task)
        }

        // 提交带返回值的任务到通用线程池
        fun <T> submit(task: Callable<T>): Future<T> {
            return threadPoolExecutor.submit(task)
        }

        // 安排定时任务
        fun schedule(task: Runnable?, delay:  Long, unit: TimeUnit?): ScheduledFuture<*> {
            return scheduledExecutor.schedule(task, delay, unit)
        }

        // 安排周期性任务
        fun scheduleAtFixedRate(
            task: Runnable?,
            initialDelay: Long,
            period: Long,
            unit: TimeUnit?
        ): ScheduledFuture<*> {
            return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit)
        }

        // 安排带固定延迟的周期性任务
        fun scheduleWithFixedDelay(
            task: Runnable?,
            initialDelay: Long,
            delay: Long,
            unit: TimeUnit?
        ): ScheduledFuture<*> {
            return scheduledExecutor.scheduleWithFixedDelay(task, initialDelay, delay, unit)
        }

        // 关闭线程池
        fun shutdown() {
            threadPoolExecutor.shutdown()
            scheduledExecutor.shutdown()
        }

        // 立即关闭线程池
        fun shutdownNow() {
            threadPoolExecutor.shutdownNow()
            scheduledExecutor.shutdownNow()
        }


        fun <T> submitNoWait(tCallable: Callable<T>) {
            threadPoolExecutor.submit(tCallable)
        } //实现一个同一个接口批量请求带一个参数然后获得批量所有的返回结果后返回带一个key知道那些请求成功

    }


}