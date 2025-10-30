package com.mss301.paymentservice.repository; 
 
import com.mss301.paymentservice.model.PaymentCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Command Repository
@Repository
public interface PaymentCommandRepository extends JpaRepository<PaymentCommand, Long> {
   PaymentCommand findBySubscriptionId(Long orderId);
}
