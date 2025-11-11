package com.mss301.paymentservice.model;

import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.model.dtos.response.SubscriptionResponse;
import com.mss301.paymentservice.model.dtos.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payment_db")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQuery {
    @Id
    private String id;
    private Long paymentId;
    private SubscriptionResponse subscription;
    private UserResponse user;
    private Long amount;
    private String orderInfo;
    private String momoRequestId;
    private String momoTransId;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status;
}
