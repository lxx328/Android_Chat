package com.dexter.little_smart_chat.data

data class RequestResponse<T>(
    val message: String?,
    val code: Int,
    val data: T? // 可空类型更安全
)
