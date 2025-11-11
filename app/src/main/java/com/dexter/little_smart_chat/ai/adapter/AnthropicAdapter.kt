package com.dexter.little_smart_chat.ai.adapter

import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * Anthropic Claude模型适配器
 * 适配Anthropic Claude系列模型
 */
class AnthropicAdapter : AIModelAdapter {
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    override fun getSupportedModels(): List<String> {
        return listOf(
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307",
            "claude-2.1",
            "claude-2.0"
        )
    }
    
    override fun getDefaultModel(): String {
        return "claude-3-haiku-20240307"
    }
    
    override fun createChatRequest(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double,
        maxTokens: Int?,
        stream: Boolean
    ): ApiRequest {
        // Anthropic使用不同的消息格式
        val convertedMessages = convertAnthropicMessages(messages)
        val systemMessage = extractSystemMessage(messages)
        
        return ApiRequest(
            model = model,
            messages = convertedMessages,
            temperature = temperature,
            maxTokens = maxTokens ?: 1024,
            stream = stream,
            system = systemMessage
        )
    }
    
    override fun parseChatResponse(response: ApiResponse): String? {
        return response.content?.firstOrNull()?.text
    }
    
    override fun parseStreamResponse(data: String): String? {
        if (data.trim().isEmpty()) {
            return null
        }
        
        return try {
            val jsonData = data.removePrefix("data: ").trim()
            if (jsonData == "[DONE]") return null
            
            val jsonElement = jsonParser.parse(jsonData)
            val jsonObject = jsonElement.asJsonObject
            
            // Anthropic流式响应格式
            val type = jsonObject.get("type")?.asString
            when (type) {
                "content_block_delta" -> {
                    val delta = jsonObject.getAsJsonObject("delta")
                    delta?.get("text")?.asString
                }
                "content_block_start" -> {
                    val contentBlock = jsonObject.getAsJsonObject("content_block")
                    contentBlock?.get("text")?.asString
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isResponseComplete(data: String): Boolean {
        return data.contains("\"type\":\"message_stop\"") ||
               data.trim() == "data: [DONE]"
    }
    
    override fun getApiEndpoint(): String {
        return "https://api.anthropic.com/v1"
    }
    
    override fun getStreamApiEndpoint(): String {
        return "https://api.anthropic.com/v1/messages"
    }
    
    override fun getAuthHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "x-api-key" to apiKey,
            "Content-Type" to "application/json",
            "anthropic-version" to "2023-06-01"
        )
    }
    
    /**
     * 转换消息格式为Anthropic格式
     */
    private fun convertAnthropicMessages(messages: List<ApiRequest.Message>): List<ApiRequest.Message> {
        return messages.filter { it.role != "system" }.map { message ->
            ApiRequest.Message(
                role = if (message.role == "assistant") "assistant" else "user",
                content = message.content
            )
        }
    }
    
    /**
     * 提取系统消息
     */
    private fun extractSystemMessage(messages: List<ApiRequest.Message>): String? {
        return messages.find { it.role == "system" }?.content
    }
} 