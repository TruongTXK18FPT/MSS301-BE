package com.mss301.paymentservice.repository;

import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.model.PaymentCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Command Repository
@Repository
public interface PaymentCommandRepository extends JpaRepository<PaymentCommand, String> {

    Optional<PaymentCommand> findByPayosPaymentLinkId(String paymentLinkId);

    Optional<PaymentCommand> findByPayosOrderCode(Long orderCode);

    boolean existsBySubscriptionIdAndStatus(Long subscriptionId, Status status);
}
