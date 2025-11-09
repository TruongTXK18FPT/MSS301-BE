package com.mss301.chatbotservice.service;

import com.mss301.chatbotservice.model.dtos.response.ApiResponse;
import com.mss301.chatbotservice.model.dtos.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "${service.user.name}", url = "${service.user.url}")
public interface UserService {
    @GetMapping("/users/my-info")
    ApiResponse<UserResponse> getMyInfo();
}
