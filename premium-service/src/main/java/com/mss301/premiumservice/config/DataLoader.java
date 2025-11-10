package com.mss301.premiumservice.config;

import com.mss301.premiumservice.constant.Currency;
import com.mss301.premiumservice.constant.PlanStatus;
import com.mss301.premiumservice.constant.Unit;
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.dtos.request.EntitlementRequest;
import com.mss301.premiumservice.model.dtos.request.PlanRequest;
import com.mss301.premiumservice.service.EntitlementService;
import com.mss301.premiumservice.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private PlanService planService;

    @Autowired
    private EntitlementService entitlementService;

    @Override
    public void run(String... args) throws Exception {
        if (entitlementService.findAll().isEmpty()) {
            initEntitlement();
        }

        if (planService.findAll().isEmpty()) {
            initPlan();
        }
    }

    private void initEntitlement() {
        EntitlementRequest aiGeneration = EntitlementRequest.builder()
                .code("AI_MINDMAP_GEN")
                .name("AI Mindmap Generation")
                .description("Generate mindmaps using AI per month")
                .defaultLimit(10)
                .unit(Unit.GB)
                .build();
        entitlementService.save(aiGeneration);

        // 2. Export Format
        EntitlementRequest exportFormat = EntitlementRequest.builder()
                .code("EXPORT_FORMAT")
                .name("Export Formats")
                .description("Export mindmap to PNG, PDF, SVG")
                .defaultLimit(1)
                .unit(Unit.GB)
                .build();
        entitlementService.save(exportFormat);

        // 3. Cloud Storage
        EntitlementRequest cloudStorage = EntitlementRequest.builder()
                .code("CLOUD_STORAGE")
                .name("Cloud Storage")
                .description("Store mindmaps in cloud (GB)")
                .defaultLimit(1)
                .unit(Unit.GB)
                .build();
        entitlementService.save(cloudStorage);

        // 4. Collaboration
        EntitlementRequest collaboration = EntitlementRequest.builder()
                .code("COLLAB_USERS")
                .name("Collaboration Users")
                .description("Number of users can collaborate on a mindmap")
                .defaultLimit(1)
                .unit(Unit.GB)
                .build();
        entitlementService.save(collaboration);

        // 5. Template Access
        EntitlementRequest templates = EntitlementRequest.builder()
                .code("TEMPLATE_ACCESS")
                .name("Premium Templates")
                .description("Access to premium mindmap templates")
                .defaultLimit(10)
                .unit(Unit.GB)
                .build();
        entitlementService.save(templates);

        // 6. AI Tokens
        EntitlementRequest aiTokens = EntitlementRequest.builder()
                .code("AI_TOKENS")
                .name("AI Tokens")
                .description("Monthly AI processing tokens")
                .defaultLimit(100000)
                .unit(Unit.GB)
                .build();
        entitlementService.save(aiTokens);
    }

    private void initPlan() {
        Entitlement aiGen = entitlementService.getById(1L);
        Entitlement export = entitlementService.getById(2L);
        Entitlement storage = entitlementService.getById(3L);
        Entitlement collab = entitlementService.getById(4L);
        Entitlement templates = entitlementService.getById(5L);
        Entitlement aiTokens = entitlementService.getById(6L);

        // 1. FREE Plan
        PlanRequest freePlan = PlanRequest.builder()
                .code("FREE")
                .name("Free Plan")
                .description("Basic mindmap creation with AI")
                .billingCycle(30)
                .priceCents(0)
                .currency(Currency.VND)
                .entitlementsId(List.of(
                        aiGen.getEntitlementId(),      // 10 AI generations/month
                        export.getEntitlementId(),     // PNG export only
                        storage.getEntitlementId(),    // 1 GB storage
                        aiTokens.getEntitlementId()    // 100k tokens
                ))
                .build();
        planService.save(freePlan);

        // 2. BASIC Plan
        PlanRequest basicPlan = PlanRequest.builder()
                .code("BASIC")
                .name("Basic Plan")
                .description("For individual users with more AI generations")
                .billingCycle(30)
                .priceCents(9900000) // 99,000 VND
                .currency(Currency.VND)
                .entitlementsId(List.of(
                        aiGen.getEntitlementId(),      // 50 AI generations/month
                        export.getEntitlementId(),     // PNG, PDF export
                        storage.getEntitlementId(),    // 5 GB storage
                        templates.getEntitlementId(),  // 10 premium templates
                        aiTokens.getEntitlementId()    // 500k tokens
                ))
                .build();
        planService.save(basicPlan);

        // 3. PRO Plan
        PlanRequest proPlan = PlanRequest.builder()
                .code("PRO")
                .name("Pro Plan")
                .description("For professionals with advanced features")
                .billingCycle(30)
                .priceCents(29900000) // 299,000 VND
                .currency(Currency.VND)
                .entitlementsId(List.of(
                        aiGen.getEntitlementId(),      // Unlimited AI generations
                        export.getEntitlementId(),     // All formats (PNG, PDF, SVG)
                        storage.getEntitlementId(),    // 20 GB storage
                        collab.getEntitlementId(),     // 5 collaborators
                        templates.getEntitlementId(),  // 50 premium templates
                        aiTokens.getEntitlementId()    // 2M tokens
                ))
                .build();
        planService.save(proPlan);

        // 4. TEAM Plan
        PlanRequest teamPlan = PlanRequest.builder()
                .code("TEAM")
                .name("Team Plan")
                .description("For teams with collaboration features")
                .billingCycle(30)
                .priceCents(99900000) // 999,000 VND
                .currency(Currency.VND)
                .entitlementsId(List.of(
                        aiGen.getEntitlementId(),      // Unlimited AI generations
                        export.getEntitlementId(),     // All formats
                        storage.getEntitlementId(),    // 100 GB storage
                        collab.getEntitlementId(),     // 20 collaborators
                        templates.getEntitlementId(),  // Unlimited templates
                        aiTokens.getEntitlementId()    // 10M tokens
                ))
                .build();
        planService.save(teamPlan);
    }
}
