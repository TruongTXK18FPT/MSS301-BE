package com.mss301.paymentservice.model;

import com.mss301.paymentservice.constant.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payment_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQuery {
    @Id
    private String id;
    private Long paymentId;
    private Long subscriptionId;
    private Long userId;
    private Long amount;
    private String orderInfo;
    private String momoRequestId;
    private String momoTransId;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status;
}
