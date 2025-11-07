package com.mss301.premiumservice.controller; 
 
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.dtos.request.EntitlementRequest;
import com.mss301.premiumservice.model.dtos.response.EntitlementResponse;
import com.mss301.premiumservice.service.EntitlementService;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.*; 
 
import java.util.List; 
 
@RestController 
@RequestMapping("/api/v1/entitlements")
@CrossOrigin 
public class EntitlementController { 
 
    @Autowired 
    private EntitlementService entitlementService; 
 
    @GetMapping 
    public ResponseEntity<List<EntitlementResponse>> findAll() {
        return ResponseEntity.ok(entitlementService.findAll()); 
    } 
 
    @PostMapping 
    public ResponseEntity<EntitlementResponse> save(@RequestBody EntitlementRequest entitlement) {
        return ResponseEntity.ok(entitlementService.save(entitlement)); 
    } 
 
    @GetMapping("/{entitlementId}") 
    public ResponseEntity<EntitlementResponse> findByEntitlementId(@PathVariable("entitlementId") Long entitlementId) {
        EntitlementResponse entitlement = entitlementService.findByEntitlementId(entitlementId);
        if (entitlement != null) { 
            return ResponseEntity.ok(entitlement); 
        } 
        return ResponseEntity.notFound().build(); 
    } 
 
    @PutMapping("/{entitlementId}") 
    public ResponseEntity<EntitlementResponse> update(@PathVariable Long entitlementId, @RequestBody EntitlementRequest entitlement) {
        EntitlementResponse updatedEntitlement = entitlementService.update(entitlementId, entitlement);
        if (updatedEntitlement != null) { 
            return ResponseEntity.ok(updatedEntitlement); 
        } 
        return ResponseEntity.notFound().build(); 
    } 
 
    @DeleteMapping("/{entitlementId}") 
    public ResponseEntity<EntitlementResponse> delete(@PathVariable Long entitlementId) {
        EntitlementResponse deletedEntitlement = entitlementService.delete(entitlementId);
        if (deletedEntitlement != null) { 
            return ResponseEntity.ok(deletedEntitlement); 
        } 
        return ResponseEntity.notFound().build(); 
    } 
} 
