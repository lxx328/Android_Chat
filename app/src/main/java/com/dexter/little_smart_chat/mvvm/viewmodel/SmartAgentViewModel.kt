package com.dexter.little_smart_chat.mvvm.viewmodel

import ChatMessage
import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dexter.little_smart_chat.BuildConfig
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.data.ResponseMode
import com.dexter.little_smart_chat.mvvm.model.SmartAgentModel
import com.dexter.little_smart_chat.mvvm.model.SmartAgentModel.Companion.END_FLAG
import com.dexter.little_smart_chat.mvvm.model.SmartAgentModel.Companion.ERROR_FLAG
import com.dexter.little_smart_chat.network.model.LocalApiRequest
import com.dexter.little_smart_chat.network.model.agentName
import com.dexter.little_smart_chat.utils.GreetingUtils
import com.dexter.little_smart_chat.utils.OPUtils
import com.dexter.little_smart_chat.utils.StreamTypewriterManager
import com.dexter.little_smart_chat.utils.yzs.TTSDropletDialogManager
import com.dexter.little_smart_chat.utils.yzs.YZSOnlineTTSUtils
import com.unisound.cloud.RecognizerConstant
import com.unisound.cloud.RecognizerEvent
import com.unisound.cloud.RecognizerListener
import com.unisound.cloud.SpeechRecognizer
import com.unisound.cloud.SpeechUtility
import com.unisound.demo.util.loadConfigureFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Collections

class SmartAgentViewModel (application: Application) : AndroidViewModel(application) {

        init {
            // 初始化YZSASR
            initYZSAsrOnline()

            initStreamTypewriterEarly()

        }


        private val model = SmartAgentModel(application.applicationContext)

        // 使用线程安全的集合
        private val _messages = MutableStateFlow<List<ChatMessage>>(Collections.emptyList())
        val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

        // 加载状态
        private val _loading = MutableStateFlow(false)
        val loading: StateFlow<Boolean> = _loading.asStateFlow()

        // 错误状态
        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        private val _recordText = MutableStateFlow<String>("")
        val recordText: StateFlow<String> = _recordText.asStateFlow()

        var agentName: String = com.dexter.little_smart_chat.network.model.agentName.ModelXLoop.value


        //创建一个线程安全的队列


        // 当前请求Job，用于取消
        private var currentRequestJob: Job? = null

        //yzsASR
        private var yzsASROnline: SpeechRecognizer? = null

        //yzsTTS
//        private val yzsTTS: YZSTTSUtils = YZSTTSUtils
        private val yzsTTS: YZSOnlineTTSUtils = YZSOnlineTTSUtils

        //流式的识别stringBuffer
        private val _recognizerBuffer = MutableStateFlow( "")
        val recognizerBuffer: StateFlow<String> = _recognizerBuffer.asStateFlow()

        // 添加流式响应控制
        private var streamingJob: Job? = null
        private var currentMessageId: String? = null

        // 流式内容缓存和TTS优化
        private var lastStreamedContent: String = ""
        private val ttsBuffer = StringBuilder() // TTS缓冲区
        private var lastTtsLength = 0 // 上次TTS的长度

        @Volatile
        private var isRecognizerWork = false

        // 添加打字机管理器
        private var streamTypewriter: StreamTypewriterManager? = null
        private var currentTypingMessageIndex = -1


        // 添加TTS对话框管理器
        private var ttsDropletDialogManager: TTSDropletDialogManager? = null

        companion object {
            val key = BuildConfig.llm
            private const val MAX_MESSAGE_COUNT = 100 // 限制消息数量防止内存溢出
            private const val TAG = "AiChatViewModel"

            private const val TTS_CHUNK_SIZE = 15 // TTS分块大小（字符数）
            private const val TTS_PUNCTUATION = "。！？；\n" // 标点符号

        }

