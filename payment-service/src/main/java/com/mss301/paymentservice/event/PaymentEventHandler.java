package com.mss301.paymentservice.event;

import com.mss301.paymentservice.model.PaymentCommand;
import com.mss301.paymentservice.model.PaymentQuery;
import com.mss301.paymentservice.repository.PaymentQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventHandler {

    @Autowired
    private PaymentQueryRepository queryRepository;

    @EventListener
    @Async
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        log.info("Handling PaymentCreatedEvent for subscriptionId: {}",
                event.getPayment().getPlanId());
        try {
            PaymentCommand command = event.getPayment();
            PaymentQuery query = convertToQuery(command);

            queryRepository.save(query);
            log.info("Payment view saved to MongoDB with ID: {}", query.getPlanId());
        } catch (Exception e) {
            log.error("Failed to save payment view to MongoDB", e);
        }
    }

    @EventListener
    @Async
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Handling PaymentStatusUpdatedEvent for subscriptionId: {}",
                event.getPayment().getPlanId());

        try {
            PaymentCommand command = event.getPayment();
            PaymentQuery existing = queryRepository.findByOrderId(command.getOrderId());

            if (existing != null) {
                existing.setStatus(command.getStatus());
                existing.setPayosOrderCode(command.getPayosOrderCode());
                existing.setPayosPaymentLinkId(command.getPayosPaymentLinkId());
                existing.setPayosTransactionRef(command.getPayosTransactionRef());
                existing.setBankCode(command.getBankCode());
                existing.setBankName(command.getBankName());
                existing.setAccountNumber(command.getAccountNumber());
                existing.setCounterAccountName(command.getCounterAccountName());
                existing.setCounterAccountNumber(command.getCounterAccountNumber());
                existing.setTransactionDatetime(command.getTransactionDatetime());
                existing.setUpdatedAt(command.getUpdatedAt());

                queryRepository.save(existing);
                log.info("Payment view updated in MongoDB for ID: {}", existing.getPlanId());
            } else {
                log.warn("Payment view not found for subscriptionId: {}", command.getPlanId());
            }
        } catch (Exception e) {
            log.error("Failed to update payment view in MongoDB", e);
        }
    }

    private PaymentQuery convertToQuery(PaymentCommand command) {
        return PaymentQuery.builder()
                .orderId(command.getOrderId())
                .userId(command.getUserId())
                .subscriptionId(command.getSubscriptionId())
                .planId(command.getPlanId())
                .amount(command.getAmount())
                .orderInfo(command.getOrderInfo())
                .payosOrderCode(command.getPayosOrderCode())
                .payosPaymentLinkId(command.getPayosPaymentLinkId())
                .payosTransactionRef(command.getPayosTransactionRef())
                .paymentUrl(command.getPaymentUrl())
                .bankCode(command.getBankCode())
                .bankName(command.getBankName())
                .accountNumber(command.getAccountNumber())
                .counterAccountName(command.getCounterAccountName())
                .counterAccountNumber(command.getCounterAccountNumber())
                .transactionDatetime(command.getTransactionDatetime())
                .createdAt(command.getCreatedAt())
                .updatedAt(command.getUpdatedAt())
                .status(command.getStatus())
                .build();
    }
}
