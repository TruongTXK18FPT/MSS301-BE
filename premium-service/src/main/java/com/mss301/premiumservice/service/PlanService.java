package com.mss301.premiumservice.service; 
 
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.model.dtos.response.PlanResponse;

import java.util.List; 
 
public interface PlanService { 
   List<PlanResponse> findAll();
   PlanResponse findByPlanId(Long planId);
   PlanResponse save(PlanRequest plan);
   PlanResponse update(Long planId, PlanRequest plan);
   PlanResponse delete(Long planId);

   Plan getById(Long planId);
} 
