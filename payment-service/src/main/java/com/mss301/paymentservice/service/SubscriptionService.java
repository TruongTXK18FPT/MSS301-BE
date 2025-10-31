package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.response.SubscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "${service.premium.name}", url = "${service.premium.url}/subscriptions")
public interface SubscriptionService {
    @GetMapping("/user/{userId}")
    ResponseEntity<List<SubscriptionResponse>> findSubscriptionByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/{subscriptionId}")
    ResponseEntity<SubscriptionResponse> findBySubscriptionId(@PathVariable("subscriptionId") Long subscriptionId);
}
