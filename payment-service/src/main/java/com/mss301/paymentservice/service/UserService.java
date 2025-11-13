package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.response.ApiResponse;
import com.mss301.paymentservice.model.dtos.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${service.user}", url = "${service.url}")

public interface UserService {
    @GetMapping("/authenticate/users/my-info")
    ApiResponse<UserResponse> getMyInfo();

    @GetMapping("/authenticate/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
}
