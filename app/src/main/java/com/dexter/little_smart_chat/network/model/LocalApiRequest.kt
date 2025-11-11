package com.dexter.little_smart_chat.network.model

import com.google.gson.annotations.SerializedName

data class LocalApiRequest (
    @SerializedName("agentName")
    val agentName: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("snId")
    val snId: String,

    @SerializedName("messageId")
    val messageId: String ? = null,

    @SerializedName("contextId")
    val contextId: String? = null,

    @SerializedName("acceptedOutputModes")
    val acceptedOutputModes: List<String>,

    @SerializedName("customHeaders")
    val customHeaders: Map<String, Any>,

    // "agentId":"agent01",
    //  "agentType":"xloop",
    //  "userId":"yiqi.zhang",
    @SerializedName("agentId")
    val agentId: String? = "",
    @SerializedName("agentType")
    val agentType: String? = "",
    @SerializedName("userId")
    val userId: String? = "",
)

// 响应模式
sealed class agentName(val value: String) {
//    object ModelBAOER: agentName("baoer") // 模型A
    object ModelXJZ : agentName("xjz") // 模型XJZ
    object ModelXLoop : agentName("xloop_rag") // 模型XJZ
}

