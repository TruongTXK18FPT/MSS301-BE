package com.mss301.paymentservice.model;

import com.mss301.paymentservice.constant.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Query/Read Model for MongoDB (CQRS pattern)
 * Optimized for read operations and analytics
 */
@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentQuery {
    @Id
    private String orderId;

    private Long userId;
    private Long subscriptionId;
    private Long planId;
    private Long amount;
    private String orderInfo;

    // PayOS fields
    private Long payosOrderCode; // PayOS orderCode (timestamp) - used to find payment from return URL
    private String payosPaymentLinkId;
    private String payosTransactionRef;
    private String paymentUrl;

    // Bank details
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String counterAccountName;
    private String counterAccountNumber;
    private LocalDateTime transactionDatetime;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Status
    private Status status;
}
