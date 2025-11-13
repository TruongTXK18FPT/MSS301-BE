package com.mss301.paymentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Technical audit log for webhook events (idempotency check)
 */
@Entity
@Table(name = "payment_webhook_log", indexes = @Index(name = "idx_webhook_id", columnList = "webhook_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id", nullable = false, unique = true, length = 100)
    private String webhookId; // paymentLinkId + signature hash

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload; // JSON webhook data

    @Column(name = "signature", length = 500)
    private String signature;

    @Builder.Default
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
