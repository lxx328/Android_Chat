package com.dexter.little_smart_chat.weight

import android.content.Context
import android.hardware.SensorManager
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import cn.jzvd.Jzvd
import cn.jzvd.JzvdStd
import com.dexter.little_smart_chat.R

class JusticeJzPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private lateinit var jzvdStd: JzvdStd

    override fun onFinishInflate() {
        super.onFinishInflate()
        val view = inflate(context, R.layout.video_online_player_fragment, this)// inflate的返回是对应布局的根布局
        jzvdStd = view.findViewById(R.id.jzvd_std)
    }

    fun setUp(url: String, title: String) {
        jzvdStd.setUp(url, title, Jzvd.SCREEN_NORMAL)
    }

    fun startPlay() {
        jzvdStd.startVideo()
    }

    fun pausePlay() {
        jzvdStd.onStatePause()
    }

    fun resumePlay() {
        jzvdStd.onStatePlaying()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Jzvd.releaseAllVideos()
    }
}