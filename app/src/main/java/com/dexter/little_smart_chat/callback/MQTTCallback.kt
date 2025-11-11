package com.dexter.little_smart_chat.callback

import org.eclipse.paho.mqttv5.common.MqttMessage

interface MQTTCallback {
    fun onMQTTMessage(topic: String?, message: MqttMessage?)
}