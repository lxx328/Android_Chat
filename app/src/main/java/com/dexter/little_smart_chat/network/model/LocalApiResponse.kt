package com.dexter.little_smart_chat.network.model

import com.dexter.little_smart_chat.network.model.ApiRequest.Content
import com.dexter.little_smart_chat.network.model.ApiRequest.GenerationConfig
import com.dexter.little_smart_chat.network.model.ApiRequest.Message
import com.google.gson.annotations.SerializedName

data class LocalApiResponse (

    @SerializedName("contextId")
    val contextId: String? = null,
    @SerializedName("final")
    val final: Boolean? = null,
    @SerializedName("kind")
    val kind: String? = null,
    @SerializedName("status")
    val status: Status? = null,
    @SerializedName("taskId")
    val taskId: String? = null,
    @SerializedName("validation_errors")
    val validation_errors: List<String>? = null
){

    data class Status(
        @SerializedName("message")
        val message: Message? = null,
        @SerializedName("state")
        val state: String? = null,
        @SerializedName("timestamp")
        val timestamp: String? = null
    )

    data class Message(
        @SerializedName("contextId")
        val contextId: String? = null,
        @SerializedName("kind")
        val kind: String? = null,
        @SerializedName("messageId")
        val messageId: String? = null,
        @SerializedName("parts")
        val parts: List<Content>? = null,
        @SerializedName("role")
        val role: String? = null,
        @SerializedName("taskId")
        val taskId: String? = null
    )

    data class Content(
        @SerializedName("kind")
        val kind: String? = null,
        @SerializedName("text")
        val text: String? = null
    )
}