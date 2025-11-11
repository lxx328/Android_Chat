package com.dexter.little_smart_chat.utils

import android.content.Context
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.dexter.little_smart_chat.MyApplication
import com.unisound.dictation.AsrEvent
import com.unisound.dictation.IAsrResultListener
import com.unisound.dictation.Option
import com.unisound.dictation.UnisoundDictation
import com.unisound.dictation.audio.AndroidRecordAudioSource
import java.security.Permission
import kotlin.properties.Delegates

object YZSOfflineASRUtils {
    var dictation: UnisoundDictation? = null

    private var isInit = false

    fun init( listener:IAsrResultListener) {
        if (isInit){
            return
        }
        dictation = UnisoundDictation(MyApplication.instance?.getAppContext(), AndroidRecordAudioSource())
        /**
         * 标点是否支持，注意打开标点，标点输出对性能有影响，差的CPU会影响输出速度。
         */
        dictation?.setOption(Option.ASR_OPTION_PUNCTUATION_ENABLED, true)
        //请把dic模型文件资源拷贝到这个目录下面
//        val asrmodel = applicationContext.getExternalFilesDir("dictationmodels")
//        dictation?.setOption(Option.ASR_OPTION_MODEL_PATH, asrmodel)
        dictation?.setOption(Option.ASR_OPTION_LANGUAGE, "cn")
        dictation?.setOption(Option.ASR_OPTION_PRINT_JNI_LOG,true)

        //语法文件
//        dictation?.setOption(Option.ASR_OPTION_LOAD_GRAMMAR_FILES,"grammar.dat")
//
//        dictation?.setOption(Option.ASR_OPTION_GRAMMAR_VERSION,"6")

        //数字处理，注意，如果开启导致不能断句，无法出标点
//        dictation?.setOption(Option.ASR_OPTION_POST_PROC_ENABLED, true)
        //尾部静音时间，默认0.5
//        dictation?.setOption(Option.ASR_OPTION_VAD_END_SIL_TIME,1.5)
        dictation?.setOption(Option.ASR_OPTION_SCENE_INFO, "do_post:false;use_sp:true;sp_min_sil_sec:0.5;sp_min_speech_sec:0.3;remove_tag:0;")
        //识别回调
//        dictation?.addListener(object : IAsrResultListener {
//            override fun onResult(event: Int, result: String?) {
//                //获取结果
//                Log.e("Dictation", "event:" + event + ",result：" + result);
//            }
//
//            override fun onEvent(event: Int, msg: String?) {
//                Log.e("Dictation", "Event:" + event)
//                if (event == AsrEvent.ASR_EVENT_OFFLINE_VAD_START) {
//                    Log.e("Dictation", "Event: ASR_EVENT_OFFLINE_VAD_START")
//                } else if (event == AsrEvent.ASR_EVENT_OFFLINE_VAD_END) {
//                    Log.e("Dictation", "Event: ASR_EVENT_OFFLINE_VAD_END")
//                    //断句后，可以cancel 来结束识别
//                }
//
//            }
//
//            override fun onError(error: Int, msg: String?) {
//                Log.e("Dictation", "error:" + error);
//            }
//        })
        dictation?.addListener(listener)
        dictation?.init()
        isInit = true
    }

    fun cancel() {
        if (!isInit){
             return
        }
        dictation?.cancel()
    }

    fun start(context: Context) {
        if (!isInit){
            return
        }
        //检查是否有权限
        if (OPUtils.PermissionChecker.checkPermissions(
               MyApplication.instance?.getAppContext(),
                arrayOf(OPUtils.PermissionChecker.RECORD_AUDIO)
            )
        ) {

            dictation!!.startAsr()
        } else {
            context.let {
                Toast.makeText(it, "请检查录音权限", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun release() {
        if (!isInit){
            return
        }
        dictation?.release()
        dictation = null
    }
}