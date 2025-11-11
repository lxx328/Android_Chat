package com.dexter.little_smart_chat.utils

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorderManager(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var isRecording = false
    
    companion object {
        private const val TAG = "AudioRecorderManager"
        private const val RECORDING_DIR = "recordings"
    }
    
    /**
     * 开始录音
     * @return 录音文件路径，如果失败返回null
     */
    fun startRecording(): String? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return null
        }
        
        try {
            // 创建录音目录
            val recordingDir = File(context.filesDir, RECORDING_DIR)
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            // 创建录音文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "recording_$timestamp.mp3"
            currentRecordingFile = File(recordingDir, fileName)
            
            // 初始化MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000) // 128kbps
                setAudioSamplingRate(44100) // 44.1kHz
                setOutputFile(currentRecordingFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "Recording started: ${currentRecordingFile?.absolutePath}")
            return currentRecordingFile?.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * 停止录音
     * @return 录音文件路径，如果失败返回null
     */
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val filePath = currentRecordingFile?.absolutePath
            Log.d(TAG, "Recording stopped: $filePath")
            
            // 检查文件是否存在且大小大于0
            if (currentRecordingFile?.exists() == true && currentRecordingFile!!.length() > 0) {
                return filePath
            } else {
                Log.w(TAG, "Recording file is empty or doesn't exist")
                currentRecordingFile?.delete()
                currentRecordingFile = null
                return null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * 取消录音
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            // 删除录音文件
            currentRecordingFile?.delete()
            currentRecordingFile = null
            
            Log.d(TAG, "Recording cancelled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording: ${e.message}")
            cleanup()
        }
    }
    
    /**
     * 获取所有录音文件
     */
    fun getAllRecordings(): List<File> {
        val recordingDir = File(context.filesDir, RECORDING_DIR)
        if (!recordingDir.exists()) {
            return emptyList()
        }
        
        return recordingDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("mp3", "m4a", "wav")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 删除录音文件
     */
    fun deleteRecording(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            val result = file.delete()
            Log.d(TAG, "Recording deleted: $filePath, result: $result")
            result
        } else {
            Log.w(TAG, "Recording file not found: $filePath")
            false
        }
    }
    
    /**
     * 获取录音文件信息
     */
    fun getRecordingInfo(filePath: String): RecordingInfo? {
        val file = File(filePath)
        if (!file.exists()) {
            return null
        }
        
        return RecordingInfo(
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            duration = getAudioDuration(filePath),
            createdTime = Date(file.lastModified())
        )
    }
    
    /**
     * 获取音频时长（需要MediaMetadataRetriever）
     */
    private fun getAudioDuration(filePath: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio duration: ${e.message}")
            0L
        }
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder: ${e.message}")
        }
        mediaRecorder = null
        isRecording = false
        currentRecordingFile = null
    }
    
    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 获取当前录音文件
     */
    fun getCurrentRecordingFile(): File? = currentRecordingFile
    
    /**
     * 释放资源
     */
    fun release() {
        cleanup()
    }
    
    /**
     * 录音文件信息数据类
     */
    data class RecordingInfo(
        val name: String,
        val path: String,
        val size: Long,
        val duration: Long,
        val createdTime: Date
    )
} 