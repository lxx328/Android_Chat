package com.dexter.little_smart_chat.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.text.DecimalFormat

/**
 * 音频工具类
 * 提供音频文件相关的实用功能
 */
object AudioUtils {
    
    private const val TAG = "AudioUtils"
    
    /**
     * 获取音频文件时长（毫秒）
     */
    fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration: ${e.message}")
            0L
        }
    }
    
    /**
     * 格式化音频时长显示
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return when {
            minutes > 0 -> "${minutes}分${remainingSeconds}秒"
            seconds > 0 -> "${seconds}秒"
            else -> "0秒"
        }
    }
    
    /**
     * 获取音频文件大小（字节）
     */
    fun getAudioFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }
    
    /**
     * 格式化文件大小显示
     */
    fun formatFileSize(sizeBytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            sizeBytes < 1024 -> "${sizeBytes}B"
            sizeBytes < 1024 * 1024 -> "${df.format(sizeBytes / 1024.0)}KB"
            else -> "${df.format(sizeBytes / (1024.0 * 1024.0))}MB"
        }
    }
    
    /**
     * 验证音频文件是否有效
     */
    fun isValidAudioFile(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() == 0L) {
                return false
            }
            
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            
            duration != null && duration > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error validating audio file: ${e.message}")
            false
        }
    }
    
    /**
     * 清理空的或损坏的录音文件
     */
    fun cleanupInvalidFiles(context: Context): Int {
        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) return 0
        
        var cleanedCount = 0
        recordingsDir.listFiles()?.forEach { file ->
            if (file.isFile && !isValidAudioFile(file)) {
                if (file.delete()) {
                    cleanedCount++
                    Log.d(TAG, "Deleted invalid audio file: ${file.name}")
                }
            }
        }
        
        return cleanedCount
    }
    
    /**
     * 获取录音目录的总大小
     */
    fun getRecordingsDirSize(context: Context): Long {
        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) return 0L
        
        var totalSize = 0L
        recordingsDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }
        
        return totalSize
    }
    
    /**
     * 检查是否需要清理存储空间
     */
    fun shouldCleanupStorage(context: Context, maxSizeMB: Long = 100): Boolean {
        val currentSize = getRecordingsDirSize(context)
        val maxSizeBytes = maxSizeMB * 1024 * 1024
        return currentSize > maxSizeBytes
    }
} 