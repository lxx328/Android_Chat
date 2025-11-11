package com.dexter.little_smart_chat.utils.yzs
import android.content.Context
import android.util.Log
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.utils.OPUtils
import com.unisound.active.IActiveListener
import com.unisound.active.SDKActive
import com.unisound.offline.tts.DefaultModelsManager
import com.unisound.offline.tts.ITtsEventListener
import com.unisound.offline.tts.TtsEvent
import com.unisound.offline.tts.TtsOption
import com.unisound.offline.tts.TtsVoiceName
import com.unisound.offline.tts.UnisoundOfflineTtsEngine
import com.unisound.utils.AssetsUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object YZSTTSUtils {
    private lateinit var unisoundTtsEngine: UnisoundOfflineTtsEngine

    // 使用线程安全的队列和协程管理
    private val ttsQueue = ConcurrentLinkedQueue<String>()
    private val isPlaying = AtomicBoolean(false)
    private val shouldContinue = AtomicBoolean(true)

    // 使用协程作用域管理TTS任务
    private val ttsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentPlayJob: Job? = null

    private lateinit var ttsPath: String
    private var voiceNameChoice = 0
    private val TAG = "YZSTTSUtils"

    @Volatile
    private var isInit = false

    // TTS配置
    private const val MIN_TEXT_LENGTH = 2 // 最小播放文本长度
    private const val MAX_QUEUE_SIZE = 100 // 队列最大长度

    fun initTts() {
        ttsPath =MyApplication.instance?.getAppContext()?.getExternalFilesDir("tts")?.absolutePath!!
        checkIsInit()
    }

    private fun initEngines() {
        val ttsmodel =MyApplication.instance?.getAppContext()?.getExternalFilesDir("ttsmodels")
        if (ttsmodel == null || !ttsmodel.exists()) {
            OPUtils.Logger.dtf(TAG, "请先初始化TTS路径")
            return
        }
        val backModelPath =  ttsmodel.absolutePath + File.separator + "backend_model_cn_offline_shasha"
        val frontModelPath = ttsmodel.absolutePath + File.separator + "frontend_model_cn_offline"

        unisoundTtsEngine =MyApplication.instance?.getAppContext()?.let {
           MyApplication.instance?.getAppContext()?.let { it1 ->
                UnisoundOfflineTtsEngine.Builder()
                    .setTtsOption(TtsOption.TTS_OPTION_LANGUAGE, "cn")
                    .setSpeed(60)
                    .setModelManager(MyApplication.instance?.getAppContext()?.let {
                        CustomerModelManager(
                            it,"ttsmodels",
                            ttsmodel.absolutePath)
                    })
                    .setUserDicPath(ttsmodel.absolutePath+"/user_dict.txt")
                    .setTtsOption(TtsOption.TTS_OPTION_SAVE_TTS_PATH, ttsPath)
                    .setTtsOption(TtsOption.TTS_OPTION_SAVE_TTS, false)
                    .setTtsOption(TtsOption.TTS_OPTION_VOICE_NAME, TtsVoiceName.shasha)
                    //如果资源文件没有在aar中，则需要外部设置，不同用户不同使用方式，推荐外部设置这样代码量小
                    .setTtsOption(TtsOption.TTS_OPTION_BACK_MODEL_PATH, backModelPath)
                    .setTtsOption(TtsOption.TTS_OPTION_FRONT_MODEL_PATH, frontModelPath)
                    .setTtsOption(TtsOption.TTS_OPTION_PHONE_ENABLED,true)
                    .setTtsOption(
                        TtsOption.TTS_OPTION_SAVE_TTS_TYPE,
                        TtsOption.TtsFileType.WAV
                    )
                    .build(it1)
            }
        }!!
        unisoundTtsEngine.setTtsOption(TtsOption.TTS_OPTION_PRINT_JNI_LOG, true)
        unisoundTtsEngine.addTtsEventListener(object : ITtsEventListener {
            override fun onEvent(event: Int, msg: String?) {
                when (event) {
                    TtsEvent.TTS_EVENT_INIT_SUCCESS -> {
                       OPUtils.Logger.dtf(TAG, "unisoundTtsEngine 初始化成功")
                    }

                    TtsEvent.TTS_EVENT_ANALYZE_START -> {
                        Log.d(TAG, "TTS开始分析")
                    }

                    TtsEvent.TTS_EVENT_ANALYZE_END -> {
                        Log.d(TAG, "TTS分析结束")
                    }

                    TtsEvent.TTS_EVENT_PAUSE -> {
                        Log.e(TAG, "暂停播放")
                    }

                    TtsEvent.TTS_EVENT_RESUME -> {
                        Log.e(TAG, "继续播放")
                    }

                    TtsEvent.TTS_EVENT_PLAY_START -> {
                        Log.e(TAG, "开始播放")

                    }

                    TtsEvent.TTS_EVENT_PLAY_END -> {
                        Log.e(TAG, "结束播放")
                        isPlaying.set(false)
                        // 播放结束后，处理队列中的下一个任务
                        processNextInQueue()
                    }

                    TtsEvent.TTS_EVENT_CHANGE_VOICE_SUCCESS -> {
                        Log.e(TAG, "切换成功了")
                    }
                    TtsEvent.TTS_EVENT_PHONE_RESULT->{
                        Log.e(TAG, "phone::${msg}")
                    }
                }
            }

            override fun onError(error: Int, msg: String?) {
                Log.e(TAG, "TTS 初始化错误 onError:${error},msg:${msg}")
                isPlaying.set(false)
                // 错误时也要处理下一个队列任务
                processNextInQueue()
            }
        })
        isInit = true

        val init: Boolean = unisoundTtsEngine.init()//不能省略这是初始化的重要操作

        OPUtils.Logger.dtf(TAG, "TTS 初始化结果 init:${init}")

    }

    /**
     * 检查TTS是否被授权
     */
    fun checkIsInit(){
        //激活
        SDKActive.getInstance().active(MyApplication.instance?.getAppContext(), object : IActiveListener {
            override fun success() {
                OPUtils.Logger.dtf(TAG, "TTS已激活")
                //初始化参数
                initEngines()
            }

            override fun onError(i: Int, s: String) {
                OPUtils.Logger.etf(TAG, "激活失败 code: $i error: $s",null)
            }
        })
    }

    /**
     * 添加文本到播放队列（适用于流式输入）
     */
    fun enqueueText(text: String) {
        if (!isInit()) {
            Log.w(TAG, "TTS未初始化，忽略文本: $text")
            return
        }

        //过滤字符"~"
        val trimmedText = text.trim()

        if (trimmedText.length < MIN_TEXT_LENGTH) {
            Log.d(TAG, "文本太短，忽略: $trimmedText")
            return
        }

        // 防止队列过长
        if (ttsQueue.size >= MAX_QUEUE_SIZE) {
            Log.w(TAG, "TTS队列已满，清理旧内容")
            ttsQueue.clear()
        }

        Log.d(TAG, "添加到TTS队列: $trimmedText (队列长度: ${ttsQueue.size})")
        ttsQueue.offer(trimmedText)

        // 如果当前没有播放，开始处理队列
        if (!isPlaying.get()) {
            processNextInQueue()
        }
    }

    /**
     * 直接播放文本（立即打断当前播放）
     */
    fun playImmediately(text: String) {
        if (!isInit()) {
            Log.w(TAG, "TTS未初始化")
            return
        }

        stop() // 停止当前播放
        clearQueue() // 清空队列
        enqueueText(text) // 添加到队列并播放
    }

    /**
     * 处理队列中的下一个任务
     */
    private fun processNextInQueue() {
        if (!shouldContinue.get() || isPlaying.get()) {
            return
        }

        val nextText = ttsQueue.poll()
        if (nextText != null) {
            isPlaying.set(true)
            Log.d(TAG, "开始播放队列中的文本: $nextText")
            try {
                unisoundTtsEngine.playTts(nextText.replace("~", "").replace("～", ""))
            } catch (e: Exception) {
                Log.e(TAG, "播放TTS失败", e)
                isPlaying.set(false)
                // 播放失败时，继续处理下一个
                processNextInQueue()
            }
        }
    }

    /**
     * 兼容旧接口的播放方法
     */
    fun play(text: String) {
        enqueueText(text)
    }

    /**
     * 停止播放并清空队列
     */
    fun stop() {
        if (!isInit()) {
            return
        }

        shouldContinue.set(false)
        isPlaying.set(false)

        try {
            unisoundTtsEngine.stop()
        } catch (e: Exception) {
            Log.e(TAG, "停止TTS失败", e)
        }

        clearQueue()
        shouldContinue.set(true)
        Log.d(TAG, "TTS已停止，队列已清空")
    }

    /**
     * 清空播放队列
     */
    fun clearQueue() {
        val size = ttsQueue.size
        ttsQueue.clear()
        Log.d(TAG, "清空TTS队列，移除了 $size 个项目")
    }

    /**
     * 暂停播放（不清空队列）
     */
    fun pause() {
        if (!isInit()) {
            return
        }
        try {
            unisoundTtsEngine.pause()
        } catch (e: Exception) {
            Log.e(TAG, "暂停TTS失败", e)
        }
    }

    /**
     * 恢复播放
     */
    fun resume() {
        if (!isInit()) {
            return
        }
        try {
            unisoundTtsEngine.resume()
        } catch (e: Exception) {
            Log.e(TAG, "恢复TTS失败", e)
        }
    }

    /**
     * 切换语音
     */
    fun changeVoice(voiceName: TtsVoiceName) {
        if (!isInit()) {
            return
        }
        try {
            unisoundTtsEngine.changeVoiceName(voiceName)
        } catch (e: Exception) {
            Log.e(TAG, "切换语音失败", e)
        }
    }

    /**
     * 获取队列状态
     */
    fun getQueueInfo(): String {
        return "队列长度: ${ttsQueue.size}, 正在播放: ${isPlaying.get()}"
    }

    /**
     * 释放
     */
    fun release() {
        if (!isInit()){
            Log.e(TAG, " release 请先初始化TTS")
            return
        }
        stop()
        currentPlayJob?.cancel()
        ttsScope.cancel()

        try {
            unisoundTtsEngine.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放TTS资源失败", e)
        }

        isInit = false
        Log.i(TAG, "TTS资源已释放")
    }
    /**
     * 初始化
     */
    /**
     * 检查是否已初始化
     */
    fun isInit(): Boolean {
        return try {
            ::unisoundTtsEngine.isInitialized && unisoundTtsEngine.init() && isInit
        } catch (e: Exception) {
            Log.e(TAG, "检查TTS初始化状态失败", e)
            false
        }
    }



    class CustomerModelManager(val context: Context, assetDir:String, outDir:String):
        DefaultModelsManager(assetDir,outDir){

        override fun prepare(context: Context?): Boolean {
            //做拷贝一些文件的动作。拷贝用户词典到一个目录。和setUserDicPath保存一样的路径就好了
            val ttsmodel = context?.getExternalFilesDir("ttsmodels")
            AssetsUtils.copyFileToPath2(context, "user_dict.txt", ttsmodel?.absolutePath+"/user_dict.txt")
            return super.prepare(context)
        }
    }

}