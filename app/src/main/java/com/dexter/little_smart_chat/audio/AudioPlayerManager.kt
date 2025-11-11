package com.dexter.little_smart_chat.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * 音频播放管理器
 * 负责音频文件的播放和管理
 */
class AudioPlayerManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioPlayerManager"
        
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null
        
        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private var isPlaying = false
    private var isPaused = false
    
    /**
     * 播放音频文件
     */
    fun playAudio(file: File, onCompletion: () -> Unit = {}) {
        if (isPlaying) {
            stopPlaying()
        }
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnCompletionListener {
                    stopPlaying()
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopPlaying()
                    false
                }
                prepare()
                start()
            }
            
            currentFile = file
            isPlaying = true
            isPaused = false
            
            Log.d(TAG, "Playing started: ${file.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "playAudio failed: ${e.message}")
            stopPlaying()
        }
    }
    
    /**
     * 暂停播放
     */
    fun pausePlaying() {
        if (isPlaying && !isPaused) {
            mediaPlayer?.pause()
            isPaused = true
            Log.d(TAG, "Playing paused")
        }
    }
    
    /**
     * 恢复播放
     */
    fun resumePlaying() {
        if (isPlaying && isPaused) {
            mediaPlayer?.start()
            isPaused = false
            Log.d(TAG, "Playing resumed")
        }
    }
    
    /**
     * 停止播放
     */
    fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopPlaying failed: ${e.message}")
        } finally {
            mediaPlayer = null
            currentFile = null
            isPlaying = false
            isPaused = false
        }
    }
    
    /**
     * 获取当前播放进度（毫秒）
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
    fun isPlaying(): Boolean {
        return isPlaying
    }
    
    /**
     * 检查是否已暂停
     */
    fun isPaused(): Boolean {
        return isPaused
    }
    
    /**
     * 获取当前播放文件
     */
    fun getCurrentFile(): File? {
        return currentFile
    }
} 