package com.dexter.little_smart_chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.utils.AudioRecorderManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingAdapter(
    private val recordings: MutableList<AudioRecorderManager.RecordingInfo>,
    private val onPlayClick: (AudioRecorderManager.RecordingInfo) -> Unit,
    private val onDeleteClick: (AudioRecorderManager.RecordingInfo) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.recordingTitle)
        val durationText: TextView = itemView.findViewById(R.id.recordingDuration)
        val dateText: TextView = itemView.findViewById(R.id.recordingDate)
        val sizeText: TextView = itemView.findViewById(R.id.recordingSize)
        val playButton: ImageButton = itemView.findViewById(R.id.playButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]
        
        holder.titleText.text = recording.name
        holder.durationText.text = formatDuration(recording.duration)
        holder.dateText.text = formatDate(recording.createdTime)
        holder.sizeText.text = formatFileSize(recording.size)
        
        holder.playButton.setOnClickListener {
            onPlayClick(recording)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(recording)
        }
    }

    override fun getItemCount(): Int = recordings.size

    fun updateRecordings(newRecordings: List<AudioRecorderManager.RecordingInfo>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    fun addRecording(recording: AudioRecorderManager.RecordingInfo) {
        recordings.add(0, recording)
        notifyItemInserted(0)
    }

    fun removeRecording(recording: AudioRecorderManager.RecordingInfo) {
        val position = recordings.indexOf(recording)
        if (position != -1) {
            recordings.removeAt(position)
            notifyItemRemoved(position)
        }
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

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    private fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "${sizeBytes} B"
            sizeBytes < 1024 * 1024 -> "${String.format("%.1f", sizeBytes / 1024.0)} KB"
            else -> "${String.format("%.1f", sizeBytes / (1024.0 * 1024.0))} MB"
        }
    }
} 