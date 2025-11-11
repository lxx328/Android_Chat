package com.dexter.little_smart_chat

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dexter.little_smart_chat.config.AIConfig
import com.dexter.little_smart_chat.service.SystemStatusService
import com.dexter.little_smart_chat.utils.AudioUtils
import com.dexter.little_smart_chat.utils.OPUtils
import com.dexter.little_smart_chat.utils.yzs.YZSOnlineTTSUtils
import com.unisound.active.AICodeType
import com.unisound.cloud.RecognizerConstant
import com.unisound.cloud.SpeechUtility
import com.dexter.little_smart_chat.utils.yzs.YZSTTSUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {


    private var ai_conversion_id = ""
    companion object {
        var instance: MyApplication? = null
        @Volatile
        var modeName = "小智"
        private const val TAG = "MyApplication"

        var isInitXZ = true

    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Application created")

        // 初始化AI配置
        AIConfig.initialize(this)

        // 启动系统状态服务
        startSystemStatusService()

        // 初始化AI框架（如果有配置的API密钥）
        initializeAIFramework()

        // 清理无效的录音文件
        cleanupAudioFiles()

        initYZSAIASR()

        initYZSOnlineTTS()

//        initYZSTTS()

    }


    private fun startSystemStatusService() {
        try {
            val intent = Intent(this, SystemStatusService::class.java)
            startService(intent)
            Log.d(TAG, "SystemStatusService started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SystemStatusService: ${e.message}")
        }
    }

    /**
     * 初始化AI框架
     */
    private fun initializeAIFramework() {
        if (AIConfig.hasValidApiKey()) {
            val currentProvider = AIConfig.getCurrentProvider()
            val apiKey = AIConfig.getApiKeyForProvider(currentProvider)

            apiKey?.let { key ->
                val aiManager = com.dexter.little_smart_chat.ai.manager.AIModelManager.getInstance()
                aiManager.initialize(key, currentProvider)
            }
        }
    }

    /**
     * 清理无效的录音文件
     */
    private fun cleanupAudioFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cleanedCount = AudioUtils.cleanupInvalidFiles(this@MyApplication)
                if (cleanedCount > 0) {
                    Log.d(TAG, "Cleaned up $cleanedCount invalid audio files")
                }

                // 检查存储空间
                if (AudioUtils.shouldCleanupStorage(this@MyApplication)) {
                    Log.w(TAG, "Audio storage is getting full, consider cleanup")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio cleanup: ${e.message}")
            }
        }
    }

    fun getAppContext(): Context? {
        return instance?.applicationContext
    }

    /**
     * 初始化YZS语音识别
     */
    private fun initYZSAIASR() {
        SpeechUtility.createUtility(this)
        SpeechUtility.getUtility()
            .setParameter(RecognizerConstant.ASR_APP_KEY, BuildConfig.yzsASRKey)
        SpeechUtility.getUtility()
            .setParameter(RecognizerConstant.ASR_APP_SECRET, BuildConfig.yzsASRSecret)
        //当前版本号
        Log.d(
            TAG,
            ("SDK 版本号：" + SpeechUtility.getVersion()).toString() + "asr 初始化完成 "
        )
        SpeechUtility.setLogLevel(4)
    }

    /**
     * 初始化TTSOnline
     */
    private fun initYZSOnlineTTS() {
       val tts = YZSOnlineTTSUtils
    }


    /**
     * 初始化offline YZS TTS
     */
    private fun initYZSTTS() {
        // 初始化YZSTTs offline 模式
        com.unisound.active.Config.setAppKey(BuildConfig.yzsKey) //申请的ASR的KEY
        com.unisound.active.Config.setAppSecret(BuildConfig.yzsSecret) //申请的ASR的SECRET
        com.unisound.active.Config.setUdid(OPUtils.getSN(this)) //设置设备ID
        com.unisound.active.Config.setAiCode(AICodeType.AI_SSP_DICTATION_TTS_OFF) //授权类型
        com.unisound.active.Config.setLogEnabled(true)

        YZSTTSUtils.initTts()
    }

    fun getAi_conversion_id(): String {
        return ai_conversion_id
    }

    fun setAi_conversion_id(ai_conversion_id: String) {
        this@MyApplication.ai_conversion_id = ai_conversion_id
    }

}
