package com.dexter.little_smart_chat.utils

import kotlin.random.Random

/**
 * 开场白工具类
 * 提供随机开场白功能，支持用户名自定义
 */
object GreetingUtils {

    /**
     * 开场白列表，使用 {userName} 作为用户名占位符
     */
    private val greetingList = listOf(
        "您好，{userName}！![笑脸]欢迎使用我们的系统！\n(android.resource://com.xctech.esop/mipmap/smiling_face)",
        "早上好，{userName}！![微笑]希望您今天工作顺利！\n(android.resource://com.xctech.esop/mipmap/winking_smiling_face)",
        "下午好，{userName}！![AI大脑]让我们开始今天的工作吧！\n(android.resource://com.xctech.esop/mipmap/ai_brain)",
        "晚上好，{userName}！![星星]感谢您的辛勤工作！\n(android.resource://com.xctech.esop/mipmap/star_1)",
        "嗨，{userName}！![开心]很高兴再次见到您！\n(android.resource://com.xctech.esop/mipmap/winking_smiling_face_two)",
        "欢迎回来，{userName}！![准备就绪]\n(android.resource://com.xctech.esop/mipmap/switch_1)准备好迎接新的挑战了吗？",
        "您好，{userName}！![思考]\n(android.resource://com.xctech.esop/mipmap/ai_brain)今天有什么计划呢？",
        "亲爱的{userName}，![合作]\n(android.resource://com.xctech.esop/mipmap/smiling_face_two)让我们一起提高工作效率吧！",
        "尊敬的{userName}，![欢迎]\n(android.resource://com.xctech.esop/mipmap/winking_smiling_face)欢迎使用ESOP系统！",
        "嗨，{userName}！![新开始]\n(android.resource://com.xctech.esop/mipmap/star_1_checked)新的一天，新的开始！",
        "您好，{userName}！![美好]让我们把今天变得更美好！(\n" +
                "android.resource://com.xctech.esop/mipmap/smiling_face)",
        "早安，{userName}！![活力]愿您今天充满活力！\n" +
                "(android.resource://com.xctech.esop/mipmap/winking_smiling_face_two)",
        "问候，{userName}！![合作伙伴]期待与您的愉快合作！\n" +
                "(android.resource://com.xctech.esop/mipmap/ai_brain)",
        "欢迎，{userName}！![价值创造]让我们一起创造价值！\n" +
                "(android.resource://com.xctech.esop/mipmap/star_1)",
        "您好，{userName}！![精彩一天]准备好迎接精彩的一天了吗？\n" +
                "(android.resource://com.xctech.esop/mipmap/smiling_face_two)"
    )


    /**
     * 随机获取一个开场白
     * @param userName 用户名，如果为空则使用默认的"用户"
     * @return 包含用户名的开场白字符串
     */
    fun rand(userName: String = "用户"): String {
        val randomGreeting = greetingList[Random.nextInt(greetingList.size)]
        return randomGreeting.replace("{userName}", userName)
    }


    /**
     * 随机获取一个开场白（重载方法，兼容不同调用方式）
     * @param userName 用户名
     * @return 包含用户名的开场白字符串
     */
    fun random(userName: String = "用户"): String {
        return rand(userName)
    }

    /**
     * 获取所有开场白模板（用于测试或其他用途）
     * @return 开场白模板列表
     */
    fun getAllGreetingTemplates(): List<String> {
        return greetingList.toList()
    }

    /**
     * 获取开场白数量
     * @return 开场白总数
     */
    fun getGreetingCount(): Int {
        return greetingList.size
    }

    /**
     * 根据时间段获取合适的开场白
     * @param userName 用户名
     * @param hour 小时数 (0-23)
     * @return 包含用户名的时间相关开场白
     */
    fun getTimeBasedGreeting(userName: String = "用户", hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): String {
        val timeBasedGreeting = when (hour) {
            in 7..12 -> "{userName}，早上好！😀 希望您今天工作顺利！公司正在做精益调研，如果你有好的想法和建议，请跟我说说吧<br>![早晨](mipmap://esop_chat_back_2)"
            in 13..16 -> "{userName}，下午好！🌹今天上班还愉快吗？公司正在做精益调研，如果你有好的想法和建议，请跟我说说吧<br>![工作](mipmap://esop_chat_back_2)"
            in 17..23 -> "{userName}，晚上好！🌙 辛苦了一天了，我们来聊聊今天生产中的收获或者烦恼吧。公司正在做精益调研，如果你有好的想法和建议，请跟我说说吧<br>![感谢](mipmap://esop_chat_back_2)"
            else -> "{userName}，您好，！很高兴见到您！"
        }
        return timeBasedGreeting.replace("{userName}", userName)
    }

