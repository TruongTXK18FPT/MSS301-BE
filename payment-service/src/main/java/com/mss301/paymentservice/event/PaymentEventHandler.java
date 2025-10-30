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
                event.getPayment().getSubscriptionId());
        try {
            PaymentCommand command = event.getPayment();
            PaymentQuery query = convertToQuery(command);

            queryRepository.save(query);
            log.info("Payment view saved to MongoDB with ID: {}", query.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to save payment view to MongoDB", e);
        }
    }

    @EventListener
    @Async
    public void handlePaymentStatusUpdated(PaymentStatusUpdatedEvent event) {
        log.info("Handling PaymentStatusUpdatedEvent for subscriptionId: {}",
                event.getPayment().getSubscriptionId());

        try {
            PaymentCommand command = event.getPayment();
            PaymentQuery existing = queryRepository.findBySubscriptionId(command.getSubscriptionId());

            if (existing != null) {
                existing.setStatus(command.getStatus());
                existing.setMomoTransId(command.getMomoTransId());
                existing.setUpdatedAt(command.getUpdatedAt());

                queryRepository.save(existing);
                log.info("Payment view updated in MongoDB for ID: {}", existing.getPaymentId());
            } else {
                log.warn("Payment view not found for subscriptionId: {}", command.getSubscriptionId());
            }
        } catch (Exception e) {
            log.error("Failed to update payment view in MongoDB", e);
        }
    }

    private PaymentQuery convertToQuery(PaymentCommand command) {
        return new PaymentQuery(
                null, // MongoDB will generate ID
                command.getPaymentId(),
                command.getSubscriptionId(),
                command.getUserId(),
                command.getAmount(),
                command.getOrderInfo(),
                command.getMomoRequestId(),
                command.getMomoTransId(),
                command.getPaymentUrl(),
                command.getCreatedAt(),
                command.getUpdatedAt(),
                command.getStatus()
        );
    }
}
