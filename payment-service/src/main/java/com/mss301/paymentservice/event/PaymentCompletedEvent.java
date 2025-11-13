package com.mss301.paymentservice.event;

import com.mss301.paymentservice.constant.Status;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {

    private final String orderId;
    private final Long planId;
    private final Long userId;
    private final Long amount;
    private final Status status;
    private final String momoTransId;
    private final LocalDateTime completedAt;

    public PaymentCompletedEvent(Object source, String orderId,
                                  Long userId, Long planId, Long amount, Status status,
                                  String momoTransId, LocalDateTime completedAt) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.planId = planId;
        this.amount = amount;
        this.status = status;
        this.momoTransId = momoTransId;
        this.completedAt = completedAt;
    }
}