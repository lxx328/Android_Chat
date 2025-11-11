package com.dexter.little_smart_chat.data


data class MqttReplyData(val  commandId: String, val replyTime: String, val success: Boolean, val SN: String)
