package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.response.PlanResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${service.premium.name}", url = "${service.premium.url}/plans")
public interface PlanService {
    @GetMapping("/{planId}")
    ResponseEntity<PlanResponse> findByPlanId(@PathVariable("planId") Long planId);
}