    fun getTimeBasedGreetingLLM(userName: String = "承智灵"): String {
        val timeBasedGreeting = when (userName) {
            "承智灵" -> "Hi! 我是承智灵 — 祥承经营理念的实践建议者，我可以结合你的实际情况以及对应的经营理念和相关资料，给到一些更落地的行动建议。"
            "祥机智" -> "Hi！我是祥机智 — 祥承经营理念的传播大使，我熟知祥承所有的经营理念和对应的书面文档内容，可以找我了解相应原文和资料哦！"
            else -> "{userName}，您好，！很高兴见到您！"
        }
        return timeBasedGreeting.replace("{userName}", userName)
    }

    fun processTTSText(text: String): String {
        if (text.isBlank()) return text

        var processedText = text

        // 1. 首先处理最可能引起问题的内容：表情符号和特殊字符
        processedText = removeEmojisAndSpecialChars(processedText)

        // 2. 移除所有HTML标签
        processedText = processedText.replace(Regex("<[^>]*>"), "")

        // 3. 移除图片标记（各种协议格式）
        processedText = removeImageMarkers(processedText)

        // 4. 移除表格内容
        processedText = removeTableContent(processedText)

        // 5. 移除Markdown格式
        processedText = removeMarkdownFormatting(processedText)

        // 6. 最终清理和规范化
        processedText = finalCleanup(processedText)

        return processedText
    }

    private fun removeEmojisAndSpecialChars(text: String): String {
        var result = text

        // Unicode表情符号范围（全面覆盖）
        val emojiRanges = arrayOf(
            "\uD83C[\uDF00-\uDFFF]", "\uD83D[\uDC00-\uDDFF]", "\uD83E[\uDD00-\uDDFF]",
            "[\u2600-\u26FF]", "[\u2700-\u27BF]", "[\u2300-\u23FF]",
            "[\u2B50-\u2B55]", "[\u2934-\u2935]", "[\u3030-\u303F]",
            "[\u3297-\u3299]", "[\uFE00-\uFE0F]", "[\u2190-\u21FF]"
        )

        emojiRanges.forEach { range ->
            result = result.replace(Regex(range), " ")
        }

        // 颜文字和文本表情
        val textEmoticons = listOf(
            Regex("""[:;=8B][\-\^]?[\)\(\\\/\|\[\]DdPpOo3*]"""), // :) :( :D :P
            Regex("""[\)\(\\\/\|\[\]DdPpOo3*][\-\^]?[:;=8B]"""), // (: ): D:
            Regex("""[xXoO*][_\-.][xXoO*]"""), // x_x o_o *-*
            Regex("""[Tt][_\-.][Tt]"""), // T_T t_t
            Regex("""[><][_\-.][><]"""), // >_< >-<
            Regex("""\^[\-\^]_?[\-\^]?\^"""), // ^_^ ^-^ ^^
            Regex("""-[\-_]?-"""), // -_- --
            Regex("""[oO][_\-.][oO]""") // o_o O_O
        )

        textEmoticons.forEach { pattern ->
            result = pattern.replace(result, " ")
        }

        // 移除复杂颜文字和键盘符号
        result = result.replace(Regex("""[╯╰°□°┻━┬ノ︵┛┗┓┏╮╭]"""), " ")

        // 移除中文表情标签
        result = result.replace(Regex("""\[[^\]]*?(?:表情|笑脸|哭脸|大笑|流泪|汗|晕|惊)[^\]]*?\]"""), " ")

        return result
    }

    private fun removeImageMarkers(text: String): String {
        var result = text

        // 所有可能的图片标记格式
        val imagePatterns = listOf(
            Regex("""!\[[^\]]*?\]\([^)]*?\)"""), // 通用markdown图片
            Regex("""<img[^>]*>"""), // HTML图片标签
            Regex("""!\[[^\]]*?\]\s*$"""), // 无链接的图片标记
            Regex("!\\[([^\\]]*)\\]\\(file:///android_asset/[^)]*\\)"),
            Regex("!\\[([^\\]]*)\\]\\(mipmap://[^)]*\\)"),
            Regex("!\\[([^\\]]*)\\]\\(drawable://[^)]*\\)"),
            Regex("!\\[([^\\]]*)\\]\\(https?://[^)]*\\)"),
            Regex("!\\[([^\\]]*)\\]\\(android\\.resource://[^)]*\\)") // 新增：处理android.resource协议
        )

        imagePatterns.forEach { pattern ->
            result = pattern.replace(result, " ")
        }

        return result
    }

