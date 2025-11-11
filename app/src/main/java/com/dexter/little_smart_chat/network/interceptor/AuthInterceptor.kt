package com.dexter.little_smart_chat.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 认证拦截器
 * 用于添加API密钥等认证信息
 */
class AuthInterceptor(
    private val provider: String,
    private val apiKey: String
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        // 根据不同的API提供商添加认证头
        when (provider.lowercase()) {
            "openai" -> {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("Content-Type", "application/json")
            }
            "anthropic" -> {
                requestBuilder.addHeader("x-api-key", apiKey)
                requestBuilder.addHeader("Content-Type", "application/json")
                requestBuilder.addHeader("anthropic-version", "2023-06-01")
            }
            "google" -> {
                // Google使用查询参数而不是头部
                val url = originalRequest.url.newBuilder()
                    .addQueryParameter("key", apiKey)
                    .build()
                requestBuilder.url(url)
                requestBuilder.addHeader("Content-Type", "application/json")
            }
            else -> {
                // 默认使用OpenAI格式
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("Content-Type", "application/json")
            }
        }
        
        return chain.proceed(requestBuilder.build())
    }
} 