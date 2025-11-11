package com.dexter.little_smart_chat

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dexter.little_smart_chat.adapter.CharacterAdapter
import com.dexter.little_smart_chat.adapter.MemoAdapter
import com.dexter.little_smart_chat.data.Character
import com.dexter.little_smart_chat.data.Memo
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import android.widget.FrameLayout
import android.widget.Toast
import android.util.Log
import android.view.WindowManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.dexter.little_smart_chat.utils.StatusBarUpdater
import com.dexter.little_smart_chat.service.SystemStatusService
import com.dexter.little_smart_chat.audio.AudioRecorderManager
import com.dexter.little_smart_chat.audio.AudioPlayerManager
import com.dexter.little_smart_chat.mvvm.viewmodel.SmartAgentViewModel
import com.dexter.little_smart_chat.utils.OPUtils
import com.dexter.little_smart_chat.utils.RecordingPlayDialog
import com.dexter.little_smart_chat.utils.yzs.BING_BING_NEUTRAL_PLUS
import com.dexter.little_smart_chat.utils.yzs.SHA_SHA_NEUTRAL_PLUS
import com.dexter.little_smart_chat.utils.yzs.TIAN_TIAN_NEUTRAL_PLUS
import com.dexter.little_smart_chat.utils.yzs.XIAO_LIANG_NEUTRAL_PLUS
import com.dexter.little_smart_chat.utils.yzs.XIAO_QIN_NEUTRAL_PLUS
import com.dexter.little_smart_chat.utils.yzs.YZSOnlineTTSUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unisound.dictation.UnisoundDictation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var mainContentContainer: FrameLayout
    private lateinit var mainAgentView: View
    private lateinit var memoListView: View
    private lateinit var characterDrawer: View
    private lateinit var helpModal: View
    private lateinit var statusMessage: TextView

    // Main Agent View Components
    private lateinit var characterAvatar: ImageView
    private lateinit var btnViewMemos: LinearLayout
    private lateinit var btnSwitchCharacter: LinearLayout
    private lateinit var btnHelp: LinearLayout

    // Interaction Button Components
    private lateinit var staticButton: View
    private lateinit var holdInteractionOverlay: CardView
    private lateinit var cancelButton: FrameLayout
    private lateinit var memoButton: FrameLayout
    private lateinit var recordingStatus: TextView
    private lateinit var thinkingState: View

    // 新增微信风格UI组件
    private lateinit var cancelZoneBackground: View
    private lateinit var memoZoneBackground: View
    private lateinit var cancelIcon: ImageView
    private lateinit var memoIcon: ImageView
    private lateinit var cancelText: TextView
    private lateinit var memoText: TextView
    private lateinit var recordingTime: TextView
    private lateinit var recordingIndicator: View
    private lateinit var operationHint: TextView

    // TTS水滴对话框组件
    private var ttsDropletContainer: FrameLayout? = null
    private var ttsContentText: TextView? = null
    private var ttsScrollView: ScrollView? = null
    private var ttsIndicator: View? = null

    // Character Drawer Components
    private var characterDrawerContainer: FrameLayout? = null
    private var drawerOverlay: View? = null
    private var drawerContent: CardView? = null
    private var characterRecyclerView: RecyclerView? = null
    private var settingsButton: LinearLayout? = null

    // Memo List Components
    private lateinit var btnBack: ImageButton
    private lateinit var memoRecyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var btnNewMemo: View

    // Help Modal Components
    private lateinit var btnCloseHelp: ImageButton
    private lateinit var btnGotIt: View

    private lateinit var recordView: View
    private var recordEvent : MotionEvent? = null //原子可变

    private lateinit var loadingIndicator: FrameLayout //加载指示器

    // 手势和矩阵相关变量
    private var isInCancelZone = 0 // 0 发送状态，1 记录状态，2 取消状态
    private lateinit var componentRectCancel: RectF  // 取消组件在屏幕上的矩阵位置1
    private lateinit var componentRectMemo: RectF // 备忘录组件在屏幕上的矩阵位置
    private var cancelThreshold = 80f // 取消阈值


    // 状态变量
    private var isKeyboardMode = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Data
    private var currentCharacter: Character = Character(1, "Girl", "👧", "Friendly and helpful assistant")
    private val characters = listOf(
        Character(1, "承智灵", "👧", "Friendly and helpful assistant"),
        Character(2, "祥机智", "👦", "Smart and knowledgeable companion"),
        Character(3, "xloop", "👱‍♀️", "Creative and imaginative friend")
    )

    private val memos = mutableListOf<Memo>()
    private lateinit var characterAdapter: CharacterAdapter
    private lateinit var memoAdapter: MemoAdapter

    // Recording
    private lateinit var audioRecorderManager: AudioRecorderManager
    private lateinit var audioPlayerManager: AudioPlayerManager
    private var isRecording = false
    private var recordingFile: File? = null
    private lateinit var recordingWaveformIcon: ImageView
    
    // Status bar components
    private lateinit var timeText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var batteryIcon: ImageView
    private lateinit var statusBarUpdater: StatusBarUpdater

    // 使用WeakReference避免内存泄漏
    private var mainHandler: Handler? = null
    private var engine: UnisoundDictation? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123

        private const val TAG = "MainActivity"
    }
    // 添加生命周期状态检查
    private var isActivityDestroyed = false

    //初始化viewModel
    private val aiViewModel: SmartAgentViewModel by viewModels()

    private var chatListener: OnChatListener? = null


    // 录音计时器
    private var recordingTimer: Handler? = null
    private var recordingStartTime: Long = 0
    private var recordingTimerRunnable: Runnable? = null

    private var recordText: String? = null

    private lateinit var memoManager: MemoManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_main)
        
        // 初始化音频管理器
        audioRecorderManager = AudioRecorderManager.getInstance(this)
        audioPlayerManager = AudioPlayerManager.getInstance(this)
        mainHandler = Handler(mainLooper)

        // 初始化备忘录管理器
        memoManager = MemoManager(this)

        setFullScreen()
        initializeViews()
        initCalculateComponentMatrix()
        setupClickListeners()
        setupRecyclerViews()
        setupRecording()
        checkPermissions()
        
        // 启动系统状态服务
        startSystemStatusService()

        observeViewModel()

        // 加载保存的备忘录数据
        loadMemos()

        // 显示首次打开的提示
        firstOpeningRemarks()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setFullScreen()
        }
    }

    private fun initializeViews() {
        // Main containers
        mainContentContainer = findViewById(R.id.mainContentContainer)
        mainAgentView = findViewById(R.id.mainAgentView)
        memoListView = findViewById(R.id.memoListView)
        characterDrawer = findViewById(R.id.characterDrawer)
        helpModal = findViewById(R.id.helpModal)
        statusMessage = findViewById(R.id.statusMessage)

        // Main agent view
        characterAvatar = findViewById(R.id.characterAvatar)
        btnViewMemos = findViewById(R.id.layout_query_notes)
        btnSwitchCharacter = findViewById(R.id.layout_switch_character)
        btnHelp = findViewById(R.id.layout_tips)

        // Interaction button
        staticButton = findViewById(R.id.staticButton)
        holdInteractionOverlay = findViewById(R.id.holdInteractionOverlay)
        cancelButton = findViewById(R.id.cancelButton)
        memoButton = findViewById(R.id.memoButton)
        recordingStatus = findViewById(R.id.recordingStatus)
        thinkingState = findViewById(R.id.thinkingState)
        recordingWaveformIcon = findViewById(R.id.recordingWaveformIcon)

        // 新增微信风格UI组件初始化
        cancelZoneBackground = findViewById(R.id.cancelZoneBackground)
        memoZoneBackground = findViewById(R.id.memoZoneBackground)
        cancelIcon = findViewById(R.id.cancelIcon)
        memoIcon = findViewById(R.id.memoIcon)
        cancelText = findViewById(R.id.cancelText)
        memoText = findViewById(R.id.memoText)
        recordingTime = findViewById(R.id.recordingTime)
        recordingIndicator = findViewById(R.id.recordingIndicator)
        operationHint = findViewById(R.id.operationHint)



        // Memo list
        btnBack = findViewById(R.id.btnBack)
        memoRecyclerView = findViewById(R.id.memoRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        btnNewMemo = findViewById(R.id.btnNewMemo)

        // Help modal
        btnCloseHelp = findViewById(R.id.btnCloseHelp)
        btnGotIt = findViewById(R.id.btnGotIt)
        
        // Status bar components
        timeText = findViewById(R.id.timeText)
        wifiIcon = findViewById(R.id.wifiIcon)
        batteryIcon = findViewById(R.id.batteryIcon)
        statusBarUpdater = StatusBarUpdater(this, timeText, wifiIcon, batteryIcon)
        
        // Character drawer
        initializeCharacterDrawer()
        
        // Enable character switch button
        btnSwitchCharacter.isEnabled = true
        btnSwitchCharacter.alpha = 1.0f

        loadingIndicator = findViewById(R.id.loading_frame)

//        // TTS水滴对话框组件初始化
        initializeTTSDropletDialog()

    }

    /**
     * 初始化TTS水滴对话框
     */
    private fun initializeTTSDropletDialog() {
        try {
            Log.d(TAG, "开始初始化TTS水滴对话框")

            // 检查mainAgentView是否已初始化
            if (!::mainAgentView.isInitialized) {
                Log.e(TAG, "mainAgentView未初始化，无法获取TTS组件")
                return
            }

            Log.d(TAG, "mainAgentView已初始化: ${mainAgentView.javaClass.simpleName}")

            // 方法1：通过mainAgentView获取
            ttsDropletContainer = mainAgentView.findViewById(R.id.ttsDropletDialog)
            ttsContentText = mainAgentView.findViewById(R.id.ttsContentText)
            ttsScrollView = mainAgentView.findViewById(R.id.ttsScrollView)

            Log.d(TAG, "方法1结果 - container: ${ttsDropletContainer != null}, text: ${ttsContentText != null}, scroll: ${ttsScrollView != null}")

            // 最终结果验证
            if (ttsDropletContainer != null) {
                Log.d(TAG, "✅ TTS水滴对话框初始化成功")
                Log.d(TAG, "  - container: ${ttsDropletContainer?.javaClass?.simpleName}")
                Log.d(TAG, "  - text: ${ttsContentText?.javaClass?.simpleName}")
                Log.d(TAG, "  - scroll: ${ttsScrollView?.javaClass?.simpleName}")

                // 测试显示对话框
//                testTTSDropletDialog()
            } else {
                Log.e(TAG, "❌ TTS水滴对话框初始化完全失败，所有方法都无法获取到容器")

                // 列出mainAgentView中的所有子视图用于调试
                listChildViews(mainAgentView)
            }

        } catch (e: Exception) {
            Log.e(TAG, "TTS水滴对话框初始化失败: ${e.message}", e)
        }
    }

    /**
     * 列出视图的所有子视图（调试用）
     */
    private fun listChildViews(parent: View, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        Log.d(TAG, "${indent}View: ${parent.javaClass.simpleName}, id: ${getViewIdName(parent.id)}")

        if (parent is ViewGroup) {
            for (i in 0 until parent.childCount) {
                listChildViews(parent.getChildAt(i), depth + 1)
            }
        }
    }

    /**
     * 获取View ID名称（调试用）
     */
    private fun getViewIdName(id: Int): String {
        return try {
            if (id == View.NO_ID) {
                "NO_ID"
            } else {
                resources.getResourceEntryName(id)
            }
        } catch (e: Exception) {
            "UNKNOWN_ID_$id"
        }
    }
    /**
     * 测试TTS对话框显示隐藏（调试用）
     */
    private fun testTTSDropletDialog() {
        Log.d(TAG, "测试TTS对话框显示功能")

        ttsDropletContainer?.let { container ->
            Log.d(TAG, "直接显示TTS对话框进行测试")
            container.visibility = View.VISIBLE
            container.alpha = 1f
            ttsContentText?.text = "测试内容显示"

            // 3秒后隐藏
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "隐藏测试TTS对话框")
                container.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {
                        container.visibility = View.GONE
                    }
                    .start()
            }, 3000)
        } ?: run {
            Log.e(TAG, "TTS对话框容器为空，无法测试")
        }
    }

    private fun initializeCharacterDrawer() {
        try {
            characterDrawerContainer = findViewById(R.id.characterDrawer)
            drawerOverlay = findViewById(R.id.drawerOverlay)
            drawerContent = findViewById(R.id.drawerContent)
            characterRecyclerView = findViewById(R.id.characterRecyclerView)
            settingsButton = findViewById(R.id.settingsButton)
            
            // Setup character drawer click listeners
            setupCharacterDrawerListeners()
            
            Log.d("MainActivity", "Character drawer initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Character drawer initialization failed: ${e.message}")
            // Disable character switch button if drawer fails to initialize
            btnSwitchCharacter?.isEnabled = false
            btnSwitchCharacter?.alpha = 0.5f
        }
    }
    
    private fun setupCharacterDrawerListeners() {
        drawerOverlay?.setOnClickListener { hideCharacterDrawer() }
        settingsButton?.setOnClickListener {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
            hideCharacterDrawer()
        }
    }

    private fun setupClickListeners() {
        // Main agent view buttons - 为整个布局和图标都设置点击监听
        btnViewMemos.setOnClickListener { showMemoList() }
        findViewById<View>(R.id.btnViewMemos).setOnClickListener { showMemoList() }
        
        btnSwitchCharacter?.setOnClickListener { 
            showCharacterDrawer()
        }
        findViewById<View>(R.id.btnSwitchCharacter)?.setOnClickListener {
            showCharacterDrawer()
        }
        
        btnHelp.setOnClickListener { showHelpModal() }
        findViewById<View>(R.id.btnHelp).setOnClickListener { showHelpModal() }

        // Memo list
        btnBack.setOnClickListener { showMainAgentView() }
        btnNewMemo.setOnClickListener {
            Toast.makeText(this, "New memo feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Help modal
        btnCloseHelp.setOnClickListener { hideHelpModal() }
        btnGotIt.setOnClickListener { hideHelpModal() }

        // Recording buttons
        cancelButton.setOnClickListener { cancelRecording() }
        memoButton.setOnClickListener { saveRecording() }
    }

    private fun setupRecyclerViews() {
        // Character adapter
        characterRecyclerView?.let { recyclerView ->
            characterAdapter = CharacterAdapter(characters, currentCharacter.id) { character ->
                currentCharacter = character
                updateCharacterAvatar()
                hideCharacterDrawer()
            }

            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = characterAdapter
            }
        }

        // Memo adapter - 更新为支持新功能
        memoAdapter = MemoAdapter(
            memos = memos,
            onMemoClick = { memo ->
                // 点击备忘录项时的处理
                if (memo.recordingPath != null) {
                    // 如果有录音文件，显示播放对话框
                    showRecordingPlayDialog(memo)
                } else {
                    // 如果没有录音文件，显示详情Toast
                    Toast.makeText(this, memo.content, Toast.LENGTH_LONG).show()
                }
            },
            onDeleteClick = { memo ->
                // 显示删除确认对话框
                showDeleteMemoDialog(memo)
            }
        )
        memoRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = memoAdapter
        }
    }

    /**
     * 显示录音播放对话框
     */
    private fun showRecordingPlayDialog(memo: Memo) {
        val playDialog = RecordingPlayDialog(this, memo)
        playDialog.show()
    }

    /**
     * 显示删除备忘录确认对话框
     */
    private fun showDeleteMemoDialog(memo: Memo) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除备忘录")
            .setMessage("确定要删除备忘录 \"${memo.title}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteMemo(memo)
            }
            .setNegativeButton("取消", null)
            .show()
    }


    private fun setupRecording() {
        var isLongPress = false
        var pressStartTime = 0L
        val longPressThreshold = 300L // 300ms长按阈值，提高灵敏度

//        staticButton.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    pressStartTime = System.currentTimeMillis()
//                    isLongPress = false
//                    return@setOnTouchListener true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val pressDuration = System.currentTimeMillis() - pressStartTime
//                    if (pressDuration >= longPressThreshold && !isLongPress && !isRecording) {
//                        isLongPress = true
//                        startRecording()
//                    }
//                    if (isRecording) {
//                        handleRecordingDrag(event)
//                    }
//                    return@setOnTouchListener true
//                }
//                MotionEvent.ACTION_UP -> {
//                    val pressDuration = System.currentTimeMillis() - pressStartTime
//                    if (isRecording) {
//                        stopRecording()
//                    } else if (pressDuration < longPressThreshold) {
//                        // 单击提示
//                        Toast.makeText(this, "录制时间太短，请长按录制", Toast.LENGTH_SHORT).show()
//                    }
//                    return@setOnTouchListener true
//                }
//            }
//            false
//        }

        staticButton.setOnTouchListener { view, event ->
            // 先检查权限，如果没有权限则请求
            if (!OPUtils.PermissionChecker.checkPermission(
                    this,
                    OPUtils.PermissionChecker.RECORD_AUDIO
                )
            ) {
                // 保存触摸事件，权限获取后继续处理
                recordView = view
                recordEvent = event
                OPUtils.PermissionChecker.requestPermissions(
                    this,
                    arrayOf(OPUtils.PermissionChecker.RECORD_AUDIO),
                    OPUtils.PermissionChecker.RECORD_AUDIO_CODE
                )
                return@setOnTouchListener true
            }

            // 有权限时直接处理触摸事件
            handleVoiceHintTouch(view, event)
        }
    }

    private fun initCalculateComponentMatrix(){
//        componentRectCancel = calculateComponentMatrix(cancelButton)
//        componentRectMemo = calculateComponentMatrix(memoButton)

    }
    /**
     * 处理语音提示按钮的触摸事件（使用矩阵检测）
     */
    private fun handleVoiceHintTouch(view: View, event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                OPUtils.Logger.d(TAG, "录音开始: (${event.rawX}, ${event.rawY})")
                startVoiceRecord()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                handleVoiceMove(event)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                OPUtils.Logger.d(TAG, "录音结束: 最终区域=$isInCancelZone")
                handleVoiceRecordEnd()
                true
            }
            else -> false
        }
    }
