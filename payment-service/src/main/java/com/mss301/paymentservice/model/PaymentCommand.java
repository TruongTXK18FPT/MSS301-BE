package com.mss301.paymentservice.model;

import com.mss301.paymentservice.constant.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Command Entity (Write Model - PostgreSQL)
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCommand {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "order_info", length = 500)
    private String orderInfo;

    // PayOS fields (renamed from MoMo)
    @Column(name = "payos_order_code")
    private Long payosOrderCode; // PayOS orderCode (timestamp) - used to find payment from return URL

    @Column(name = "payos_payment_link_id", length = 100)
    private String payosPaymentLinkId;

    @Column(name = "payos_transaction_ref", length = 100)
    private String payosTransactionRef;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    // Bank transaction details
    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "counter_account_name", length = 200)
    private String counterAccountName;

    @Column(name = "counter_account_number", length = 50)
    private String counterAccountNumber;

    @Column(name = "transaction_datetime")
    private LocalDateTime transactionDatetime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
}