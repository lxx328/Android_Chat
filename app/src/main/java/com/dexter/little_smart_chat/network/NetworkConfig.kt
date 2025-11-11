package com.dexter.little_smart_chat.network

/**
 * 网络配置类
 * 提供统一的网络配置和常量定义
 */
object NetworkConfig {
    
    // 基础配置
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 60L
    const val WRITE_TIMEOUT = 60L
    
    // API配置
    const val BASE_URL_OPENAI = "https://api.openai.com/v1/"
    const val BASE_URL_ANTHROPIC = "https://api.anthropic.com/v1/"
    const val BASE_URL_GOOGLE = "https://generativelanguage.googleapis.com/v1/"
    const val BASE_URL_CUSTOM = "https://your-custom-api.com/"
    
    // 请求头配置
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_USER_AGENT = "User-Agent"
    
    // 默认User-Agent
    const val DEFAULT_USER_AGENT = "LittleSmartChat/1.0"
} 