package com.dexter.little_smart_chat.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

//实现序列化
@Parcelize
data class MqttData(
    @SerializedName("topicArray")
    val topicArray: List<String> ,
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("broker")
    val broker: String
) : Parcelable

