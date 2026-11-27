package com.hsis.registrarapi.config;

import com.hsis.registrarapi.entity.Role;
import com.hsis.registrarapi.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RoleDataSeeder {

    @Bean
    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> defaultRoles = List.of(
                    "ROLE_STUDENT",
                    "ROLE_ADVISOR",
                    "ROLE_REGISTRAR",
                    "ROLE_ADMIN",
                    "ROLE_INSTRUCTOR"
            );

            for (String roleName : defaultRoles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = new Role();
                    role.setName(roleName);
                    roleRepository.save(role);
                }
            }
        };
    }
}