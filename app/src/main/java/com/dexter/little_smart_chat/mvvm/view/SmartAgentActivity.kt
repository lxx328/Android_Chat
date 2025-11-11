package com.dexter.little_smart_chat.mvvm.view

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.R
import com.dexter.little_smart_chat.adapter.CharacterAdapter
import com.dexter.little_smart_chat.adapter.MemoAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.dexter.little_smart_chat.data.Character
import com.dexter.little_smart_chat.data.Memo
import com.dexter.little_smart_chat.utils.AudioRecorderManager
import com.dexter.little_smart_chat.utils.SystemStatusManager
import com.dexter.little_smart_chat.service.SystemStatusService

class SmartAgentActivity : AppCompatActivity() {

    // View references
    private lateinit var timeText: TextView
    private lateinit var mainAgentView: View
    private lateinit var memoListView: View
    private lateinit var interactionButton: View
    private lateinit var characterDrawer: View
    private lateinit var helpModal: View
    private lateinit var statusMessage: TextView

    // 新的交互按钮组件
    private lateinit var staticButton: View
    private lateinit var holdInteractionOverlay: CardView
    private lateinit var thinkingState: View
    private lateinit var recordingStatus: TextView
    private lateinit var cancelButtonCard: CardView
    private lateinit var memoButtonCard: CardView
    private lateinit var recordingWaveformIcon: ImageView

    // 系统状态组件
    private lateinit var wifiIcon: ImageView
    private lateinit var batteryIcon: ImageView
    private lateinit var systemStatusManager: SystemStatusManager

    // State variables
    private var isRecording = false
    private var currentView = ViewState.HOME
    private var hoveredAction: RecordingAction? = null
    private val handler = Handler(Looper.getMainLooper())

    // Recording variables
    private lateinit var audioRecorderManager: AudioRecorderManager
    private var recordingFilePath: String? = null

    // Last MOVE position (screen coords)
    private var lastMoveRawX: Float? = null
    private var lastMoveRawY: Float? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    // Data
    private val characters = listOf(
        Character(1, "Aria (Default)", "👩‍🎤", "Creative and energetic"),
        Character(2, "Leo", "👨‍🔬", "Analytical and precise"),
        Character(3, "Maya", "👩‍🎨", "Artistic and thoughtful")
    )

    private val memos = mutableListOf(
        Memo(1, "Grocery List", "Milk, eggs, bread, and apples.", "2024-05-10"),
        Memo(2, "Meeting Notes", "Discuss Q3 goals and roadmap.", "2024-05-09"),
        Memo(3, "Idea for new feature", "Implement dark mode toggle.", "2024-05-08")
    )

    private var selectedCharacterId = 1

    enum class ViewState {
        HOME, MEMOS
    }

    enum class RecordingAction {
        CANCEL, SAVE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_main)

        initializeViews()
        setupListeners()
        setupRecyclerViews()
        updateTime()
        startTimeUpdater()

        // 检查录音权限
        checkPermissions()

