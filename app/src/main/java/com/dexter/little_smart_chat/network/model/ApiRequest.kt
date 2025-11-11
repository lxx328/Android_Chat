package com.dexter.little_smart_chat.network.model

import com.google.gson.annotations.SerializedName

/**
 * API请求模型
 * 设计为通用格式，支持OpenAI、Anthropic、Google等不同厂商
 */
data class ApiRequest(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("messages")
    val messages: List<Message>,
    
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    
    @SerializedName("stream")
    val stream: Boolean = false,
    
    @SerializedName("top_p")
    val topP: Double? = null,
    
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double? = null,
    
    @SerializedName("presence_penalty")
    val presencePenalty: Double? = null,
    
    // Anthropic特有字段
    @SerializedName("system")
    val system: String? = null,

    @SerializedName("maxTokensToSample")
    val maxTokensToSample: Int? = null,
    
    // Google特有字段
    @SerializedName("contents")
    val contents: List<Content>? = null,
    
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null
) {
    
    /**
     * 消息格式
     */
    data class Message(
        @SerializedName("role")
        val role: String, // user, assistant, system
        
        @SerializedName("content")
        val content: String,
        
        // 可选字段用于特殊情况
        @SerializedName("name")
        val name: String? = null
    )
    
    /**
     * Google Gemini Content格式
     */
    data class Content(
        @SerializedName("parts")
        val parts: List<Part>,
        
        @SerializedName("role")
        val role: String? = null
    )
    
    data class Part(
        @SerializedName("text")
        val text: String
    )
    
    /**
     * Google Gemini生成配置
     */
    data class GenerationConfig(
        @SerializedName("temperature")
        val temperature: Double? = null,
        
        @SerializedName("maxOutputTokens")
        val maxOutputTokens: Int? = null,
        
        @SerializedName("topP")
        val topP: Double? = null,
        
        @SerializedName("topK")
        val topK: Int? = null
    )
} 