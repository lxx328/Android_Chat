package com.dexter.little_smart_chat.utils

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.dexter.little_smart_chat.utils.yzs.TTSDropletDialogManager
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

/**
 * 渐变式流式打字机管理器
 * 集成TypewriterAnimator的加速算法
 */
class StreamTypewriterManager(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "StreamTypewriterManager"

        // 打字速度配置（来自TypewriterAnimator）
        private const val INITIAL_DELAY = 200L      // 初始延迟（毫秒）- 从150L增加到200L
        private const val FINAL_DELAY = 80L         // 最终延迟（毫秒）- 从30L增加到80L
        private const val DIRECT_OUTPUT_THRESHOLD = 60L // 直接输出阈值 - 从25L增加到60L

        private const val ACCELERATION_FACTOR = 0.95f // 加速因子 - 从0.85f增加到0.95f（减缓加速）

        // TTS水滴对话框配置
        private const val DROPLET_SHOW_ANIMATION_DURATION = 300L
        private const val DROPLET_HIDE_ANIMATION_DURATION = 250L
        private const val DROPLET_AUTO_HIDE_DELAY = 3000L // 修改为3秒后自动隐藏

        // 标点符号停顿配置（来自TypewriterAnimator）
        private val PAUSE_CHARS = mapOf(
            '。' to 400L,
            '！' to 400L,
            '？' to 400L,
            '；' to 200L,
            '，' to 100L,
            '、' to 80L,
            ':' to 150L,
            '：' to 150L,
            '\n' to 200L
        )
    }



    private val newContentQueue = ConcurrentLinkedQueue<String>()
    private var isStreamActive = false
    private var bufferJob: Job? = null
    private val fullContent = StringBuilder()

    // 打字机状态变量
    private var currentTypedLength = 0
    private var currentDelay = INITIAL_DELAY
    private var typedCharCount = 0

    // TTS水滴对话框相关变量
    private var ttsDropletContainer: FrameLayout? = null
    private var ttsContentText: TextView? = null
    private var autoHideJob: Job? = null
    private var isTTSDialogShowing = false
    private var ttsScrollView: ScrollView? = null
    private var ttsIndicator: View? = null
    private var indicatorBlinkJob: Job? = null

    // TTS内容缓存
    private val ttsContentBuffer = StringBuilder()

    private var ttsDialogManager: TTSDropletDialogManager? = null

    interface StreamTypewriterCallback {
        fun onContentUpdate(displayText: String)
        fun onStreamComplete(finalText: String)
        fun onTTSDialogShow(content: String)
        fun onTTSDialogHide()
    }

    private var callback: StreamTypewriterCallback? = null

    private var contentCompleteCheckJob: Job? = null



    /**
     * 设置TTS水滴对话框的View引用和管理器
     */
    fun setTTSDropletViews(
        container: FrameLayout?,
        contentText: TextView?,
        scrollView: ScrollView? = null,
        dialogManager: TTSDropletDialogManager? = null
    ) {
        Log.d(TAG, "设置TTS水滴对话框Views: container=${container != null}, text=${contentText != null}, scroll=${scrollView != null}")

        this.ttsDropletContainer = container
        this.ttsContentText = contentText
        this.ttsScrollView = scrollView
        this.ttsDialogManager = dialogManager

        if (container != null && contentText != null) {
            Log.d(TAG, "TTS水滴对话框Views设置成功")
        } else {
            Log.e(TAG, "TTS水滴对话框Views设置失败，某些组件为null")
        }
    }

    /**
     * 开始流式打字机 - 不再管理TTS对话框显示
     */
    fun startStream(callback: StreamTypewriterCallback) {
        Log.d(TAG, "开始渐变式流式打字机")
        Log.d(TAG, "TTS组件状态: container=${ttsDropletContainer != null}, text=${ttsContentText != null}")

        this.callback = callback
        isStreamActive = true
        fullContent.clear()
        newContentQueue.clear()

        // 重置打字机状态
        currentTypedLength = 0
        currentDelay = INITIAL_DELAY
        typedCharCount = 0

        // 不再显示TTS对话框，由TTSDropletDialogManager管理

        // 立即启动缓冲区处理
        startBufferProcessing()
    }


    /**
     * 启动内容完成检测任务
     * 定时检测打字机是否完成所有内容显示，完成后自动关闭TTS对话框
     */
    private fun startContentCompleteCheck() {
        // 取消之前的检测任务
        contentCompleteCheckJob?.cancel()

        contentCompleteCheckJob = scope.launch {
            Log.d(TAG, "启动内容完成检测任务")

            while (isStreamActive || currentTypedLength < fullContent.length) {
                delay(200) // 每200ms检测一次

                val totalContent = fullContent.toString()
                val isTypingComplete = currentTypedLength >= totalContent.length
                val isContentNotEmpty = totalContent.isNotEmpty()

                // 检测打字机是否真正完成
                if (!isStreamActive && isTypingComplete && isContentNotEmpty) {
                    Log.d(TAG, "检测到打字机已完成所有内容")

                    // 验证TTS内容同步
                    withContext(Dispatchers.Main) {
                        val currentTTSContent = ttsContentText?.text?.toString() ?: ""
                        val ttsMatches = currentTTSContent == totalContent

                        Log.d(TAG, "最终验证 - 内容长度: ${totalContent.length}, TTS同步: $ttsMatches")

                        if (ttsMatches) {
                            Log.d(TAG, "✓ 内容完全同步，启动3秒延迟关闭")
                            scheduleAutoHideTTSDialog()
                            return@withContext // 任务完成，退出检测
                        } else {
                            Log.w(TAG, "TTS内容不同步，等待下次检测")
                        }
                    }
                }
            }

            // 兜底逻辑：如果检测循环结束但还没启动关闭，强制启动
            Log.d(TAG, "内容完成检测循环结束，启动兜底关闭逻辑")
            scheduleAutoHideTTSDialog()
        }
    }
