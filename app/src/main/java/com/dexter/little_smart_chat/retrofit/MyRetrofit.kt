package com.dexter.little_smart_chat.retrofit

import android.content.Context
import android.util.Log
import com.dexter.h20_local_ai_demo.utils.NetworkUtils
import com.dexter.little_smart_chat.contants.Constants
import com.dexter.little_smart_chat.retrofit.api.ApiServicePost
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.Charset

/**
 * Created by Dexter on 05/08/2022.
 * 一个封装okhttp的请求类
 */
object MyRetrofit {
    private val httpLoggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    // 远程
    private fun createApiService(baseUrl: String): ApiServicePost {
        val client = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(AuthInterceptor("your_token"))
            .build()
        return Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiServicePost::class.java)
    }

    // 获取合适的 ApiService
    fun getApiService(context: Context): ApiServicePost {
        val baseUrl = if (NetworkUtils.isNetworkAvailable(context)) {
            Constants.CHAT_URL
        } else {
            Constants.CHAT_URL_LOCAL
        }
        return createApiService(baseUrl)
    }

    // 你可以保留原有的 apiService 作为默认
    val apiService: ApiServicePost by lazy {
        createApiService(Constants.CHAT_URL)
    }

}