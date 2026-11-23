package com.hsis.registrarapi.repository;

import com.hsis.registrarapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Custom query method
    Optional<User> findByEmail(String email);
    
}