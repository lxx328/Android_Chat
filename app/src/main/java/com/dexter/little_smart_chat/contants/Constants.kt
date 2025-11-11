package com.dexter.little_smart_chat.contants

object Constants {
    const val CHAT_URL = "http://10.10.84.153:5011/"
    const val CHAT_URL_LOCAL = "http://127.0.0.1:5858/"//监听本地

    const val CHAT_MESSAGE_ENDPOINT = "v1/chat/completions"

    //Mqtt_topic
    const val MQTT_TOPIC_REFRESH_DATE = "advertisement/plan/update/"
    const val MQTT_TOPIC_REFRESH_DATE_BROADCAST= "refresh_base_data_Broadcast"
}