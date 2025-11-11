package com.dexter.little_smart_chat.mvvm.view

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.adapter.RecordingAdapter
import com.dexter.little_smart_chat.utils.AudioPlayerManager
import com.dexter.little_smart_chat.utils.AudioRecorderManager
import android.widget.Toast

class RecordingListActivity : AppCompatActivity() {
    
    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var recordingAdapter: RecordingAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var backButton: ImageButton
    
    private val recordings = mutableListOf<AudioRecorderManager.RecordingInfo>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording_list)
        
        initializeViews()
        initializeManagers()
        setupRecyclerView()
        loadRecordings()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.recordingsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        backButton = findViewById(R.id.backButton)
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun initializeManagers() {
        audioRecorderManager = AudioRecorderManager(this)
        audioPlayerManager = AudioPlayerManager(this)
    }
    
    private fun setupRecyclerView() {
        recordingAdapter = RecordingAdapter(
            recordings = recordings,
            onPlayClick = { recording ->
                playRecording(recording)
            },
            onDeleteClick = { recording ->
                showDeleteDialog(recording)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecordingListActivity)
            adapter = recordingAdapter
        }
    }
    
    private fun loadRecordings() {
        val recordingFiles = audioRecorderManager.getAllRecordings()
        recordings.clear()
        
        recordingFiles.forEach { file ->
            val info = audioRecorderManager.getRecordingInfo(file.absolutePath)
            info?.let { recordings.add(it) }
        }
        
        recordingAdapter.notifyDataSetChanged()
        updateEmptyState()
    }
    
    private fun playRecording(recording: AudioRecorderManager.RecordingInfo) {
        val success = audioPlayerManager.playAudio(recording.path) {
            // 播放完成回调
            Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
        }
        
        if (success) {
            Toast.makeText(this, "开始播放: ${recording.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteDialog(recording: AudioRecorderManager.RecordingInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除录音")
            .setMessage("确定要删除录音文件 \"${recording.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteRecording(recording)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteRecording(recording: AudioRecorderManager.RecordingInfo) {
        val success = audioRecorderManager.deleteRecording(recording.path)
        
        if (success) {
            recordings.remove(recording)
            recordingAdapter.notifyDataSetChanged()
            updateEmptyState()
            Toast.makeText(this, "录音已删除", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateEmptyState() {
        if (recordings.isEmpty()) {
            emptyStateText.visibility = TextView.VISIBLE
            recyclerView.visibility = RecyclerView.GONE
        } else {
            emptyStateText.visibility = TextView.GONE
            recyclerView.visibility = RecyclerView.VISIBLE
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
        audioRecorderManager.release()
    }
} 