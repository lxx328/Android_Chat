package com.dexter.little_smart_chat.mvvm.model

import android.content.Context
import android.util.Log
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.data.DifyChatMesRequest
import com.dexter.little_smart_chat.data.RequestResponse
import com.dexter.little_smart_chat.network.model.LocalApiRequest
import com.dexter.little_smart_chat.retrofit.MyRetrofit
import com.dexter.little_smart_chat.utils.OPUtils
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class SmartAgentModel (private val context: Context){

        companion object {
            const val keyPrefix = "Bearer "
            private const val REQUEST_TIMEOUT = 30_000L // 30秒超时
            private const val STREAM_TIMEOUT = 60_000L // 流式请求60秒超时
            private const val TAG = "AIChatModel"

            const val END_FLAG = "end-flow"//流结束标识
            const val ERROR_FLAG = "error-flow"//流结束标识
        }

        //创建一个原则性

        /**
         * 阻塞式回复 - 增强异常处理和超时控制
         */
        suspend fun sendMessageToModel(
            key: String,
            request: LocalApiRequest
        ): RequestResponse<String> {
            return try {
                // 添加超时控制
                val result = withTimeoutOrNull(REQUEST_TIMEOUT) {
                    val response = MyRetrofit.getApiService(context)
                        .postRequestObjectAsync_LLM(keyPrefix + key, request)

                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code()}: ${response.message()}")
                    }

                    val responseBody = response.body()
                        ?: throw IOException("Response body is null")

                    val jsonString = responseBody.string()
                    if (jsonString.isBlank()) {
                        throw IOException("Empty response body")
                    }

                    extractContentFromJson(jsonString)
                }

                result?.let {
                    RequestResponse(message = "success", code = 200, data = it)
                } ?: RequestResponse(message = "Request timeout", code = 408, data = null)

            } catch (e: SocketTimeoutException) {
                OPUtils.Logger.etf(TAG, "Socket timeout", e)
                RequestResponse(message = "网络超时，请稍后重试", code = 408, data = null)
            } catch (e: IOException) {
                OPUtils.Logger.etf(TAG, "IO error", e)
                RequestResponse(message = "网络连接失败，请检查网络设置", code = 500, data = null)
            } catch (e: JSONException) {
                OPUtils.Logger.etf(TAG, "JSON parse error", e)
                RequestResponse(message = "数据解析失败", code = 500, data = null)
            } catch (e: Exception) {
                OPUtils.Logger.etf(TAG, "Unexpected error", e)
                RequestResponse(message = "未知错误: ${e.message}", code = 500, data = null)
            }
        }

        /**
         * 流式回复 - 增强异常处理和资源管理
         */
        suspend fun sendMessageToModelStream(
            key: String,
            request: LocalApiRequest
        ): Flow<String> = flow {
            var reader: BufferedReader? = null
            try {
                val call = MyRetrofit.getApiService(context)
                    .postRequestObjectStream_LLM(keyPrefix + key, request)

                val response = withTimeoutOrNull(STREAM_TIMEOUT) {
                    call.execute()
                } ?: throw TimeoutException("Stream request timeout")

                if (!response.isSuccessful) {
                    OPUtils.Logger.etf(TAG, "Request failed: HTTP ${response.code()}",null)
                    emit("请求失败：HTTP ${response.code()}")
                    return@flow
                }

                val body = response.body()
                if (body == null) {
                    OPUtils.Logger.etf(TAG, "Response body is null",null)
                    emit("响应数据为空")
                    return@flow
                }

                reader = BufferedReader(InputStreamReader(body.byteStream(), "UTF-8"))
                var content = ""
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    try {
                        val delta = parseStreamContent(line!!)
                        if (delta.isNotEmpty()) {
                            when (delta) {
                                END_FLAG -> {
                                    emit(END_FLAG)
                                    break
                                }
                                ERROR_FLAG -> {
                                    emit(ERROR_FLAG)
                                    break
                                }
                                else -> {
                                    content += delta
                                    emit(content)
                                }
                            }


                        }
                    } catch (e: JSONException) {
                        OPUtils.Logger.wtf(TAG, "Failed to parse line: $line error: ${e.message}")
                        // 继续处理下一行，不中断整个流
                        continue
                    }
                }

            } catch (e: TimeoutException) {
                OPUtils.Logger.wtf(TAG, "Request timeout ：${e.message}")
                emit("请求超时，请稍后重试")
            } catch (e: IOException) {
                OPUtils.Logger.wtf(TAG, "Network error: ${e.message}")
                emit("网络连接中断")
            } catch (e: Exception) {
                OPUtils.Logger.wtf(TAG, "Unexpected error: ${e.message}")
                emit("发生错误: ${e.message}")
            } finally {
                // 确保资源释放
                try {
                    reader?.close()
                } catch (e: IOException) {
                    OPUtils.Logger.wtf(TAG, "Failed to close reader error: ${e.message}")
                }
            }
        }.flowOn(Dispatchers.IO)


    /**
     * 流式回复 - 增强异常处理和资源管理异步
     */

