package com.mss301.chatbotservice.enums; // Định nghĩa package chứa enum chế độ phản hồi.

public enum ResponseMode { // Enum diễn tả các chế độ phản hồi chatbot hỗ trợ.
    CHAT, // Chế độ trò chuyện văn bản cơ bản.
    MINDMAP, // Chế độ tạo sơ đồ tư duy.
    VOICECHAT, // Chế độ hội thoại bằng giọng nói/TTS.
    EXERCISE // Chế độ luyện tập/bài tập.
}
