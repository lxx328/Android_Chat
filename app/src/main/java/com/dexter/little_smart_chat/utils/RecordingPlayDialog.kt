package com.dexter.little_smart_chat.utils

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.data.Memo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音播放弹窗管理器
 * 负责显示录音播放界面，包含播放控制、进度条、ASR内容显示等功能
 */
class RecordingPlayDialog(
    private val context: Context,
    private val memo: Memo
) {

    private var dialog: Dialog? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null

    // UI组件
    private lateinit var dialogTitle: TextView
    private lateinit var recordingDuration: TextView
    private lateinit var recordingDate: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var progressSeekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var toggleContentButton: ImageButton
    private lateinit var asrContentContainer: View
    private lateinit var asrContentText: TextView
    private lateinit var closeButton: ImageButton

    private var isPlaying = false
    private var isContentVisible = true

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL = 100L // 100ms更新一次进度
    }

    /**
     * 显示播放对话框
     */
    fun show() {
        if (dialog?.isShowing == true) {
            return
        }

        createDialog()
        initializeViews()
        setupListeners()
        updateRecordingInfo()

        dialog?.show()
    }

    /**
     * 隐藏播放对话框
     */
    fun dismiss() {
        stopPlayback()
        dialog?.dismiss()
        dialog = null
    }

    private fun createDialog() {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_recording_play)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }

    private fun initializeViews() {
        dialog?.let { d ->
            dialogTitle = d.findViewById(R.id.dialogTitle)
            recordingDuration = d.findViewById(R.id.recordingDuration)
            recordingDate = d.findViewById(R.id.recordingDate)
            playPauseButton = d.findViewById(R.id.playPauseButton)
            progressSeekBar = d.findViewById(R.id.progressSeekBar)
            currentTime = d.findViewById(R.id.currentTime)
            totalTime = d.findViewById(R.id.totalTime)
            toggleContentButton = d.findViewById(R.id.toggleContentButton)
            asrContentContainer = d.findViewById(R.id.asrContentContainer)
            asrContentText = d.findViewById(R.id.asrContentText)
            closeButton = d.findViewById(R.id.closeButton)
        }
    }

    private fun setupListeners() {
        // 播放/暂停按钮
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                pausePlayback()
            } else {
                startPlayback()
            }
        }

        // 进度条拖动
        progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    val duration = mediaPlayer!!.duration
                    val position = (progress * duration / 100).toInt()
                    mediaPlayer!!.seekTo(position)
                    updateCurrentTime(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 显示/隐藏内容按钮
        toggleContentButton.setOnClickListener {
            toggleContentVisibility()
        }

        // 关闭按钮
        closeButton.setOnClickListener {
            dismiss()
        }

        // 对话框关闭监听
        dialog?.setOnDismissListener {
            stopPlayback()
        }
    }

    private fun updateRecordingInfo() {
        dialogTitle.text = memo.title
        recordingDuration.text = formatDuration(memo.recordingDuration)
        recordingDate.text = memo.date
        totalTime.text = formatDuration(memo.recordingDuration)
        currentTime.text = "0:00"

        // 设置ASR内容
        if (!memo.asrContent.isNullOrEmpty()) {
            asrContentText.text = memo.asrContent
            asrContentContainer.visibility = View.VISIBLE
        } else {
            asrContentText.text = "暂无识别内容"
            asrContentContainer.visibility = View.VISIBLE
        }
    }

    private fun startPlayback() {
        if (memo.recordingPath == null) {
            ToastUtils.showError(context, "录音文件不存在")
            return
        }

        val file = File(memo.recordingPath)
        if (!file.exists()) {
            ToastUtils.showError(context, "录音文件已被删除")
            return
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(memo.recordingPath)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        this@RecordingPlayDialog.isPlaying = true
                        updatePlayPauseButton()
                        startProgressUpdate()
                    }
                    setOnCompletionListener {
                        this@RecordingPlayDialog.isPlaying = false
                        updatePlayPauseButton()
                        stopProgressUpdate()
                        progressSeekBar.progress = 0
                        currentTime.text = "0:00"
                    }
                    setOnErrorListener { _, _, _ ->
                        ToastUtils.showError(context, "播放失败")
                        stopPlayback()
                        true
                    }
                }
            } else {
                mediaPlayer?.start()
                isPlaying = true
                updatePlayPauseButton()
                startProgressUpdate()
            }
        } catch (e: Exception) {
            ToastUtils.showError(context, "播放失败：${e.message}")
            stopPlayback()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayPauseButton()
        stopProgressUpdate()
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // 忽略释放时的异常
        }
        mediaPlayer = null
        isPlaying = false
        updatePlayPauseButton()
        stopProgressUpdate()
    }

    private fun updatePlayPauseButton() {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        playPauseButton.setImageResource(iconRes)
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (mediaPlayer != null && isPlaying) {
                    try {
                        val currentPosition = mediaPlayer!!.currentPosition
                        val duration = mediaPlayer!!.duration

                        if (duration > 0) {
                            val progress = (currentPosition * 100 / duration).coerceIn(0, 100)
                            progressSeekBar.progress = progress
                            updateCurrentTime(currentPosition)
                        }

                        handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                    } catch (e: Exception) {
                        // 忽略更新时的异常
                    }
                }
            }
        }
        handler.post(progressUpdateRunnable!!)
    }

    private fun stopProgressUpdate() {
        progressUpdateRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        progressUpdateRunnable = null
    }

    private fun updateCurrentTime(positionMs: Int) {
        currentTime.text = formatDuration(positionMs.toLong())
    }

    private fun toggleContentVisibility() {
        isContentVisible = !isContentVisible
        asrContentContainer.visibility = if (isContentVisible) View.VISIBLE else View.GONE

        val iconRes = if (isContentVisible) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        toggleContentButton.setImageResource(iconRes)
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}:${String.format("%02d", remainingSeconds)}"
        } else {
            "0:${String.format("%02d", remainingSeconds)}"
        }
    }
}