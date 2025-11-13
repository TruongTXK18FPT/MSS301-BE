package com.mss301.premiumservice.service;

import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.model.UsageMeter;
import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
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
import java.util.Optional;
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

    // @Autowired
    // private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @Override
    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        log.info("Creating subscription - userId: {}, planId: {}, beneficiaryEmail: {}",
                request.getUserId(), request.getPlanId(), request.getBeneficiaryEmail());

        // 1. Get Plan info
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Plan not found: " + request.getPlanId()));

        // 2. Determine beneficiary
        Long beneficiaryUserId = request.getUserId(); // Default to current user

        if (request.getBeneficiaryEmail() != null && !request.getBeneficiaryEmail().isEmpty()) {
            // Guardian purchase flow
            try {
                // TODO: Call profile-service to get beneficiary user by email
                // UserResponse beneficiary = profileService.getUserByEmail(beneficiaryEmail);
                // TODO: Validate guardian-student relationship
                // profileService.validateGuardianStudent(currentUserId, beneficiary.getId());
                // beneficiaryUserId = beneficiary.getId();

                log.info("Guardian purchase: user {} buying for {}",
                        request.getUserId(), request.getBeneficiaryEmail());
            } catch (Exception e) {
                log.error("Failed to process guardian purchase", e);
                throw new RuntimeException("Invalid beneficiary or guardian relationship", e);
            }
        }

        // 3. Create Subscription (PENDING_PAYMENT status)
        Subscription subscription = new Subscription();
        subscription.setUserId(beneficiaryUserId);
        subscription.setPlan(plan);
        subscription.setSubscriptionStatus(SubscriptionStatus.PENDING_PAYMENT);
        subscription.setRenewal(request.isRenewal());
        // Set temporary dates - will be updated when payment completes
        LocalDateTime now = LocalDateTime.now();
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusMonths(plan.getBillingCycle()));

        Subscription saved = subscriptionRepository.save(subscription);

        log.info("Subscription {} created for user {} (PENDING_PAYMENT)",
                saved.getSubscriptionId(), beneficiaryUserId);

        return convertToSubscriptionResponse(saved);
    }

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
                    LocalDateTime.now());
            usageMeterRepository.save(newUsageMeter);
            usageMeterResponses.add(convertToUsageMeterResponse(newUsageMeter));
        }

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // // 3. ✅ Call Payment Service với đầy đủ thông tin
        // PaymentRequest paymentRequest = PaymentRequest.builder()
        // .userId(request.getUserId())
        //// .subscriptionId(savedSubscription.getSubscriptionId())
        // .planId(plan.getPlanId())
        // .amount(plan.getPrice()) // ✅ Gửi số tiền
        // .orderInfo("Subscription for plan: " + plan.getName())
        // .build();
        //
        // try {
        // ApiResponse<PaymentResponse> paymentResponse =
        // paymentService.createPayment(paymentRequest);
        //
        return SubscriptionWithPaymentResponse.builder()
                .subscriptionId(savedSubscription.getSubscriptionId())
                .planId(plan.getPlanId())
                .planName(plan.getName())
                .amount(plan.getPrice())
                .paymentUrl("")
                .build();
        //
        // } catch (Exception e) {
        // log.error("Failed to create payment for subscription: {}",
        // savedSubscription.getSubscriptionId(), e);
        //
        // // Rollback subscription
        // subscriptionRepository.delete(savedSubscription);
        //
        // throw new RuntimeException("Failed to create payment", e);
        // }
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

    @Override
    public SubscriptionResponse getCurrentActiveSubscription(Long userId) {
        Optional<Subscription> subscription = subscriptionRepository.findCurrentActiveSubscription(
                userId,
                SubscriptionStatus.SUBSCRIBED,
                LocalDateTime.now());
        if (subscription.isPresent()) {
            return convertToSubscriptionResponse(subscription.get());
        }
        return null; // No active subscription
    }

    @Override
    public List<SubscriptionResponse> getSubscriptionHistory(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return subscriptions.stream()
                .map(this::convertToSubscriptionResponse)
                .collect(Collectors.toList());
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        return convertToSubscriptionResponse(subscription, null);
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription,
            List<UsageMeterResponse> usageMeterResponses) {
        UserResponse user = null;
        try {
            ApiResponse<UserResponse> userApiResponse = userService.getMe();
            if (userApiResponse != null && userApiResponse.getResult() != null) {
                user = userApiResponse.getResult();
            }
        } catch (Exception e) {
            log.warn("Error fetching user info for subscription {}: {}",
                    subscription.getSubscriptionId(), e.getMessage());
            // Continue without user info - subscription can still be returned
        }

        try {
            PlanResponse planResponse = convertPlanToResponse(subscription.getPlan());
            if (planResponse == null) {
                log.error("Failed to convert plan to response for subscription {}",
                        subscription.getSubscriptionId());
                throw new RuntimeException("Failed to convert plan to response");
            }

            return SubscriptionResponse.builder()
                    .subscriptionId(subscription.getSubscriptionId())
                    .user(user)
                    .plan(planResponse)
                    .startDate(subscription.getStartDate())
                    .endDate(subscription.getEndDate())
                    .subscriptionStatus(subscription.getSubscriptionStatus())
                    .renewal(subscription.isRenewal())
                    .createdAt(subscription.getCreatedAt())
                    .updatedAt(subscription.getUpdatedAt())
                    .usageMeters(usageMeterResponses)
                    .build();
        } catch (Exception e) {
            log.error("Error converting subscription to response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert subscription to response: " + e.getMessage(), e);
        }
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

        Entitlement ent = usageMeter.getEntitlement(); // ✅ Extract entitlement data

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

    private PlanResponse convertPlanToResponse(Plan plan) {
        if (plan == null) {
            log.error("Plan is null when converting to response");
            return null;
        }
        try {
            PlanResponse planResponse = PlanResponse.builder()
                    .planId(plan.getPlanId())
                    .code(plan.getCode())
                    .name(plan.getName())
                    .description(plan.getDescription())
                    .billingCycle(plan.getBillingCycle())
                    .price(plan.getPrice())
                    .currency(plan.getCurrency())
                    .planStatus(plan.getStatus())
                    .createdAt(plan.getCreatedAt())
                    .updatedAt(plan.getUpdatedAt())
                    .entitlements(plan.getEntitlements()) // Include entitlements if available
                    .build();
            log.debug("Converted plan {} to response", plan.getPlanId());
            return planResponse;
        } catch (Exception e) {
            log.error("Error converting plan {} to response: {}", plan.getPlanId(), e.getMessage(), e);
            throw new RuntimeException("Failed to convert plan to response", e);
        }
    }
}