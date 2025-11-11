package com.dexter.little_smart_chat.ai.adapter

import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * Google Gemini模型适配器
 * 适配Google Gemini系列模型
 */
class GoogleGeminiAdapter : AIModelAdapter {
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    override fun getSupportedModels(): List<String> {
        return listOf(
            "gemini-pro",
            "gemini-pro-vision",
            "gemini-1.5-pro",
            "gemini-1.5-flash"
        )
    }
    
    override fun getDefaultModel(): String {
        return "gemini-pro"
    }
    
    override fun createChatRequest(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double,
        maxTokens: Int?,
        stream: Boolean
    ): ApiRequest {
        // Google使用不同的消息格式
        val contents = convertToGeminiFormat(messages)
        val generationConfig = ApiRequest.GenerationConfig(
            temperature = temperature,
            maxOutputTokens = maxTokens,
            topP = 0.8,
            topK = 40
        )
        
        return ApiRequest(
            model = model,
            messages = emptyList(), // Gemini不使用messages字段
            contents = contents,
            generationConfig = generationConfig,
            stream = stream
        )
    }
    
    override fun parseChatResponse(response: ApiResponse): String? {
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
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
            
            // Google流式响应格式
            val candidates = jsonObject.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    val part = parts[0].asJsonObject
                    part.get("text")?.asString
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isResponseComplete(data: String): Boolean {
        return data.contains("\"finishReason\":\"STOP\"") ||
               data.trim() == "data: [DONE]"
    }
    
    override fun getApiEndpoint(): String {
        return "https://generativelanguage.googleapis.com/v1beta"
    }
    
    override fun getStreamApiEndpoint(): String {
        return "https://generativelanguage.googleapis.com/v1beta/models"
    }
    
    override fun getAuthHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json"
        )
    }
    
    /**
     * 转换消息格式为Google Gemini格式
     */
    private fun convertToGeminiFormat(messages: List<ApiRequest.Message>): List<ApiRequest.Content> {
        return messages.filter { it.role != "system" }.map { message ->
            val role = when (message.role) {
                "assistant" -> "model"
                else -> "user"
            }
            
            ApiRequest.Content(
                parts = listOf(ApiRequest.Part(text = message.content)),
                role = role
            )
        }
    }
} 