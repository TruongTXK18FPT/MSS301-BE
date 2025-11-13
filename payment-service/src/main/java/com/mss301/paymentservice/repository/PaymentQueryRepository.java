package com.mss301.paymentservice.repository;

import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.model.PaymentQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentQueryRepository extends MongoRepository<PaymentQuery, String> {

    List<PaymentQuery> findByStatus(Status status);

    PaymentQuery findByOrderId(String orderId);

    Page<PaymentQuery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
