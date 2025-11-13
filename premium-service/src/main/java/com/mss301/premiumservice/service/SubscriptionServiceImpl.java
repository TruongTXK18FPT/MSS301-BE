package com.mss301.premiumservice.service;

import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.model.UsageMeter;
import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.request.PaymentRequest;
import com.mss301.premiumservice.model.dtos.response.*;
import com.mss301.premiumservice.repository.PlanRepository;
import com.mss301.premiumservice.repository.SubscriptionRepository;
import com.mss301.premiumservice.constant.SubscriptionStatus;
import com.mss301.premiumservice.repository.UsageMeterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UsageMeterRepository usageMeterRepository;

//    @Autowired
//    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @Override
    public SubscriptionWithPaymentResponse createSubscriptionWithPayment(
            SubscriptionRequest request) {

        // 1. Get Plan info
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Plan not found: " + request.getPlanId()));

        // 2. Create Subscription (PENDING status)
        Subscription subscription = new Subscription();
        subscription.setUserId(request.getUserId());
        subscription.setPlan(plan);
        subscription.setSubscriptionStatus(SubscriptionStatus.SUBSCRIBED);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusMonths(plan.getBillingCycle()));
        subscription.setRenewal(request.isRenewal());

        // 3. Save Usage Meters
        List<UsageMeterResponse> usageMeterResponses = new ArrayList<>();

        for (Entitlement entitlement : plan.getEntitlements()) {
            UsageMeter newUsageMeter = new UsageMeter(
                    subscription.getUserId(),
                    entitlement,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMonths(plan.getBillingCycle()),
                    0L,
                    entitlement.getDefaultLimit(),
                    LocalDateTime.now()
            );
            usageMeterRepository.save(newUsageMeter);
            usageMeterResponses.add(convertToUsageMeterResponse(newUsageMeter));
        }

        Subscription savedSubscription = subscriptionRepository.save(subscription);

//        // 3. ✅ Call Payment Service với đầy đủ thông tin
//        PaymentRequest paymentRequest = PaymentRequest.builder()
//                .userId(request.getUserId())
////                .subscriptionId(savedSubscription.getSubscriptionId())
//                .planId(plan.getPlanId())
//                .amount(plan.getPriceCents()) // ✅ Gửi số tiền
//                .orderInfo("Subscription for plan: " + plan.getName())
//                .build();
//
//        try {
//            ApiResponse<PaymentResponse> paymentResponse = paymentService.createPayment(paymentRequest);
//
            return SubscriptionWithPaymentResponse.builder()
                    .subscriptionId(savedSubscription.getSubscriptionId())
                    .planId(plan.getPlanId())
                    .planName(plan.getName())
                    .amount(plan.getPriceCents())
                    .paymentUrl("")
                    .build();
//
//        } catch (Exception e) {
//            log.error("Failed to create payment for subscription: {}",
//                    savedSubscription.getSubscriptionId(), e);
//
//            // Rollback subscription
//            subscriptionRepository.delete(savedSubscription);
//
//            throw new RuntimeException("Failed to create payment", e);
//        }
    }

    @Override
    public List<SubscriptionResponse> findSubscriptionByUserId(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
                .map(subscription -> {
                    List<UsageMeter> usageMeters = usageMeterRepository.findByUserId(userId);
                    List<UsageMeterResponse> usageMeterResponses = usageMeters.stream()
                            .map(this::convertToUsageMeterResponse)
                            .collect(Collectors.toList());
                    return convertToSubscriptionResponse(subscription, usageMeterResponses);
                })
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionResponse findBySubscriptionId(Long subscriptionId) {
        System.out.println("Finding subscription with ID: " + subscriptionId);
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElse(null);
        System.out.println("Found subscription: " + subscription);
        if (subscription == null) {
            throw new IllegalArgumentException("Subcription with ID " + subscriptionId + " does not exist.");
        }
        return convertToSubscriptionResponse(subscription);
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        return convertToSubscriptionResponse(subscription, null);
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription, List<UsageMeterResponse> usageMeterResponses) {
        UserResponse user = null;
        try {
            ApiResponse<UserResponse> userApiResponse = userService.getMe();
            if (userApiResponse != null && userApiResponse.getResult() != null) {
                user = userApiResponse.getResult();
            }
        } catch (Exception e) {
            System.err.println("Error fetching user info: " + e.getMessage());
            return null;
        }

        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .user(user)
                .plan(subscription.getPlan())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .renewal(subscription.isRenewal())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .usageMeters(usageMeterResponses)
                .build();
    }

    private UsageMeterResponse convertToUsageMeterResponse(UsageMeter usageMeter) {
        UserResponse user = null;
        try {
            ApiResponse<UserResponse> userApiResponse = userService.getMe();
            if (userApiResponse != null && userApiResponse.getResult() != null) {
                user = userApiResponse.getResult();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error fetching user info: " + e.getMessage());
        }

        Entitlement ent = usageMeter.getEntitlement();  // ✅ Extract entitlement data

        return UsageMeterResponse.builder()
                .usageMeterId(usageMeter.getUsageMeterId())
                .user(user)
                .entitlementId(ent.getEntitlementId())
                .entitlementName(ent.getName())
                .entitlementCode(ent.getCode())
                .defaultLimit(ent.getDefaultLimit())
                .unit(ent.getUnit())
                .periodStart(usageMeter.getPeriodStart())
                .periodEnd(usageMeter.getPeriodEnd())
                .used(usageMeter.getUsed())
                .limit(usageMeter.getLimit())
                .updatedAt(usageMeter.getUpdatedAt())
                .build();
    }
}