package com.unisound.demo.util

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.unisound.cloud.SpeechRecognizer
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class FileAudio(val path: String, val mainHandler: Handler) {

    @Volatile
    private var isOpen = false

    private var thread: Thread? = null

    private var speech: WeakReference<SpeechRecognizer>? = null

    /**
     * 开始识别
     */
    fun open(speech: SpeechRecognizer) {
        this.speech = WeakReference(speech)

        if (!isOpen) {
            thread = thread {
                val file = File(path)

                val fis = FileInputStream(file)

                val buffer = ByteArray(1024)

                var len = -1;
                //文件读取结束，或则主动结束
                val recognizer = this.speech!!.get()
                while ((fis.read(buffer)).also {
                        len = it
                    } != -1 && isOpen) {
                    //写入需要发送的内容
                    recognizer!!.writeAudio(buffer, len)
                    //稍微延迟一下 100ms
                    SystemClock.sleep(50)
                }
                if (isOpen) {
                    mainHandler.post {
                        DemoPrint("读取文件结束，发送结束识别的指令")
                        val recognizer = this.speech!!.get()
                        recognizer!!.stopListening()
                    }
                }
                isOpen = false
            }
            isOpen = true
        }
    }

    /**
     * 主动结束识别。
     */
    fun close() {
        if (isOpen) {
            isOpen = false
            thread = null
        }
    }
}