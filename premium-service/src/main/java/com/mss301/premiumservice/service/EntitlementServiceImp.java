package com.mss301.premiumservice.service;

import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.EntitlementRequest;
import com.mss301.premiumservice.model.dtos.response.EntitlementResponse;
import com.mss301.premiumservice.repository.EntitlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EntitlementServiceImp implements EntitlementService {

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Override
    public List<EntitlementResponse> findAll() {
        List<Entitlement> entitlements = entitlementRepository.findAll();
        return entitlements.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EntitlementResponse findByEntitlementId(Long entitlementId) {
        Entitlement entitlement = entitlementRepository.findById(entitlementId).orElse(null);
        if (entitlement == null) {
            return null;
        }
        return EntitlementResponse.builder()
                .entitlementId(entitlement.getEntitlementId())
                .code(entitlement.getCode())
                .name(entitlement.getName())
                .description(entitlement.getDescription())
                .defaultLimit(entitlement.getDefaultLimit())
                .unit(entitlement.getUnit())
                .build();
    }

    @Override
    @Transactional
    public EntitlementResponse save(EntitlementRequest entitlement) {
        Entitlement newEntitlement = Entitlement.builder()
                .code(entitlement.getCode())
                .name(entitlement.getName())
                .description(entitlement.getDescription())
                .defaultLimit(entitlement.getDefaultLimit())
                .unit(entitlement.getUnit())
                .build();

        Entitlement savedEntitlement = entitlementRepository.save(newEntitlement);

        return convertToResponse(savedEntitlement);
    }

    @Override
    public EntitlementResponse update(Long entitlementId, EntitlementRequest entitlementRequest) {
        Entitlement entitlementById = entitlementRepository.findById(entitlementId).orElse(null);
        if (entitlementById != null) {
            // Update fields
            entitlementById.setCode(entitlementRequest.getCode());
            entitlementById.setName(entitlementRequest.getName());
            entitlementById.setDescription(entitlementRequest.getDescription());
            entitlementById.setDefaultLimit(entitlementRequest.getDefaultLimit());
            entitlementById.setUnit(entitlementRequest.getUnit());

            Entitlement updatedEntitlement = entitlementRepository.save(entitlementById);

            // Quan hệ Many-to-Many sẽ được JPA tự động sync nhờ cascade
            return convertToResponse(updatedEntitlement);
        }
        return null;
    }

    @Override
    @Transactional
    public EntitlementResponse delete(Long entitlementId) {
        Entitlement entitlement = entitlementRepository.findById(entitlementId)
                .orElse(null);

        if (entitlement == null) {
            return null;
        }

        EntitlementResponse response = convertToResponse(entitlement);

        // Remove entitlement khỏi tất cả plans trước
        if (entitlement.getPlans() != null) {
            for (Plan plan : entitlement.getPlans()) {
                plan.getEntitlements().remove(entitlement);
            }
            entitlement.getPlans().clear();
        }

        // JPA tự động xóa record trong join table
        entitlementRepository.delete(entitlement);

        return response;
    }

    @Override
    public Entitlement getById(Long entitlementId) {
        return entitlementRepository.findById(entitlementId).orElse(null);
    }

    private EntitlementResponse convertToResponse(Entitlement entitlement) {
        return EntitlementResponse.builder()
                .entitlementId(entitlement.getEntitlementId())
                .code(entitlement.getCode())
                .name(entitlement.getName())
                .description(entitlement.getDescription())
                .defaultLimit(entitlement.getDefaultLimit())
                .unit(entitlement.getUnit())
                .build();
    }
} 
