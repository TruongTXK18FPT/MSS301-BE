package com.mss301.authservice.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.entity.Role;
import com.mss301.authservice.entity.Tenant;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.entity.UserRole;
import com.mss301.authservice.repository.RoleRepository;
import com.mss301.authservice.repository.TenantRepository;
import com.mss301.authservice.repository.UserRepository;
import com.mss301.authservice.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Data Initializer for creating default users and roles
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        // Create default tenant if not exists
        Tenant defaultTenant = createDefaultTenant();

        // Create default roles if not exist
        List<Role> roles = createDefaultRoles(defaultTenant);

        // Create test users if not exist
        createTestUsers(defaultTenant, roles);

        log.info("Data initialization completed successfully.");
    }

    private Tenant createDefaultTenant() {
        if (tenantRepository.existsByName("MSS301")) {
            return tenantRepository.findByName("MSS301").orElseThrow();
        }

        Tenant tenant = new Tenant();
        tenant.setName("MSS301");
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        tenant = tenantRepository.save(tenant);
        log.info("Created default tenant: {}", tenant.getName());
        return tenant;
    }

    private List<Role> createDefaultRoles(Tenant tenant) {
        List<String> roleNames = Arrays.asList("ADMIN", "USER", "GUARDIAN", "TEACHER");

        return roleNames.stream()
                .map(roleName -> {
                    return roleRepository
                            .findByNameAndTenantId(roleName, tenant.getId())
                            .orElseGet(() -> {
                                Role role = new Role();
                                role.setName(roleName);
                                role.setTenantId(tenant.getId());
                                role = roleRepository.save(role);
                                log.info("Created role: {} for tenant: {}", roleName, tenant.getName());
                                return role;
                            });
                })
                .toList();
    }

    private void createTestUsers(Tenant tenant, List<Role> roles) {
        // Test users data
        List<TestUserData> testUsers = Arrays.asList(
                new TestUserData("mss301admin@gmail.com", "mss301admin", "ADMIN"),
                new TestUserData("mss301user@gmail.com", "mss301user", "USER"),
                new TestUserData("mssguardian@gmail.com", "mssguardian", "GUARDIAN"),
                new TestUserData("mss301teacher@gmail.com", "mss301teacher", "TEACHER"));

        for (TestUserData userData : testUsers) {
            if (!userRepository.existsByEmail(userData.email)) {
                // Create user
                UserAccount user = new UserAccount();
                user.setTenantId(tenant.getId());
                user.setEmail(userData.email);
                user.setUsername(userData.username);
                user.setPassword(passwordEncoder.encode("123456789"));
                user.setStatus(UserAccount.UserStatus.ACTIVE);
                user.setEmailVerified(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                user = userRepository.save(user);
                log.info("Created test user: {}", userData.email);

                // Assign role to user
                Role userRole = roles.stream()
                        .filter(role -> role.getName().equals(userData.roleName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Role not found: " + userData.roleName));

                UserRole userRoleAssignment = new UserRole();
                userRoleAssignment.setUserId(user.getId());
                userRoleAssignment.setRoleId(userRole.getId());
                userRoleRepository.save(userRoleAssignment);

                log.info("Assigned role {} to user {}", userData.roleName, userData.email);
            } else {
                log.info("Test user already exists: {}", userData.email);
            }
        }
    }

    private static class TestUserData {
        final String email;
        final String username;
        final String roleName;

        TestUserData(String email, String username, String roleName) {
            this.email = email;
            this.username = username;
            this.roleName = roleName;
        }
    }
}
