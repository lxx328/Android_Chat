package com.dexter.little_smart_chat.ai.adapter

import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse

/**
 * AI模型适配器接口
 * 为不同的AI模型提供统一的接口
 */
interface AIModelAdapter {
    
    /**
     * 获取支持的模型列表
     */
    fun getSupportedModels(): List<String>
    
    /**
     * 获取默认模型
     */
    fun getDefaultModel(): String
    
    /**
     * 创建聊天请求
     */
    fun createChatRequest(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
        stream: Boolean = false
    ): ApiRequest
    
    /**
     * 解析聊天响应（阻塞式）
     */
    fun parseChatResponse(response: ApiResponse): String?
    
    /**
     * 解析流式响应
     */
    fun parseStreamResponse(data: String): String?
    
    /**
     * 检查响应是否完成
     */
    fun isResponseComplete(data: String): Boolean
    
    /**
     * 获取API端点
     */
    fun getApiEndpoint(): String
    
    /**
     * 获取流式API端点
     */
    fun getStreamApiEndpoint(): String
    
    /**
     * 获取认证头
     */
    fun getAuthHeaders(apiKey: String): Map<String, String>
    
    /**
     * 转换消息格式（如果需要）
     */
    fun convertMessages(messages: List<ApiRequest.Message>): Any {
        return messages
    }
}
