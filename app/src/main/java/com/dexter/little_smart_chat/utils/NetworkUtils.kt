package com.dexter.h20_local_ai_demo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.text.SimpleDateFormat

object NetworkUtils {
    private val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        //打印结果和当前的时间序列化 YY-MM-DD hh-mm-ss
        val result = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        Log.d("NetworkUtils", "isNetworkAvailable: $result , time: ${sf.format(System.currentTimeMillis())} " )
        return result
    }
}