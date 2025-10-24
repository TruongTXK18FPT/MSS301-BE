package com.mss301.profileservice.controller;

import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.mss301.profileservice.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

        private final WebClient ttWebClient;

        @GetMapping("/provinces")
        @Cacheable("provinces")
        public ApiResponse<Object> getProvinces() {
                Object data = ttWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder.path("/new-provinces").build())
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @GetMapping("/districts")
        @Cacheable(cacheNames = "districts", key = "#provinceId")
        public ApiResponse<Object> getDistricts(String provinceId) {
                Object data = ttWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/new-provinces/{code}/districts")
                                                .build(provinceId))
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @GetMapping("/wards")
        @Cacheable(cacheNames = "wards", key = "#districtId")
        public ApiResponse<Object> getWards(String districtId) {
                Object data = ttWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/new-districts/{code}/wards")
                                                .build(districtId))
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @GetMapping("/full")
        public ApiResponse<Object> getFull(String provinceCode, String wardCode) {
                Object data = ttWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/new-full-address")
                                                .queryParam("provinceCode", provinceCode)
                                                .queryParam("wardCode", wardCode)
                                                .build())
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @GetMapping("/search")
        public ApiResponse<Object> search(String keyword, Integer limit) {
                Object data = ttWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/search-new-address")
                                                .queryParam("keyword", keyword)
                                                .queryParam("limit", limit == null ? 20 : limit)
                                                .build())
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @PostMapping("/convert")
        public ApiResponse<Object> convert(@RequestBody Map<String, Object> body) {
                Object data = ttWebClient
                                .post()
                                .uri("/convert/address")
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }

        @PostMapping("/convert-batch")
        public ApiResponse<Object> convertBatch(@RequestBody Map<String, Object> body) {
                Object data = ttWebClient
                                .post()
                                .uri("/convert/batch")
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(Object.class)
                                .block();
                return ApiResponse.success(data);
        }
}
