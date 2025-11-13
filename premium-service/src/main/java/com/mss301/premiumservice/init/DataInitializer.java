package com.mss301.premiumservice.init;

import com.mss301.premiumservice.constant.Currency;
import com.mss301.premiumservice.constant.PlanStatus;
import com.mss301.premiumservice.model.Entitlement;
import com.mss301.premiumservice.model.Plan;
import com.mss301.premiumservice.repository.EntitlementRepository;
import com.mss301.premiumservice.repository.PlanRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialize demo plans on startup
 */
@Component
@Slf4j
@Order(1) // Chạy đầu tiên để khởi tạo entitlements và plans
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing/updating student plans with entitlements...");

        // Lấy hoặc tạo các entitlements cần thiết
        Entitlement aiGen = getOrCreateEntitlement("AI_MINDMAP_GEN", "AI Mindmap Generation",
                "Generate mindmaps using AI per month", 10);
        Entitlement export = getOrCreateEntitlement("EXPORT_FORMAT", "Export Formats",
                "Export mindmap to PNG, PDF, SVG", 1);
        Entitlement storage = getOrCreateEntitlement("CLOUD_STORAGE", "Cloud Storage",
                "Store mindmaps in cloud (GB)", 1);
        Entitlement collab = getOrCreateEntitlement("COLLAB_USERS", "Collaboration Users",
                "Number of users can collaborate on a mindmap", 1);
        Entitlement templates = getOrCreateEntitlement("TEMPLATE_ACCESS", "Premium Templates",
                "Access to premium mindmap templates", 10);
        Entitlement aiTokens = getOrCreateEntitlement("AI_TOKENS", "AI Tokens",
                "Monthly AI processing tokens", 100000);

        // Plan 1: Stellar Student - 10,000 VND (1 month)
        Plan stellarStudent = planRepository.findByCode("STELLAR_STUDENT")
                .orElse(new Plan());
        boolean isNew = stellarStudent.getPlanId() == null;
        stellarStudent.setCode("STELLAR_STUDENT");
        stellarStudent.setName("Stellar Student");
        stellarStudent.setDescription("Gói cơ bản dành cho học sinh với các tính năng premium thiết yếu");
        stellarStudent.setBillingCycle(1);
        stellarStudent.setPrice(10000L); // 10,000 VND
        stellarStudent.setCurrency(Currency.VND);
        stellarStudent.setStatus(PlanStatus.ACTIVE);
        log.info("{} Stellar Student plan - Price: {} VND", isNew ? "Creating" : "Updating", stellarStudent.getPrice());

        // Entitlements cho Stellar Student: AI Generation, Export, Storage, AI Tokens
        // Merge entitlements để đảm bảo chúng được attach vào persistence context
        List<Entitlement> stellarEntitlements = new ArrayList<>();
        stellarEntitlements.add(entityManager.merge(aiGen));
        stellarEntitlements.add(entityManager.merge(export));
        stellarEntitlements.add(entityManager.merge(storage));
        stellarEntitlements.add(entityManager.merge(aiTokens));
        stellarStudent.setEntitlements(stellarEntitlements);
        planRepository.save(stellarStudent);
        entityManager.flush();
        entityManager.clear();
        log.info("Saved Stellar Student - ID: {}, Price: {} VND", stellarStudent.getPlanId(),
                stellarStudent.getPrice());

        // Plan 2: Galaxy Explorer - 11,000 VND (1 month)
        Plan galaxyExplorer = planRepository.findByCode("GALAXY_EXPLORER")
                .orElse(new Plan());
        boolean isNewGalaxy = galaxyExplorer.getPlanId() == null;
        galaxyExplorer.setCode("GALAXY_EXPLORER");
        galaxyExplorer.setName("Galaxy Explorer");
        galaxyExplorer
                .setDescription("Tất cả tính năng Stellar Student cộng với khóa học độc quyền và huy hiệu đặc biệt");
        galaxyExplorer.setBillingCycle(1);
        galaxyExplorer.setPrice(11000L); // 11,000 VND
        galaxyExplorer.setCurrency(Currency.VND);
        galaxyExplorer.setStatus(PlanStatus.ACTIVE);
        log.info("{} Galaxy Explorer plan - Price: {} VND", isNewGalaxy ? "Creating" : "Updating",
                galaxyExplorer.getPrice());

        // Entitlements cho Galaxy Explorer: Tất cả Stellar + Templates + Collaboration
        // Merge entitlements để đảm bảo chúng được attach vào persistence context
        // (Sau entityManager.clear(), cần merge lại tất cả)
        List<Entitlement> galaxyEntitlements = new ArrayList<>();
        galaxyEntitlements.add(entityManager.merge(aiGen));
        galaxyEntitlements.add(entityManager.merge(export));
        galaxyEntitlements.add(entityManager.merge(storage));
        galaxyEntitlements.add(entityManager.merge(aiTokens));
        galaxyEntitlements.add(entityManager.merge(templates));
        galaxyEntitlements.add(entityManager.merge(collab));
        galaxyExplorer.setEntitlements(galaxyEntitlements);
        planRepository.save(galaxyExplorer);
        entityManager.flush();
        entityManager.clear();
        log.info("Saved Galaxy Explorer - ID: {}, Price: {} VND", galaxyExplorer.getPlanId(),
                galaxyExplorer.getPrice());

        // Plan 3: Universe Master - 12,000 VND (1 month)
        Plan universeMaster = planRepository.findByCode("UNIVERSE_MASTER")
                .orElse(new Plan());
        boolean isNewUniverse = universeMaster.getPlanId() == null;
        universeMaster.setCode("UNIVERSE_MASTER");
        universeMaster.setName("Universe Master");
        universeMaster
                .setDescription("Tất cả tính năng Galaxy Explorer cộng với 1-on-1 coaching và quyền truy cập sớm");
        universeMaster.setBillingCycle(1);
        universeMaster.setPrice(12000L); // 12,000 VND
        universeMaster.setCurrency(Currency.VND);
        universeMaster.setStatus(PlanStatus.ACTIVE);
        log.info("{} Universe Master plan - Price: {} VND", isNewUniverse ? "Creating" : "Updating",
                universeMaster.getPrice());

        // Entitlements cho Universe Master: Tất cả Galaxy (đã có tất cả)
        // Merge entitlements để đảm bảo chúng được attach vào persistence context
        // (Sau entityManager.clear(), cần merge lại tất cả)
        List<Entitlement> universeEntitlements = new ArrayList<>();
        universeEntitlements.add(entityManager.merge(aiGen));
        universeEntitlements.add(entityManager.merge(export));
        universeEntitlements.add(entityManager.merge(storage));
        universeEntitlements.add(entityManager.merge(aiTokens));
        universeEntitlements.add(entityManager.merge(templates));
        universeEntitlements.add(entityManager.merge(collab));
        universeMaster.setEntitlements(universeEntitlements);
        planRepository.save(universeMaster);
        entityManager.flush();
        entityManager.clear();
        log.info("Saved Universe Master - ID: {}, Price: {} VND", universeMaster.getPlanId(),
                universeMaster.getPrice());

        log.info("✅ Initialized/Updated 3 student plans with entitlements:");
        log.info("  - Stellar Student: 10,000 VND (1 month) - {} entitlements", stellarEntitlements.size());
        log.info("  - Galaxy Explorer: 11,000 VND (1 month) - {} entitlements", galaxyEntitlements.size());
        log.info("  - Universe Master: 12,000 VND (1 month) - {} entitlements", universeEntitlements.size());
    }

    private Entitlement getOrCreateEntitlement(String code, String name, String description, long defaultLimit) {
        return entitlementRepository.findByCode(code).orElseGet(() -> {
            Entitlement entitlement = new Entitlement();
            entitlement.setCode(code);
            entitlement.setName(name);
            entitlement.setDescription(description);
            entitlement.setDefaultLimit(defaultLimit);
            // Sử dụng GB làm unit mặc định (có thể thay đổi tùy theo entitlement)
            entitlement.setUnit(com.mss301.premiumservice.constant.Unit.GB);
            return entitlementRepository.save(entitlement);
        });
    }

}