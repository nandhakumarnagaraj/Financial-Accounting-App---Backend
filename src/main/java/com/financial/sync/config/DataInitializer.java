package com.financial.sync.config;

import com.financial.sync.entity.Role;
import com.financial.sync.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing roles...");
        
        createRoleIfNotExists(Role.RoleName.ROLE_ADMIN);
        createRoleIfNotExists(Role.RoleName.ROLE_ACCOUNTANT);
        createRoleIfNotExists(Role.RoleName.ROLE_ANALYST);
        
        log.info("Roles initialization completed");
    }
    
    private void createRoleIfNotExists(Role.RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        } else {
            log.info("Role already exists: {}", roleName);
        }
    }
}