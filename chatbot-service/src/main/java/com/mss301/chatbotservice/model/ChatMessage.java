package com.mss301.chatbotservice.model;

import com.mss301.chatbotservice.enums.ChatRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
@Table(name="chat_messages")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_messages_id")
    private Long id;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private ChatRole role;

    @Column(name = "content")
    private String content;

    @Column(name = "tokens_used")
    private Long tokensUsed;

    @Column(name = "create_at", updatable = false)
    @CreationTimestamp()
    private LocalDateTime createdAt;

}
