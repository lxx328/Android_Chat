# LittleSmartChat 功能说明

## 📱 应用概述
LittleSmartChat 是一个智能聊天应用，支持语音录制、AI对话和多种大模型适配。

## ✨ 主要功能

### 1. 录音系统
- **长按录音**: 长按按钮开始录音，松开停止
- **动态波形**: 录音时显示Apple风格的动态波形动画
- **文件管理**: 自动保存到应用私有目录，支持文件验证和清理
- **音频格式**: 支持AAC格式，44.1kHz采样率

### 2. 动画系统
- **Apple风格设计**: 圆角、渐变、自然缓动效果
- **按钮动画**: 进入/退出动画，呼吸效果
- **波形动画**: 8帧流畅波形，100ms间隔
- **充电动画**: 4帧电池充电效果，300ms间隔

### 3. 状态栏显示
- **实时时间**: 每分钟自动更新
- **WiFi状态**: 连接/断开状态显示
- **电池状态**: 三色显示（绿/黄/红），充电动画

### 4. AI框架
- **多模型支持**: OpenAI GPT、Anthropic Claude、Google Gemini
- **响应式接口**: 支持阻塞和流式响应
- **安全隔离**: 独立的网络和模型适配层

### 5. 网络框架
- **Retrofit集成**: 统一的网络请求管理
- **认证拦截器**: 自动处理不同API的认证
- **日志拦截器**: 详细的请求/响应日志

## 🎨 设计特色

### Apple风格元素
- 圆角设计语言
- 柔和的动画过渡
- 自然的缓动效果
- 一致的视觉风格

### 动画效果
- 呼吸效果（1000ms周期）
- 进入/退出动画（overshoot/accelerate插值）
- 波形动态效果
- 充电流光效果

## 🔧 技术架构

### 核心组件
- **AudioRecorderManager**: 录音管理
- **AudioPlayerManager**: 播放管理
- **StatusBarUpdater**: 状态栏更新
- **AIModelManager**: AI模型管理
- **NetworkManager**: 网络请求管理
- **ToastUtils**: 统一提示管理
- **AudioUtils**: 音频工具类

### 文件结构
```
app/src/main/
├── java/com/dexter/little_smart_chat/
│   ├── audio/              # 音频管理
│   ├── utils/              # 工具类
│   ├── network/            # 网络框架
│   ├── ai/                 # AI适配框架
│   ├── service/            # 后台服务
│   └── mvvm/               # MVVM架构
├── res/
│   ├── drawable/           # 图标和动画
│   ├── anim/              # 动画资源
│   └── layout/            # 布局文件
```

## 📋 权限要求
- RECORD_AUDIO: 录音功能
- ACCESS_WIFI_STATE: WiFi状态检测
- ACCESS_NETWORK_STATE: 网络状态检测
- READ_MEDIA_AUDIO: 音频文件访问
- MODIFY_AUDIO_SETTINGS: 音频设置修改

## 🚀 使用说明

### 录音功能
1. 长按录音按钮开始录音
2. 拖拽到左侧取消录音
3. 拖拽到右侧保存为备忘录
4. 松开按钮发送给AI

### AI对话
1. 在设置中配置API密钥
2. 选择AI模型（GPT/Claude/Gemini）
3. 发送语音或文字消息
4. 支持流式和阻塞式响应

### 文件管理
- 录音文件自动保存
- 应用启动时清理无效文件
- 存储空间自动监控
- 支持手动清理

## 🔧 配置说明

### API配置
```kotlin
AIConfig.setOpenAIApiKey("your-api-key")
AIConfig.setCurrentProvider("openai")
```

### 录音配置
- 采样率: 44.1kHz
- 编码: AAC
- 比特率: 128kbps
- 格式: MP4

### 动画配置
- 波形帧数: 8帧
- 充电帧数: 4帧
- 按钮动画: 400ms进入，200ms退出
- 呼吸动画: 1000ms周期

## 📞 技术支持
如有问题，请查看日志输出或联系开发团队。 