    /**
     * 设置TTS水滴对话框的View引用
     */
    fun setTTSDropletViews(
        container: FrameLayout?,
        contentText: TextView?,
        scrollView: ScrollView? = null
    ) {
        OPUtils.Logger.d(TAG, "ViewModel接收TTS水滴对话框Views:")
        OPUtils.Logger.d(TAG, "  container=${container != null} (${container?.javaClass?.simpleName})")
        OPUtils.Logger.d(TAG, "  contentText=${contentText != null} (${contentText?.javaClass?.simpleName})")
        OPUtils.Logger.d(TAG, "  scrollView=${scrollView != null} (${scrollView?.javaClass?.simpleName})")
        OPUtils.Logger.d(TAG, "  streamTypewriter已初始化: ${streamTypewriter != null}")

        if (container == null) {
            OPUtils.Logger.e(TAG, "❌ TTS水滴对话框容器为null，无法设置到StreamTypewriter")
            return
        }

        // 初始化TTS对话框管理器
        if (ttsDropletDialogManager == null) {
            ttsDropletDialogManager = TTSDropletDialogManager(viewModelScope)
            OPUtils.Logger.d(TAG, "✅ TTS对话框管理器已创建")
        }

        // 设置组件到管理器
        ttsDropletDialogManager?.setTTSDropletViews(container, contentText, scrollView)

        // 设置TTS状态监听器
        YZSOnlineTTSUtils.setTTSStatusListener(object : YZSOnlineTTSUtils.TTSStatusListener {
            override fun onTTSStartAnalyze() {
                OPUtils.Logger.d(TAG, "🎵 TTS开始分析，显示对话框")
                ttsDropletDialogManager?.onTTSStartAnalyze()
            }

            override fun onTTSStartPlay() {
                OPUtils.Logger.d(TAG, "🎵 TTS开始播放")
                ttsDropletDialogManager?.onTTSStartPlay()
            }

            override fun onTTSPlayComplete() {
                OPUtils.Logger.d(TAG, "🎵 TTS播放完成")
                ttsDropletDialogManager?.onTTSPlayComplete()
            }

            override fun onTTSQueueEmpty() {
                OPUtils.Logger.d(TAG, "🎵 TTS队列为空，准备隐藏对话框")
                ttsDropletDialogManager?.onTTSQueueEmpty()
            }

            override fun onTTSError(error: String) {
                OPUtils.Logger.e(TAG, "🎵 TTS出错，隐藏对话框: $error")
                ttsDropletDialogManager?.onTTSError(error)
            }
        })

        if (streamTypewriter == null) {
            OPUtils.Logger.w(TAG, "⚠️ StreamTypewriter未初始化，先进行初始化")
            initStreamTypewriterEarly()
        }

        streamTypewriter?.setTTSDropletViews(container, contentText, scrollView, ttsDropletDialogManager)
        OPUtils.Logger.d(TAG, "✅ TTS水滴对话框Views已传递给StreamTypewriter")
    }


