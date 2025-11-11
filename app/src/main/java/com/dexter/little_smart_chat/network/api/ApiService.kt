package com.dexter.little_smart_chat.network.api

import com.dexter.little_smart_chat.network.model.ApiRequest
import com.dexter.little_smart_chat.network.model.ApiResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 统一的API服务接口
 * 支持阻塞和流式响应
 */
interface ApiService {
    
    /**
     * 阻塞式聊天请求
     */
    @POST("api/v1/chat")
    suspend fun chatCompletion(@Body request: ApiRequest): Response<ApiResponse>
    
    /**
     * 流式聊天请求
     */
    @Streaming
    @POST("api/v1/chat/stream")
    suspend fun chatCompletionStream(@Body request: ApiRequest): Response<ResponseBody>
    
    /**
     * 语音转文字
     */
    @POST("v1/audio/transcriptions")
    @Multipart
    suspend fun speechToText(
        @Part("file") file: okhttp3.MultipartBody.Part,
        @Part("model") model: String,
        @Part("language") language: String? = null
    ): Response<ApiResponse>
    
    /**
     * 文字转语音
     */
    @POST("v1/audio/speech")
    suspend fun textToSpeech(@Body request: ApiRequest): Response<ResponseBody>
    
    /**
     * 获取模型列表
     */
    @GET("v1/models")
    suspend fun getModels(): Response<ApiResponse>
    
    /**
     * 健康检查
     */
    @GET("health")
    suspend fun healthCheck(): Response<ApiResponse>
} 