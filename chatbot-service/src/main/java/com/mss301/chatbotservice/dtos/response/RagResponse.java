package com.mss301.chatbotservice.dtos.response; // Package chứa DTO phản hồi RAG.

import com.mss301.chatbotservice.enums.LLMProvider; // Dùng enum nội bộ nhà cung cấp LLM.
import com.mss301.chatbotservice.enums.ResponseMode; // Dùng enum nội bộ chế độ phản hồi.
import lombok.AllArgsConstructor; // Lombok sinh constructor đủ tham số.
import lombok.Data; // Lombok Data sinh getter/setter/toString.
import lombok.NoArgsConstructor; // Lombok sinh constructor không tham số.

import java.time.LocalDateTime; // Thời gian tạo phản hồi.

@Data // Sinh getter/setter/toString/equals/hashCode.
@NoArgsConstructor // Giúp deserialization không tham số.
@AllArgsConstructor // Cho phép khởi tạo đầy đủ giá trị.
public class RagResponse { // DTO phản hồi từ RAG service.
    private ResponseMode mode; // Chế độ phản hồi áp dụng.
    private LLMProvider llmProvider; // Nhà cung cấp LLM xử lý yêu cầu.
    private String queryText; // Nội dung câu hỏi ban đầu.
    private Object content; // Nội dung phản hồi (text/mindmap/exercise).
    private LocalDateTime timestamp; // Dấu thời gian phản hồi.
    private int chunksUsed; // Số đoạn context đã tiêu thụ.
}
