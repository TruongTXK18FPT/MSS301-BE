package com.mss301.premiumservice.service; 
 
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.dtos.request.EntitlementRequest;
import com.mss301.premiumservice.model.dtos.response.EntitlementResponse;

import java.util.List; 
 
public interface EntitlementService { 
   List<EntitlementResponse> findAll();
   EntitlementResponse findByEntitlementId(Long entitlementId);
   EntitlementResponse save(EntitlementRequest entitlement);
   EntitlementResponse update(Long entitlementId, EntitlementRequest entitlement);
   EntitlementResponse delete(Long entitlementId);

   Entitlement getById(Long entitlementId);
} 
