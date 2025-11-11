package com.dexter.little_smart_chat.data

sealed class StreamMode(val value: Boolean) {
    object Stream : StreamMode(true)
    object NonStream : StreamMode(false)
}