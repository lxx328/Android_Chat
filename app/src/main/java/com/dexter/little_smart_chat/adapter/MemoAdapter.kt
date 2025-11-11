package com.dexter.little_smart_chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.data.Memo

class MemoAdapter(
    private val memos: MutableList<Memo>,
    private val onMemoClick: (Memo) -> Unit,
    private val onDeleteClick: (Memo) -> Unit
) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    inner class MemoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleContainer: View = view.findViewById(R.id.titleContainer)
        val checkBox: CheckBox = view.findViewById(R.id.memoCheckBox)
        val title: TextView = view.findViewById(R.id.memoTitle)
        val titleStrikeThroughLine: View = view.findViewById(R.id.titleStrikeThroughLine)
        val content: TextView = view.findViewById(R.id.memoContent)
        val date: TextView = view.findViewById(R.id.memoDate)
        val duration: TextView = view.findViewById(R.id.memoDuration)
        val asrContent: TextView = view.findViewById(R.id.memoAsrContent)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memos[position]

        holder.checkBox.isChecked = memo.isChecked
        holder.title.text = memo.title
        holder.content.text = memo.content
        holder.date.text = memo.date

        // 处理录音时长显示
        if (memo.recordingPath != null && memo.recordingDuration > 0) {
            holder.duration.visibility = View.VISIBLE
            holder.duration.text = formatDuration(memo.recordingDuration)
        } else {
            holder.duration.visibility = View.GONE
        }

        // 处理ASR内容显示
        if (!memo.asrContent.isNullOrEmpty()) {
            holder.asrContent.visibility = View.VISIBLE
            holder.asrContent.text = "识别：${memo.asrContent}"
        } else {
            holder.asrContent.visibility = View.GONE
        }

        // 根据CheckBox状态更新UI
        updateItemUI(holder, memo.isChecked)

        // CheckBox点击事件
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            memo.isChecked = isChecked
            updateItemUI(holder, isChecked)
        }

        // 点击事件（仅在CheckBox未选中时生效）
        holder.itemView.setOnClickListener {
            if (!memo.isChecked) {
                onMemoClick(memo)
            }
        }

        // 删除按钮点击事件（始终生效）
        holder.deleteButton.setOnClickListener {
            onDeleteClick(memo)
        }
    }

    override fun getItemCount() = memos.size

    /**
     * 添加备忘录
     */
    fun addMemo(memo: Memo) {
        memos.add(0, memo)
        notifyItemInserted(0)
    }

    /**
     * 删除备忘录
     */
    fun removeMemo(memo: Memo) {
        val position = memos.indexOf(memo)
        if (position != -1) {
            memos.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * 更新备忘录
     */
    fun updateMemo(memo: Memo) {
        val position = memos.indexOfFirst { it.id == memo.id }
        if (position != -1) {
            memos[position] = memo
            notifyItemChanged(position)
        }
    }

    /**
     * 格式化时长显示
     */
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

    /**
     * 根据CheckBox状态更新Item UI
     */
    private fun updateItemUI(holder: MemoViewHolder, isChecked: Boolean) {
        if (isChecked) {
            // 标题行保持正常背景
            holder.titleContainer.alpha = 1.0f
            // 其他区域（内容、ASR内容、日期）变为灰色
            holder.content.alpha = 0.4f
            holder.date.alpha = 0.4f
            holder.asrContent.alpha = 0.4f
            // 显示标题擦除线
            holder.titleStrikeThroughLine.visibility = View.VISIBLE
        } else {
            // 所有区域恢复正常背景
            holder.titleContainer.alpha = 1.0f
            holder.content.alpha = 1.0f
            holder.date.alpha = 1.0f
            holder.asrContent.alpha = 1.0f
            // 隐藏标题擦除线
            holder.titleStrikeThroughLine.visibility = View.GONE
        }
    }
}

//package com.dexter.little_smart_chat.adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.dexter.little_smart_chat.R
//import com.dexter.little_smart_chat.data.Memo
//
//class MemoAdapter(
//    private val memos: MutableList<Memo>,
//    private val onMemoClick: (Memo) -> Unit,
//    private val onDeleteClick: (Memo) -> Unit
//) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {
//
//    inner class MemoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val title: TextView = view.findViewById(R.id.memoTitle)
//        val content: TextView = view.findViewById(R.id.memoContent)
//        val date: TextView = view.findViewById(R.id.memoDate)
//        val duration: TextView = view.findViewById(R.id.memoDuration)
//        val asrContent: TextView = view.findViewById(R.id.memoAsrContent)
//        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_memo, parent, false)
//        return MemoViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
//        val memo = memos[position]
//
//        holder.title.text = memo.title
//        holder.content.text = memo.content
//        holder.date.text = memo.date
//
//        // 处理录音时长显示
//        if (memo.recordingPath != null && memo.recordingDuration > 0) {
//            holder.duration.visibility = View.VISIBLE
//            holder.duration.text = formatDuration(memo.recordingDuration)
//        } else {
//            holder.duration.visibility = View.GONE
//        }
//
//        // 处理ASR内容显示
//        if (!memo.asrContent.isNullOrEmpty()) {
//            holder.asrContent.visibility = View.VISIBLE
//            holder.asrContent.text = "识别：${memo.asrContent}"
//        } else {
//            holder.asrContent.visibility = View.GONE
//        }
//
//        // 点击事件
//        holder.itemView.setOnClickListener {
//            onMemoClick(memo)
//        }
//
//        // 删除按钮点击事件
//        holder.deleteButton.setOnClickListener {
//            onDeleteClick(memo)
//        }
//    }
//
//    override fun getItemCount() = memos.size
//
//    /**
//     * 添加备忘录
//     */
//    fun addMemo(memo: Memo) {
//        memos.add(0, memo)
//        notifyItemInserted(0)
//    }
//
//    /**
//     * 删除备忘录
//     */
//    fun removeMemo(memo: Memo) {
//        val position = memos.indexOf(memo)
//        if (position != -1) {
//            memos.removeAt(position)
//            notifyItemRemoved(position)
//        }
//    }
//
//    /**
//     * 更新备忘录
//     */
//    fun updateMemo(memo: Memo) {
//        val position = memos.indexOfFirst { it.id == memo.id }
//        if (position != -1) {
//            memos[position] = memo
//            notifyItemChanged(position)
//        }
//    }
//
//    /**
//     * 格式化时长显示
//     */
//    private fun formatDuration(durationMs: Long): String {
//        val seconds = durationMs / 1000
//        val minutes = seconds / 60
//        val remainingSeconds = seconds % 60
//        return if (minutes > 0) {
//            "${minutes}:${String.format("%02d", remainingSeconds)}"
//        } else {
//            "0:${String.format("%02d", remainingSeconds)}"
//        }
//    }
//}