//    fun sendMessageStreamAsync(
//        key: String,
//        request: LocalApiRequest
//    ): Flow<String> = flow {
//        val call = MyRetrofit.getApiService(context)
//            .postRequestObjectStream_LLM(keyPrefix + key, request)
//
//
//        // 使用 channel 来连接 OkHttp 的异步回调和 Kotlin Flow
//        val channel = Channel<String>()
//
//        call.enqueue(object : retrofit2.Callback<ResponseBody> {
//            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                channel.trySend("网络连接中断").isSuccess
//                channel.close(t)
//            }
//
//            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
//                if (!response.isSuccessful) {
//                    channel.trySend("请求失败：HTTP ${response.code()}").isSuccess
//                    channel.close()
//                    return
//                }
//
//                val source = response.body()?.source()
//                if (source == null) {
//                    channel.trySend("响应数据为空").isSuccess
//                    channel.close()
//                    return
//                }
//                // 在一个单独的协程中处理流式数据
//                CoroutineScope(Dispatchers.IO).launch {
//                    try {
//                        while (!source.exhausted()) {
//                            val line = source.readUtf8LineStrict()
//                            if (line.isNotEmpty()) {
//                                val delta = parseStreamContentLLM(line)
//                                channel.send(delta) // 发送给 Flow
//                            }
//                        }
//                    } catch (e: Exception) {
//                        channel.close(e)
//                    } finally {
//                        source.close()
//                        channel.close()
//                    }
//                }
//            }
//        })
//
//        // 从 channel 接收数据并发送给 flow
//        for (value in channel) {
//            emit(value)
//        }
//    }

    /**
     * 流式回复 - 增强异常处理和资源管理
     */
    fun sendMessageStreamAsync(
        key: String,
        request: LocalApiRequest
    ): Flow<String> = flow {
        val call = MyRetrofit.getApiService(context)
            .postRequestObjectStream_LLM(keyPrefix + key, request)

        val channel = Channel<String>(Channel.UNLIMITED)

        call.enqueue(object : retrofit2.Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                runBlocking {
                    channel.send("网络连接中断")

                    channel.close(t)
                }
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!response.isSuccessful) {
                    runBlocking {
                        channel.send("请求失败：HTTP ${response.code()}")
                        channel.send(ERROR_FLAG)
                        channel.close()
                    }
                    return
                }

                val source = response.body()?.source()
                if (source == null) {
                    runBlocking {
                        channel.send("响应数据为空")
                        channel.send(ERROR_FLAG)
                        channel.close()
                    }
                    return
                }

                // 在 IO 协程中处理流式数据
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val contentBuilder = StringBuilder()
                        var previousContent = ""

                        while (!source.exhausted()) {
                            val line = source.readUtf8LineStrict()

                            Log.d("Dx++", "line: $line")
                            if (line.startsWith("data: ")) {
                                val content = parseStreamContentLLM(line)

                                when {
                                    content == "[STREAM_END]" -> {
                                        // 流结束
                                        channel.send(END_FLAG)
                                        channel.close()
                                        break
                                    }
                                    content.isNotEmpty() -> {
                                        // 检查是否是重复内容（如"正在生成回复..."）
                                        if (content != previousContent && !content.contains("正在生成回复")) {
                                            // 只发送新的内容增量
                                            if (content.startsWith(contentBuilder.toString())) {
                                                // 如果新内容包含了之前的内容，只发送增量部分
                                                val delta = content.substring(contentBuilder.length)
                                                if (delta.isNotEmpty()) {
                                                    channel.send(delta)
                                                }
                                                contentBuilder.clear()
                                                contentBuilder.append(content)
                                            } else {
                                                // 全新的内容
                                                channel.send(content)
                                                contentBuilder.append(content)
                                            }
                                            previousContent = content
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        channel.send(ERROR_FLAG)
                        channel.close(e)
                    } finally {
                        try {
                            source.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        channel.close()
                    }
                }
            }
        })

        // 从 channel 接收数据并发送给 flow
        try {
            for (value in channel) {
                emit(value)
            }
        } catch (e: Exception) {
            emit("流式传输异常: ${e.message}")
        }
    }

    /**
         * 提取 answer - 增强安全性
         */
        private fun extractContentFromJson(jsonString: String): String {
            return try {
                if (jsonString.isBlank()) return ""

                val json = JSONObject(jsonString)
                val answer = json.optString("answer", "")

                // 基本的内容安全检查
                if (answer.length > 10000) { // 限制内容长度
                    OPUtils.Logger.wtf(TAG, "Answer too long, truncating")
                    return answer.substring(0, 10000) + "..."
                }

                answer
            } catch (e: JSONException) {
                OPUtils.Logger.etf(TAG, "JSON parsing failed: $jsonString  error: ${e.message}",null)
                ""
            } catch (e: Exception) {
                OPUtils.Logger.etf(TAG, "Unexpected error in extractContentFromJson error: ${e.message}",null)
                ""
            }
        }

        /**
         * 解析流式返回 - 增强错误处理
         */
        private fun parseStreamContent(line: String): String {
            try {
                if (line.isBlank()) return ""

                val prefix = "data: {\"event\": \"message\""
                val endfix = "data: {\"event\": \"message_end\""
                val errorfix = "data: {\"event\": \"error\""
                if (line.startsWith(endfix)) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    val json = JSONObject(jsonPart)
                    if (json.has("conversation_id")) {
                        val conversationId = json.getString("conversation_id")
                        Log.d("dxx", "dify 获取conversation_id: $conversationId")
                    }
                    return END_FLAG}
                if (line.startsWith(errorfix)) {
                    OPUtils.Logger.etf(TAG, "dify 返回数据出现 Error: $line",null)
                    return ERROR_FLAG}
                if (!line.startsWith(prefix)) return ""

                val jsonPart = line.removePrefix("data: ").trim()
                if (jsonPart.isBlank()) return ""

                val json = JSONObject(jsonPart)
                val answer = json.optString("answer", "")

                // 内容安全检查
                return if (answer.length <= 10000) answer else {
                    OPUtils.Logger.wtf(TAG, "Stream content too long, truncating")
                    answer.substring(0, 10000)
                }

            } catch (e: JSONException) {
                OPUtils.Logger.wtf(TAG, "Failed to parse stream line: $line error: ${e.message}")
                return ""
            } catch (e: Exception) {
                OPUtils.Logger.etf(TAG, "Unexpected error in parseStreamContent error: ${e.message}",null)
                return ""
            }
        }

    /**
     * 解析流式内容
     * @param line SSE 格式的一行数据
     * @return 解析出的文本内容
     */
    private fun parseStreamContentLLM(line: String): String {
        try {
            // SSE 格式：只处理 data: 开头的行
            if (!line.startsWith("data: ")) {
                return ""
            }

            // 提取 JSON 数据
            val jsonStr = line.substring(6).trim()
            if (jsonStr.isEmpty()) {
                return ""
            }

            // 旧版本 Gson 的使用方式
            val parser = JsonParser()
            val jsonElement = parser.parse(jsonStr)

            if (!jsonElement.isJsonObject) {
                return ""
            }

            val jsonObject = jsonElement.asJsonObject

            // 检查是否是最终消息（完成状态）
            if (jsonObject.has("final") && jsonObject.get("final").asBoolean) {
                val status = jsonObject.getAsJsonObject("status")
                if (status?.get("state")?.asString == "completed") {
                    return "[STREAM_END]" // 特殊标记表示流结束
                }
            }

            // 处理状态更新消息
            if (jsonObject.has("kind") && jsonObject.get("kind").asString == "status-update") {
                val status = jsonObject.getAsJsonObject("status")
                val message = status?.getAsJsonObject("message")

                if (message != null && message.has("parts")) {
                    val conversionId = message.get("context_id")
                    if (conversionId != null && !conversionId.isJsonNull) {
                        MyApplication.instance?.setAi_conversion_id(conversionId.asString)
                    }
                    val parts = message.getAsJsonArray("parts")
                    if (parts != null && parts.size() > 0) {
                        val firstPart = parts.get(0).asJsonObject
                        if (firstPart.has("kind") && firstPart.get("kind").asString == "text") {
                            val text = firstPart.get("text")
                            if (text != null && !text.isJsonNull) {
                                return text.asString
                            }
                        }
                    }
                }
            }

            // 处理 artifact-update 消息
            if (jsonObject.has("kind") && jsonObject.get("kind").asString == "artifact-update") {
//                val artifact = jsonObject.getAsJsonObject("artifact")
//                if (artifact != null && artifact.has("parts")) {
//                    val parts = artifact.getAsJsonArray("parts")
//                    if (parts != null && parts.size() > 0) {
//                        val firstPart = parts.get(0).asJsonObject
//                        if (firstPart.has("kind") && firstPart.get("kind").asString == "text") {
//                            val text = firstPart.get("text")
//                            if (text != null && !text.isJsonNull) {
//                                return text.asString
//                            }
//                        }
//                    }
//                }
                //不做处理
                return ""
            }

            return ""
        } catch (e: Exception) {
            // 解析失败时返回空字符串
            e.printStackTrace()
            return ""
        }
    }
}