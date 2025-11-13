package com.mss301.premiumservice.controller;

import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.model.dtos.response.ApiResponse;
import com.mss301.premiumservice.model.dtos.response.PlanResponse;
import com.mss301.premiumservice.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/premium/plans")
public class PlanController {

    @Autowired
    private PlanService planService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> findAll() {
        List<PlanResponse> plans = planService.findAll();
        ApiResponse<List<PlanResponse>> response = ApiResponse.<List<PlanResponse>>builder()
                .code(200)
                .message("Plans retrieved successfully")
                .result(plans)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> save(@RequestBody PlanRequest plan) {
        PlanResponse savedPlan = planService.save(plan);
        ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                .code(200)
                .message("Plan created successfully")
                .result(savedPlan)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> findByPlanId(@PathVariable("planId") Long planId) {
        PlanResponse plan = planService.findByPlanId(planId);
        if (plan != null) {
            ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                    .code(200)
                    .message("Plan retrieved successfully")
                    .result(plan)
                    .build();
            return ResponseEntity.ok(response);
        }
        ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                .code(404)
                .message("Plan not found")
                .build();
        return ResponseEntity.status(404).body(response);
    }

    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> update(@PathVariable Long planId, @RequestBody PlanRequest plan) {
        PlanResponse updatedPlan = planService.update(planId, plan);
        if (updatedPlan != null) {
            ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                    .code(200)
                    .message("Plan updated successfully")
                    .result(updatedPlan)
                    .build();
            return ResponseEntity.ok(response);
        }
        ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                .code(404)
                .message("Plan not found")
                .build();
        return ResponseEntity.status(404).body(response);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> delete(@PathVariable Long planId) {
        PlanResponse deletedPlan = planService.delete(planId);
        if (deletedPlan != null) {
            ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                    .code(200)
                    .message("Plan deleted successfully")
                    .result(deletedPlan)
                    .build();
            return ResponseEntity.ok(response);
        }
        ApiResponse<PlanResponse> response = ApiResponse.<PlanResponse>builder()
                .code(404)
                .message("Plan not found")
                .build();
        return ResponseEntity.status(404).body(response);
    }
}
