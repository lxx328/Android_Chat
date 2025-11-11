package com.dexter.little_smart_chat.mvvm.viewmodel

import android.util.Log
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.data.ConfigRequest
import com.dexter.little_smart_chat.data.DeviceInfo
import com.dexter.little_smart_chat.data.RequestResponse
import com.dexter.little_smart_chat.retrofit.MyRetrofit
import com.xctech.advertise.mvvm.utils.GeneralUtils
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException

class MqttServiceModel {
    companion object {
        const val TAG = "MqttServiceModel"
        private const val REQUEST_TIMEOUT = 30_000L // 30秒超时

    }



    suspend fun queryMqttInfo(
        request: ConfigRequest
    ): RequestResponse<String> {
        //post 请求获取实例然后调用api
        return try  {
            // 添加超时控制
            val result = withTimeoutOrNull(REQUEST_TIMEOUT) {
                val response =
                    MyApplication.instance?.applicationContext?.let {
                        GeneralUtils.getSN(it)?.let {
                            MyApplication.instance?.applicationContext?.let { it1 ->
                                MyRetrofit.getApiService(it1)
                                    .postRequestMqttMsg(it, DeviceInfo.getTenantId(),request)
                            }
                        }
                    }
                if (response != null) {
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code()}: ${response.message()}")
                    }
                }
                val responseBody = response?.body()
                    ?: throw IOException("Response body is null")

                val jsonString = responseBody.string()
                if (jsonString.isBlank()) {
                    throw IOException("Empty response body")
                }
                jsonString
            }

            result?.let {
                RequestResponse(message = "success", code = 200, data = it)
            } ?: RequestResponse(message = "Request timeout", code = 408, data = null)


        }catch (e: SocketTimeoutException) {
            Log.d(TAG, "Socket timeout", e)
            RequestResponse(message = "网络超时，请稍后重试", code = 408, data = null)
        } catch (e: IOException) {
            Log.d(TAG, "IO error", e)
            RequestResponse(message = "网络连接失败，请检查网络设置", code = 500, data = null)
        } catch (e: JSONException) {
            Log.d(TAG, "JSON parse error", e)
            RequestResponse(message = "数据解析失败", code = 500, data = null)
        } catch (e: Exception) {
            Log.d(TAG, "Unexpected error", e)
            RequestResponse(message = "未知错误: ${e.message}", code = 500, data = null)
        }




    }
}