package com.dexter.little_smart_chat.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.dexter.little_smart_chat.utils.AudioUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音管理器
 * 负责录音文件的创建、保存和管理
 */
class AudioRecorderManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorderManager"
        private const val AUDIO_FOLDER = "recordings"
        
        @Volatile
        private var INSTANCE: AudioRecorderManager? = null
        
        fun getInstance(context: Context): AudioRecorderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioRecorderManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

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
    /**
     * 开始录音
     * @return 返回录音文件
     */
    fun startRecording(): File? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return currentFile
        }

        try {
            // 创建录音文件
            currentFile = createAudioFile()

            // 初始化MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                    Log.d(TAG, "Recording started: ${currentFile?.absolutePath}")
                } catch (e: IOException) {
                    Log.e(TAG, "prepare() failed: ${e.message}")
                    release()
                    currentFile?.delete()
                    currentFile = null
                    return null
                }
            }

            return currentFile

        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed: ${e.message}")
            stopRecording()
            currentFile?.delete()
            currentFile = null
            return null
        }
    }

    /**
     * 停止录音
     * @return 返回录音文件
     */
    fun stopRecording(): File? {
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

            Log.d(TAG, "Recording stopped: ${currentFile?.absolutePath}")

            // 验证录音文件是否有效
            if (currentFile != null && AudioUtils.isValidAudioFile(currentFile!!)) {
                val duration = AudioUtils.getAudioDuration(currentFile!!)
                Log.d(TAG, "Recording duration: ${AudioUtils.formatDuration(duration)}")
                return currentFile
            } else {
                Log.w(TAG, "Invalid recording file, deleting...")
                currentFile?.delete()
                currentFile = null
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "stopRecording failed: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentFile?.delete()
            currentFile = null
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
        } catch (e: Exception) {
            Log.e(TAG, "cancelRecording failed: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
            currentFile?.delete()
            currentFile = null
        }
    }

    /**
     * 获取录音文件信息
     */
    fun getRecordingInfo(filePath: String): RecordingInfo? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "Recording file not found: $filePath")
            return null
        }

        return try {
            RecordingInfo(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                duration = getAudioDuration(filePath),
                createdTime = Date(file.lastModified())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recording info: ${e.message}")
            null
        }
    }

    /**
     * 获取音频时长
     */
    private fun getAudioDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio duration: ${e.message}")
            0L
        }
    }

    /**
     * 创建录音文件
     */
    private fun createAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUDIO_$timestamp.m4a"

        // 创建录音文件夹
        val folder = File(context.getExternalFilesDir(null), AUDIO_FOLDER)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        // 创建录音文件
        return File(folder, fileName)
    }

    /**
     * 获取所有录音文件
     */
    fun getAllRecordings(): List<File> {
        val folder = File(context.getExternalFilesDir(null), AUDIO_FOLDER)
        return if (folder.exists()) {
            folder.listFiles()?.filter { it.isFile && it.extension == "m4a" }?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 获取所有录音文件信息
     */
    fun getAllRecordingInfos(): List<RecordingInfo> {
        return getAllRecordings().mapNotNull { file ->
            getRecordingInfo(file.absolutePath)
        }
    }

    /**
     * 删除录音文件
     */
    fun deleteRecording(file: File): Boolean {
        return if (file.exists() && !isRecording) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * 清理所有录音文件
     */
    fun clearAllRecordings() {
        if (isRecording) return
        
        val folder = File(context.getExternalFilesDir(null), AUDIO_FOLDER)
        if (folder.exists()) {
            folder.listFiles()?.forEach { it.delete() }
        }
    }
    
    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean {
        return isRecording
    }
    
    /**
     * 获取当前录音文件
     */
    fun getCurrentFile(): File? {
        return currentFile
    }
} 