//    private fun handleVoiceHintTouch(view: View, event: MotionEvent): Boolean {
//        return when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                startVoiceRecord()
//                true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                handleVoiceMove(event)
//                true
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                handleVoiceRecordEnd()
//                true
//            }
//            else -> false
//        }
//    }

    /**
     * 计算组件在屏幕上的矩阵位置
     */
    private fun calculateComponentMatrix(view: View): RectF {
        val location = IntArray(2)
        view.getLocationOnScreen(location)

        // 增加触摸容错区域：每边增加30dp
        val extraPadding = (30 * resources.displayMetrics.density).toInt() // 30dp转换为像素

        return RectF(
            (location[0] - extraPadding).toFloat(),
            (location[1] - extraPadding).toFloat(),
            (location[0] + view.width + extraPadding).toFloat(),
            (location[1] + view.height + extraPadding).toFloat()
        )
//        val location = IntArray(2)
//        view.getLocationOnScreen(location)
//
//        return RectF(
//            location[0].toFloat(),
//            location[1].toFloat(),
//            (location[0] + view.width).toFloat(),
//            (location[1] + view.height).toFloat()
//        )
    }

    /**
     * 处理语音移动事件（稳定版）
     *
     * 主要逻辑:
     * 1. 以按钮上边缘为界限，上方为取消区域。
     * 2. 手指进入取消区域，UI变为红色“松手取消”。
     * 3. 手指滑回按钮下方区域，UI恢复蓝色“松手发送”。
     * 4. 此逻辑主要处理上下滑动，符合主流应用的交互习惯。
     */
    /**
     * 处理语音移动事件（微信风格版本）
     */

    /**
     * 处理语音移动事件（优化版本 - 提高手势识别灵敏度）
     */
    private fun handleVoiceMove(event: MotionEvent) {
        if (!isRecording) return

        val touchX = event.rawX
        val touchY = event.rawY

        val wasInCancelZone = isInCancelZone

        // 动态重新计算矩阵位置（确保holdInteractionOverlay已显示）
        if (::cancelButton.isInitialized && ::memoButton.isInitialized) {
            componentRectCancel = calculateComponentMatrix(cancelButton)
            componentRectMemo = calculateComponentMatrix(memoButton)
        }

        // 检查是否在取消区域或备忘录区域
        when {
            componentRectCancel.contains(touchX, touchY) -> {
                if (isInCancelZone != 1) {
                    isInCancelZone = 1
                    addHapticFeedback() // 进入区域时震动反馈
                    OPUtils.Logger.d(TAG, "进入取消区域: ($touchX, $touchY)")
                }
            }
            componentRectMemo.contains(touchX, touchY) -> {
                if (isInCancelZone != 2) {
                    isInCancelZone = 2
                    addHapticFeedback() // 进入区域时震动反馈
                    OPUtils.Logger.d(TAG, "进入备忘录区域: ($touchX, $touchY)")
                }
            }
            else -> {
                if (isInCancelZone != 0) {
                    isInCancelZone = 0
                    OPUtils.Logger.d(TAG, "回到发送区域: ($touchX, $touchY)")
                }
            }
        }

        // 只有在状态真正改变时才更新UI
        if (wasInCancelZone != isInCancelZone) {
            updateWeChatRecordingState()
        }
    }