    /**
     * 提前初始化流式打字机 - 不启动Stream
     */
    private fun initStreamTypewriterEarly() {
        OPUtils.Logger.d(TAG, "提前初始化StreamTypewriter")

        if (streamTypewriter == null) {
            streamTypewriter = StreamTypewriterManager(viewModelScope)
            OPUtils.Logger.d(TAG, "StreamTypewriter已创建，等待设置TTS组件和启动")
        }
    }
        /**
         * 发送消息 - 增强错误处理和取消支持
         */
        fun sendMessage(agentName:String,message: String, mode: ResponseMode, mapHeaders: Map<String, Any> = emptyMap()) {
            // 输入验证
            if (message.isBlank()) {
                _error.value = "消息不能为空"
                return
            }

            if (message.length > 2000) {
                _error.value = "消息长度不能超过2000字符"
                return
            }

            // 取消之前的请求
            cancelCurrentRequest()
            var request: LocalApiRequest? = null
            when (agentName) {
                com.dexter.little_smart_chat.network.model.agentName.ModelXLoop.value -> {
                    OPUtils.Logger.d(TAG, "发送消息 - 模型XLoop")
                    request = LocalApiRequest(
                        agentName = agentName,
                        message = message,
                        contextId = MyApplication.instance?.getAi_conversion_id(),
                        snId = OPUtils.getSN(MyApplication.instance?.getAppContext()),
                        acceptedOutputModes = listOf("text/plain", "video/mp4"),
                        customHeaders = mapHeaders,
                        agentId = "agent01",
                        agentType = "xloop",
                        userId = "yiqi.zhang"
                    )
                }

                else -> {
                    OPUtils.Logger.d(TAG, "发送消息 - 模型OTHER")
                    request = LocalApiRequest(
                        agentName = agentName,
                        message = message,
                        contextId = MyApplication.instance?.getAi_conversion_id(),
                        snId = OPUtils.getSN(MyApplication.instance?.getAppContext()),
                        acceptedOutputModes = listOf("text/plain", "video/mp4"),
                        customHeaders = mapHeaders,
                    )
                }
            }

            //打印request
            OPUtils.Logger.d("Dx++", "发送消息 - request: $request")


            // 生成新的消息ID
            currentMessageId = System.currentTimeMillis().toString()

            _loading.value = true
            _error.value = null

            currentRequestJob = viewModelScope.launch {
                try {
                    when (mode) {
                        is ResponseMode.Block -> handleBlockResponse(request)
                        is ResponseMode.Stream -> handleStreamResponse(request)
                        else -> {
                            _error.value = "不支持的响应模式"
                            _loading.value = false
                        }
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        /**
         * 处理阻塞式响应
         */
        private suspend fun handleBlockResponse(request: LocalApiRequest) {
            val response = model.sendMessageToModel(key, request)
            _loading.value = false

            when (response.code) {
                200 -> {
                    response.data?.let { content ->
                        val aiMsg = ChatMessage.Text(content, left = true)
                        addMessage(aiMsg)
                    } ?: run {
                        _error.value = "AI回复内容为空"
                    }
                }
                408 -> _error.value = response.message
                else -> _error.value = "AI回复失败: ${response.message}"
            }
        }

        /**
         * 处理流式响应
         */
        private suspend fun handleStreamResponse(request: LocalApiRequest) {
            val messageId = currentMessageId

            // 先插入一条空AI消息
            val aiMsg = ChatMessage.Markdown("", left = true)
            addMessage(aiMsg)
            // 获取当前消息索引 - 这里是关键修复
            currentTypingMessageIndex = _messages.value.size - 1

            // 重新初始化打字机
            initStreamTypewriter()

            lastStreamedContent = ""
            ttsBuffer.clear()
            lastTtsLength = 0
            streamingJob = viewModelScope.launch {
                try {
                    model.sendMessageStreamAsync( key, request)
                        .buffer(64)
                        .catch { e ->
                            if (messageId == currentMessageId) { // 只处理当前消息的错误
                                _loading.value = false
                                _error.value = "流式响应错误: ${e.message}"
                            }
                        }
                        .collect { partialContent ->
                            // 检查是否仍然是当前消息
                            if (messageId == currentMessageId) {
                                if (END_FLAG == partialContent) {
                                    handleStreamEnd()
                                } else if (ERROR_FLAG == partialContent) {
                                    updateStreamMessage("${MyApplication.modeName}脑袋有点转不过来了，请您重写说一遍")
                                    handleStreamEnd()
                                } else if ("网络连接中断" == partialContent || "请求超时，请稍后重试" == partialContent
                                    || partialContent.startsWith("发生错误") || partialContent.startsWith(
                                        "请求失败"
                                    )
                                    || "响应数据为空" == partialContent
                                )
                                {
                                    updateStreamMessage("${MyApplication.modeName}脑袋暂未开机，请稍后重试")
                                    handleStreamEnd()
                                } else {
                                    updateStreamMessage(partialContent)
                                }
                            }
                        }
                } catch (e: Exception) {
                    if (messageId == currentMessageId) {
                        handleError(e)
                    }
                }
            }

        }

        /**
         * 更新流式消息
         */
        private fun updateStreamMessage(partialContent: String) {
            if (currentMessageId == null) return

            val current = _messages.value
            if (current.isNotEmpty() && current.last() is ChatMessage.Markdown) {
                val lastMsg = current.last() as ChatMessage.Markdown

                // 计算新增内容
                val newPart = if (partialContent.startsWith(lastStreamedContent)) {
                    partialContent.removePrefix(lastStreamedContent)
                } else {
                    partialContent
                }
                if (newPart.isNotEmpty()) {
                    lastStreamedContent = partialContent
                    ttsBuffer.append(newPart)
                    // 智能TTS分块处理
                    processTTSBuffer()
                    // 添加到打字机
                    // 方案1：尝试使用打字机
                    try {
                        streamTypewriter?.addStreamContent(newPart)
                        OPUtils.Logger.d(TAG, "添加到打字机: '$newPart'")
                    } catch (e: Exception) {
                        OPUtils.Logger.e(TAG, "打字机添加内容失败: ${e.message}")
                    }

//                // 方案2：直接更新UI（确保内容显示）
//                val updatedMsg = lastMsg.copy(markdownContent = partialContent)
//                val newList = current.dropLast(1) + updatedMsg
//                _messages.value = newList

                }

            }
        }

        /**
         * 一个模仿大模型回复的开场白方法
         * 包含TTS播报和打字机效果，支持图片显示
         */
        fun firstOpeningRemarks(userName: String = "承智灵") {
            // 如果正在加载中，取消当前请求
            if (_loading.value) {
                cancelCurrentRequest()
            }

            // 获取随机开场白（包含图片）
            val greetingMessage = GreetingUtils.getTimeBasedGreetingLLM(userName)

            // 生成新的消息ID
            currentMessageId = System.currentTimeMillis().toString()

            _loading.value = true
            _error.value = null

            // 先插入一条空AI消息
            val aiMsg = ChatMessage.Markdown("", left = true)
            addMessage(aiMsg)
            // 获取当前消息索引
            currentTypingMessageIndex = _messages.value.size - 1

            // 重新初始化打字机
            initStreamTypewriter()

            lastStreamedContent = ""
            ttsBuffer.clear()
            lastTtsLength = 0

            // 启动模拟流式响应
            streamingJob = viewModelScope.launch {
                try {
                    simulateStreamingResponse(greetingMessage)
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }



        /**
         * 模拟流式响应，逐字符显示开场白
         */
        private suspend fun simulateStreamingResponse(fullMessage: String) {
            val messageId = currentMessageId

            // 模拟打字速度，每个字符间隔50-150ms
            var accumulatedText = ""

            for (i in fullMessage.indices) {
                // 检查是否仍然是当前消息
                if (messageId != currentMessageId) {
                    OPUtils.Logger.d(TAG, "开场白被中断")
                    return
                }

                // 逐字符添加
                accumulatedText += fullMessage[i]

                // 更新流式消息
                updateStreamMessage(accumulatedText)

            }

            // 完成流式响应
            handleStreamEnd()
        }


        /**
         * 智能TTS缓冲区处理
         */
        private fun processTTSBuffer() {
            val bufferContent = ttsBuffer.toString()
            val currentLength = bufferContent.length

            // 检查是否有新内容需要TTS
            if (currentLength > lastTtsLength) {
                val newContent = bufferContent.substring(lastTtsLength)

                // 策略1: 遇到标点符号时播放
                //  策略2: 遇到存在(android.resource://...的字段替换成如如图
                val lastPunctuationIndex = bufferContent.lastIndexOfAny(TTS_PUNCTUATION.toCharArray())
                if (lastPunctuationIndex > lastTtsLength) {
                    val ttsContent = bufferContent.substring(lastTtsLength, lastPunctuationIndex + 1)

                    // 处理图片标记
                    val ttsContentNew = GreetingUtils.processTTSText(ttsContent)

                    playTTS(ttsContentNew.trim())
                    lastTtsLength = lastPunctuationIndex + 1
                    return
                }

                // 策略2: 缓冲区达到一定长度时播放
//            if (newContent.length >= TTS_CHUNK_SIZE) {
//                // 尝试在词边界分割
//                val breakPoint = findWordBoundary(newContent, TTS_CHUNK_SIZE)
//                if (breakPoint > 0) {
//                    val ttsContent = bufferContent.substring(lastTtsLength, lastTtsLength + breakPoint)
//                    playTTS(ttsContent.trim())
//                    lastTtsLength += breakPoint
//                }
//            }

                // 策略3: 检查图片标记完整性
//            val newContent = bufferContent.substring(lastTtsLength)
//            val imageRegex = Regex("!\\[[^\\]]*\\]\\([^)]*\\)")
//            val imageMatch = imageRegex.find(newContent)
//
//            if (imageMatch != null && imageMatch.range.last < newContent.length - 1) {
//                // 图片标记完整，可以播放到图片结束位置
//                val endIndex = lastTtsLength + imageMatch.range.last + 1
//                val ttsContent = bufferContent.substring(lastTtsLength, endIndex)
//                playTTS(ttsContent.trim())
//                lastTtsLength = endIndex
//                return
//            }
            }

            // 在流式结束时播放剩余内容
            if (!_loading.value && lastTtsLength < currentLength) {
                val remainingContent = bufferContent.substring(lastTtsLength).trim()
                if (remainingContent.isNotEmpty()) {
                    // 处理图片标记
                    val ttsContentNew = GreetingUtils.processTTSText(remainingContent)

                    playTTS(ttsContentNew)
                    lastTtsLength = currentLength
                }
            }
        }

        /**
         * 寻找词边界
         */
        private fun findWordBoundary(text: String, preferredLength: Int): Int {
            if (text.length <= preferredLength) return text.length

            // 在首选长度附近寻找空格或标点
            for (i in preferredLength downTo preferredLength / 2) {
                if (i < text.length && (text[i].isWhitespace() || TTS_PUNCTUATION.contains(text[i]))) {
                    return i + 1
                }
            }

            return preferredLength
        }

        /**
         * TTS播放 - 使用队列
         */
        private fun playTTS(text: String) {
            if (text.trim().isEmpty()) return

            try {
//            OPUtils.Logger.d(TAG, "添加到TTS队列: $text")
//            yzsTTS.enqueueText(text)
                // 处理图片标记，转换为TTS友好的文本
                val ttsText = GreetingUtils.processTTSText(text)
                OPUtils.Logger.d(TAG, "原始文本: $text")
                OPUtils.Logger.d(TAG, "TTS文本: $ttsText")

                if (ttsText.trim().isNotEmpty()) {
                    yzsTTS.enqueueText(ttsText)
                }
            } catch (e: Exception) {
                OPUtils.Logger.e(TAG, "TTS播放失败: ${e.message}")
            }
        }

        /**
         * 线程安全地添加消息
         */
        private fun addMessage(message: ChatMessage) {
            val content = when (message) {
                is ChatMessage.Text -> message.content
                is ChatMessage.Markdown -> message.markdownContent
                else -> ""
            }

            val currentList = _messages.value.toMutableList()
            currentList.add(message)

            // 限制消息数量，防止内存溢出
            if (currentList.size > MAX_MESSAGE_COUNT) {
                currentList.removeAt(0)
            }

            _messages.value = currentList.toList()
        }

    /**
     * 添加用戶输入内容
     */
    fun addUserInput(message: String) {
        // 立即隐藏TTS对话框（如果正在显示）
        streamTypewriter?.hideTTSDropletDialogImmediately()

        addMessage(ChatMessage.Text(message, left = false))

        val mapHeaders = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $key"
        )
        sendMessage(agentName,message, ResponseMode.Stream, mapHeaders)
    }

        /**
         * 处理错误
         */
        private fun handleError(e: Exception) {
            _loading.value = false
            _error.value = when (e) {
                is CancellationException -> null // 忽略取消异常
                else -> {
                    OPUtils.Logger.e(TAG, "Request failed: ${e.message}")
                    "请求失败: ${e.message}"
                }
            }
        }

    /**
     * 取消当前请求
     */
    fun cancelCurrentRequest() {
        // 立即隐藏TTS对话框
        ttsDropletDialogManager?.hideDialogImmediately()

        // 停止打字机动画
        streamTypewriter?.stop()

        // 取消当前请求
        currentRequestJob?.cancel()
        streamingJob?.cancel()

        // 停止并清空TTS
        yzsTTS.stop()

        // 重置状态
        currentRequestJob = null
        streamingJob = null
        lastStreamedContent = ""
        ttsBuffer.clear()
        lastTtsLength = 0

        // 清理空消息
        checkLastElenmentsIsEmpty()

        _loading.value = false

        OPUtils.Logger.i(TAG, "已强制中断当前会话")
    }

//        /**
//         * 初始化流式打字机
//         */
//        private fun initStreamTypewriter() {
//            OPUtils.Logger.d(TAG, "开始初始化打字机")
//
//            streamTypewriter?.stop()
//
//            streamTypewriter = StreamTypewriterManager(viewModelScope).apply {
//                startStream(object : StreamTypewriterManager.StreamTypewriterCallback {
//                    override fun onContentUpdate(displayText: String) {
//                        if (currentTypingMessageIndex >= 0) {
//                            updateMessageAtIndex(currentTypingMessageIndex, displayText)
//                            OPUtils.Logger.v(TAG, "打字机更新内容: ${displayText.take(20)}...")
//                        } else {
//                            OPUtils.Logger.w(TAG, "打字机更新失败，索引无效: $currentTypingMessageIndex")
//                        }
//                    }
//
//                    override fun onStreamComplete(finalText: String) {
//                        if (currentTypingMessageIndex >= 0) {
//                            updateMessageAtIndex(currentTypingMessageIndex, finalText)
//                            OPUtils.Logger.i(TAG, "打字机动画完成: ${finalText.take(50)}...")
//                        } else {
//                            OPUtils.Logger.w(TAG, "打字机完成失败，索引无效: $currentTypingMessageIndex")
//                        }
//                    }
//
//                    override fun onTTSDialogShow(content: String) {
//                        OPUtils.Logger.d(TAG, "TTS对话框显示: $content")
//                    }
//
//                    override fun onTTSDialogHide() {
//                        OPUtils.Logger.d(TAG, "TTS对话框隐藏")
//                    }
//                })
//            }
//
//            OPUtils.Logger.d(TAG, "打字机初始化完成，目标消息索引: $currentTypingMessageIndex")
//        }

    /**
     * 初始化流式打字机 - 启动Stream
     */
    private fun initStreamTypewriter() {
        OPUtils.Logger.d(TAG, "开始初始化打字机流")

        // 如果还未创建，先创建
        if (streamTypewriter == null) {
            initStreamTypewriterEarly()
        }

        // 停止之前的流
        streamTypewriter?.stop()

        // 启动新的流
        streamTypewriter?.startStream(object : StreamTypewriterManager.StreamTypewriterCallback {
            override fun onContentUpdate(displayText: String) {
                if (currentTypingMessageIndex >= 0) {
                    updateMessageAtIndex(currentTypingMessageIndex, displayText)
                    OPUtils.Logger.v(TAG, "打字机更新内容: ${displayText.take(20)}...")
                } else {
                    OPUtils.Logger.w(TAG, "打字机更新失败，索引无效: $currentTypingMessageIndex")
                }
            }

            override fun onStreamComplete(finalText: String) {
                if (currentTypingMessageIndex >= 0) {
                    updateMessageAtIndex(currentTypingMessageIndex, finalText)
                    OPUtils.Logger.i(TAG, "打字机动画完成: ${finalText.take(50)}...")
                } else {
                    OPUtils.Logger.w(TAG, "打字机完成失败，索引无效: $currentTypingMessageIndex")
                }
            }

            override fun onTTSDialogShow(content: String) {
                OPUtils.Logger.d(TAG, "✅ TTS对话框显示回调: $content")
            }

            override fun onTTSDialogHide() {
                OPUtils.Logger.d(TAG, "✅ TTS对话框隐藏回调")
            }
        })

        OPUtils.Logger.d(TAG, "打字机初始化完成，目标消息索引: $currentTypingMessageIndex")
    }

        /**
         * 处理流式结束
         */
        private fun handleStreamEnd() {
            OPUtils.Logger.i(TAG, "流式响应结束}")

//        // 结束打字机流
            streamTypewriter?.endStream()

            // 设置加载状态为false
            _loading.value = false

            //最后再调用一次放置存在未处理的TTS内容
            processTTSBuffer()
            OPUtils.Logger.i(TAG, "流式响应处理完成")
        }

        /**
         * 更新指定索引的消息
         */
        private fun updateMessageAtIndex(index: Int, content: String) {
            if (index < 0) {
                OPUtils.Logger.w(TAG, "无效的消息索引: $index")
                return
            }

            val currentList = _messages.value.toMutableList()
            if (index < currentList.size && currentList[index] is ChatMessage.Markdown) {
                val updatedMsg = (currentList[index] as ChatMessage.Markdown).copy(
                    markdownContent = content
                )
                currentList[index] = updatedMsg
                _messages.value = currentList.toList()
                OPUtils.Logger.v(TAG, "更新消息索引 $index: ${content.take(50)}...")
            } else {
                OPUtils.Logger.w(TAG, "无法更新消息索引 $index, 列表大小: ${currentList.size}")
            }
        }


        private fun checkLastElenmentsIsEmpty(){
            //如果最后一条消息是空的则删除
            if (messages.value.isNotEmpty() ) {
                //不管是markdown类型还是 text类型
                if (messages.value.last() is ChatMessage.Text || messages.value.last() is ChatMessage.Markdown) {
                    when (val lastMsg = messages.value.last()) {
                        is ChatMessage.Text -> {
                            if (lastMsg.content.isEmpty()) {
                                _messages.value = messages.value.dropLast(1)
                            }
                        }
                        is ChatMessage.Markdown -> {
                            if (lastMsg.markdownContent.isEmpty()) {
                                _messages.value = messages.value.dropLast(1)
                            }
                        }
                        else -> {
                            //do nothing
                        }
                    }

                }
            }
        }
        /**
         * 清除错误状态
         */
        fun clearError() {
            _error.value = null
        }

        /**
         * 恢复历史消息 - 增加验证
         */
        fun restoreMessages(list: List<ChatMessage>) {
            if (list.size > MAX_MESSAGE_COUNT) {
                OPUtils.Logger.w(TAG, "Message list too large, truncating")
                _messages.value = list.takeLast(MAX_MESSAGE_COUNT)
            } else {
                _messages.value = list.toList() // 创建副本避免外部修改
            }
        }

        /**
         * 清空聊天记录
         */
        fun clearMessages() {
            cancelCurrentRequest()
            _messages.value = emptyList()
            lastStreamedContent = ""
        }

        fun yzsRelease() {
            //释放资源
            if (yzsASROnline != null ) {
                if (isRecognizerWork){
                    stopYZSAsrOnline()
                }
                yzsASROnline!!.release()
                yzsASROnline = null
            }
        }

        /**
         * 初始化在线ASR
         */
        private fun initYZSAsrOnline() {
            // 创建语音理解对象，appKey和 secret通过 http://dev.hivoice.cn/ 网站申请
            //app
            yzsASROnline = SpeechRecognizer()
            //设置领域，默认是SpeechConstant.ASR_DOMAIN_GENER
            yzsASROnline!!.setParameter(
                RecognizerConstant.ASR_DOMAIN_KEY,
                RecognizerConstant.ASR_DOMAIN_GENERAL + "," + RecognizerConstant.ASR_DOMAIN_HOME
            )
            //设置是否使用识别
            yzsASROnline!!.setParameter(
                RecognizerConstant.ASR_INNER_RECORDER_KEY,
                RecognizerConstant.ASR_VALUE_TRUE)

            //设置领域，默认是SpeechConstant.ASR_DOMAIN_GENER
            yzsASROnline!!.setParameter(RecognizerConstant.ASR_DOMAIN_KEY, RecognizerConstant.ASR_DOMAIN_GENERAL)

            //设置是否将数字转化为阿拉伯数字
            yzsASROnline!!.setParameter(RecognizerConstant.ASR_NUM_CONVERT_KEY, RecognizerConstant.ASR_VALUE_TRUE)
            //设置语言的语种。
            yzsASROnline!!.setParameter(RecognizerConstant.ASR_LANG_KEY, RecognizerConstant.ASR_LANG_CN)
            //测试四川话
//        yzsASROnline!!.setParameter(RecognizerConstant.ASR_LANG_KEY, RecognizerConstant.ASR_LANG_SICHUANESE)
            //设置采样率
            yzsASROnline!!.setParameter(RecognizerConstant.ASR_SAMPLE_KEY, RecognizerConstant.ASR_SAMPLE_16K)

            //关闭主动结束识别

            //设置是否开启云端vad 。默认是开启 ,不会自动结束
            yzsASROnline!!.setParameter(RecognizerConstant.ASR_SERVER_VAD_KEY, RecognizerConstant.ASR_VALUE_FALSE);

            printYZSOnlineParam()

            loadConfig(yzsASROnline!!)
        }

        fun startYZSAsrOnline() {
            isRecognizerWork = true
            //开始录音
            yzsASROnline!!.startListening(object : RecognizerListener {
                override fun onVolumeChanged(volume: Int, buffer: ByteArray, len: Int) {
                    OPUtils.Logger.d(TAG,"onVolumeChanged:$volume  len :$len  " )
                }

                override fun onEvent(event: Int) {
                    if (event == RecognizerEvent.ASR_EVENT_SPEECH_START) {
                        OPUtils.Logger.d(TAG,"开始识别：onBeginOfSpeech")
                    } else if (event == RecognizerEvent.ASR_EVENT_SPEECH_END) {
                        OPUtils.Logger.d(TAG,"结束识别：onEndOfSpeech")
                    }
                }

                override fun onResult(p0: String?, p1: Boolean, p2: Boolean) {
                    val msg = "识别结果:$p0,是否识别结束:${if(p1){
                        "是"
                    }else{
                        "否"
                    }
                    }"
                    OPUtils.Logger.d(TAG,"识别结果： $msg")
                    if (p1) {
                        if (p0?.isNotEmpty() == true){
                            addUserInput(p0)

                        }else{
                            _error.value = "未识别到内容"
                        }
                    }
                    _recordText.value  = p0!!.ifEmpty {
                        "未识别到内容"
                    }
                    isRecognizerWork = false                }

//                override fun onResult(result: String, finished: Boolean) {
//                    val msg = "识别结果:$result,是否识别结束:${if(finished){
//                        "是"
//                    }else{
//                        "否"
//                    }
//                    }"
//                    OPUtils.Logger.d(TAG,"识别结果： $msg")
//                    if (finished) {
//                        if (result.isNotEmpty()){
//                            addUserInput(result)
//
//                        }else{
//                            _error.value = "未识别到内容"
//                        }
//                    }
//                    _recordText.value  = result.ifEmpty {
//                        "未识别到内容"
//                    }
//                    isRecognizerWork = false
//
//                }

                override fun onError(errorCode: Int, msg: String) {
                    OPUtils.Logger.d(TAG,"onError:$errorCode,msg:$msg")
                    // 显示错误信息
                    _error.value = "识别错误: $msg"
                    isRecognizerWork = false
                }

                override fun onFinished() {
                }
            })
        }

        fun stopYZSAsrOnline() {
            //停止录音
            yzsASROnline!!.stopListening()
        }

        fun cancelYZSAsrOnline() {
            //取消录音
            yzsASROnline!!.cancel()

            //必须停止不然因为标识位问题第二次录制会不生效
            yzsASROnline!!.stopListening()
        }

        /**
         * 打印在线ASR参数
         */
        private fun printYZSOnlineParam() {
            val print = StringBuilder()
            print.append("APP_KEY:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_APP_KEY)).append("\n")
            print.append("APP_SECRET:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_APP_SECRET))
                .append("\n")
            print.append("ASR_SERVER:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_SERVER_KEY))
                .append("\n")
            print.append("DOMAIN:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_DOMAIN_KEY)).append("\n")
            print.append("LANG:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_LANG_KEY)).append("\n")
            print.append("FORMAT:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_FORMAT_KEY)).append("\n")
            print.append("SAMPLE:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_SAMPLE_KEY)).append("\n")
            print.append("VARIBLE:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_VARIABLE_KEY))
                .append("\n")
            print.append("PUNCTUATION:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_PUNCTUATION_KEY))
                .append("\n")
            print.append("NUM_CONVERT:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_NUM_CONVERT_KEY))
                .append("\n")
            print.append("NEAR:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_NEAR_KEY)).append("\n")
            print.append("USER_ID:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_USER_ID_KEY))
                .append("\n")
            print.append("VAD:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_SERVER_VAD_KEY)).append("\n")
            print.append("VAD START:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_VAD_START_SILENCE_KEY))
                .append("\n")
            print.append("VAD END:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_VAD_END_SILENCE_KEY))
                .append("\n")
            print.append("COMPRESS:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_AUDIO_COMPRESS_KEY))
                .append("\n")
            print.append("INNER_RECORDER:").append(yzsASROnline!!.getParameter(RecognizerConstant.ASR_INNER_RECORDER_KEY))
                .append("\n")

            OPUtils.Logger.d(TAG,"在线ASR参数：$print")
        }

        /**
         * 加载配置文件的内容。在返回结果
         */
        private fun loadConfig(speechRecognizer: SpeechRecognizer): HashMap<String, String> {
            val map = loadConfigureFile()
            //设置param
            for (value in map.entries) {
                if (value.key.startsWith("asr_") && value.key.endsWith("_key")) {
                    speechRecognizer.setParameter(value.key, value.value)
                }
            }
            //设置日志等级
            val value = map["log_level"]
            value?.let{
                val intLevel = it.toIntOrNull()?:2
                SpeechUtility.setLogLevel(intLevel)
            }
            return map
        }


        /**
         * 停止TTS
         */
        fun stopTTS(){
            yzsTTS.stop()
        }

        /**
         * 暂停TTS
         */
        fun pauseTTS(){
            yzsTTS.pause()
        }

        /**
         * 恢复TTS
         */
        fun resumeTTS(){
            yzsTTS.resume()
        }

        /**
         * 释放TTS
         */
        private fun yzsTTRelease(){
            yzsTTS.stop()
        }


        /**
         * 立即完成当前打字动画
         */
        fun completeTypingImmediately() {
            streamTypewriter?.completeImmediately()
        }

        /**
         * 处理图片内容，将图片标记替换为"如图"
         */
        private fun processImageContentForTTS(content: String): String {
            if (content.isEmpty()) return content

            var processedContent = content

            // 处理标准 markdown 图片语法: ![alt](file:///android_asset/...)
            val markdownImagePattern = Regex("!\\[([^\\]]*)\\]\\(file:///android_asset/[^)]*\\)")
            processedContent = markdownImagePattern.replace(processedContent) { matchResult ->
                val altText = matchResult.groupValues[1]
                when {
                    altText.isNotEmpty() -> "如图${altText}"
                    else -> "如图"
                }
            }

            // 处理旧格式的图片路径: (file:///android_asset/...)
            val imagePatterns = listOf(
                Regex("!\\[([^\\]]*)\\]\\(file:///android_asset/[^)]*\\)"),
                Regex("!\\[([^\\]]*)\\]\\(mipmap://[^)]*\\)"),
                Regex("!\\[([^\\]]*)\\]\\(drawable://[^)]*\\)"),
                Regex("!\\[([^\\]]*)\\]\\(https?://[^)]*\\)")
            )
            for (imagePattern in imagePatterns) {
                processedContent = imagePattern.replace(processedContent) { matchResult ->
                    val altText = matchResult.groupValues[1]
                    when {
                        altText.isNotEmpty() -> "如图${altText}"
                        else -> "如图"
                    }
                }
            }


            // 清理多余的空格和重复的"如图"
            processedContent = processedContent
                .replace(Regex("如图+"), "如图")
                .replace(Regex("\\s+"), " ")
                .trim()

            OPUtils.Logger.d(TAG, "图片内容处理: '$content' -> '$processedContent'")
            return processedContent
        }

    /**
     * 中断TTS播放和显示内容更新
     * 专门用于中断TTS和显示内容，不影响正在进行的网络请求
     */
    fun interruptTTSAndDisplay() {
        //todo new
        OPUtils.Logger.i(TAG, "中断TTS播放和显示内容更新")

        // 立即隐藏TTS对话框
        ttsDropletDialogManager?.hideDialogImmediately()

        // 停止打字机动画
        streamTypewriter?.stop()

        // 停止并清空TTS队列
        yzsTTS.stop()

        // 取消流式处理任务
        streamingJob?.cancel()
        streamingJob = null

        // 重置TTS相关状态
        lastStreamedContent = ""
        ttsBuffer.clear()
        lastTtsLength = 0

        // 但保持网络请求继续（如果正在进行）
        OPUtils.Logger.i(TAG, "TTS和显示内容已中断，网络请求将继续")
    }

    override fun onCleared() {
        super.onCleared()
        streamTypewriter?.stop()
        cancelCurrentRequest()
        yzsTTRelease()
        yzsRelease()
    }
}