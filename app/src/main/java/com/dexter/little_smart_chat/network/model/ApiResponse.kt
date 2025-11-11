package com.dexter.little_smart_chat.network.model

import com.google.gson.annotations.SerializedName

/**
 * API响应模型
 * 设计为通用格式，支持OpenAI、Anthropic、Google等不同厂商
 */
data class ApiResponse(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("object")
    val `object`: String? = null,
    
    @SerializedName("created")
    val created: Long? = null,
    
    @SerializedName("model")
    val model: String? = null,
    
    @SerializedName("choices")
    val choices: List<Choice>? = null,
    
    @SerializedName("usage")
    val usage: Usage? = null,
    
    // Anthropic特有字段
    @SerializedName("content")
    val content: List<ContentBlock>? = null,
    
    @SerializedName("role")
    val role: String? = null,
    
    @SerializedName("stop_reason")
    val stopReason: String? = null,
    
    @SerializedName("stop_sequence")
    val stopSequence: String? = null,
    
    // Google特有字段
    @SerializedName("candidates")
    val candidates: List<Candidate>? = null,
    
    @SerializedName("promptFeedback")
    val promptFeedback: PromptFeedback? = null,
    
    // 流式响应字段
    @SerializedName("delta")
    val delta: Delta? = null,
    
    @SerializedName("finish_reason")
    val finishReason: String? = null
) {
    
    /**
     * OpenAI Choice格式
     */
    data class Choice(
        @SerializedName("index")
        val index: Int = 0,
        
        @SerializedName("message")
        val message: Message? = null,
        
        @SerializedName("delta")
        val delta: Delta? = null,
        
        @SerializedName("finish_reason")
        val finishReason: String? = null,
        
        @SerializedName("logprobs")
        val logprobs: Any? = null
    )
    
    data class Message(
        @SerializedName("role")
        val role: String,
        
        @SerializedName("content")
        val content: String,
        
        @SerializedName("name")
        val name: String? = null
    )
    
    data class Delta(
        @SerializedName("role")
        val role: String? = null,
        
        @SerializedName("content")
        val content: String? = null
    )
    
    /**
     * Anthropic Content Block格式
     */
    data class ContentBlock(
        @SerializedName("type")
        val type: String,
        
        @SerializedName("text")
        val text: String? = null
    )
    
    /**
     * Google Candidate格式
     */
    data class Candidate(
        @SerializedName("content")
        val content: GeminiContent? = null,
        
        @SerializedName("finishReason")
        val finishReason: String? = null,
        
        @SerializedName("index")
        val index: Int? = null,
        
        @SerializedName("safetyRatings")
        val safetyRatings: List<SafetyRating>? = null
    )
    
    data class GeminiContent(
        @SerializedName("parts")
        val parts: List<GeminiPart>,
        
        @SerializedName("role")
        val role: String? = null
    )
    
    data class GeminiPart(
        @SerializedName("text")
        val text: String
    )
    
    data class SafetyRating(
        @SerializedName("category")
        val category: String,
        
        @SerializedName("probability")
        val probability: String
    )
    
    data class PromptFeedback(
        @SerializedName("safetyRatings")
        val safetyRatings: List<SafetyRating>? = null
    )
    
    /**
     * 使用统计
     */
    data class Usage(
        @SerializedName("prompt_tokens")
        val promptTokens: Int = 0,
        
        @SerializedName("completion_tokens")
        val completionTokens: Int = 0,
        
        @SerializedName("total_tokens")
        val totalTokens: Int = 0
    )
} 