        // 启动系统状态服务
        startSystemStatusService()
    }

    private fun initializeViews() {
        timeText = findViewById(R.id.timeText)
        mainAgentView = findViewById(R.id.mainAgentView)
        memoListView = findViewById(R.id.memoListView)
        interactionButton = findViewById(R.id.interactionButton)
        characterDrawer = findViewById(R.id.characterDrawer)
        helpModal = findViewById(R.id.helpModal)
        statusMessage = findViewById(R.id.statusMessage)

        // 初始化新的交互按钮组件
        staticButton = findViewById(R.id.staticButton)
        holdInteractionOverlay = findViewById(R.id.holdInteractionOverlay)
        thinkingState = findViewById(R.id.thinkingState)
        recordingStatus = findViewById(R.id.recordingStatus)
//        cancelButtonCard = findViewById(R.id.cancelButtonCard)
//        memoButtonCard = findViewById(R.id.memoButtonCard)
        recordingWaveformIcon = findViewById(R.id.recordingWaveformIcon)

        // 初始化录音管理器
        audioRecorderManager = AudioRecorderManager(this)

        // 初始化系统状态组件
        wifiIcon = findViewById(R.id.wifiIcon)
        batteryIcon = findViewById(R.id.batteryIcon)
        systemStatusManager = SystemStatusManager(this, timeText, wifiIcon, batteryIcon)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        // Menu buttons - 为整个布局和图标都设置点击监听
        findViewById<View>(R.id.btnSwitchCharacter).setOnClickListener {
            showCharacterDrawer()
        }
        findViewById<View>(R.id.layout_switch_character).setOnClickListener {
            showCharacterDrawer()
        }

        findViewById<View>(R.id.btnViewMemos).setOnClickListener {
            showMemoList()
        }
        findViewById<View>(R.id.layout_query_notes).setOnClickListener {
            showMemoList()
        }

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            showHelpModal()
        }
        findViewById<View>(R.id.layout_tips).setOnClickListener {
            showHelpModal()
        }

        // Back button in memo list
        findViewById<View>(R.id.btnBack).setOnClickListener {
            showHome()
        }

        // Character drawer overlay
        findViewById<View>(R.id.drawerOverlay).setOnClickListener {
            hideCharacterDrawer()
        }

        // Help modal close
        findViewById<View>(R.id.btnCloseHelp).setOnClickListener {
            hideHelpModal()
        }

        findViewById<View>(R.id.btnGotIt).setOnClickListener {
            hideHelpModal()
        }

        // 静态按钮触摸处理
        staticButton.setOnTouchListener { _, event ->
            handleStaticButtonTouch(event)
            true
        }
    }

    private fun handleStaticButtonTouch(event: MotionEvent): Boolean {
        var isLongPress = false
        var pressStartTime = 0L
        val longPressThreshold = 300L // 300ms长按阈值，提高灵敏度

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastMoveRawX = null
                lastMoveRawY = null
                hoveredAction = null
                val pressDuration = System.currentTimeMillis() - pressStartTime
                if (pressDuration >= longPressThreshold && !isLongPress && !isRecording) {
                    isLongPress = true
                    startRecording()
                }


            }
            MotionEvent.ACTION_MOVE -> {
                if (isRecording) {
                    checkHoverState(event)
                }

                lastMoveRawX = event.rawX
                lastMoveRawY = event.rawY

            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val pressDuration = System.currentTimeMillis() - pressStartTime
                if (isRecording) {
                    val rawX = lastMoveRawX ?: event.rawX
                    val rawY = lastMoveRawY ?: event.rawY

                    val cancelLocation = IntArray(2)
                    val memoLocation = IntArray(2)
                    cancelButtonCard.getLocationOnScreen(cancelLocation)
                    memoButtonCard.getLocationOnScreen(memoLocation)

                    val actionLocal = when {
                        isPointInView(rawX, rawY, cancelButtonCard, cancelLocation) -> 1 // 取消
                        isPointInView(rawX, rawY, memoButtonCard, memoLocation) -> 2   // 保存备忘
                        else -> 3 // 发送
                    }

                    stopRecording(actionLocal )
                } else if (pressDuration < longPressThreshold) {
                    // 单击提示
                    Toast.makeText(this, "录制时间太短，请长按录制", Toast.LENGTH_SHORT).show()
                }
                // 重置
                lastMoveRawX = null
                lastMoveRawY = null
            }
        }
        return true
    }

    private fun checkHoverState(event: MotionEvent) {
        val location = IntArray(2)
        val cancelLocation = IntArray(2)
        val memoLocation = IntArray(2)

        staticButton.getLocationOnScreen(location)
        cancelButtonCard.getLocationOnScreen(cancelLocation)
        memoButtonCard.getLocationOnScreen(memoLocation)

        val x = location[0] + event.x
        val y = location[1] + event.y

        hoveredAction = when {
            isPointInView(x, y, cancelButtonCard, cancelLocation) -> {
                updateHoverState(RecordingAction.CANCEL)
                RecordingAction.CANCEL
            }
            isPointInView(x, y, memoButtonCard, memoLocation) -> {
                updateHoverState(RecordingAction.SAVE)
                RecordingAction.SAVE
            }
            else -> {
                updateHoverState(null)
                null
            }
        }
    }

    private fun isPointInView(x: Float, y: Float, view: View, location: IntArray): Boolean {
        return x >= location[0] && x <= location[0] + view.width &&
                y >= location[1] && y <= location[1] + view.height
    }

    private fun updateHoverState(action: RecordingAction?) {
        // Update cancel button appearance
        cancelButtonCard.scaleX = if (action == RecordingAction.CANCEL) 1.25f else 1f
        cancelButtonCard.scaleY = if (action == RecordingAction.CANCEL) 1.25f else 1f
        cancelButtonCard.setCardBackgroundColor(
            getColor(if (action == RecordingAction.CANCEL) R.color.red_500 else android.R.color.white)
        )

        // Update memo button appearance
        memoButtonCard.scaleX = if (action == RecordingAction.SAVE) 1.25f else 1f
        memoButtonCard.scaleY = if (action == RecordingAction.SAVE) 1.25f else 1f
        memoButtonCard.setCardBackgroundColor(
            getColor(if (action == RecordingAction.SAVE) R.color.green_500 else android.R.color.white)
        )

        // Update recording status text
        recordingStatus.text = when (action) {
            RecordingAction.CANCEL -> "左滑取消"
            RecordingAction.SAVE -> "右滑动生成备忘"
            null -> "松开发送"
        }
    }

    private fun startRecording() {
        if (isRecording) return // 防止重复录制

        // 检查录音权限
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            return
        }

        try {
            // 使用AudioRecorderManager开始录音
            recordingFilePath = audioRecorderManager.startRecording()

            if (recordingFilePath != null) {
                isRecording = true

                // Hide static button with animation
                val exitAnimation = AnimationUtils.loadAnimation(this, R.anim.button_exit_animation)
                staticButton.startAnimation(exitAnimation)
                exitAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        staticButton.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                // Show recording overlay with enter animation
                holdInteractionOverlay.visibility = View.VISIBLE
                val enterAnimation = AnimationUtils.loadAnimation(this, R.anim.button_enter_animation)
                holdInteractionOverlay.startAnimation(enterAnimation)

                // Start waveform animation
                startWaveformAnimation()

                Log.d("SmartAgentActivity", "Recording started: $recordingFilePath")
            } else {
                Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show()
            Log.e("SmartAgentActivity", "Recording failed: ${e.message}")
        }
    }

    private fun stopRecording(local: Int) {
        if (!isRecording) return // 防止重复停止
        try {
            // 使用AudioRecorderManager停止录音
//            val savedFilePath = audioRecorderManager.stopRecording()
//            isRecording = false
//
//            if (savedFilePath != null) {
//                recordingFilePath = savedFilePath
//
//                // Determine action
//                val action = when (hoveredAction) {
//                    RecordingAction.CANCEL -> {
//                        cancelRecording()
//                        "cancel"
//                    }
//                    RecordingAction.SAVE -> {
//                        saveRecording()
//                        "save"
//                    }
//                    null -> {
//                        processRecording()
//                        "send"
//                    }
//                }
//            } else {
//                Toast.makeText(this, "录音保存失败", Toast.LENGTH_SHORT).show()
//            }

            when (local) {
                1 -> {
                    //1位取消发送
                    cancelRecording()
                }
                2 -> {
                    //2位保存备忘录
                    saveRecording()
                }
                3 -> {
                    //3位发送
                    processRecording()
                }
                else -> {}
            }

            // Stop waveform animation
            stopWaveformAnimation()

            // Hide recording overlay with exit animation
            val exitAnimation = AnimationUtils.loadAnimation(this, R.anim.button_exit_animation)
            holdInteractionOverlay.startAnimation(exitAnimation)
            exitAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    holdInteractionOverlay.visibility = View.GONE

                    // Show static button with enter animation
                    staticButton.visibility = View.VISIBLE
                    val enterAnimation = AnimationUtils.loadAnimation(this@SmartAgentActivity, R.anim.button_enter_animation)
                    staticButton.startAnimation(enterAnimation)
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            Log.d("SmartAgentActivity", "Recording stopped: $recordingFilePath")

        } catch (e: Exception) {
            // 即使停止失败，也要重置状态
            isRecording = false
            staticButton.visibility = View.VISIBLE
            holdInteractionOverlay.visibility = View.GONE

            // Stop waveform animation
            stopWaveformAnimation()

            Toast.makeText(this, "录音停止失败", Toast.LENGTH_SHORT).show()
            Log.e("SmartAgentActivity", "Stop recording failed: ${e.message}")
        }

        // Reset hover states
        updateHoverState(null)
        hoveredAction = null
    }

    private fun showStatusMessage(message: String) {
        statusMessage.text = message
        statusMessage.visibility = View.VISIBLE
        statusMessage.alpha = 0f
        statusMessage.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Hide after 3 seconds
        handler.postDelayed({
            statusMessage.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    statusMessage.visibility = View.GONE
                }
                .start()
        }, 3000)
    }

    // 显示思考状态
    private fun showThinkingState() {
        staticButton.visibility = View.GONE
        holdInteractionOverlay.visibility = View.GONE
        thinkingState.visibility = View.VISIBLE
        thinkingState.alpha = 0f
        thinkingState.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    // 隐藏思考状态
    private fun hideThinkingState() {
        thinkingState.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                thinkingState.visibility = View.GONE
                staticButton.visibility = View.VISIBLE
            }
            .start()
    }

    // 取消录音
    private fun cancelRecording() {
        audioRecorderManager.cancelRecording()
        recordingFilePath = null
        showStatusMessage("录音已取消")
        Toast.makeText(this, "录音已取消", Toast.LENGTH_SHORT).show()
    }

    // 保存录音到备忘录
    private fun saveRecording() {
        if (recordingFilePath != null) {
            val recordingInfo = audioRecorderManager.getRecordingInfo(recordingFilePath!!)
            val memo = Memo(
                id = memos.size + 1,
                title = "语音备忘录 ${memos.size + 1}",
                content = "录音文件: ${recordingInfo?.name ?: "未知文件"}，时长: ${formatDuration(recordingInfo?.duration ?: 0)}，保存时间: ${getCurrentDateTime()}",
                date = getCurrentDateTime()
            )
            memos.add(memo)

            showStatusMessage("录音已保存到备忘录")
            Toast.makeText(this, "备忘录已保存!", Toast.LENGTH_SHORT).show()

            Log.d("SmartAgentActivity", "Recording saved to memo: $recordingFilePath")
        }
    }

    // 处理录音（发送给AI）
    private fun processRecording() {
        if (recordingFilePath != null) {
            showStatusMessage("消息已发送给智能助手")

            // 显示思考状态
            handler.postDelayed({
                showThinkingState()
                // 3秒后隐藏思考状态
                handler.postDelayed({
                    hideThinkingState()
                }, 3000)
            }, 500)

            Log.d("SmartAgentActivity", "Recording processed: $recordingFilePath")
        }
    }

    // 格式化时长
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}分${remainingSeconds}秒"
        } else {
            "${remainingSeconds}秒"
        }
    }

    // 启动系统状态服务
    private fun startSystemStatusService() {
        val intent = Intent(this, SystemStatusService::class.java)
        startService(intent)
    }

    // 停止系统状态服务
    private fun stopSystemStatusService() {
        val intent = Intent(this, SystemStatusService::class.java)
        stopService(intent)
    }

    // 获取当前日期时间
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // 权限请求结果处理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "麦克风权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要麦克风权限才能进行录音", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 检查权限
    private fun checkPermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        }
    }

    // 在Activity销毁时释放资源
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        audioRecorderManager.release()
        systemStatusManager.release()
        stopSystemStatusService()
    }

    private fun showHome() {
        currentView = ViewState.HOME
        mainAgentView.visibility = View.VISIBLE
        memoListView.visibility = View.GONE
        interactionButton.visibility = View.VISIBLE
    }

    private fun showMemoList() {
        currentView = ViewState.MEMOS
        mainAgentView.visibility = View.GONE
        memoListView.visibility = View.VISIBLE
        interactionButton.visibility = View.GONE
    }

    private fun showCharacterDrawer() {
        characterDrawer.visibility = View.VISIBLE
        val drawerContent = findViewById<View>(R.id.drawerContent)

        // Animate drawer sliding in
        drawerContent.translationX = -drawerContent.width.toFloat()
        drawerContent.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Fade in overlay
        findViewById<View>(R.id.drawerOverlay).apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun hideCharacterDrawer() {
        val drawerContent = findViewById<View>(R.id.drawerContent)

        // Animate drawer sliding out
        drawerContent.animate()
            .translationX(-drawerContent.width.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Fade out overlay
        findViewById<View>(R.id.drawerOverlay).animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                characterDrawer.visibility = View.GONE
            }
            .start()
    }

    private fun showHelpModal() {
        helpModal.visibility = View.VISIBLE
        helpModal.alpha = 0f
        helpModal.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideHelpModal() {
        helpModal.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                helpModal.visibility = View.GONE
            }
            .start()
    }

    private fun setupRecyclerViews() {
        // Character RecyclerView
        val characterRecyclerView = findViewById<RecyclerView>(R.id.characterRecyclerView)
        characterRecyclerView.layoutManager = LinearLayoutManager(this)
        characterRecyclerView.adapter = CharacterAdapter(characters, selectedCharacterId) { character ->
            selectedCharacterId = character.id
            updateSelectedCharacter(character)
            hideCharacterDrawer()
        }

        // Memo RecyclerView
        val memoRecyclerView = findViewById<RecyclerView>(R.id.memoRecyclerView)
        val emptyState = findViewById<TextView>(R.id.emptyState)

        memoRecyclerView.layoutManager = LinearLayoutManager(this)
//        memoRecyclerView.adapter = MemoAdapter(memos)

        // Show/hide empty state
        if (memos.isEmpty()) {
            memoRecyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            memoRecyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }

        // New memo button
        findViewById<View>(R.id.btnNewMemo).setOnClickListener {
            // Handle new memo creation
            showStatusMessage("New memo feature coming soon!")
        }

        // Settings button in drawer
        findViewById<View>(R.id.settingsButton).setOnClickListener {
            hideCharacterDrawer()
            showStatusMessage("Settings feature coming soon!")
        }
    }

    private fun updateSelectedCharacter(character: Character) {
        // Update main view with selected character
        val characterAvatar = findViewById<ImageView>(R.id.characterAvatar)
        when (character.id) {
            1 -> characterAvatar.setImageResource(R.mipmap.gril)
            2 -> characterAvatar.setImageResource(R.mipmap.boy)
            3 -> characterAvatar.setImageResource(R.mipmap.cloud)
        }

        // 更新角色名称显示
        findViewById<TextView>(R.id.tvSwitchCharacterNickname)?.text = character.name
    }

    /**
     * 开始波形动画
     */
    private fun startWaveformAnimation() {
        recordingWaveformIcon.visibility = View.VISIBLE
        recordingWaveformIcon.setImageResource(R.drawable.recording_wave_animation)

        // 延迟一帧启动动画，确保drawable已经设置
        recordingWaveformIcon.post {
            val frameAnimation = recordingWaveformIcon.drawable as? android.graphics.drawable.AnimationDrawable
            frameAnimation?.start()
        }

        // 添加按钮动画
        val buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.recording_button_animation)
        holdInteractionOverlay.startAnimation(buttonAnimation)
    }

    /**
     * 停止波形动画
     */
    private fun stopWaveformAnimation() {
        val frameAnimation = recordingWaveformIcon.drawable as? android.graphics.drawable.AnimationDrawable
        frameAnimation?.stop()

        recordingWaveformIcon.visibility = View.GONE
        holdInteractionOverlay.clearAnimation()
    }

    private fun updateTime() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = sdf.format(Date())
    }

    private fun startTimeUpdater() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 60000) // Update every minute
            }
        }, 60000)
    }


}