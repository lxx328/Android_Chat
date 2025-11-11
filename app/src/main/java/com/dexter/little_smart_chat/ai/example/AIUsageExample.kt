package com.dexter.little_smart_chat.ai.example

import android.content.Context
import android.util.Log
import com.dexter.little_smart_chat.ai.manager.AIModelManager
import com.dexter.little_smart_chat.network.model.ApiRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * AI框架使用示例
 * 展示如何使用三个框架进行AI交互
 */
class AIUsageExample(private val context: Context) {
    
    private val aiManager = AIModelManager.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val TAG = "AIUsageExample"
    }
    
    /**
     * 初始化AI框架
     */
    fun initializeAI(apiKey: String, provider: String = "openai") {
        aiManager.initialize(apiKey, provider)
        Log.d(TAG, "AI框架已初始化，提供商: $provider")
    }
    
    /**
     * 示例1：阻塞式聊天
     */
    fun exampleBlockingChat() {
        scope.launch {
            try {
                val messages = listOf(
                    ApiRequest.Message(role = "user", content = "你好，请介绍一下自己")
                )
                
                val response = aiManager.chatCompletion(
                    messages = messages,
                    model = "gpt-3.5-turbo",
                    temperature = 0.7
                )
                
                if (response != null) {
                    Log.d(TAG, "AI回复: $response")
                } else {
                    Log.e(TAG, "聊天请求失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "阻塞式聊天错误: ${e.message}")
            }
        }
    }
    
    /**
     * 示例2：流式聊天
     */
    fun exampleStreamingChat() {
        scope.launch {
            try {
                val messages = listOf(
                    ApiRequest.Message(role = "user", content = "请写一首关于春天的诗")
                )
                
                aiManager.chatCompletionStream(
                    messages = messages,
                    model = "gpt-4",
                    temperature = 0.8
                ).collect { chunk ->
                    // 实时显示AI回复的每个片段
                    Log.d(TAG, "收到流式数据: $chunk")
                    print(chunk)
                }
            } catch (e: Exception) {
                Log.e(TAG, "流式聊天错误: ${e.message}")
            }
        }
    }
    
    /**
     * 示例3：语音转文字（暂未实现）
     */
    fun exampleSpeechToText(audioFile: java.io.File) {
        scope.launch {
            try {
                // TODO: 实现语音转文字功能
                Log.d(TAG, "语音转文字功能待实现")
            } catch (e: Exception) {
                Log.e(TAG, "语音转文字错误: ${e.message}")
            }
        }
    }
    
    /**
     * 示例4：文字转语音（暂未实现）
     */
    fun exampleTextToSpeech(text: String) {
        scope.launch {
            try {
                // TODO: 实现文字转语音功能
                Log.d(TAG, "文字转语音功能待实现")
            } catch (e: Exception) {
                Log.e(TAG, "文字转语音错误: ${e.message}")
            }
        }
    }
    
    /**
     * 示例5：切换模型提供商
     */
    fun exampleSwitchProvider() {
        try {
            // 切换到Anthropic Claude
            aiManager.switchProvider("anthropic")
            Log.d(TAG, "已切换到Anthropic提供商")
            
            scope.launch {
                try {
                    val messages = listOf(
                        ApiRequest.Message(role = "user", content = "用中文回答：什么是人工智能？")
                    )
                    
                    val response = aiManager.chatCompletion(
                        messages = messages,
                        model = "claude-3-sonnet-20240229"
                    )
                    
                    if (response != null) {
                        Log.d(TAG, "Claude回复: $response")
                    } else {
                        Log.e(TAG, "Claude请求失败")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Claude聊天错误: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换提供商错误: ${e.message}")
        }
    }
    
    /**
     * 示例6：健康检查
     */
    fun exampleHealthCheck() {
        scope.launch {
            try {
                val isHealthy = aiManager.healthCheck()
                
                if (isHealthy) {
                    Log.d(TAG, "AI服务健康")
                } else {
                    Log.w(TAG, "AI服务异常")
                }
            } catch (e: Exception) {
                Log.e(TAG, "健康检查错误: ${e.message}")
            }
        }
    }
    
    /**
     * 示例7：获取支持的模型列表
     */
    fun exampleGetSupportedModels() {
        try {
            val models = aiManager.getSupportedModels()
            Log.d(TAG, "当前支持的模型: $models")
        } catch (e: Exception) {
            Log.e(TAG, "获取模型列表错误: ${e.message}")
        }
    }
    
    /**
     * 示例8：多轮对话
     */
    fun exampleMultiTurnConversation() {
        scope.launch {
            try {
                val messages = mutableListOf<ApiRequest.Message>()
                
                // 第一轮对话
                messages.add(ApiRequest.Message(role = "user", content = "我想学习编程"))
                
                val response1 = aiManager.chatCompletion(
                    messages = messages.toList(),
                    model = "gpt-3.5-turbo"
                )
                
                response1?.let { reply ->
                    messages.add(ApiRequest.Message(role = "assistant", content = reply))
                    Log.d(TAG, "第一轮回复: $reply")
                    
                    // 第二轮对话
                    messages.add(ApiRequest.Message(role = "user", content = "我应该从哪种语言开始？"))
                    
                    val response2 = aiManager.chatCompletion(
                        messages = messages.toList(),
                        model = "gpt-3.5-turbo"
                    )
                    
                    response2?.let { reply2 ->
                        Log.d(TAG, "第二轮回复: $reply2")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "多轮对话错误: ${e.message}")
            }
        }
    }
} 