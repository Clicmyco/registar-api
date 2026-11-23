package com.hsis.registrarapi.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
     
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "reset_code")
    private String resetCode;

    @Column(name = "reset_code_expiry")
    private LocalDateTime resetCodeExpiry;


    // --- NEW: The Many-to-Many Bridge ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles", // The name of the new bridge table
        joinColumns = @JoinColumn(name = "user_id"), // The column for the User
        inverseJoinColumns = @JoinColumn(name = "role_id") // The column for the Role
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }

    public LocalDateTime getResetCodeExpiry() { return resetCodeExpiry; }
    public void setResetCodeExpiry(LocalDateTime resetCodeExpiry) { this.resetCodeExpiry = resetCodeExpiry; }


    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // NEW: Getters and Setters for Roles
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // Joins First, Middle (if present), and Last name without double spaces
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (middleName != null && !middleName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName.trim());
        }
        return sb.toString();
    }
}