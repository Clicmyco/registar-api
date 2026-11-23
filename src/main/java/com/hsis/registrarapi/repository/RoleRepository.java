package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // We will need this later to find roles by their exact name (like "ROLE_STUDENT")
    Optional<Role> findByName(String name);
}