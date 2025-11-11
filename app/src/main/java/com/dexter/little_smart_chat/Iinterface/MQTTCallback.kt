package com.xctech.advertise.mvvm.Iinterface

import org.eclipse.paho.mqttv5.common.MqttMessage

interface MQTTCallback {
    fun onMQTTMessage(topic: String?, message: MqttMessage?)
}