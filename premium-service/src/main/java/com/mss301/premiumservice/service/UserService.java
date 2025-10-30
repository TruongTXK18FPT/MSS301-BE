package com.mss301.premiumservice.service;


import com.mss301.premiumservice.model.dtos.response.ApiResponse;
import com.mss301.premiumservice.model.dtos.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${service.user.name}", url = "${service.user.url}/users")
public interface UserService {

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo();

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
}
