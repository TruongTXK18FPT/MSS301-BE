package com.mss301.authservice.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mss301.authservice.entity.Role;
import com.mss301.authservice.entity.UserAccount;
import com.mss301.authservice.repository.RoleRepository;
import com.mss301.authservice.repository.UserRepository;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        // Create default roles if not exist
        List<Role> roles = createDefaultRoles();

        // Create test users if not exist
        createTestUsers(roles);

        log.info("Data initialization completed successfully.");
    }

    private List<Role> createDefaultRoles() {
        List<String> roleNames = Arrays.asList("ADMIN", "STUDENT", "GUARDIAN", "TEACHER");

        return roleNames.stream()
                .<Role>map(roleName -> {
                    return roleRepository.findByName(roleName).orElseGet(() -> {
                        Role role = new Role();
                        role.setName(roleName);
                        role = roleRepository.save(role);
                        log.info("Created role: {}", roleName);
                        return role;
                    });
                })
                .toList();
    }

    private void createTestUsers(List<Role> roles) {
        // Test users data
        List<TestUserData> testUsers = Arrays.asList(
                new TestUserData("mss301admin@gmail.com", "ADMIN"),
                new TestUserData("mss301student@gmail.com", "STUDENT"),
                new TestUserData("mssguardian@gmail.com", "GUARDIAN"),
                new TestUserData("mss301teacher@gmail.com", "TEACHER"));

        for (TestUserData userData : testUsers) {
            if (!userRepository.existsByEmail(userData.email)) {
                // Create user
                UserAccount user = new UserAccount();
                user.setTenantId(null); // No tenant relationship for now
                user.setEmail(userData.email);
                // fullName will be set in profile-service, not needed for test users
                user.setPassword(passwordEncoder.encode("123456789"));
                user.setStatus(UserAccount.UserStatus.ACTIVE);
                user.setEmailVerified(true);
                user.setIsGoogleUser(false); // Set required boolean fields
                user.setPasswordSetupRequired(false);
                user.setProfileCompleted(false);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                user = userRepository.save(user);
                log.info("Created test user: {}", userData.email);

                // Assign role to user (single role)
                Role userRole = roles.stream()
                        .filter(role -> role.getName().equals(userData.roleName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Role not found: " + userData.roleName));

                user.setRoleId(userRole.getId());
                userRepository.save(user);

                log.info("Assigned role {} (id={}) to user {}", userData.roleName, userRole.getId(), userData.email);
            } else {
                log.info("Test user already exists: {}", userData.email);
            }
        }
    }

    private static class TestUserData {
        final String email;
        final String roleName;

        TestUserData(String email, String roleName) {
            this.email = email;
            this.roleName = roleName;
        }
    }
}
