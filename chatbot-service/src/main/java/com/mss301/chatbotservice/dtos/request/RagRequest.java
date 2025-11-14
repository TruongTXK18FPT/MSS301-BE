package com.mss301.chatbotservice.dtos.request; // Package định nghĩa DTO yêu cầu RAG.

import com.mss301.chatbotservice.enums.LLMProvider; // Import enum nhà cung cấp LLM nội bộ.
import com.mss301.chatbotservice.enums.ResponseMode; // Import enum chế độ phản hồi nội bộ.
import lombok.Builder; // Sử dụng Lombok Builder để tạo đối tượng.
import lombok.Builder.Default; // Lombok Builder.Default giữ giá trị mặc định.
import lombok.Data; // Lombok Data sinh sẵn getter/setter.

@Data // Sinh getter/setter/toString/equals/hashCode.
@Builder // Cho phép khởi tạo bằng pattern builder.
public class RagRequest { // DTO gửi yêu cầu tới RAG service.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private String documentId = ""; // ID tài liệu ưu tiên lọc ngữ cảnh.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private String chapterId = ""; // ID chương ưu tiên lọc ngữ cảnh.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private String lessonId = ""; // ID bài học ưu tiên lọc ngữ cảnh.
    private String fileStoreName; // Google File Search Store name (cho file-search).
    private String queryText; // Câu hỏi người dùng.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private ResponseMode mode = ResponseMode.CHAT; // Chế độ phản hồi mong muốn.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private LLMProvider llmProvider = LLMProvider.GEMINI; // Nhà cung cấp LLM sử dụng.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private boolean useSemantic = true; // Cho phép search semantic.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private Boolean useDocuments = false; // Bật/tắt nghiền tài liệu cụ thể.
    @Default // Giữ giá trị mặc định khi dùng builder.
    private Integer topK = 7; // Số đoạn kết quả cần lấy.
}
