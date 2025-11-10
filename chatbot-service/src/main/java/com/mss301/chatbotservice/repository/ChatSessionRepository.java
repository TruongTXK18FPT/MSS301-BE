package com.mss301.chatbotservice.repository; 
 
import com.mss301.chatbotservice.model.ChatSession; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List;
import java.util.Optional;

@Repository 
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> { 
   Optional<ChatSession> findById(Long id);
   List<ChatSession> findAll();
   List<ChatSession> findByUserId(Long userId);
} 
