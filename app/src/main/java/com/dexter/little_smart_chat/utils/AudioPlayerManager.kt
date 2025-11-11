package com.dexter.little_smart_chat.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayerManager(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingFile: String? = null
    private var isPlaying = false
    
    companion object {
        private const val TAG = "AudioPlayerManager"
    }
    
    /**
     * 播放音频文件
     * @param filePath 音频文件路径
     * @param onCompletion 播放完成回调
     * @return 是否成功开始播放
     */
    fun playAudio(filePath: String, onCompletion: (() -> Unit)? = null): Boolean {
        if (isPlaying) {
            stopAudio()
        }
        
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file not found: $filePath")
                return false
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            
            currentPlayingFile = filePath
            isPlaying = true
            
            // 设置播放完成监听器
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                currentPlayingFile = null
                onCompletion?.invoke()
            }
            
            // 设置错误监听器
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                isPlaying = false
                currentPlayingFile = null
                onCompletion?.invoke()
                true
            }
            
            Log.d(TAG, "Started playing: $filePath")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}")
            cleanup()
            return false
        }
    }
    
    /**
     * 暂停播放
     */
    fun pauseAudio() {
        if (isPlaying && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            Log.d(TAG, "Audio paused: $currentPlayingFile")
        }
    }
    
    /**
     * 恢复播放
     */
    fun resumeAudio() {
        if (isPlaying && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            Log.d(TAG, "Audio resumed: $currentPlayingFile")
        }
    }
    
    /**
     * 停止播放
     */
    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isPlaying = false
            currentPlayingFile = null
            
            Log.d(TAG, "Audio stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
            cleanup()
        }
    }
    
    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    /**
     * 获取音频总时长（毫秒）
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }
    
    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean = isPlaying && mediaPlayer?.isPlaying == true
    
    /**
     * 检查是否已暂停
     */
    fun isPaused(): Boolean = isPlaying && mediaPlayer?.isPlaying == false
    
    /**
     * 获取当前播放的文件路径
     */
    fun getCurrentPlayingFile(): String? = currentPlayingFile
    
    /**
     * 设置音量
     */
    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer?.setVolume(leftVolume, rightVolume)
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
        isPlaying = false
        currentPlayingFile = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        cleanup()
    }
} 