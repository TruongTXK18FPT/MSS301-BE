package com.mss301.premiumservice.service;


import com.mss301.premiumservice.model.dtos.response.ApiResponse;
import com.mss301.premiumservice.model.dtos.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "${service.user}", url = "${service.url}")
public interface UserService {

    @GetMapping("/authenticate/users/me")
    ApiResponse<UserResponse> getMe();

}
