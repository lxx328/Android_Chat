package com.dexter.little_smart_chat.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.StandardCharsets

/**
 * 日志拦截器
 * 用于记录网络请求和响应的详细信息
 */
class LoggingInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "NetworkLog"
        private const val MAX_BODY_LENGTH = 4096
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        // 记录请求信息
        logRequest(request)
        
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        
        // 记录响应信息并返回可能修改过的response
        return logResponse(response, endTime - startTime)
    }
    
    private fun logRequest(request: okhttp3.Request) {
        try {
            Log.d(TAG, "=== Request ===")
            Log.d(TAG, "URL: ${request.url}")
            Log.d(TAG, "Method: ${request.method}")
            Log.d(TAG, "Headers: ${request.headers}")
            
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyString = buffer.readString(StandardCharsets.UTF_8)
                if (bodyString.length <= MAX_BODY_LENGTH) {
                    Log.d(TAG, "Body: $bodyString")
                } else {
                    Log.d(TAG, "Body: ${bodyString.substring(0, MAX_BODY_LENGTH)}...")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging request: ${e.message}")
        }
    }
    
    private fun logResponse(response: Response, duration: Long): Response {
        return try {
            Log.d(TAG, "=== Response ===")
            Log.d(TAG, "URL: ${response.request.url}")
            Log.d(TAG, "Code: ${response.code}")
            Log.d(TAG, "Message: ${response.message}")
            Log.d(TAG, "Duration: ${duration}ms")
            Log.d(TAG, "Headers: ${response.headers}")
            
            response.body?.let { body ->
                val bodyString = body.string()
                if (bodyString.length <= MAX_BODY_LENGTH) {
                    Log.d(TAG, "Body: $bodyString")
                } else {
                    Log.d(TAG, "Body: ${bodyString.substring(0, MAX_BODY_LENGTH)}...")
                }
                
                // 重新创建ResponseBody，因为string()会消耗原始body
                val newBody = ResponseBody.create(body.contentType(), bodyString)
                response.newBuilder().body(newBody).build()
            } ?: response
        } catch (e: Exception) {
            Log.e(TAG, "Error logging response: ${e.message}")
            response
        }
    }
} 