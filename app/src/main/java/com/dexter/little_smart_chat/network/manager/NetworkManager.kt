package com.dexter.little_smart_chat.network.manager

import android.util.Log
import com.dexter.little_smart_chat.network.NetworkConfig
import com.dexter.little_smart_chat.network.api.ApiService
import com.dexter.little_smart_chat.network.interceptor.AuthInterceptor
import com.dexter.little_smart_chat.network.interceptor.LoggingInterceptor
import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 网络管理器
 * 统一管理AI模型的网络请求，支持阻塞和流式调用
 */
class NetworkManager private constructor() {
    
    companion object {
        private const val TAG = "NetworkManager"
        
        @Volatile
        private var INSTANCE: NetworkManager? = null
        
        fun getInstance(): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkManager().also { INSTANCE = it }
            }
        }

        //测试环境
        private const val TSET_URL = "http://10.10.84.153:5011/"
        //正式环境
        private const val PRODUCT_URL = "https://api.little-smart-chat.com/"
    }
    
    private var apiService: ApiService? = null
    private var currentProvider: String = "openai"
    private var currentApiKey: String = ""
    
    /**
     * 初始化网络管理器
     */
    fun initialize(apiKey: String, provider: String = "openai") {
        this.currentApiKey = apiKey
        this.currentProvider = provider
        this.apiService = createApiService(provider, apiKey)
        Log.d(TAG, "NetworkManager initialized for provider: $provider")
    }
    
    /**
     * 阻塞式聊天完成
     */
    suspend fun chatCompletion(request: ApiRequest): ApiResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: throw IllegalStateException("NetworkManager not initialized")
                val response = service.chatCompletion(request)
                
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e(TAG, "Chat completion failed: ${response.code()} ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat completion error: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 流式聊天完成
     */
    suspend fun chatCompletionStream(
        request: ApiRequest,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: throw IllegalStateException("NetworkManager not initialized")
                val response = service.chatCompletionStream(request)
                
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        processStreamResponse(responseBody, onChunk, onComplete, onError)
                    } ?: run {
                        onError(Exception("Empty response body"))
                    }
                } else {
                    onError(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Stream chat completion error: ${e.message}", e)
                onError(e)
            }
        }
    }
    
    /**
     * 语音转文字
     */
    suspend fun speechToText(audioFile: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: throw IllegalStateException("NetworkManager not initialized")
                // TODO: 实现语音转文字API调用
                Log.d(TAG, "Speech to text not implemented yet")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Speech to text error: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 文字转语音
     */
    suspend fun textToSpeech(text: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: throw IllegalStateException("NetworkManager not initialized")
                // TODO: 实现文字转语音API调用
                Log.d(TAG, "Text to speech not implemented yet")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Text to speech error: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 健康检查
     */
    suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: return@withContext false
                val response = service.healthCheck()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Health check error: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * 获取支持的模型列表
     */
    suspend fun getModels(): List<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val service = apiService ?: throw IllegalStateException("NetworkManager not initialized")
                val response = service.getModels()
                
                if (response.isSuccessful) {
                    // TODO: 解析模型列表响应
                    Log.d(TAG, "Get models not fully implemented yet")
                    null
                } else {
                    Log.e(TAG, "Get models failed: ${response.code()} ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get models error: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 创建API服务
     */
    private fun createApiService(provider: String, apiKey: String): ApiService {
//        val baseUrl = when (provider) {
//            "openai" -> "https://api.openai.com/"
//            "anthropic" -> "https://api.anthropic.com/"
//            "google" -> "https://generativelanguage.googleapis.com/"
//            else -> "https://api.openai.com/"
//        }
        val baseUrl = TSET_URL
        
        val client = OkHttpClient.Builder()
//            .addInterceptor(AuthInterceptor(provider, apiKey))
            .addInterceptor(LoggingInterceptor())
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * 处理流式响应
     */
    private fun processStreamResponse(
        responseBody: ResponseBody,
        onChunk: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            
            reader.use { bufferedReader ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let { chunk ->
                        if (chunk.isNotEmpty()) {
                            onChunk(chunk)
                        }
                    }
                }
            }
            
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing stream response: ${e.message}", e)
            onError(e)
        } finally {
            responseBody.close()
        }
    }
} 