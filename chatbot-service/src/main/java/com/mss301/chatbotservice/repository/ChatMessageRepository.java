package com.mss301.chatbotservice.repository; 
 
import com.mss301.chatbotservice.model.ChatMessage; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
 
import java.util.List; 
 
@Repository 
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> { 
   ChatMessage findByChatMessageId(Long chatMessageId); 
   List<ChatMessage> findAll(); 
} 
