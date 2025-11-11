package com.dexter.little_smart_chat.retrofit.api

import com.dexter.little_smart_chat.data.RequestResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiServicePost {
    @POST("api/{endpoint}")
    suspend fun postRequestAsync(
        @Path("endpoint") endpoint: String,
        @Body body: Map<String, Any>
    ): RequestResponse<Any>

    // app/src/main/java/com/dexter/h20_local_ai_demo/api/ApiServicePost.kt
    @POST("v1/chat/completions")
    suspend fun postRequestObjectAsync(@Body body: Any): ResponseBody


    @Streaming
    @POST("v1/chat/completions")
    fun postRequestObjectStream(
        @Body body: Any
    ): Call<ResponseBody>

    @Headers("Content-Type:application/json; charset=UTF-8")
    //dify
    @POST("api/v1/chat/stream")
    fun postRequestObjectAsync_LLM(
        @Header("Authorization") auth: String,
        @Body body: Any
    ): Response<ResponseBody>

    @Headers( "Content-Type:application/json;charset=UTF-8")
    @Streaming
    @POST("api/v1/chat/stream")
    fun postRequestObjectStream_LLM(
        @Header("Authorization") auth: String,
        @Body body: Any
    ): Call<ResponseBody>

    /**
     * 通过sn获取mqtt信息
     */
    @Headers("Content-Type: application/json;charset=UTF-8")
    @POST("admin-api/device/uploadConfig/getBySn")
    suspend fun postRequestMqttMsg(
        @Header("sn") sn: String,
        @Header("tenantId") tenantId: Int,
        @Body body: Any
    ): retrofit2.Response<ResponseBody>
}

