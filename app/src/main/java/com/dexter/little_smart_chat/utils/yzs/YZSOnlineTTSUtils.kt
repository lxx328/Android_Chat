package com.dexter.little_smart_chat.utils.yzs

import android.util.Log
import com.dexter.little_smart_chat.BuildConfig
import com.unisound.cloud.SpeechSynthesizer
import com.unisound.cloud.SynthesizerConstant
import com.unisound.cloud.SynthesizerEvent
import com.unisound.cloud.SynthesizerListener
import com.unisound.demo.util.DemoPrint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object YZSOnlineTTSUtils {
    private var speechSynthesizer: SpeechSynthesizer? = null
    private val progress = StringBuilder()


    // 使用线程安全的队列和协程管理
    private val ttsQueue = ConcurrentLinkedQueue<String>()
    private val isPlaying = AtomicBoolean(false)
    private val shouldContinue = AtomicBoolean(true)

    // 使用协程作用域管理TTS任务
    private val ttsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentPlayJob: Job? = null

    private lateinit var ttsPath: String
    private var voiceNameChoice = 0

    private const val TAG = "YZSOnlineTTSUtils"

    @Volatile
    private var isInit = false

    // TTS配置
    private const val MIN_TEXT_LENGTH = 2 // 最小播放文本长度
    private const val MAX_QUEUE_SIZE = 100 // 队列最大长度
    private const val DEFAULT_V_SPEED = 50 // 队列最大长度

    init {
        initParam()
    }

    private fun initParam() {
        try {
            speechSynthesizer = SpeechSynthesizer()
            /*用于测试版本需要申请 云端TTS*/
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_APP_KEY, BuildConfig.yzsTTSOnlineKey)
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_APP_SECRET, BuildConfig.yzsTTSOnlinSecret)
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_VOLUME_KEY, "60")
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_SPEED_KEY, "50")
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_PITCH_KEY, "50")
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_BRIGHT_KEY, "50")
            speechSynthesizer!!.setParameter(
                SynthesizerConstant.TTS_VCN_KEY,
                BING_BING_NEUTRAL_PLUS
            ) //设置发音人。必须设置。
            isInit = true
        }catch (e: Exception){
            e.printStackTrace()
            DemoPrint(e.message.toString())
        }
        printParam()
    }



    private fun playTTS(text: String) {
        speechSynthesizer!!.startSpeaking(text, object : SynthesizerListener {
            override fun onEvent(event: Int) {
                when (event) {
                    SynthesizerEvent.TTS_EVENT_SYNTHESIS_START -> {
                        DemoPrint( "Event: TTS_EVENT_SYNTHESIS_START")
                        progress.append("开始合成").append("\n")
                        Log.d(TAG, "合成开始")
                    }
                    SynthesizerEvent.TTS_EVENT_SYNTHESIS_END -> {
                        DemoPrint( "Event: TTS_EVENT_SYNTHESIS_END")
                        progress.append("结束合成").append("\n")
                        Log.d(TAG, "合成结束")
                    }
                    SynthesizerEvent.TTS_EVENT_PLAY_START -> {
                        DemoPrint("Event: TTS_EVENT_PLAY_START")
                        progress.append("开始播放").append("\n")
                        Log.d(TAG, "开始播放")
                    }
                    SynthesizerEvent.TTS_EVENT_PLAY_END -> {
                        DemoPrint( "Event: TTS_EVENT_PLAY_END")
                        progress.append("结束播放").append("\n")
                        Log.d(TAG, "播放结束")
                        isPlaying.set(false)
                        // 播放结束后，处理队列中的下一个任务
                        processNextInQueue()
                    }
                    SynthesizerEvent.TTS_EVENT_PAUSE -> {
                        DemoPrint( "Event: TTS_EVENT_PAUSE")
                        progress.append("暂停播放").append("\n")
                        Log.d(TAG, "暂停播放")
                    }
                    SynthesizerEvent.TTS_EVENT_RESUME -> {
                        progress.append("重新开始播放").append("\n")
                        DemoPrint( "Event: TTS_EVENT_RESUME")
                        Log.d(TAG, "重新开始播放")
                    }
                }
                DemoPrint( "Event:$event")
            }

            override fun onError(errorCode: Int, msg: String) {
                DemoPrint( "errorCode:$errorCode,msg:$msg")
                Log.d(TAG, "errorCode:$errorCode,msg:$msg")
                isPlaying.set(false)
                // 播放结束后，处理队列中的下一个任务
                processNextInQueue()
            }
        })
    }

    private fun printParam() {
        val print = StringBuilder()
        print.append("APP_KEY:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_APP_KEY)).append("\n")
        print.append("APP_SECRET:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_APP_SECRET))
            .append("\n")
        print.append("TTS_SERVER:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_SERVER_KEY))
            .append("\n")
        print.append("VCN:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_VCN_KEY)).append("\n")
        print.append("SPEED:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_SPEED_KEY)).append("\n")
        print.append("VOLUME:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_VOLUME_KEY)).append("\n")
        print.append("PITCH:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_PITCH_KEY)).append("\n")
        print.append("BRIGHT:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_BRIGHT_KEY))
            .append("\n")
        print.append("USERID:").append(speechSynthesizer!!.getParameter(SynthesizerConstant.TTS_USERID_KEY))
            .append("\n")
        DemoPrint(print.toString())
    }

    /**
     * 添加文本到播放队列（适用于流式输入）
     */
    fun enqueueText(text: String) {
        try {
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
        }catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG, "添加文本到队列失败 error:", e)
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
                speechSynthesizer!!.startSpeaking(nextText.replace("~", "").replace("～", ""),  object : SynthesizerListener {
                    override fun onEvent(event: Int) {
                        when (event) {
                            SynthesizerEvent.TTS_EVENT_SYNTHESIS_START -> {
                                DemoPrint( "Event: TTS_EVENT_SYNTHESIS_START")
                                progress.append("开始合成").append("\n")
                                Log.d(TAG, "合成开始")
                                // 通知开始分析
                                ttsStatusListener?.onTTSStartAnalyze()
                            }
                            SynthesizerEvent.TTS_EVENT_SYNTHESIS_END -> {
                                DemoPrint( "Event: TTS_EVENT_SYNTHESIS_END")
                                progress.append("结束合成").append("\n")
                                Log.d(TAG, "合成结束")
                            }
                            SynthesizerEvent.TTS_EVENT_PLAY_START -> {
                                DemoPrint("Event: TTS_EVENT_PLAY_START")
                                progress.append("开始播放").append("\n")
                                Log.d(TAG, "开始播放")
                                // 通知开始播放
                                ttsStatusListener?.onTTSStartPlay()
                            }
                            SynthesizerEvent.TTS_EVENT_PLAY_END -> {
                                DemoPrint( "Event: TTS_EVENT_PLAY_END")
                                progress.append("结束播放").append("\n")
                                Log.d(TAG, "播放结束")
                                isPlaying.set(false)
                                // 通知播放完成
                                ttsStatusListener?.onTTSPlayComplete()
                                // 播放结束后，处理队列中的下一个任务
                                processNextInQueue()
                                // 检查队列是否为空
//                                if (ttsQueue.isEmpty()) {
//                                    ttsStatusListener?.onTTSQueueEmpty()
//                                }
                            }
                            SynthesizerEvent.TTS_EVENT_PAUSE -> {
                                DemoPrint( "Event: TTS_EVENT_PAUSE")
                                progress.append("暂停播放").append("\n")
                                Log.d(TAG, "暂停播放")
                            }
                            SynthesizerEvent.TTS_EVENT_RESUME -> {
                                progress.append("重新开始播放").append("\n")
                                DemoPrint( "Event: TTS_EVENT_RESUME")
                                Log.d(TAG, "重新开始播放")
                            }
                        }
                        DemoPrint( "Event:$event")
                    }

                    override fun onError(errorCode: Int, msg: String) {
                        DemoPrint( "errorCode:$errorCode,msg:$msg")
                        Log.d(TAG, "errorCode:$errorCode,msg:$msg")
                        isPlaying.set(false)
                        // 通知错误
                        ttsStatusListener?.onTTSError("errorCode:$errorCode,msg:$msg")
                        // 播放结束后，处理队列中的下一个任务
                        processNextInQueue()
                        // 检查队列是否为空
//                        if (ttsQueue.isEmpty()) {
//                            ttsStatusListener?.onTTSQueueEmpty()
//                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "播放TTS失败", e)
                isPlaying.set(false)
                // 通知错误
                ttsStatusListener?.onTTSError("播放TTS失败: ${e.message}")
                // 播放失败时，继续处理下一个
                processNextInQueue()
//                // 检查队列是否为空
//                if (ttsQueue.isEmpty()) {
//                    ttsStatusListener?.onTTSQueueEmpty()
//                }
            }
        } else {
            // 队列为空
            ttsStatusListener?.onTTSQueueEmpty()
        }
    }
//    private fun processNextInQueue() {
//        if (!shouldContinue.get() || isPlaying.get()) {
//            return
//        }
//
//        val nextText = ttsQueue.poll()
//        if (nextText != null) {
//            isPlaying.set(true)
//            Log.d(TAG, "开始播放队列中的文本: $nextText")
//            try {
//                speechSynthesizer!!.startSpeaking(nextText.replace("~", "").replace("～", ""),  object : SynthesizerListener {
//                    override fun onEvent(event: Int) {
//                        when (event) {
//                            SynthesizerEvent.TTS_EVENT_SYNTHESIS_START -> {
//                                DemoPrint( "Event: TTS_EVENT_SYNTHESIS_START")
//                                progress.append("开始合成").append("\n")
//                                Log.d(TAG, "合成开始")
//                            }
//                            SynthesizerEvent.TTS_EVENT_SYNTHESIS_END -> {
//                                DemoPrint( "Event: TTS_EVENT_SYNTHESIS_END")
//                                progress.append("结束合成").append("\n")
//                                Log.d(TAG, "合成结束")
//                            }
//                            SynthesizerEvent.TTS_EVENT_PLAY_START -> {
//                                DemoPrint("Event: TTS_EVENT_PLAY_START")
//                                progress.append("开始播放").append("\n")
//                                Log.d(TAG, "开始播放")
//                            }
//                            SynthesizerEvent.TTS_EVENT_PLAY_END -> {
//                                DemoPrint( "Event: TTS_EVENT_PLAY_END")
//                                progress.append("结束播放").append("\n")
//                                Log.d(TAG, "播放结束")
//                                isPlaying.set(false)
//                                // 播放结束后，处理队列中的下一个任务
//                                processNextInQueue()
//                            }
//                            SynthesizerEvent.TTS_EVENT_PAUSE -> {
//                                DemoPrint( "Event: TTS_EVENT_PAUSE")
//                                progress.append("暂停播放").append("\n")
//                                Log.d(TAG, "暂停播放")
//                            }
//                            SynthesizerEvent.TTS_EVENT_RESUME -> {
//                                progress.append("重新开始播放").append("\n")
//                                DemoPrint( "Event: TTS_EVENT_RESUME")
//                                Log.d(TAG, "重新开始播放")
//                            }
//                        }
//                        DemoPrint( "Event:$event")
//                    }
//
//                    override fun onError(errorCode: Int, msg: String) {
//                        DemoPrint( "errorCode:$errorCode,msg:$msg")
//                        Log.d(TAG, "errorCode:$errorCode,msg:$msg")
//                        isPlaying.set(false)
//                        // 播放结束后，处理队列中的下一个任务
//                        processNextInQueue()
//                    }
//                })
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "播放TTS失败", e)
//                isPlaying.set(false)
//                // 播放失败时，继续处理下一个
//                processNextInQueue()
//            }
//        }
//    }

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
            speechSynthesizer!!.stopSpeaking()
        } catch (e: Exception) {
            e.printStackTrace()
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
            speechSynthesizer!!.pauseSpeaking()
        } catch (e: Exception) {
            e.printStackTrace()
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
            speechSynthesizer!!.resumeSpeaking()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "恢复TTS失败", e)
        }
    }

    /**
     * 切换语音
     */
    fun changeVoice(voiceName: String,voiceSpeed : Double) {
        if (!isInit()) {
            return
        }
        try {
            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_VCN_KEY,voiceName)

            speechSynthesizer!!.setParameter(SynthesizerConstant.TTS_SPEED_KEY,(DEFAULT_V_SPEED * voiceSpeed).toString()  )

            Log.d(TAG, "切换语音成功 :$voiceName")
        } catch (e: Exception) {
            e.printStackTrace()
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
            speechSynthesizer!!.release()
        } catch (e: Exception) {
            e.printStackTrace()
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
            return isInit
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "检查TTS初始化状态失败", e)
            false
        }
    }

    // 添加TTS状态监听器
    interface TTSStatusListener {
        fun onTTSStartAnalyze()      // TTS开始分析
        fun onTTSStartPlay()         // TTS开始播放
        fun onTTSPlayComplete()      // TTS播放完成
        fun onTTSQueueEmpty()        // TTS队列为空
        fun onTTSError(error: String) // TTS出错
    }

    private var ttsStatusListener: TTSStatusListener? = null

    /**
     * 设置TTS状态监听器
     */
    fun setTTSStatusListener(listener: TTSStatusListener?) {
        this.ttsStatusListener = listener
        Log.d(TAG, "TTS状态监听器已设置: ${listener != null}")
    }
}