package com.dexter.little_smart_chat.config

import android.content.Context
import android.content.SharedPreferences

/**
 * AI配置文件
 * 安全管理API密钥和配置信息
 */
object AIConfig {
    
    private const val PREF_NAME = "ai_config"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
    private const val KEY_GOOGLE_API_KEY = "google_api_key"
    private const val KEY_CURRENT_PROVIDER = "current_provider"
    private const val KEY_DEFAULT_MODEL = "default_model"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_MAX_TOKENS = "max_tokens"
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化配置
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 设置OpenAI API密钥
     */
    fun setOpenAIApiKey(apiKey: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取OpenAI API密钥
     */
    fun getOpenAIApiKey(): String? {
        return prefs.getString(KEY_OPENAI_API_KEY, null)
    }
    
    /**
     * 设置Anthropic API密钥
     */
    fun setAnthropicApiKey(apiKey: String) {
        prefs.edit().putString(KEY_ANTHROPIC_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取Anthropic API密钥
     */
    fun getAnthropicApiKey(): String? {
        return prefs.getString(KEY_ANTHROPIC_API_KEY, null)
    }
    
    /**
     * 设置Google API密钥
     */
    fun setGoogleApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GOOGLE_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取Google API密钥
     */
    fun getGoogleApiKey(): String? {
        return prefs.getString(KEY_GOOGLE_API_KEY, null)
    }
    
    /**
     * 设置当前提供商
     */
    fun setCurrentProvider(provider: String) {
        prefs.edit().putString(KEY_CURRENT_PROVIDER, provider).apply()
    }
    
    /**
     * 获取当前提供商
     */
    fun getCurrentProvider(): String {
        return prefs.getString(KEY_CURRENT_PROVIDER, "openai") ?: "openai"
    }
    
    /**
     * 设置默认模型
     */
    fun setDefaultModel(model: String) {
        prefs.edit().putString(KEY_DEFAULT_MODEL, model).apply()
    }
    
    /**
     * 获取默认模型
     */
    fun getDefaultModel(): String {
        return prefs.getString(KEY_DEFAULT_MODEL, "gpt-3.5-turbo") ?: "gpt-3.5-turbo"
    }
    
    /**
     * 设置温度参数
     */
    fun setTemperature(temperature: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
    }
    
    /**
     * 获取温度参数
     */
    fun getTemperature(): Float {
        return prefs.getFloat(KEY_TEMPERATURE, 0.7f)
    }
    
    /**
     * 设置最大token数
     */
    fun setMaxTokens(maxTokens: Int) {
        prefs.edit().putInt(KEY_MAX_TOKENS, maxTokens).apply()
    }
    
    /**
     * 获取最大token数
     */
    fun getMaxTokens(): Int {
        return prefs.getInt(KEY_MAX_TOKENS, 2048)
    }
    
    /**
     * 根据提供商获取API密钥
     */
    fun getApiKeyForProvider(provider: String): String? {
        return when (provider.lowercase()) {
            "openai" -> getOpenAIApiKey()
            "anthropic" -> getAnthropicApiKey()
            "google" -> getGoogleApiKey()
            else -> null
        }
    }
    
    /**
     * 检查是否有可用的API密钥
     */
    fun hasValidApiKey(): Boolean {
        return getOpenAIApiKey() != null || 
               getAnthropicApiKey() != null || 
               getGoogleApiKey() != null
    }
    
    /**
     * 清除所有配置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 获取配置摘要
     */
    fun getConfigSummary(): String {
        return """
            当前提供商: ${getCurrentProvider()}
            默认模型: ${getDefaultModel()}
            温度: ${getTemperature()}
            最大Token: ${getMaxTokens()}
            OpenAI API: ${if (getOpenAIApiKey() != null) "已配置" else "未配置"}
            Anthropic API: ${if (getAnthropicApiKey() != null) "已配置" else "未配置"}
            Google API: ${if (getGoogleApiKey() != null) "已配置" else "未配置"}
        """.trimIndent()
    }
} 