package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.model.Role;
import com.myapp.investment_dashboard_backend.model.User;
import com.myapp.investment_dashboard_backend.repository.RoleRepository;
import com.myapp.investment_dashboard_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        initializeRoles();
        initializeUsers();
    }

    private void initializeRoles() {
        log.info("Initializing roles...");

        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role("ADMIN", "Administrator with full access");
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }

        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role("USER", "Regular user with limited access");
            roleRepository.save(userRole);
            log.info("Created USER role");
        }

        if (!roleRepository.existsByName("MANAGER")) {
            Role managerRole = new Role("MANAGER", "Manager with elevated privileges");
            roleRepository.save(managerRole);
            log.info("Created MANAGER role");
        }

        log.info("Role initialization completed");
    }

    private void initializeUsers() {
        log.info("Initializing users...");

        // Create admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@example.com");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
            
            userRepository.save(adminUser);
            log.info("Created admin user (username: admin, password: admin123)");
        }

        // Create test user if not exists
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setEmail("testuser@example.com");
            testUser.setPasswordHash(passwordEncoder.encode("test123"));
            
            Role userRole = roleRepository.findByName("USER").orElseThrow();
            testUser.setRoles(new HashSet<>(Set.of(userRole)));
            
            userRepository.save(testUser);
            log.info("Created test user (username: testuser, password: test123)");
        }

        // Create manager user if not exists
        if (!userRepository.existsByUsername("manager")) {
            User managerUser = new User();
            managerUser.setUsername("manager");
            managerUser.setEmail("manager@example.com");
            managerUser.setPasswordHash(passwordEncoder.encode("manager123"));
            
            Role userRole = roleRepository.findByName("USER").orElseThrow();
            Role managerRole = roleRepository.findByName("MANAGER").orElseThrow();
            managerUser.setRoles(new HashSet<>(Set.of(userRole, managerRole)));
            
            userRepository.save(managerUser);
            log.info("Created manager user (username: manager, password: manager123)");
        }

        // Patch for existing 'user' account that might not have roles
        userRepository.findByUsername("user").ifPresent(user -> {
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                log.info("Found user 'user' with no roles. Assigning default 'USER' role.");
                Role userRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("Error: USER role not found. Cannot assign default role."));
                user.setRoles(new HashSet<>(Set.of(userRole)));
                userRepository.save(user);
            }
        });

        log.info("User initialization completed");
    }
} 