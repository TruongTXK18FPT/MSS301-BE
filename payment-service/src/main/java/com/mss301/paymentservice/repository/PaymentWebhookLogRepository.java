package com.mss301.paymentservice.repository;

import com.mss301.paymentservice.model.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {
    Optional<PaymentWebhookLog> findByWebhookId(String webhookId);

    boolean existsByWebhookId(String webhookId);
}
