package com.dexter.little_smart_chat.ai.adapter

import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * OpenAI模型适配器
 * 适配OpenAI GPT系列模型
 */
class OpenAIAdapter : AIModelAdapter {
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    override fun getSupportedModels(): List<String> {
        return listOf(
            "gpt-4",
            "gpt-4-turbo-preview",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k"
        )
    }
    
    override fun getDefaultModel(): String {
        return "gpt-3.5-turbo"
    }
    
    override fun createChatRequest(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double,
        maxTokens: Int?,
        stream: Boolean
    ): ApiRequest {
        return ApiRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = stream
        )
    }
    
    override fun parseChatResponse(response: ApiResponse): String? {
        return response.choices?.firstOrNull()?.message?.content
    }
    
    override fun parseStreamResponse(data: String): String? {
        if (data.trim() == "data: [DONE]" || data.trim().isEmpty()) {
            return null
        }
        
        return try {
            val jsonData = data.removePrefix("data: ").trim()
            val jsonElement = jsonParser.parse(jsonData)
            val jsonObject = jsonElement.asJsonObject
            
            val choices = jsonObject.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val choice = choices[0].asJsonObject
                val delta = choice.getAsJsonObject("delta")
                delta?.get("content")?.asString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isResponseComplete(data: String): Boolean {
        return data.trim() == "data: [DONE]" || 
               data.contains("\"finish_reason\":")
    }
    
    override fun getApiEndpoint(): String {
        return "https://api.openai.com/v1"
    }
    
    override fun getStreamApiEndpoint(): String {
        return "https://api.openai.com/v1/chat/completions"
    }
    
    override fun getAuthHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
    }
} 