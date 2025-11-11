package com.dexter.little_smart_chat.utils.yzs
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.*

/**
 * TTS水滴对话框管理器
 * 基于真实TTS播放状态控制显示/隐藏
 */
class TTSDropletDialogManager(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TTSDropletDialogManager"
        private const val SHOW_ANIMATION_DURATION = 300L
        private const val HIDE_ANIMATION_DURATION = 250L
        private const val AUTO_HIDE_DELAY = 2000L // 播放完成后3秒自动隐藏
    }

    // TTS对话框组件
    private var ttsDropletContainer: FrameLayout? = null
    private var ttsContentText: TextView? = null
    private var ttsScrollView: ScrollView? = null

    // 状态管理
    private var isDialogShowing = false
    private var autoHideJob: Job? = null
    private var currentContent = StringBuilder()

    /**
     * 设置TTS对话框组件
     */
    fun setTTSDropletViews(
        container: FrameLayout?,
        contentText: TextView?,
        scrollView: ScrollView? = null
    ) {
        Log.d(TAG, "设置TTS对话框组件: container=${container != null}, text=${contentText != null}")

        this.ttsDropletContainer = container
        this.ttsContentText = contentText
        this.ttsScrollView = scrollView
    }

    /**
     * TTS开始分析时显示对话框
     */
    fun onTTSStartAnalyze() {
        Log.d(TAG, "TTS开始分析，显示对话框")
        showDialog()
    }

    /**
     * TTS开始播放（可选的额外反馈）
     */
    fun onTTSStartPlay() {
        Log.d(TAG, "TTS开始播放")
        // 可以在这里添加播放状态指示
    }

    /**
     * TTS播放完成
     */
    fun onTTSPlayComplete() {
        Log.d(TAG, "TTS播放完成")
        // 播放完成不立即隐藏，等待队列为空
    }

    /**
     * TTS队列为空，开始延迟隐藏
     */
    fun onTTSQueueEmpty() {
        Log.d(TAG, "TTS队列为空，启动延迟隐藏")
        scheduleAutoHide()
    }

    /**
     * TTS出错时隐藏对话框
     */
    fun onTTSError(error: String) {
        Log.e(TAG, "TTS出错，隐藏对话框: $error")
        hideDialogImmediately()
    }

    /**
     * 更新对话框内容（由打字机调用）
     */
    fun updateContent(content: String) {
        scope.launch(Dispatchers.Main) {
            if (isDialogShowing) {
                ttsContentText?.text = content
                currentContent.clear()
                currentContent.append(content)

                // 智能滚动
                ttsScrollView?.post {
                    if (content.length < 24) {
                        ttsScrollView?.smoothScrollTo(0, 0)
                    } else {
                        ttsScrollView?.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    /**
     * 立即隐藏对话框（用于新消息时）
     */
    fun hideDialogImmediately() {
        Log.d(TAG, "立即隐藏TTS对话框")

        // 取消自动隐藏任务
        autoHideJob?.cancel()

        // 立即隐藏
        hideDialog()
    }

    /**
     * 显示对话框
     */
    private fun showDialog() {
        if (isDialogShowing) {
            Log.d(TAG, "TTS对话框已经在显示中")
            return
        }

        scope.launch(Dispatchers.Main) {
            ttsDropletContainer?.let { container ->
                Log.d(TAG, "开始显示TTS对话框动画")
                isDialogShowing = true
                container.visibility = View.VISIBLE

                // 清空内容
                currentContent.clear()
                ttsContentText?.text = ""

                // 执行显示动画
                container.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SHOW_ANIMATION_DURATION)
                    .withStartAction {
                        container.scaleX = 0.8f
                        container.scaleY = 0.8f
                        container.alpha = 0f
                    }
                    .start()

                Log.d(TAG, "TTS对话框已显示")
            } ?: run {
                Log.e(TAG, "TTS对话框容器为null，无法显示")
            }
        }
    }

    /**
     * 隐藏对话框
     */
    private fun hideDialog() {
        if (!isDialogShowing) return

        scope.launch(Dispatchers.Main) {
            ttsDropletContainer?.let { container ->
                // 执行隐藏动画
                container.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(HIDE_ANIMATION_DURATION)
                    .withEndAction {
                        container.visibility = View.GONE
                        isDialogShowing = false
                    }
                    .start()

                Log.d(TAG, "TTS对话框已隐藏")
            }
        }
    }

    /**
     * 安排自动隐藏
     */
    private fun scheduleAutoHide() {
        // 取消之前的任务
        autoHideJob?.cancel()

        autoHideJob = scope.launch {
            delay(AUTO_HIDE_DELAY)
            hideDialog()
        }
    }

    /**
     * 获取当前显示状态
     */
    fun isShowing(): Boolean {
        return isDialogShowing
    }
}