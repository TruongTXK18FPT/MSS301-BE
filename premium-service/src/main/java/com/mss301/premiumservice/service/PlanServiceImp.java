package com.mss301.premiumservice.service;

import com.mss301.premiumservice.constant.PlanStatus;
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.model.dtos.response.PlanResponse;
import com.mss301.premiumservice.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanServiceImp implements PlanService {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EntitlementService entitlementService;

    @Override
    public List<PlanResponse> findAll() {
        List<Plan> plans = planRepository.findAll();
        return plans.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlanResponse findByPlanId(Long planId) {
        Plan plan = planRepository.findById(planId).orElse(null);
        if (plan == null) {
            return null;
        }
        return convertToResponse(plan);
    }

    @Override
    public PlanResponse save(PlanRequest plan) {
        List<Entitlement> entitlements = getAllEntitlementFromIds(plan.getEntitlementsId());

        Plan newPlan = new Plan(
                0L,
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getBillingCycle(),
                plan.getPriceCents(),
                plan.getCurrency(),
                PlanStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                entitlements
        );

        Plan savedPlan = planRepository.save(newPlan);

        // Synchronize quan hệ 2 chiều
        for (Entitlement entitlement : entitlements) {
            if (entitlement.getPlans() == null) {
                entitlement.setPlans(new ArrayList<>());
            }
            if (!entitlement.getPlans().contains(savedPlan)) {
                entitlement.getPlans().add(savedPlan);
            }
        }

        return convertToResponse(savedPlan);
    }

    @Override
    public PlanResponse update(Long planId, PlanRequest planRequest) {
        Plan planById = planRepository.findById(planId).orElse(null);
        if (planById != null) {
            // Remove plan khỏi entitlements cũ
            if (planById.getEntitlements() != null) {
                for (Entitlement oldEntitlement : planById.getEntitlements()) {
                    if (oldEntitlement.getPlans() != null) {
                        oldEntitlement.getPlans().remove(planById);
                    }
                }
            }

            // Update fields
            planById.setCode(planRequest.getCode());
            planById.setName(planRequest.getName());
            planById.setDescription(planRequest.getDescription());
            planById.setBillingCycle(planRequest.getBillingCycle());
            planById.setPriceCents(planRequest.getPriceCents());
            planById.setCurrency(planRequest.getCurrency());
            planById.setUpdatedAt(LocalDateTime.now());

            // Set entitlements mới và sync
            List<Entitlement> newEntitlements = getAllEntitlementFromIds(planRequest.getEntitlementsId());
            planById.setEntitlements(newEntitlements);

            // Add plan vào entitlements mới
            for (Entitlement newEntitlement : newEntitlements) {
                if (newEntitlement.getPlans() == null) {
                    newEntitlement.setPlans(new ArrayList<>());
                }
                if (!newEntitlement.getPlans().contains(planById)) {
                    newEntitlement.getPlans().add(planById);
                }
            }

            Plan updatedPlan = planRepository.save(planById);
            return convertToResponse(updatedPlan);
        }
        return null;
    }

    @Override
    public PlanResponse delete(Long planId) {
        Plan planById = planRepository.findById(planId).orElse(null);
        if (planById != null) {
//            PlanResponse response = convertToResponse(planById);
//
//            // Remove plan khỏi tất cả entitlements
//            if (planById.getEntitlements() != null) {
//                for (Entitlement entitlement : planById.getEntitlements()) {
//                    if (entitlement.getPlans() != null) {
//                        entitlement.getPlans().remove(planById);
//                    }
//                }
//            }
//
//            // Clear entitlements trước khi delete
//            planById.getEntitlements().clear();
//            planRepository.save(planById); // Save để cleanup join table
//            planRepository.delete(planById);

            planById.setStatus(PlanStatus.INACTIVE);
            planById.setUpdatedAt(LocalDateTime.now());

            Plan savedPlan = planRepository.save(planById);

            return convertToResponse(savedPlan);
        }
        return null;
    }

    @Override
    public Plan getById(Long planId) {
        return planRepository.findById(planId).orElse(null);
    }

    private PlanResponse convertToResponse(Plan plan) {
        return PlanResponse.builder()
                .planId(plan.getPlanId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .billingCycle(plan.getBillingCycle())
                .priceCents(plan.getPriceCents())
                .currency(plan.getCurrency())
                .planStatus(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .entitlements(plan.getEntitlements())
                .build();
    }

    private List<Entitlement> getAllEntitlementFromIds(List<Long> entitlementsId) {
        List<Entitlement> entitlements = new ArrayList<>();

        for (Long entitlementId : entitlementsId) {
            Entitlement entitlement = entitlementService.getById(entitlementId);
            if (entitlement != null) {
                entitlements.add(entitlement);
            }
        }

        return entitlements;
    }
} 
