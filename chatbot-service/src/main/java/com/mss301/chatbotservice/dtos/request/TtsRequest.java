package com.mss301.chatbotservice.dtos.request; // Package chứa DTO yêu cầu TTS.

import com.mss301.chatbotservice.enums.ResponseMode; // Dùng enum nội bộ để đồng bộ với RAG.
import lombok.AllArgsConstructor; // Lombok sinh constructor đầy đủ tham số.
import lombok.Data; // Lombok Data sinh getter/setter/toString.
import lombok.NoArgsConstructor; // Lombok sinh constructor không tham số.

@Data // Bảo đảm có getter/setter.
@NoArgsConstructor // Hỗ trợ khởi tạo mặc định.
@AllArgsConstructor // Hỗ trợ khởi tạo đủ tham số.
public class TtsRequest { // DTO gửi yêu cầu chuyển văn bản thành giọng nói.
    private String text; // Nội dung văn bản cần chuyển đổi.
    private ResponseMode mode; // Chế độ phản hồi để đồng bộ xử lý.
}
