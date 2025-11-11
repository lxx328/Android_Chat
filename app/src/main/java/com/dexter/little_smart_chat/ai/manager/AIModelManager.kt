package com.dexter.little_smart_chat.ai.manager

import com.dexter.little_smart_chat.ai.adapter.AIModelAdapter
import com.dexter.little_smart_chat.ai.adapter.AnthropicAdapter
import com.dexter.little_smart_chat.ai.adapter.GoogleGeminiAdapter
import com.dexter.little_smart_chat.ai.adapter.OpenAIAdapter
import com.dexter.little_smart_chat.network.manager.NetworkManager
import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

/**
 * AI模型管理器
 * 统一管理不同的大模型，提供响应式接口
 */
class AIModelManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AIModelManager? = null
        
        fun getInstance(): AIModelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIModelManager().also { INSTANCE = it }
            }
        }
    }
    
    private val networkManager = NetworkManager.getInstance()
    private val adapters = mapOf(
        "openai" to OpenAIAdapter(),
        "anthropic" to AnthropicAdapter(),
        "google" to GoogleGeminiAdapter()
    )
    
    private var currentProvider: String = "openai"
    private var currentAdapter: AIModelAdapter = adapters["openai"]!!
    
    /**
     * 初始化AI模型管理器
     */
    fun initialize(apiKey: String, provider: String = "openai") {
        currentProvider = provider
        currentAdapter = adapters[provider] ?: adapters["openai"]!!
        networkManager.initialize(apiKey, provider)
    }
    
    /**
     * 获取支持的模型列表
     */
    fun getSupportedModels(): List<String> {
        return currentAdapter.getSupportedModels()
    }
    
    /**
     * 阻塞式聊天请求
     */
    suspend fun chatCompletion(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double = 0.7,
        maxTokens: Int? = null
    ): String? {
        return try {
            val request = currentAdapter.createChatRequest(
                messages = messages,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens,
                stream = false
            )
            
            val response = networkManager.chatCompletion(request)
            response?.let { apiResponse ->
                currentAdapter.parseChatResponse(apiResponse)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 流式聊天请求 - 响应式Flow
     */
    fun chatCompletionStream(
        messages: List<ApiRequest.Message>,
        model: String,
        temperature: Double = 0.7,
        maxTokens: Int? = null
    ): Flow<String> = callbackFlow {
        val request = currentAdapter.createChatRequest(
            messages = messages,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true
        )
        
        var isComplete = false
        
        networkManager.chatCompletionStream(
            request = request,
            onChunk = { data ->
                if (!isComplete) {
                    val parsedContent = currentAdapter.parseStreamResponse(data)
                    if (parsedContent != null) {
                        trySend(parsedContent)
                    }
                    
                    if (currentAdapter.isResponseComplete(data)) {
                        isComplete = true
                        close()
                    }
                }
            },
            onComplete = {
                if (!isComplete) {
                    isComplete = true
                    close()
                }
            },
            onError = { error ->
                close(error)
            }
        )
        
        awaitClose { 
            // Cleanup if needed
        }
    }
    
    /**
     * 语音转文字
     */
    suspend fun speechToText(
        audioFile: java.io.File,
        model: String = "whisper-1",
        language: String? = null
    ): String? {
        return try {
            // TODO: 实现语音转文字的逻辑
            // 由于不同API的语音转文字接口可能不同，需要根据currentProvider来处理
            null // 暂时返回null，等待实现
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 文字转语音
     */
    suspend fun textToSpeech(
        text: String,
        model: String = "tts-1",
        voice: String = "alloy"
    ): ByteArray? {
        return try {
            // TODO: 实现文字转语音的逻辑
            null // 暂时返回null，等待实现
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 健康检查
     */
    suspend fun healthCheck(): Boolean {
        return try {
            networkManager.healthCheck()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 切换模型提供商
     */
    fun switchProvider(provider: String) {
        if (adapters.containsKey(provider)) {
            currentProvider = provider
            currentAdapter = adapters[provider]!!
        }
    }
    
    /**
     * 获取当前提供商
     */
    fun getCurrentProvider(): String {
        return currentProvider
    }
    
    /**
     * 获取当前适配器
     */
    fun getCurrentAdapter(): AIModelAdapter {
        return currentAdapter
    }
} 