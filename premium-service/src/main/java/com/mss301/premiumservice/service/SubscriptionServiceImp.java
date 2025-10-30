package com.mss301.premiumservice.service;

import com.mss301.premiumservice.constant.SubscriptionStatus;
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.model.UsageMeter;
import com.mss301.premiumservice.model.dtos.request.SubscriptionRequest;
import com.mss301.premiumservice.model.dtos.response.ApiResponse;
import com.mss301.premiumservice.model.dtos.response.SubscriptionResponse;
import com.mss301.premiumservice.model.dtos.response.UsageMeterResponse;
import com.mss301.premiumservice.model.dtos.response.UserResponse;
import com.mss301.premiumservice.repository.SubscriptionRepository;
import com.mss301.premiumservice.repository.UsageMeterRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionServiceImp implements SubscriptionService {

    @Autowired
    private UsageMeterRepository usageMeterRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private UserService userService;

    @Override
    public List<SubscriptionResponse> findAllSubscription() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        return subscriptions.stream()
                .map(this::convertToSubscriptionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionResponse findBySubscriptionId(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription with ID " + subscriptionId + " does not exist.");
        }
        return convertToSubscriptionResponse(subscription);
    }

    @Override
    public UsageMeterResponse findByUsageMeterId(Long usageMeterId) {
        UsageMeter usageMeter = usageMeterRepository.findByUsageMeterId(usageMeterId);
        if (usageMeter == null) {
            return null;
        }
        return convertToUsageMeterResponse(usageMeter);
    }

    @Override
    public SubscriptionResponse subscription(SubscriptionRequest subscription) {
        Plan plan = planService.getById(subscription.getPlanId());

        if (plan == null) {
            throw new IllegalArgumentException("Plan with ID " + subscription.getPlanId() + " does not exist.");
        }

        // Tạo subscription trước
        Subscription newSubscription = new Subscription(
                0L,
                subscription.getUserId(),
                plan,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(plan.getBillingCycle()),
                SubscriptionStatus.SUBSCRIBED,
                subscription.isRenewal(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Subscription savedSubscription = subscriptionRepository.save(newSubscription);

        // Tạo UsageMeter cho từng Entitlement, sử dụng dữ liệu từ savedSubscription
        List<UsageMeterResponse> usageMeterResponses = new ArrayList<>();

        for (Entitlement entitlement : plan.getEntitlements()) {
            UsageMeter newUsageMeter = new UsageMeter(
                    0L,
                    savedSubscription.getUserId(),
                    entitlement,
                    savedSubscription.getStartDate(),
                    savedSubscription.getEndDate(),
                    0L,
                    entitlement.getDefaultLimit(),
                    LocalDateTime.now()
            );
            UsageMeter savedUsageMeter = usageMeterRepository.save(newUsageMeter);
            usageMeterResponses.add(convertToUsageMeterResponse(savedUsageMeter));
        }

        return convertToSubscriptionResponse(savedSubscription, usageMeterResponses);
    }

    @Override
    public SubscriptionResponse update(Long subscriptionId, SubscriptionRequest subscription) {
        Subscription subscriptionById = subscriptionRepository.findBySubscriptionId(subscriptionId);
        if (subscriptionById != null) {
            // Cập nhật các trường

            subscriptionById.setRenewal(subscription.isRenewal());
            subscriptionById.setUpdatedAt(LocalDateTime.now());

            Subscription updatedSubscription = subscriptionRepository.save(subscriptionById);
            return convertToSubscriptionResponse(updatedSubscription);
        }
        return null;
    }

    @Override
    public SubscriptionResponse delete(Long subscriptionId) {
        Subscription subscriptionById = subscriptionRepository.findBySubscriptionId(subscriptionId);
        if (subscriptionById != null) {

            subscriptionById.setSubscriptionStatus(SubscriptionStatus.CANCELLED);
            subscriptionById.setUpdatedAt(LocalDateTime.now());

            SubscriptionResponse response = convertToSubscriptionResponse(subscriptionById);
            subscriptionRepository.save(subscriptionById);
            return response;
        }
        return null;
    }

    @Override
    public List<SubscriptionResponse> findSubscriptionByUserId(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
                .map(subscription -> {
                    List<UsageMeter> usageMeters = usageMeterRepository.findByUserId(userId);
                    List<UsageMeterResponse> usageMeterResponses = usageMeters.stream()
                            .filter(um -> um.getEntitlement() != null &&
                                    subscription.getPlan().getEntitlements().contains(um.getEntitlement()))
                            .map(this::convertToUsageMeterResponse)
                            .collect(Collectors.toList());
                    return convertToSubscriptionResponse(subscription, usageMeterResponses);
                })
                .collect(Collectors.toList());
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        return convertToSubscriptionResponse(subscription, null);
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription, List<UsageMeterResponse> usageMeterResponses) {
        UserResponse user = null;
        try {
            ApiResponse<UserResponse> userApiResponse = userService.getUserById(subscription.getUserId());
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
                .planId(subscription.getPlan().getPlanId())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .subscriptionStatus(subscription.getSubscriptionStatus())
                .renewal(subscription.isRenewal())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .usageMeters(usageMeterResponses)
                .build();
    }

    private UsageMeterResponse convertToUsageMeterResponse(UsageMeter usageMeter) {
        // Lấy thông tin user từ UserService nếu cần
        UserResponse user = null;
        try {
            ApiResponse<UserResponse> userApiResponse = userService.getUserById(usageMeter.getUserId());
            if (userApiResponse != null && userApiResponse.getResult() != null) {
                user = userApiResponse.getResult();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error fetching user info: " + e.getMessage());
        }

        return UsageMeterResponse.builder()
                .usageMeterId(usageMeter.getUsageMeterId())
                .user(user)
                .entitlementId(usageMeter.getEntitlement().getEntitlementId())
                .periodStart(usageMeter.getPeriodStart())
                .periodEnd(usageMeter.getPeriodEnd())
                .used(usageMeter.getUsed())
                .limit(usageMeter.getLimit())
                .updatedAt(usageMeter.getUpdatedAt())
                .build();
    }
}
