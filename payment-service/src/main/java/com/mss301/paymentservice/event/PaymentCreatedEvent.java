package com.mss301.paymentservice.event;

import com.mss301.paymentservice.model.PaymentCommand;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentCreatedEvent {
    private PaymentCommand payment;
}
