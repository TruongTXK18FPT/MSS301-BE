package com.mss301.chatbotservice.enums; // Package chứa enum nhà cung cấp LLM.

public enum LLMProvider { // Enum mô tả các nhà cung cấp mô hình ngôn ngữ khả dụng.
    MISTRAL, // Nhà cung cấp Mistral AI.
    GEMINI, // Nhà cung cấp Google Gemini.
    N8N, // Nền tảng workflow N8N thực hiện gọi mô hình.
    OLLAMA // Nền tảng Ollama chạy mô hình cục bộ.
}
