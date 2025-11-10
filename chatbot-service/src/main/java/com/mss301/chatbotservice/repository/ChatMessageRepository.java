package com.mss301.chatbotservice.repository; 
 
import com.mss301.chatbotservice.model.ChatMessage; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List;
import java.util.Optional;

@Repository 
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> { 
   Optional<ChatMessage> findById(Long id);
   List<ChatMessage> findAll(); 
} 
