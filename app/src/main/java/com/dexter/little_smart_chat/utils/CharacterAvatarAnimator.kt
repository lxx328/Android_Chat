package com.dexter.little_smart_chat.utils

import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.dexter.little_smart_chat.R

/**
 * 角色头像动画管理器
 * 用于控制聊天界面中角色头像的动画效果
 */
class CharacterAvatarAnimator(private val avatarView: ImageView,  @DrawableRes val resId: Int) {

    private var isAnimating = false
    private var frameAnimation: AnimationDrawable? = null

    /**
     * 开始角色头像帧动画
     * 在AI回复开始时调用
     */
    fun startThinkingAnimation() {
        if (isAnimating) return

        isAnimating = true

        // 设置帧动画资源
//        avatarView.setImageResource(R.drawable.avatar_frame_animation)
        avatarView.setImageResource(resId)
        // 获取并启动帧动画
        frameAnimation = avatarView.drawable as? AnimationDrawable
        frameAnimation?.start()
    }

    /**
     * 停止角色头像动画
     * 在AI回复结束或用户中断时调用
     */
    fun stopThinkingAnimation() {
        if (!isAnimating) return

        // 停止帧动画
        frameAnimation?.stop()
        frameAnimation = null

        isAnimating = false
    }

    /**
     * 检查是否正在动画中
     */
    fun isAnimating(): Boolean {
        return isAnimating
    }
}