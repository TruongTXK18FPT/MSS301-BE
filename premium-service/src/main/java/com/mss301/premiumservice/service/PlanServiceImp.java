package com.mss301.premiumservice.service;

import com.mss301.premiumservice.constant.PlanStatus;
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.model.dtos.response.PlanResponse;
import com.mss301.premiumservice.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
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
    @Transactional
    public PlanResponse save(PlanRequest plan) {
        List<Entitlement> entitlements = getAllEntitlementFromIds(plan.getEntitlementsId());

        Plan newPlan = Plan.builder()
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .billingCycle(plan.getBillingCycle())
                .priceCents(plan.getPriceCents())
                .currency(plan.getCurrency())
                .status(PlanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .entitlements(entitlements)
                .build();

        Plan savedPlan = planRepository.save(newPlan);

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
    @Transactional
    public PlanResponse delete(Long planId) {
        Plan plan = planRepository.findById(planId).orElse(null);
        if (plan == null) {
            return null;
        }

        PlanResponse response = convertToResponse(plan);

        // Remove plan khỏi tất cả entitlements trước
        if (plan.getEntitlements() != null) {
            for (Entitlement entitlement : plan.getEntitlements()) {
                if (entitlement.getPlans() != null) {
                    entitlement.getPlans().remove(plan);
                }
            }
            plan.getEntitlements().clear();
        }

        // JPA tự động xóa record trong join table
        planRepository.delete(plan);

        return response;
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
