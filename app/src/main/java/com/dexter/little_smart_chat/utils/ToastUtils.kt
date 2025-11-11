package com.dexter.little_smart_chat.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Toast工具类
 * 统一管理应用中的提示信息
 */
object ToastUtils {
    
    private var currentToast: Toast? = null
    
    /**
     * 显示短时间Toast
     */
    fun showShort(context: Context, message: String) {
        cancelCurrent()
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }
    
    /**
     * 显示短时间Toast（资源ID）
     */
    fun showShort(context: Context, @StringRes messageId: Int) {
        showShort(context, context.getString(messageId))
    }
    
    /**
     * 显示长时间Toast
     */
    fun showLong(context: Context, message: String) {
        cancelCurrent()
        currentToast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        currentToast?.show()
    }
    
    /**
     * 显示长时间Toast（资源ID）
     */
    fun showLong(context: Context, @StringRes messageId: Int) {
        showLong(context, context.getString(messageId))
    }
    
    /**
     * 显示成功提示
     */
    fun showSuccess(context: Context, message: String) {
        showShort(context, "✅ $message")
    }
    
    /**
     * 显示错误提示
     */
    fun showError(context: Context, message: String) {
        showShort(context, "❌ $message")
    }
    
    /**
     * 显示警告提示
     */
    fun showWarning(context: Context, message: String) {
        showShort(context, "⚠️ $message")
    }
    
    /**
     * 显示信息提示
     */
    fun showInfo(context: Context, message: String) {
        showShort(context, "ℹ️ $message")
    }
    
    /**
     * 取消当前Toast
     */
    fun cancelCurrent() {
        currentToast?.cancel()
        currentToast = null
    }
    
    /**
     * 录音相关的提示信息
     */
    object Recording {
        fun tooShort(context: Context) = showWarning(context, "录制时间太短，请长按录制")
        fun started(context: Context) = showInfo(context, "开始录音...")
        fun stopped(context: Context) = showInfo(context, "录音已停止")
        fun cancelled(context: Context) = showInfo(context, "录音已取消")
        fun saved(context: Context) = showSuccess(context, "录音已保存")
        fun failed(context: Context) = showError(context, "录音失败")
        fun invalid(context: Context) = showError(context, "录音文件无效")
        fun permissionDenied(context: Context) = showError(context, "需要录音权限")
    }
    
    /**
     * 网络相关的提示信息
     */
    object Network {
        fun connecting(context: Context) = showInfo(context, "正在连接...")
        fun connected(context: Context) = showSuccess(context, "连接成功")
        fun disconnected(context: Context) = showWarning(context, "网络连接断开")
        fun error(context: Context) = showError(context, "网络错误")
        fun timeout(context: Context) = showError(context, "连接超时")
    }
    
    /**
     * AI相关的提示信息
     */
    object AI {
        fun thinking(context: Context) = showInfo(context, "AI正在思考...")
        fun responseReceived(context: Context) = showSuccess(context, "收到AI回复")
        fun error(context: Context) = showError(context, "AI服务错误")
        fun apiKeyMissing(context: Context) = showError(context, "请先配置API密钥")
    }
} 