//    private fun handleVoiceMove(event: MotionEvent) {
//        if (!isRecording) return
//
//        val touchY = event.rawY
//        val touchX = event.rawX
//
//        val wasInCancelZone = isInCancelZone
//
//        // 检查是否在取消区域或备忘录区域
//        if (componentRectCancel.contains(touchX, touchY)) {
//            isInCancelZone = 1
//            addHapticFeedback() // 进入区域时震动反馈
//        } else if (componentRectMemo.contains(touchX, touchY)) {
//            isInCancelZone = 2
//            addHapticFeedback() // 进入区域时震动反馈
//        } else {
//            isInCancelZone = 0
//        }
//
//        if (wasInCancelZone != isInCancelZone) {
//            // 状态改变时更新UI
//            updateWeChatRecordingState()
//        }
//    }


    /**
     * 更新微信风格录音状态UI
     */
    private fun updateWeChatRecordingState() {
        if (!isRecording || isActivityDestroyed) return

        mainHandler?.post {
            if (!isActivityDestroyed && ::staticButton.isInitialized) {
                try {
                    when (isInCancelZone) {
                        1 -> {
                            // 取消状态
                            recordingStatus.text = "松开取消"
                            operationHint.text = "松开手指，取消发送"

                            // 取消区域高亮
                            cancelZoneBackground.animate().alpha(1f).setDuration(150).start()
                            memoZoneBackground.animate().alpha(0f).setDuration(150).start()

                            // 图标和文字变色
                            cancelIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))
                            cancelText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        }
                        2 -> {
                            // 备忘录状态
                            recordingStatus.text = "松开保存"
                            operationHint.text = "松开手指，保存备忘录"

                            // 备忘录区域高亮
                            memoZoneBackground.animate().alpha(1f).setDuration(150).start()
                            cancelZoneBackground.animate().alpha(0f).setDuration(150).start()

                            // 图标和文字变色
                            memoIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.white))
                            memoText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        }
                        0 -> {
                            // 正常状态
                            recordingStatus.text = "松开发送"
                            operationHint.text = "向上滑动取消"

                            // 重置区域背景
                            cancelZoneBackground.animate().alpha(0f).setDuration(150).start()
                            memoZoneBackground.animate().alpha(0f).setDuration(150).start()

                            // 重置图标和文字颜色
                            cancelIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.red_500))
                            cancelText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_500))
                            memoIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.blue_500))
                            memoText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blue_500))
                        }
                    }
                } catch (e: Exception) {
                    OPUtils.Logger.e(TAG, "Failed to update WeChat recording state: ${e.message}")
                }
            }
        }
    }

