package com.mss301.premiumservice.service;

import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.EntitlementRequest;
import com.mss301.premiumservice.model.dtos.response.EntitlementResponse;
import com.mss301.premiumservice.repository.EntitlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
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
    public EntitlementResponse save(EntitlementRequest entitlement) {
        Entitlement newEntitlement = new Entitlement(
                0L,
                entitlement.getCode(),
                entitlement.getName(),
                entitlement.getDescription(),
                entitlement.getDefaultLimit(),
                entitlement.getUnit(),
                null
        );

        Entitlement saveEntitlement = entitlementRepository.save(newEntitlement);

        return convertToResponse(saveEntitlement);
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
    public EntitlementResponse delete(Long entitlementId) {
        Entitlement entitlementById = entitlementRepository.findById(entitlementId).orElse(null);
        if (entitlementById != null) {
            EntitlementResponse response = convertToResponse(entitlementById);

            // Option 1: Soft delete (nếu có status field)
            // entitlementById.setStatus(EntitlementStatus.INACTIVE);
            // entitlementRepository.save(entitlementById);

            // Option 2: Hard delete với cleanup
            // Remove entitlement khỏi tất cả plans trước khi delete
            if (entitlementById.getPlans() != null) {
                for (Plan plan : entitlementById.getPlans()) {
                    if (plan.getEntitlements() != null) {
                        plan.getEntitlements().remove(entitlementById);
                    }
                }
            }

            // Clear plans trước khi delete
            entitlementById.getPlans().clear();
            entitlementRepository.save(entitlementById); // Save để cleanup join table
            entitlementRepository.delete(entitlementById);

            return response;
        }
        return null;
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
