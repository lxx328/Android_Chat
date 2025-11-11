package com.dexter.little_smart_chat.data

import org.json.JSONObject

/**
 * Dify聊天请求参数
 */
data class DifyChatMesRequest(
    val inputs:Map<String, Any> = emptyMap(), // 或 val inputs: JSONObject = JSONObject()
    val query: String,
    val response_mode: String = "streaming",
    val conversation_id: String? = null,
    val user: String,
    val files: List<FileObj>? = null
)

data class FileObj(
    val type: String,
    val transfer_method: String,
    val url: String
)

// 响应模式
sealed class ResponseMode(val value: String) {
    object Stream : ResponseMode("streaming")
    object Block : ResponseMode("blocking")
}
