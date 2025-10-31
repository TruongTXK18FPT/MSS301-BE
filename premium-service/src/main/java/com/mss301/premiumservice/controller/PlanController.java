package com.mss301.premiumservice.controller;

import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.model.dtos.response.PlanResponse;
import com.mss301.premiumservice.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@CrossOrigin
public class PlanController {

    @Autowired
    private PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> findAll() {
        return ResponseEntity.ok(planService.findAll());
    }

    @PostMapping
    public ResponseEntity<PlanResponse> save(@RequestBody PlanRequest plan) {
        return ResponseEntity.ok(planService.save(plan));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponse> findByPlanId(@PathVariable("planId") Long planId) {
        PlanResponse plan = planService.findByPlanId(planId);
        if (plan != null) {
            return ResponseEntity.ok(plan);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{planId}")
    public ResponseEntity<PlanResponse> update(@PathVariable Long planId, @RequestBody PlanRequest plan) {
        PlanResponse updatedPlan = planService.update(planId, plan);
        if (updatedPlan != null) {
            return ResponseEntity.ok(updatedPlan);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<PlanResponse> delete(@PathVariable Long planId) {
        PlanResponse deletedPlan = planService.delete(planId);
        if (deletedPlan != null) {
            return ResponseEntity.ok(deletedPlan);
        }
        return ResponseEntity.notFound().build();
    }
} 
