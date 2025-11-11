package com.dexter.little_smart_chat.mvvm.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexter.little_smart_chat.MyApplication
import com.dexter.little_smart_chat.data.ConfigRequest
import com.dexter.little_smart_chat.data.MqttData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.bind.DateTypeAdapter
import com.google.gson.reflect.TypeToken
import com.xctech.advertise.mvvm.utils.GeneralUtils
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.Date

class MqServiceViewModel: ViewModel(){

    companion object {
        private const val TAG = "AdvertiseViewModel"
        private const val MSG = "msg"
        private const val CODE = "code"
        private const val DATA = "data"
        private const val MQTT_TYPE  = "mqtt"

    }

    private val model = MqttServiceModel()
    //MQTT基本数据采用
    private val _mqttData = MutableLiveData<MqttData>()
    val mqttData: MutableLiveData<MqttData> = _mqttData
    // 加载状态 LiveData
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // 错误信息 LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // 初始化信息
    private val _initialized = MutableLiveData<Boolean>(false)
    val initialized: LiveData<Boolean> = _initialized

    /**
     * 获取MQTT数据
     */
    fun fetchMQTTData() {
        viewModelScope.launch {
            try {
                _loading.value = true

                val response = MyApplication.instance?.applicationContext?.let {
                    GeneralUtils.getSN(it)
                        ?.let { ConfigRequest(it, MQTT_TYPE) }?.let {
                            model.queryMqttInfo(
                                it
                            )
                        }
                }
                if (response != null) {
                    if (response.data == null) {
                        _error.value = "未获取到广告信息"
                        return@launch
                    }
                }

                if (response != null) {
                    if (response.code != 200) {
                        _error.value = response.message
                        return@launch
                    }
                }
                //2层data
                val responseData = response?.data
                if (responseData == null) {
                    _error.value = "未获取到广告信息"
                    return@launch
                }
                val jsonElement = JsonParser.parseString(responseData)
                val jsonObject = jsonElement.asJsonObject
                val mqttData = jsonObject.get("data")
                val mqttDataResult = Gson().fromJson(mqttData, MqttData::class.java)
                Log.d(TAG, "mqttData: $mqttDataResult")

                if (mqttDataResult != null) {
                    _mqttData.value = mqttDataResult // 使用新命名的变量
                } else {
                    _error.value = "MQTT数据解析失败"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message ?: "未知错误"
                Log.e("dx","获取广告信息发生异常：" + e.message)
            } finally {
                _loading.value = false
            }
        }
    }
    /**
     * 解析响应数据为指定类型对象
     *
     * @param responseData 响应数据，可以是String、JSONObject或ResponseBody
     * @param clazz 目标类类型
     * @return 解析后的类型对象，解析失败则返回null
     */
    private fun <T> parseResponseData(responseData: Any?, clazz: Class<T>): T? {
        if (responseData == null) {
            Log.w("JSON_PARSING", "Response data is null")
            return null
        }
            //获取responseData的data

        val gson = Gson()
        // 转换响应数据为字符串
        val jsonString = try {
            when (responseData) {
                is String -> responseData
                is JSONObject -> responseData.toString()
                is ResponseBody -> responseData.string()
                else -> {
                    Log.w("JSON_PARSING", "Unexpected data type: ${responseData.javaClass.name}")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e("JSON_PARSING", "Error converting response to string: ${e.message}", e)
            return null
        }

        // 验证JSON字符串
        if (jsonString.isBlank()) {
            Log.w("JSON_PARSING", "JSON string is empty")
            return null
        }

        // 尝试解析JSON
        return try {
            gson.fromJson(jsonString, clazz)
        } catch (e: Exception) {
            Log.e("JSON_PARSING", "Error parsing JSON: ${e.message}", e)
            null
        }
    }


}