    private fun removeTableContent(text: String): String {
        var result = text

        // 表格行模式
        val tablePatterns = listOf(
            Regex("""^\s*\|[^\n]*\|\s*$""", RegexOption.MULTILINE), // 标准表格行
            Regex("""^\s*\+[-+]+\+\s*$""", RegexOption.MULTILINE), // 表格分隔线
            Regex("""^\s*[\-+=\|]+\s*$""", RegexOption.MULTILINE), // 表格边框
            Regex("""^\s*\|.*\{[^}]*\}.*\|\s*$""", RegexOption.MULTILINE) // 含变量的表格
        )

        tablePatterns.forEach { pattern ->
            result = pattern.replace(result, " ")
        }

        return result
    }

    fun removeMarkdownFormatting(text: String): String {
        var result = text

        // 先处理字符串替换的模式
        val stringReplacePatterns = listOf(
            Regex("""```[^`]*?```""", RegexOption.DOT_MATCHES_ALL) to " ",
            Regex("""~~~[^~]*?~~~""", RegexOption.DOT_MATCHES_ALL) to " ",
            Regex("""^#{1,6}\s*""", RegexOption.MULTILINE) to " ",
            Regex("""^[\s]*[-*+]\s+""", RegexOption.MULTILINE) to " ",
            Regex("""^[\s]*\d+\.\s+""", RegexOption.MULTILINE) to " ",
            Regex("""^>\s*""", RegexOption.MULTILINE) to " ",
            Regex("""^[\s*_-]{3,}\s*$""", RegexOption.MULTILINE) to " ",
        )

        // 再处理函数替换的模式
        val functionReplacePatterns = listOf(
            Regex("""`[^`]*?`""") to { match: MatchResult -> match.value.replace("`", "") },
            Regex("""\[([^\]]+)\]\([^)]+\)""") to { match: MatchResult -> match.groupValues[1] },
            Regex("""\*\*([^*]+?)\*\*""") to { match: MatchResult -> match.groupValues[1] },
            Regex("""__([^_]+?)__""") to { match: MatchResult -> match.groupValues[1] },
            Regex("""\*([^*]+?)\*""") to { match: MatchResult -> match.groupValues[1] },
            Regex("""_([^_]+?)_""") to { match: MatchResult -> match.groupValues[1] },
            // 将中文引号内容替换为逗号加内容的形式
            Regex("""“([^”]+)”""") to { match: MatchResult -> "，${match.groupValues[1]}" }
        )

        // 处理字符串替换
        stringReplacePatterns.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }

        // 处理函数替换
        functionReplacePatterns.forEach { (pattern, transformer) ->
            result = pattern.replace(result) { match -> transformer(match) }
        }

        return result
    }

    private fun finalCleanup(text: String): String {
        var result = text

        // 移除可能引起问题的特殊字符
//        val problematicChars = arrayOf(
//            "[", "]", "{", "}", "(", ")", "<", ">",
//            "|", "\\", "/", "*", "#", "@", "~", "`",
//            "^", "&", "%", "$", "\"", "'", "=", "+"
//        )
        val problematicChars = arrayOf(
            "[", "]", "{", "}", "(", ")",
            "|", "\\", "/", "~", "`",
            "^", "&", "\""
        )

        problematicChars.forEach { char ->
            result = result.replace(char, " ")
        }

        // 处理连续的特殊情况
        result = result
            .replace(Regex("-{2,}"), " ") // 多个连字符
            .replace(Regex("\\.{2,}"), " ") // 多个点
            .replace(Regex("_{2,}"), " ") // 多个下划线
            .replace(Regex("\\s*[:;]\\s*"), " ") // 冒号和分号

        // 多音字处理（可根据需要扩展）
        val polyphonicWords = mapOf(
            "咯" to "洛",
            "SOP" to "<letter>SOP</letter>",
            "sop" to "<letter>sop</letter>",
            "ESOP" to "<letter>ESOP</letter>",
            "esop" to "<letter>esop</letter>",
            "Esop" to "<letter>Esop</letter>",
        )

        polyphonicWords.forEach { (from, to) ->
            result = result.replace(from, to)
        }

        // 最终空白处理
        result = result
            .replace(Regex("\\s+"), " ") // 多个空白合并为一个空格
            .replace(Regex("^\\s+|\\s+$"), "") // 去除首尾空白
            .replace(Regex("^[\\p{P}\\s]+|[\\p{P}\\s]+$"), "") // 去除首尾标点和空白

        // 安全检查：确保结果不为空且不是纯符号
        if (result.isBlank() || result.all { it.isWhitespace() || it in problematicChars.joinToString("") }) {
            return ""
        }

        return result
    }

    // 扩展函数用于安全的模式替换
    private fun Regex.replace(input: String, transformer: (MatchResult) -> String): String {
        return this.replace(input) { matchResult -> transformer(matchResult) }
    }

