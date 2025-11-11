//package com.dexter.h20_local_ai_demo.utils
//
//import com.dexter.h20_local_ai_demo.Data.ChatMessage
//
//
//object DataParseUtils{
//
//    /*
//    * message parse utlis
//    */
//// 工具函数：流式内容分段，返回List<ChatMessage>
//    fun parseStreamContentToChatMessages(content: String, isLeft: Boolean): List<ChatMessage> {
//        val result = mutableListOf<ChatMessage>()
//        val urlPattern = "(https?://[^\\s]+?\\.(png|jpg|jpeg|gif|mp4|mp3))".toRegex(RegexOption.IGNORE_CASE)
//        val webPattern = "(https?://[^\\s]+)".toRegex(RegexOption.IGNORE_CASE)
//        var lastIndex = 0
//        urlPattern.findAll(content).forEach { match ->
//            val range = match.range
//            // 前面的文本
//            if (range.first > lastIndex) {
//                val text = content.substring(lastIndex, range.first)
//                if (text.isNotBlank()) result.add(ChatMessage.Text(text, isLeft))
//            }
//            // 只加图片/视频，不加URL文本
//            val url = match.value
//            if (url.endsWith(".mp4", true) ||(url.endsWith(".mp3", true))) {
//                result.add(ChatMessage.Video(url, isLeft))
//            } else {
//                result.add(ChatMessage.Image(url, isLeft))
//            }
//            lastIndex = range.last + 1
//        }
//        // 剩余部分再查找网页链接
//        if (lastIndex < content.length) {
//            val rest = content.substring(lastIndex)
//            var lastWebIndex = 0
//            webPattern.findAll(rest).forEach { match ->
//                val range = match.range
//                if (range.first > lastWebIndex) {
//                    val text = rest.substring(lastWebIndex, range.first)
//                    if (text.isNotBlank()) result.add(ChatMessage.Text(text, isLeft))
//                }
//                val url = match.value
//                result.add(ChatMessage.Web(url, isLeft))
//                lastWebIndex = range.last + 1
//            }
//            // 剩余部分再查找文本
//            if (lastWebIndex < rest.length) {
//                val text = rest.substring(lastWebIndex)
//                if (text.isNotBlank()) result.add(ChatMessage.Text(text, isLeft))
//            }
//        }
//        return result
//    }
//}