//    private fun handleVoiceMove(event: MotionEvent) {
//        if (!isRecording) return
//
//        val touchY = event.rawY
//        val touchX = event.rawX
//
//        val wasInCancelZone = isInCancelZone
//
//        //当手指在取消区域时，取消按钮变红色，否则变蓝色
//        //当手势在录制区域时，取消按钮变黄色，否则变蓝色
//        if ( componentRectCancel.contains(touchX, touchY)){
//            isInCancelZone = 1
//
//
//            //todo 1
//        }else if (componentRectMemo.contains(touchX, touchY)){
//            isInCancelZone = 2
//
//
//            //todo 2
//        }else{
//            //todo 3
//            isInCancelZone = 0
//        }
//
//        if (wasInCancelZone != isInCancelZone) {
//            // 状态改变时更新UI
//            updateRecordingState()
//        }
//
//    }

    /**
     * 添加震动反馈
     */
    private fun addHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = this.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = this.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            // 忽略震动权限问题
        }
    }

    /**
     * 更新录音状态UI
     */
    private fun updateRecordingState() {
        if (!isRecording || isActivityDestroyed) return

        mainHandler?.post {
            if (!isActivityDestroyed && ::staticButton.isInitialized) {
                try {
                    when (isInCancelZone) {
                        1 -> {
                            recordingStatus.text = "左滑取消"
                            cancelButton.alpha = 0.7f
                            memoButton.alpha = 1.0f
                        }
                        2 -> {
                            recordingStatus.text = "右滑动生成备忘"
                            memoButton.alpha = 0.7f
                            cancelButton.alpha = 1.0f
                        }
                        0 -> {
                            recordingStatus.text = "松开发送"
                            cancelButton.alpha = 1.0f
                            memoButton.alpha = 1.0f
                        }
                        else -> {}
                     }
                } catch (e: Exception) {
                    OPUtils.Logger.e(TAG, "Failed to update recording state : ${e.message}")
                }
            }
        }
    }

    // 修改原有的startVoiceRecord方法
    private fun startVoiceRecord() {
        if (isRecording) return
        //需要检测是否在播放声音若在则暂停
        aiViewModel.pauseTTS()

        Log.d("ASR", "开始录音startVoiceRecord")

        // 同时开始ASR和文件录音
        recordingFile = audioRecorderManager.startRecording()

        startAsrOnline()
        if (recordingFile != null) {
            Log.d("ASR", "文件录音开始成功: ${recordingFile!!.absolutePath}")
        } else {
            Log.w("ASR", "文件录音开始失败，仅使用ASR录音")
        }

        // 确保状态重置
        isRecording = true // 移到这里确保状态正确
        isInCancelZone = 0

        showRecordStatus()
        chatListener?.onStartVoiceRecord()
    }

    /**
     * 取消录音
     */
    private fun cancelVoiceRecord() {
        if (!isRecording) return

        Log.d("ASR", "取消录音cancelVoiceRecord")
        //若tts还在播放则恢复
        aiViewModel.resumeTTS()
        aiViewModel.cancelYZSAsrOnline()
        isRecording = false

        // 通知外部取消录音
        chatListener?.onCancelVoiceRecord()

        // 添加取消提示
        showGestureToast(1)
    }

    /**
     * 显示取消提示
     */
    private fun showGestureToast(type: Int) {
        try {
            runOnUiThread {
                when (type) {
                    1 -> {
                        Toast.makeText(this, "录音已取消", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        Toast.makeText(this, "已添加至备忘录", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略Context相关异常
            e.printStackTrace()
            OPUtils.Logger.dtf("showCancelToast",  "showCancelToast has error ${ e.message}")
        }
    }

    /**
     * 停止录音
     */
    private fun stopVoiceRecord() {
        if (!isRecording) return
        Log.d("ASR", "结束录音cancelVoiceRecord")
        //若tts还在播放则停止
        aiViewModel.stopTTS()
        stopAsrOnline()
        chatListener?.onStopVoiceRecord()
    }

//    /**
//     * 生成备忘录
//     */
//    private fun generateMemo() {
//        Log.d("ASR", "开始生成备忘录")
//
//        try {
//            // 首先检查当前是否正在录音
//            if (!isRecording) {
//                Log.w("ASR", "当前没有在录音，无法保存备忘录")
//                Toast.makeText(this, "没有录音内容可保存", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            // 停止ASR录音
//            aiViewModel.cancelYZSAsrOnline()
//
//            // 检查是否有通过startRecording开始的录音
//            if (recordingFile == null) {
//                // 如果没有通过AudioRecorderManager开始录音，现在开始一个短录音
//                Log.d("ASR", "开始录音以保存备忘录")
//                recordingFile = audioRecorderManager.startRecording()
//
//                // 给录音一点时间
//                mainHandler?.postDelayed({
//                    // 立即停止录音获取文件
//                    val savedFile = audioRecorderManager.stopRecording()
//                    processMemoSaving(savedFile,recordText)
//                }, 100) // 100ms最短录音
//            } else {
//                // 停止现有录音
//                val savedFile = audioRecorderManager.stopRecording()
//                processMemoSaving(savedFile,recordText)
//            }
//
//            recordText = ""
//            isRecording = false
//
//        } catch (e: Exception) {
//            Log.e("ASR", "生成备忘录失败: ${e.message}")
//            Toast.makeText(this, "保存备忘录失败: ${e.message}", Toast.LENGTH_SHORT).show()
//            isRecording = false
//        }
//    }

    /**
     * 生成备忘录 - 增加2秒延迟保护
     */
    private fun generateMemo() {
        Log.d("ASR", "开始生成备忘录")

        try {
            // 首先检查当前是否正在录音
            if (!isRecording) {
                Log.w("ASR", "当前没有在录音，无法保存备忘录")
                Toast.makeText(this, "没有录音内容可保存", Toast.LENGTH_SHORT).show()
                return
            }

            // 显示正在保存的提示
            showStatusMessage("正在完整收录音频，请稍候...")

            // 停止ASR录音
            aiViewModel.cancelYZSAsrOnline()

            // 启动延迟保护机制
            startMemoSaveProtection()

        } catch (e: Exception) {
            Log.e("ASR", "生成备忘录失败: ${e.message}")
            Toast.makeText(this, "保存备忘录失败: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    /**
     * 启动备忘录保存的2秒延迟保护机制
     */
    private fun startMemoSaveProtection() {
        Log.d("ASR", "启动2秒录音保护机制")

        // 确保有文件录音在进行
        if (recordingFile == null) {
            Log.d("ASR", "没有文件录音，立即启动录音")
            recordingFile = audioRecorderManager.startRecording()
        }

        // 延迟2秒后进行保存，确保录音完整
        mainHandler?.postDelayed({
            try {
                Log.d("ASR", "2秒保护时间结束，开始保存备忘录")

                // 再延迟500ms确保录音稳定
                mainHandler?.postDelayed({
                    finalizeMemoSave()
                }, 500)

            } catch (e: Exception) {
                Log.e("ASR", "延迟保存失败: ${e.message}")
                Toast.makeText(this@MainActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                isRecording = false
            }
        }, 2000) // 2秒延迟保护
    }

    /**
     * 最终完成备忘录保存
     */
    private fun finalizeMemoSave() {
        Log.d("ASR", "开始最终保存流程")

        try {
            // 停止文件录音
            val savedFile = audioRecorderManager.stopRecording()

            // 验证录音文件
            if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
                Log.d("ASR", "录音文件验证成功: ${savedFile.absolutePath}, 大小: ${savedFile.length()} bytes")

                // 获取ASR识别的内容
                val asrContent = recordText ?: ""

                processMemoSaving(savedFile, asrContent)
            } else {
                Log.w("ASR", "录音文件无效，仅保存ASR内容")

                // 如果录音文件无效，仅保存ASR内容
                val asrContent = recordText ?: ""
                if (asrContent.isNotEmpty()) {
                    processMemoSaving(null, asrContent)
                } else {
                    Toast.makeText(this, "没有录音文件或识别内容可保存", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e("ASR", "最终保存失败: ${e.message}")
            Toast.makeText(this, "保存备忘录失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // 重置状态
            recordText = ""
            isRecording = false
            recordingFile = null
        }
    }

    /**
     * 处理备忘录保存逻辑
     */
    private fun processMemoSaving(savedFile: File?,asr:String?) {
        if (savedFile != null && savedFile.exists()) {
            Log.d("ASR", "录音文件保存成功: ${savedFile.absolutePath}")

            // 获取录音信息
            val recordingInfo = audioRecorderManager.getRecordingInfo(savedFile.absolutePath)

            // 创建备忘录对象
            val memo = Memo(
                id = memos.size + 1,
                title = "记录 ${memos.size + 1}",
                content = "保存时间： ${getCurrentDateTime()}",
                date = getCurrentDateTime(),
                recordingPath = savedFile.absolutePath,
                recordingDuration = recordingInfo?.duration ?: 0L,
                asrContent = asr // 后续可以通过ASR识别填充
            )

            // 添加到备忘录列表
            memos.add(memo)
            memoAdapter.notifyItemInserted(memos.size - 1)

            Log.d("ASR", "备忘录已保存: ${memo.title}, 路径: ${memo.recordingPath}")

            // 显示成功提示
            showGestureToast(2)
            showStatusMessage("录音已保存到备忘录")
        } else {
            Log.e("ASR", "录音文件不存在或保存失败")

            // 创建一个没有录音文件的备忘录（包含ASR识别内容）
            val asrContent = aiViewModel.recognizerBuffer.value
            if (asrContent.isNotEmpty()) {
                val memo = Memo(
                    id = memos.size + 1,
                    title = "语音备忘 ${memos.size + 1}",
                    content = "识别内容: $asrContent",
                    date = getCurrentDateTime(),
                    recordingPath = null,
                    recordingDuration = 0L,
                    asrContent = asrContent
                )

                memos.add(memo)
                memoAdapter.notifyItemInserted(memos.size - 1)

                showGestureToast(2)
                showStatusMessage("语音识别内容已保存到备忘录")
                Log.d("ASR", "保存ASR内容到备忘录: $asrContent")
            } else {
                Toast.makeText(this, "没有录音文件或识别内容可保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理录音结束
     */
    private fun handleVoiceRecordEnd() {
        if (!isRecording) return

        hintWaveformAnimation()

        //关闭dialog
        when(isInCancelZone) {
            0 -> stopVoiceRecord()
            1 -> cancelVoiceRecord()
            2 -> generateMemo()
            else -> {
                // 恢复UI状态
            }

         }
        Log.d("ASR", "录音结束handleVoiceRecordEnd:"+ isInCancelZone)

        // 重置状态
        isInCancelZone = 0
    }

    /**
     * 观察ViewModel状态
     */
    private fun observeViewModel() {

        lifecycleScope.launch {
            aiViewModel.recordText.collectLatest { text ->
                recordText = text
            }
        }

        // 设置TTS水滴对话框Views
        aiViewModel.setTTSDropletViews(
            ttsDropletContainer,
            ttsContentText,
            ttsScrollView
        )

        // 观察消息列表
        lifecycleScope.launch {
            aiViewModel.messages.collectLatest { messages ->
                if (!isActivityDestroyed) {
//                    val isNewMsg = messages.size == chatAdapter.getAllMessages().size
//                    chatAdapter.setMessages(messages)
//                    mainHandler?.post {
//                        updateChatDisplay(if (isNewMsg) 0 else 1)
//                    }
                }
            }
        }

        // 观察加载状态
        lifecycleScope.launch {
            aiViewModel.loading.collectLatest { isLoading ->
                if (!isActivityDestroyed) {
                    showLoading(isLoading)
                }
            }
        }

        // 观察错误状态
        lifecycleScope.launch {
            aiViewModel.error.collectLatest { error ->
                if (!isActivityDestroyed && error != null) {
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    aiViewModel.clearError()
                }
            }
        }
        lifecycleScope.launch {
            aiViewModel.recognizerBuffer.collectLatest { buffer ->
                if (!isActivityDestroyed) {
                    // 识别结果无需做业务
                }
            }
        }
    }


    private fun updateChatDisplay(type: Int) {//o追加消息，1新增消息
        updateEmptyState()
        //滚动到最后一行
        safeScroll(type)
    }

    private fun updateEmptyState() {
//        emptyState.visibility = if (chatAdapter.getAllMessages().isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 稳定滚动
     */
    private fun safeScroll(type: Int) {
        when (type) {
            0 -> {
                steamScrollToBottomSmooth()
            }
            1 -> {
                scrollToBottomSmooth()
            }
        }
    }

    /**
     * 平滑滚动到底部
     */
    private fun scrollToBottomSmooth() {
        try {
//            rvChatMessages.post {
//                if (chatAdapter.getAllMessages().isNotEmpty()) {
//                    rvChatMessages.smoothScrollToPosition(chatAdapter.getAllMessages().size - 1)
//                }
//            }
        }catch (e: Exception){
            e.printStackTrace()
            OPUtils.Logger.e(TAG, "scrollToBottomSmooth滚动异常 scrollToBottomSmooth："+e.message)
        }
    }

    /**
     * 消息滚动到底部
     */
    private fun steamScrollToBottomSmooth() {
//        rvChatMessages.doOnNextLayout {
//            try {
//                if (chatAdapter.getAllMessages().isNotEmpty() && ryManager.findLastCompletelyVisibleItemPosition() >=  chatAdapter.getAllMessages().size - 1){
//                    OPUtils.Logger.d(TAG, "开始执行滚动 scrollToBottomSmooth："+chatAdapter.getAllMessages().size)//>=是否在底部 在最后一项超长（仅部分可见）时仍能自动跟随
//                    ryManager.scrollToPosition(chatAdapter.getAllMessages().size - 1)
//                }}catch (e: Exception){
//                e.printStackTrace()
//                OPUtils.Logger.e(TAG, "steamScrollToBottomSmooth滚动异常 scrollToBottomSmooth："+e.message)
//            }
//        }
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun startRecording() {
        if (isRecording) return // 防止重复录制
        
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
            return
        }

        try {
            // 使用AudioRecorderManager开始录音
            recordingFile = audioRecorderManager.startRecording()
            if (recordingFile != null) {

            showRecordStatus()

            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Recording failed: ${e.message}")
        }
    }

    private fun stopRecording() {
        if (!isRecording) return // 防止重复停止
        
        try {
            // 使用AudioRecorderManager停止录音
            recordingFile = audioRecorderManager.stopRecording()
            hintRecordStatus()

            // Process the recording
            processRecording()

        } catch (e: Exception) {
            // 即使停止失败，也要重置状态
            isRecording = false
            holdInteractionOverlay.visibility = View.GONE
            staticButton.visibility = View.VISIBLE
            staticButton.alpha = 1f
            
            // Stop waveform animation
            stopWaveformAnimation()
            
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Stop recording failed: ${e.message}")
        }
    }

    private fun handleRecordingDrag(event: MotionEvent) {
        if (!isRecording) return

        val currentX = event.rawX
        val currentY = event.rawY

        // Check if finger is over cancel or memo buttons
        val cancelBounds = getViewBounds(cancelButton)
        val memoBounds = getViewBounds(memoButton)

        when {
            isPointInBounds(currentX, currentY, cancelBounds) -> {
                recordingStatus.text = "左滑取消"
                cancelButton.alpha = 0.7f
                memoButton.alpha = 1.0f
            }
            isPointInBounds(currentX, currentY, memoBounds) -> {
                recordingStatus.text = "右滑动生成备忘"
                memoButton.alpha = 0.7f
                cancelButton.alpha = 1.0f
            }
            else -> {
                recordingStatus.text = "松开发送"
                cancelButton.alpha = 1.0f
                memoButton.alpha = 1.0f
            }
        }
    }

    private fun getViewBounds(view: View): FloatArray {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return floatArrayOf(
            location[0].toFloat(),
            location[1].toFloat(),
            location[0] + view.width.toFloat(),
            location[1] + view.height.toFloat()
        )
    }

    private fun isPointInBounds(x: Float, y: Float, bounds: FloatArray): Boolean {
        return x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3]
    }

    private fun cancelRecording() {
        audioRecorderManager.cancelRecording()
        isRecording = false
        recordingFile = null
        hintWaveformAnimation()
        Toast.makeText(this, "录音已取消", Toast.LENGTH_SHORT).show()
    }


    private fun hintWaveformAnimation() {
        // 重置UI状态
        holdInteractionOverlay.visibility = View.GONE
        staticButton.visibility = View.VISIBLE
        staticButton.alpha = 1f
        stopWaveformAnimation()
    }

    private fun showWaveformAnimation() {
    }

    private fun processRecording() {
        if (recordingFile != null && recordingFile!!.exists()) {
            // 这里可以添加播放录音的测试代码
            audioPlayerManager.playAudio(recordingFile!!) {
                // 播放完成后的回调
                Log.d("MainActivity", "Recording playback completed")
            }
        } else {
            Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show()
        }
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

    private fun showMainAgentView() {
        mainAgentView.visibility = View.VISIBLE
        memoListView.visibility = View.GONE
        
        // Show recording button when back to main view
        staticButton.visibility = View.VISIBLE
    }

    private fun showMemoList() {
        mainAgentView.visibility = View.GONE
        memoListView.visibility = View.VISIBLE
        
        // Hide recording button when in memo list
        staticButton.visibility = View.GONE

        // Update empty state
        if (memos.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            memoRecyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            memoRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showCharacterDrawer() {
        characterDrawerContainer?.let { container ->
            container.visibility = View.VISIBLE
            container.alpha = 0f
            container.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideCharacterDrawer() {
        characterDrawerContainer?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                characterDrawerContainer?.visibility = View.GONE
            }
            ?.start()
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

    private fun updateCharacterAvatar() {
        // Update character avatar based on current character
        when (currentCharacter.id) {
            1 -> {characterAvatar.setImageResource(R.mipmap.gril)
            YZSOnlineTTSUtils.changeVoice(BING_BING_NEUTRAL_PLUS,0.9)
                aiViewModel.agentName = com.dexter.little_smart_chat.network.model.agentName.ModelXLoop. value
            }
            2 -> {characterAvatar.setImageResource(R.mipmap.boy)
                YZSOnlineTTSUtils.changeVoice(XIAO_LIANG_NEUTRAL_PLUS,1.0)
                aiViewModel.agentName = com.dexter.little_smart_chat.network.model.agentName.ModelXJZ. value
                if(MyApplication.isInitXZ){
                    aiViewModel.firstOpeningRemarks("祥机智")
                    MyApplication.isInitXZ = false
                }
            }
            3 ->{ characterAvatar.setImageResource(R.mipmap.xloop)
                YZSOnlineTTSUtils.changeVoice(XIAO_QIN_NEUTRAL_PLUS,1.0)
                      aiViewModel.agentName = com.dexter.little_smart_chat.network.model.agentName.ModelXLoop. value
            }
        }
        
        // 更新角色名称显示
//        findViewById<TextView>(R.id.tvSwitchCharacterNickname)?.text = currentCharacter.name
    }

    private fun showStatusMessage(message: String) {
        statusMessage.text = message
        statusMessage.visibility = View.VISIBLE
        statusMessage.alpha = 0f

        statusMessage.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                statusMessage.postDelayed({
                    statusMessage.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            statusMessage.visibility = View.GONE
                        }
                        .start()
                }, 2100)
            }
            .start()
    }



    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // 检查录音权限
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // 检查存储权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        
        // 检查音频设置权限
        if (checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }
        
        // 如果有需要申请的权限，就申请
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "所有权限已获取", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要相关权限才能录音", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setFullScreen() {
        // Hide status bar and navigation bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            
            // Hide navigation bar for older versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAsrOnline() {
        try {
            isRecording = true
            aiViewModel.startYZSAsrOnline()
        } catch (e: Exception) {
            OPUtils.Logger.e(TAG, "Failed to start ASR: ${e. message}")
        }
    }

    override fun onResume() {
        super.onResume()
        //如果有tts则播放
        aiViewModel.resumeTTS()
    }

    override fun onPause() {
        super.onPause()
        aiViewModel.pauseTTS()
    }

    /**
     * 显示欢迎语
     */
    private fun firstOpeningRemarks(){
        aiViewModel.firstOpeningRemarks("承智灵")
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


    private fun stopAsrOnline() {
        try {
            isRecording = false
            aiViewModel.stopYZSAsrOnline()
        } catch (e: Exception) {
            OPUtils.Logger.e(TAG, "Failed to stop ASR: ${e. message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 保存备忘录数据
        saveMemos()

        audioPlayerManager.stopPlaying()
        statusBarUpdater.release()
        stopSystemStatusService()

        isActivityDestroyed = true

        // 取消当前AI请求
        aiViewModel.cancelCurrentRequest()

        // 停止录音
        if (isRecording) {
            stopAsrOnline()
            isRecording = false
        }

        // 清理Handler
        mainHandler?.removeCallbacksAndMessages(null)
        mainHandler = null

        // 释放ASR引擎
        try {
            engine?.release()
            engine = null
        } catch (e: Exception) {
            OPUtils.Logger.e(TAG, "Failed to release ASR engine: ${e.message}")
        }

        // 释放TTS
        try {
            aiViewModel.yzsRelease()
        } catch (e: Exception) {
            e.printStackTrace()
            OPUtils.Logger.e(TAG, "YZS语音识别释放失败: ${e.message}")
        }

        coroutineScope.cancel()
        chatListener?.onDialogClosed()
    }

//    private fun showRecordStatus() {
//        isRecording = true
//        runOnUiThread {
//            holdInteractionOverlay.visibility = View.VISIBLE
//            recordingStatus.text = "松开发送"
//
//            // Hide static button with animation
//            val exitAnimation = AnimationUtils.loadAnimation(this, R.anim.button_exit_animation)
//            staticButton.startAnimation(exitAnimation)
//            exitAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
//                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
//                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
//                    staticButton.visibility = View.GONE
//                }
//                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
//            })
//
//            // Start waveform animation
//            startWaveformAnimation()
//
//            Log.d("MainActivity", "Recording started, static button hidden")
//
//            // Start enter animation
//            val enterAnimation = AnimationUtils.loadAnimation(this, R.anim.button_enter_animation)
//            holdInteractionOverlay.startAnimation(enterAnimation)
//        }
//    }

    /**
     * 显示录音状态（微信风格）
     */
    private fun showRecordStatus() {
        staticButton.visibility = View.GONE
        holdInteractionOverlay.visibility = View.VISIBLE

        // 重置UI状态 - 修复背景高亮残留问题
        resetRecordingUIState()


        // 开始录音时长计时
        startRecordingTimer()

        // 开始录音动画
        startWeChatRecordingAnimation()
    }

    /**
     * 重置录音UI状态 - 修复背景高亮残留问题
     */
    private fun resetRecordingUIState() {
        try {
            if (!isActivityDestroyed && ::staticButton.isInitialized) {
                // 重置录音状态文本
                recordingStatus.text = "松开发送"

                // 重置区域背景透明度
                cancelZoneBackground.alpha = 0f
                memoZoneBackground.alpha = 0f

                // 重置图标和文字颜色为默认状态
                cancelIcon.setColorFilter(ContextCompat.getColor(this, R.color.red_500))
                cancelText.setTextColor(ContextCompat.getColor(this, R.color.red_500))
                memoIcon.setColorFilter(ContextCompat.getColor(this, R.color.blue_500))
                memoText.setTextColor(ContextCompat.getColor(this, R.color.blue_500))

                // 重置isInCancelZone状态
                isInCancelZone = 0

                OPUtils.Logger.d(TAG, "录音UI状态已重置")
            }
        } catch (e: Exception) {
            OPUtils.Logger.e(TAG, "重置录音UI状态失败: ${e.message}")
        }
    }

    /**
     * 隐藏录音状态
     */
    private fun hintRecordStatus() {
        holdInteractionOverlay.visibility = View.GONE
        staticButton.visibility = View.VISIBLE

        // 停止录音时长计时
        stopRecordingTimer()

        // 停止录音动画
        stopWeChatRecordingAnimation()

        // 确保状态完全重置
        isInCancelZone = 0
    }

    /**
     * 开始录音计时
     */
    private fun startRecordingTimer() {
        recordingStartTime = System.currentTimeMillis()
        recordingTimer = Handler(Looper.getMainLooper())

        recordingTimerRunnable = object : Runnable {
            override fun run() {
                if (isRecording && !isActivityDestroyed) {
                    val duration = System.currentTimeMillis() - recordingStartTime
                    updateRecordingTime(duration)
                    recordingTimer?.postDelayed(this, 100) // 每100ms更新一次
                }
            }
        }
        recordingTimer?.post(recordingTimerRunnable!!)
    }

    /**
     * 停止录音计时
     */
    private fun stopRecordingTimer() {
        recordingTimerRunnable?.let { runnable ->
            recordingTimer?.removeCallbacks(runnable)
        }
        recordingTimer = null
        recordingTimerRunnable = null
    }

    /**
     * 更新录音时长显示
     */
    private fun updateRecordingTime(duration: Long) {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        val timeText = String.format("%02d:%02d", minutes, remainingSeconds)
        recordingTime.text = timeText
    }

    /**
     * 开始微信风格录音动画
     */
    private fun startWeChatRecordingAnimation() {
        // 录音指示器闪烁动画
        val indicatorAnimation = ObjectAnimator.ofFloat(recordingIndicator, "alpha", 1f, 0.3f)
        indicatorAnimation.duration = 800
        indicatorAnimation.repeatCount = ObjectAnimator.INFINITE
        indicatorAnimation.repeatMode = ObjectAnimator.REVERSE
        indicatorAnimation.start()

        // 录音波形图标动画
        recordingWaveformIcon.setImageResource(R.drawable.recording_wave_animation)
        val drawable = recordingWaveformIcon.drawable as? android.graphics.drawable.AnimationDrawable
        drawable?.start()
    }

    /**
     * 停止微信风格录音动画
     */
    private fun stopWeChatRecordingAnimation() {
        // 停止所有动画
        recordingIndicator.clearAnimation()
        recordingWaveformIcon.clearAnimation()

        val drawable = recordingWaveformIcon.drawable as? android.graphics.drawable.AnimationDrawable
        drawable?.stop()
    }

    /**
     * 处理备忘录保存逻辑
     */
    private fun processMemoSaving(savedFile: File?) {
        if (savedFile != null && savedFile.exists()) {
            Log.d("ASR", "录音文件保存成功: ${savedFile.absolutePath}")

            // 获取录音信息
            val recordingInfo = audioRecorderManager.getRecordingInfo(savedFile.absolutePath)

            // 创建备忘录对象
            val memo = Memo(
                id = memos.size + 1,
                title = "语音备忘 ${memos.size + 1}",
                content = "录音保存于 ${getCurrentDateTime()}",
                date = getCurrentDateTime(),
                recordingPath = savedFile.absolutePath,
                recordingDuration = recordingInfo?.duration ?: 0L,
                asrContent = null // 后续可以通过ASR识别填充
            )

            // 添加到备忘录列表
            memos.add(memo)
            memoAdapter.notifyItemInserted(memos.size - 1)

            // 持久化保存
            saveMemos()

            Log.d("ASR", "备忘录已保存: ${memo.title}, 路径: ${memo.recordingPath}")

            // 显示成功提示
            showGestureToast(2)
            showStatusMessage("录音已保存到备忘录")
        } else {
            Log.e("ASR", "录音文件不存在或保存失败")

            // 创建一个没有录音文件的备忘录（包含ASR识别内容）
            val asrContent = aiViewModel.recognizerBuffer.value
            if (asrContent.isNotEmpty()) {
                val memo = Memo(
                    id = memos.size + 1,
                    title = "语音备忘 ${memos.size + 1}",
                    content = "识别内容: $asrContent",
                    date = getCurrentDateTime(),
                    recordingPath = null,
                    recordingDuration = 0L,
                    asrContent = asrContent
                )

                memos.add(memo)
                memoAdapter.notifyItemInserted(memos.size - 1)

                // 持久化保存
                saveMemos()

                showGestureToast(2)
                showStatusMessage("语音识别内容已保存到备忘录")
                Log.d("ASR", "保存ASR内容到备忘录: $asrContent")
            } else {
                Toast.makeText(this, "没有录音文件或识别内容可保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveRecording() {
        stopRecording()
        stopRecording()
        if (recordingFile != null && recordingFile!!.exists()) {
            // 获取录音信息
            val recordingInfo = audioRecorderManager.getRecordingInfo(recordingFile!!.absolutePath)

            val memo = Memo(
                id = memos.size + 1,
                title = "语音备忘 ${memos.size + 1}",
                content = "录音保存于 ${getCurrentDateTime()}",
                date = getCurrentDateTime(),
                recordingPath = recordingFile!!.absolutePath,
                recordingDuration = recordingInfo?.duration ?: 0L,
                asrContent = null // 后续可以通过ASR识别填充
            )
            memos.add(memo)
            memoAdapter.notifyItemInserted(memos.size - 1)

            // 持久化保存
            saveMemos()

            showStatusMessage("录音已保存到备忘录")
            Toast.makeText(this, "备忘录已保存！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "保存失败：录音文件不存在", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 删除备忘录
     */
    private fun deleteMemo(memo: Memo) {
        // 如果有录音文件，也一并删除
        memo.recordingPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Log.w(TAG, "Failed to delete recording file: $path")
                }
            }
        }

        // 从列表中移除
        memoAdapter.removeMemo(memo)

        // 持久化保存
        saveMemos()

        // 更新空状态显示
        updateEmptyState()

        Toast.makeText(this, "备忘录已删除", Toast.LENGTH_SHORT).show()
    }
//    private fun hintRecordStatus() {
//        isRecording = false
//        runOnUiThread {
//            // 使用退出动画隐藏录音界面
//            val exitAnimation = AnimationUtils.loadAnimation(this, R.anim.button_exit_animation)
//            holdInteractionOverlay.startAnimation(exitAnimation)
//            exitAnimation.setAnimationListener(object :
//                android.view.animation.Animation.AnimationListener {
//                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
//                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
//                    holdInteractionOverlay.visibility = View.GONE
//
//                    // 显示静态按钮
//                    staticButton.visibility = View.VISIBLE
//                    val enterAnimation = AnimationUtils.loadAnimation(
//                        this@MainActivity,
//                        R.anim.button_enter_animation
//                    )
//                    staticButton.startAnimation(enterAnimation)
//                }
//
//                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
//            })
//
//            // Stop waveform animation
//            stopWaveformAnimation()
//
//            Log.d("MainActivity", "Recording stopped, static button shown")
//
//        }
//    }


    // 回调接口
    interface OnChatListener {
        fun onSendMessage(message: String)
        fun onStartVoiceRecord()
        fun onStopVoiceRecord()
        fun onCancelVoiceRecord()
        fun onExpandMenuClick()
        fun onDialogClosed()
    }

    /**
     * 备忘录持久化管理器
     */
    private class MemoManager(private val context: Context) {
        companion object {
            private const val PREF_NAME = "memo_storage"
            private const val KEY_MEMOS = "saved_memos"
            private const val TAG = "MemoManager"
        }

        private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        private val gson = Gson()

        /**
         * 保存备忘录列表
         */
        fun saveMemos(memos: List<Memo>) {
            try {
                val jsonString = gson.toJson(memos)
                prefs.edit().putString(KEY_MEMOS, jsonString).apply()
                Log.d(TAG, "备忘录已保存，共${memos.size}条")
            } catch (e: Exception) {
                Log.e(TAG, "保存备忘录失败: ${e.message}")
            }
        }

        /**
         * 加载备忘录列表
         */
        fun loadMemos(): MutableList<Memo> {
            return try {
                val jsonString = prefs.getString(KEY_MEMOS, null)
                if (jsonString != null) {
                    val type = object : TypeToken<List<Memo>>() {}.type
                    val loadedMemos: List<Memo> = gson.fromJson(jsonString, type)

                    // 验证录音文件是否还存在
                    val validMemos = loadedMemos.filter { memo ->
                        if (memo.recordingPath != null) {
                            val file = File(memo.recordingPath)
                            val exists = file.exists()
                            if (!exists) {
                                Log.w(TAG, "录音文件不存在，跳过备忘录: ${memo.title}")
                            }
                            exists
                        } else {
                            true // 没有录音文件的备忘录保留
                        }
                    }

                    Log.d(TAG, "加载备忘录成功，共${validMemos.size}条（原${loadedMemos.size}条）")
                    validMemos.toMutableList()
                } else {
                    Log.d(TAG, "没有保存的备忘录数据")
                    mutableListOf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载备忘录失败: ${e.message}")
                mutableListOf()
            }
        }

        /**
         * 清除所有备忘录数据
         */
        fun clearAllMemos() {
            prefs.edit().remove(KEY_MEMOS).apply()
            Log.d(TAG, "已清除所有备忘录数据")
        }
    }

    /**
     * 加载保存的备忘录
     */
    private fun loadMemos() {
        try {
            val savedMemos = memoManager.loadMemos()
            memos.clear()
            memos.addAll(savedMemos)
            memoAdapter.notifyDataSetChanged()
            Log.d(TAG, "备忘录加载完成，共${memos.size}条")
        } catch (e: Exception) {
            Log.e(TAG, "加载备忘录失败: ${e.message}")
        }
    }

    /**
     * 保存备忘录数据
     */
    private fun saveMemos() {
        try {
            memoManager.saveMemos(memos)
        } catch (e: Exception) {
            Log.e(TAG, "保存备忘录失败: ${e.message}")
        }
    }



}