//    /**
//     * 处理TTS文本，移除所有markdown标记和HTML标签
//     */
//    fun processTTSText(text: String): String {
//        var processedText = text
//
//        // 1. 移除所有HTML标签（包括 <br>, <p>, <div> 等）
//        processedText = processedText.replace(Regex("<[^>]*>"), "")
//
//        // 2. 完全移除图片标记（不保留alt文本，直接替换为"如图"）
//        val imagePatterns = listOf(
//            Regex("!\\[([^\\]]*)\\]\\(file:///android_asset/[^)]*\\)"),
//            Regex("!\\[([^\\]]*)\\]\\(mipmap://[^)]*\\)"),
//            Regex("!\\[([^\\]]*)\\]\\(drawable://[^)]*\\)"),
//            Regex("!\\[([^\\]]*)\\]\\(https?://[^)]*\\)"),
//            Regex("!\\[([^\\]]*)\\]\\(android\\.resource://[^)]*\\)") // 新增：处理android.resource协议
//        )
//
//        imagePatterns.forEach { pattern ->
////            processedText = pattern.replace(processedText, "如图") // 统一替换为"如图"
//            processedText = pattern.replace(processedText, "") // 统一替换为"如图"
//        }
//
//        // 2.5. 移除表格内容（新增）
//        // 匹配以 | 开头和结尾的表格行，包含变量占位符的情况
//        val tablePatterns = listOf(
//            // 匹配表格行：以|开始，中间包含|分隔符，以|结束
//            Regex("^\\s*\\|.*\\|\\s*$", RegexOption.MULTILINE),
//            // 匹配包含变量占位符的表格行，如 {stationName}, {processCode} 等
//            Regex("^\\s*\\|.*\\{[^}]+\\}.*\\|\\s*$", RegexOption.MULTILINE)
//        )
//
//        tablePatterns.forEach { pattern ->
//            processedText = pattern.replace(processedText, "")
//        }
//
//        // 3. 移除其他markdown标记
//        processedText = processedText
//            // 移除链接标记 [文本](链接)
//            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)")) { it.groupValues[1] }
//            // 移除粗体标记 **文本** 和 __文本__
//            .replace(Regex("\\*\\*([^*]+)\\*\\*")) { it.groupValues[1] }
//            .replace(Regex("__([^_]+)__")) { it.groupValues[1] }
//            // 移除斜体标记 *文本* 和 _文本_
//            .replace(Regex("\\*([^*]+)\\*")) { it.groupValues[1] }
//            .replace(Regex("_([^_]+)_")) { it.groupValues[1] }
//            // 移除代码标记 `代码`
//            .replace(Regex("`([^`]+)`")) { it.groupValues[1] }
//            // 移除标题标记 # ## ### 等
//            .replace(Regex("^#{1,6}\\s*"), "")
//            // 移除列表标记 - * +
//            .replace(Regex("^[\\s]*[-*+]\\s+"), "")
//            // 移除有序列表标记 1. 2. 等
//            .replace(Regex("^[\\s]*\\d+\\.\\s+"), "")
//            // 移除引用标记 >
//            .replace(Regex("^>\\s*"), "")
//            // 移除水平分割线 --- *** ___
//            .replace(Regex("^\\s*[-*_]{3,}\\s*$"), "")
//
//        // 4. 处理特殊字符和多余空白
//        processedText = processedText
//            // 移除特殊符号（保留基本标点）
//            .replace(Regex("[\\[\\](){}]"), "")
//            // 移除"--"符号
//            .replace("--", "")
//            // 将"咯"替换为"洛"暂时如此又该后期对tts看是否有语音理解的模型 ，如咯咯（ge）笑，那好咯（lo）等多音字的判断
//            .replace("咯", "洛")
//            // 将多个空白字符合并为单个空格
//            .replace(Regex("\\s+"), " ")
//            // 移除行首行尾空白
//            .trim()
//
//
//        return processedText
//    }
}