package com.unisound.demo.util

import android.util.Log
import com.unisound.cloud.SpeechRecognizer
import com.unisound.cloud.SpeechUtility
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap


const val CONFIG_PATH = "/sdcard/unisound/paas/"

//获取配置文件的key和value
/*
路径地址默认/sdcard/unisound/paas/

 */
fun loadConfigureFile(): HashMap<String, String> {
    val configureFile = File(CONFIG_PATH, "paas.properties")
    val props = Properties()
    val propertiesMap = HashMap<String, String>()
    if (configureFile.exists()) {
        var fis: FileInputStream? = null;
        try {
            fis = FileInputStream(configureFile)
            props.load(fis)
        } catch (e: Exception) {

        } finally {
            fis?.close()
        }
        val propertySet = props.entries
        for (value in propertySet) {
            propertiesMap.put(value.key.toString(), value.value.toString())
        }
    }
    return propertiesMap
}



/**
 * 加载配置文件的内容。在返回结果
 */
fun loadConfig(speechRecognizer: SpeechRecognizer): HashMap<String, String> {
    val map = loadConfigureFile()
    //设置param
    for (value in map.entries) {
        if (value.key.startsWith("asr_") && value.key.endsWith("_key")) {
            speechRecognizer.setParameter(value.key, value.value)
        }
    }
    //设置日志等级
    val value = map.get("log_level")
    value?.let{
        val intLevel = it.toIntOrNull()?:2
        SpeechUtility.setLogLevel(intLevel)
    }
    return map
}

fun DemoPrint(msg:String){
    Log .d("Demo", msg)
}