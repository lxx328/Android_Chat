package com.dexter.little_smart_chat.data

data class Memo(
    val id: Int,
    val title: String,
    val content: String,
    val date: String,
    val recordingPath: String? = null,  // 录音文件路径
    val recordingDuration: Long = 0L,   // 录音时长（毫秒）
    val asrContent: String? = null,     // ASR识别的内容
    var isChecked: Boolean = false      // CheckBox选中状态
)