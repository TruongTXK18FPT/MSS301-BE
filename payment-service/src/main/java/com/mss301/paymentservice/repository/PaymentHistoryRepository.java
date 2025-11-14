package com.mss301.paymentservice.repository;

import com.mss301.paymentservice.model.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByOrderIdOrderByCreatedAtDesc(String orderId);
}