//    /**
//     * 开始流式打字机 - 立即显示TTS对话框
//     */
//    fun startStream(callback: StreamTypewriterCallback) {
//        Log.d(TAG, "开始渐变式流式打字机")
//        this.callback = callback
//        isStreamActive = true
//        fullContent.clear()
//        newContentQueue.clear()
//
//        // 重置打字机状态
//        currentTypedLength = 0
//        currentDelay = INITIAL_DELAY
//        typedCharCount = 0
//
//        // 立即显示TTS对话框
//        showTTSDropletDialog()
//
//        // 立即启动缓冲区处理
//        startBufferProcessing()
//    }

    /**
     * 结束流式 - 延迟隐藏TTS对话框
     */
    fun endStream() {
        Log.d(TAG, "结束流式打字机")

        scope.launch {
            // 等待当前字符处理完成，但不要停止isStreamActive
            delay(200)

            // 如果还有未输出的内容，直接输出
            val totalContent = fullContent.toString()
            if (currentTypedLength < totalContent.length) {
                Log.d(TAG, "直接输出剩余内容：${totalContent.length - currentTypedLength} 个字符")
                currentTypedLength = totalContent.length

                withContext(Dispatchers.Main) {
                    callback?.onContentUpdate(totalContent)
                    // 同步更新TTS对话框内容
                    if (isTTSDialogShowing) {
                        ttsContentText?.text = totalContent
                        ttsScrollView?.post {
                            ttsScrollView?.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                callback?.onStreamComplete(totalContent)
            }

            // 等待UI更新完成后再检查
            Log.i(TAG, "打字机流式完成: ${totalContent.take(50)}...")
        }
    }

//    /**
//     * 显示TTS水滴对话框
//     */
//    private fun showTTSDropletDialog() {
//        Log.d(TAG, "尝试显示TTS水滴对话框，当前状态: isTTSDialogShowing=$isTTSDialogShowing")
//
//        if (isTTSDialogShowing) {
//            Log.d(TAG, "TTS水滴对话框已经在显示中，跳过")
//            return
//        }
//
//        Log.d(TAG, "检查TTS组件: container=${ttsDropletContainer != null}, text=${ttsContentText != null}, scroll=${ttsScrollView != null}")
//
//        scope.launch(Dispatchers.Main) {
//            ttsDropletContainer?.let { container ->
//                Log.d(TAG, "开始显示TTS水滴对话框动画")
//                isTTSDialogShowing = true
//                container.visibility = View.VISIBLE
//
//                // 清空内容缓存并设置初始状态
//                ttsContentBuffer.clear()
//                ttsContentText?.text = ""
//
//                // 执行显示动画
//                container.animate()
//                    .alpha(1f)
//                    .scaleX(1f)
//                    .scaleY(1f)
//                    .setDuration(DROPLET_SHOW_ANIMATION_DURATION)
//                    .withStartAction {
//                        container.scaleX = 0.8f
//                        container.scaleY = 0.8f
//                        container.alpha = 0f
//                    }
//                    .start()
//
//                callback?.onTTSDialogShow("")
//                Log.d(TAG, "TTS水滴对话框已显示")
//            } ?: run {
//                Log.e(TAG, "TTS水滴对话框容器为null，无法显示")
//            }
//        }
//    }

    /**
     * 立即隐藏TTS对话框 - 用于新消息时快速关闭
     */
    fun hideTTSDropletDialogImmediately() {
        Log.d(TAG, "委托隐藏TTS对话框")
        ttsDialogManager?.hideDialogImmediately()
    }
    /**
     * 隐藏TTS水滴对话框
     */
    private fun hideTTSDropletDialog() {
        if (!isTTSDialogShowing) return

        scope.launch(Dispatchers.Main) {
            ttsDropletContainer?.let { container ->
                // 执行隐藏动画
                container.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(DROPLET_HIDE_ANIMATION_DURATION)
                    .withEndAction {
                        container.visibility = View.GONE
                        isTTSDialogShowing = false
                    }
                    .start()

                callback?.onTTSDialogHide()
                Log.d(TAG, "TTS水滴对话框已隐藏")
            }
        }
    }

    /**
     * 更新TTS水滴对话框内容
     */
    private fun updateTTSDropletContent(newContent: String) {
        scope.launch(Dispatchers.Main) {
            ttsContentText?.let { textView ->
                // 将新内容添加到缓冲区
                ttsContentBuffer.append(newContent)

                // 显示完整内容
                val fullContent = ttsContentBuffer.toString()
                textView.text = fullContent

                // 自动滚动到底部显示最新内容
                ttsScrollView?.post {
                    ttsScrollView?.fullScroll(View.FOCUS_DOWN)
                }

                Log.v(TAG, "TTS对话框内容已更新")
            }
        }
    }

    /**
     * 安排自动隐藏TTS对话框 - 2秒延迟
     */
    private fun scheduleAutoHideTTSDialog() {
//        // 取消之前的自动隐藏任务
//        autoHideJob?.cancel()
//
//        autoHideJob = scope.launch {
//            delay(DROPLET_AUTO_HIDE_DELAY) // 2秒延迟
//            hideTTSDropletDialogImmediately()
//        }
    }

    /**
     * 处理打字输出时同步更新TTS对话框内容
     */
    private suspend fun processTyping() {
        val totalContent = fullContent.toString()
        val totalLength = totalContent.length

        Log.d(TAG, "开始打字处理 - 总长度: $totalLength, 当前位置: $currentTypedLength, 初始延迟: $currentDelay")

        while (currentTypedLength < totalLength && isStreamActive) {
            val char = totalContent[currentTypedLength]
            currentTypedLength++
            typedCharCount++

            // 更新显示内容
            val currentText = totalContent.substring(0, currentTypedLength)
            withContext(Dispatchers.Main) {
                callback?.onContentUpdate(currentText)
                // 同步更新TTS对话框内容（不管理显示状态）
                ttsDialogManager?.updateContent(currentText)
            }

            // 计算延迟时间（渐变算法）
            val baseDelay = calculateNextDelay()
            val punctuationDelay = getPunctuationDelay(char)
            val totalDelay = baseDelay + punctuationDelay

            // 详细日志记录延迟和进度
            if (currentTypedLength % 10 == 0 || char in PAUSE_CHARS) {
                Log.v(TAG, "打字进度: $currentTypedLength/$totalLength, 当前字符: '$char', 延迟: ${baseDelay}ms + ${punctuationDelay}ms = ${totalDelay}ms")
            }

            // 修改直接输出条件 - 只有在内容很长且速度很快时才启用
            val remainingContent = totalLength - currentTypedLength
            if (currentDelay <= DIRECT_OUTPUT_THRESHOLD && remainingContent > 100) {
                Log.d(TAG, "延迟已达到阈值(${DIRECT_OUTPUT_THRESHOLD}ms)且剩余内容较多(${remainingContent}字符)，直接输出剩余内容")

                // 直接输出剩余所有内容
                withContext(Dispatchers.Main) {
                    callback?.onContentUpdate(totalContent)
                    ttsDialogManager?.updateContent(totalContent)
                }
                currentTypedLength = totalLength
                Log.d(TAG, "直接输出完成，跳出打字循环")
                break
            }

            // 延迟
            delay(totalDelay)

            // 检查是否被外部停止
            if (!isStreamActive) {
                Log.d(TAG, "检测到流式状态已停止，退出打字循环")
                break
            }
        }

        Log.d(TAG, "打字处理完成 - 最终位置: $currentTypedLength/$totalLength, 流式状态: $isStreamActive")
    }
//    private suspend fun processTyping() {
//        val totalContent = fullContent.toString()
//        val totalLength = totalContent.length
//
//        while (currentTypedLength < totalLength && isStreamActive) {
//            val char = totalContent[currentTypedLength]
//            currentTypedLength++
//            typedCharCount++
//
//            // 更新显示内容
//            val currentText = totalContent.substring(0, currentTypedLength)
//            withContext(Dispatchers.Main) {
//                callback?.onContentUpdate(currentText)
//                // 同步更新TTS对话框内容
//                if (isTTSDialogShowing) {
//                    ttsContentText?.text = currentText
//                    ttsScrollView?.post {
//                        ttsScrollView?.fullScroll(View.FOCUS_DOWN)
//                    }
//                }
//            }
//
//            // 检查是否需要直接输出剩余内容
//            if (currentDelay <= DIRECT_OUTPUT_THRESHOLD) {
//                Log.d(TAG, "速度已达到阈值(${DIRECT_OUTPUT_THRESHOLD}ms)，直接输出剩余内容")
//
//                // 直接输出剩余所有内容
//                withContext(Dispatchers.Main) {
//                    callback?.onContentUpdate(totalContent)
//                    if (isTTSDialogShowing) {
//                        ttsContentText?.text = totalContent
//                        ttsScrollView?.post {
//                            ttsScrollView?.fullScroll(View.FOCUS_DOWN)
//                        }
//                    }
//                }
//                currentTypedLength = totalLength
//                break
//            }
//
//            // 计算延迟时间（渐变算法）
//            val baseDelay = calculateNextDelay()
//            val punctuationDelay = getPunctuationDelay(char)
//            val totalDelay = baseDelay + punctuationDelay
//
//            // 延迟
//            delay(totalDelay)
//        }
//    }


    /**
     * 添加流式内容
     */
    fun addStreamContent(newContent: String) {
        if (!isStreamActive) {
            Log.e(TAG, "请先调用 startStream() 方法")
            return
        }

        if (newContent.isNotEmpty()) {
            newContentQueue.offer(newContent)
        }
    }


    /**
     * 停止打字机
     */

    fun stop() {
        Log.d(TAG, "停止打字机")
        isStreamActive = false
        bufferJob?.cancel()
        contentCompleteCheckJob?.cancel() // 取消内容完成检测
        newContentQueue.clear()

        // 不再管理TTS对话框隐藏，由TTSDropletDialogManager管理
    }

    /**
     * 立即完成
     */
    fun completeImmediately() {
        val finalText = fullContent.toString()
        stop()
        scope.launch {
            withContext(Dispatchers.Main) {
                callback?.onContentUpdate(finalText)
                callback?.onStreamComplete(finalText)
            }

            // 立即完成时也安排3秒后隐藏
            scheduleAutoHideTTSDialog()
        }
        Log.i(TAG, "立即完成打字机: ${finalText.take(50)}...")
    }

    /**
     * 开始指示器闪烁动画
     */
    private fun startIndicatorBlink() {
        stopIndicatorBlink()

        indicatorBlinkJob = scope.launch(Dispatchers.Main) {
            while (isTTSDialogShowing && isActive) {
                ttsIndicator?.animate()
                    ?.alpha(0.2f)
                    ?.setDuration(600)
                    ?.withEndAction {
                        ttsIndicator?.animate()
                            ?.alpha(0.8f)
                            ?.setDuration(600)
                            ?.start()
                    }
                    ?.start()

                delay(1200) // 总周期1.2秒
            }
        }
    }


    /**
     * 停止指示器闪烁动画
     */
    private fun stopIndicatorBlink() {
        indicatorBlinkJob?.cancel()
        indicatorBlinkJob = null

        // 恢复指示器透明度
        scope.launch(Dispatchers.Main) {
            ttsIndicator?.alpha = 0.8f
        }
    }

//    /**
//     * 启动TTS对话框的逐字输出
//     */
//    fun startTTSDisplay(initialContent: String = "") {
//        if (!isTTSDialogShowing) {
//            showTTSDropletDialog()
//        }
//
//        if (initialContent.isNotEmpty()) {
//            updateTTSDropletContent(initialContent)
//        }
//    }

    /**
     * 添加TTS内容进行逐字输出
     */
    fun addTTSContent(content: String) {
        if (isTTSDialogShowing && content.isNotEmpty()) {
            // 逐字符添加到TTS对话框
            scope.launch {
                for (char in content) {
                    if (!isTTSDialogShowing) break

                    updateTTSDropletContent(char.toString())
                    delay(50) // 每个字符50ms间隔
                }
            }
        }
    }

    /**
     * 结束TTS显示
     */
    fun endTTSDisplay() {
        scheduleAutoHideTTSDialog()
    }



    /**
     * 启动缓冲区处理
     */
    private fun startBufferProcessing() {
        Log.d(TAG, "启动缓冲区处理")

        bufferJob = scope.launch {
            Log.d(TAG, "缓冲区处理协程开始运行")

            try {
                while (isStreamActive && isActive) {
                    val content = newContentQueue.poll()
                    if (content != null) {
                        Log.d(TAG, "处理队列内容: '$content'")
                        appendContentAndProcess(content)
                    } else {
                        delay(30) // 队列为空时等待
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "缓冲区处理协程被取消")
            } catch (e: Exception) {
                Log.e(TAG, "缓冲区处理出错: ${e.message}")
            } finally {
                Log.d(TAG, "缓冲区处理协程结束")
            }
        }

        Log.d(TAG, "缓冲区处理协程已启动: ${bufferJob?.isActive}")
    }

    /**
     * 添加内容并开始打字处理
     */
    private suspend fun appendContentAndProcess(content: String) {
        // 添加新内容到缓冲区
        //过滤文本的markdown文本todo
        val filteredContent = GreetingUtils.removeMarkdownFormatting(content)
        fullContent.append(filteredContent)
        // 开始打字处理
        processTyping()
    }


    /**
     * 计算下次延迟时间（使用TypewriterAnimator的渐变算法）
     */
    private fun calculateNextDelay(): Long {
        // 方法1：使用指数衰减（类似TypewriterAnimator）
        val exponentialDecay = (currentDelay * ACCELERATION_FACTOR).toLong()
        currentDelay = max(exponentialDecay, FINAL_DELAY)

        Log.v(TAG, "当前延迟: ${currentDelay}ms, 已输出字符数: $typedCharCount")
        return currentDelay
    }

    /**
     * 获取标点符号的额外停顿时间（来自TypewriterAnimator）
     */
    private fun getPunctuationDelay(char: Char): Long {
        val delay = PAUSE_CHARS[char] ?: 0L
        if (delay > 0) {
            Log.v(TAG, "标点符号 '$char' 额外停顿: ${delay}ms")
        }
        return delay
    }

    /**
     * 获取当前状态信息
     */
    fun getStatusInfo(): String {
        return "队列长度: ${newContentQueue.size}, 总内容: ${fullContent.length}, 已输出: $currentTypedLength, 当前延迟: ${currentDelay}ms"
    }


}