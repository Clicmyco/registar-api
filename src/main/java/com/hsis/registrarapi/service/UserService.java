package com.hsis.registrarapi.service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import com.hsis.registrarapi.dto.UserRegistrationDto;
//import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.dto.UserLoginDto; // NEW
import com.hsis.registrarapi.security.JwtUtil; // NEW
import com.hsis.registrarapi.repository.UserRepository;
import com.hsis.registrarapi.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder; // NEW
import org.springframework.stereotype.Service;
import com.hsis.registrarapi.entity.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class UserService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder; // NEW TOOL
    private final EmailService emailService;  
    // NEW: Add PasswordEncoder to the constructor
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder ,JwtUtil jwtUtil ,EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil=jwtUtil;
        this.emailService= emailService;
    }

    public User registerUser(UserRegistrationDto registrationData) {
        
        Optional<User> existingUser = userRepository.findByEmail(registrationData.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("A user with this email already exists!");
        }

        User newUser = new User();
        newUser.setEmail(registrationData.getEmail());
        
        // --- NEW: Hash the password before putting it in the entity! ---
        String scrambledPassword = passwordEncoder.encode(registrationData.getPassword());
        newUser.setPasswordHash(scrambledPassword);
        
        // --- NEW LOGIC: Role Assignment ---
        
        // 1. Try to find the "ROLE_STUDENT" in the database.
        // 2. If it does not exist (orElseGet), create a new Role, name it, and save it.
        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ROLE_STUDENT");
                    return roleRepository.save(newRole);
                });

        // 3. Add this role into the User's secure Set (collection) of roles
        newUser.getRoles().add(studentRole);

        if (registrationData.getFirstName() == null || registrationData.getFirstName().isBlank() ||
            registrationData.getLastName() == null || registrationData.getLastName().isBlank()) {
            throw new RuntimeException("First name and last name are required.");
        }

        newUser.setFirstName(registrationData.getFirstName().trim());
        newUser.setMiddleName(
            registrationData.getMiddleName() != null ? registrationData.getMiddleName().trim() : ""
        );
        newUser.setLastName(registrationData.getLastName().trim());
           

        return userRepository.save(newUser);
    }

    // --- NEW METHOD: Login ---
    public String loginUser(UserLoginDto loginData) {
        // 1. Find the user in the database
        User existingUser = userRepository.findByEmail(loginData.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password")); // Generic error

        // 2. Ask BCrypt to check if the typed password matches the database hash
        if (!passwordEncoder.matches(loginData.getPassword(), existingUser.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password"); // Generic error
        }

        if (!existingUser.isActive()) {
    throw new RuntimeException("Your account has been deactivated. Please contact an administrator.");
}

        // Extract just the String names from the Role objects using a Java Stream
        List<String> roleNames = existingUser.getRoles().stream()
                .map(Role::getName) // "Grab just the name variable from each Role"
                .collect(Collectors.toList()); // "Package them into a List"

        

        // 3. If they match, print the ID card!
        return jwtUtil.generateToken(existingUser.getEmail() ,roleNames,existingUser.getFullName());
    }
 

    public void requestPasswordReset(String email) {
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email address."));

        // Generate a cryptographically secure 6-digit code (100000 to 999999)
        SecureRandom random = new SecureRandom();
        String code = String.valueOf(100000 + random.nextInt(900000));

        existingUser.setResetCode(code);
        existingUser.setResetCodeExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(existingUser);

        emailService.sendPasswordResetCode(existingUser.getEmail(), code);
    }

    public void resetPassword(String email, String code, String newPassword) {
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email address."));

        if (existingUser.getResetCode() == null || !existingUser.getResetCode().equals(code)) {
            throw new RuntimeException("Invalid confirmation code.");
        }

        if (existingUser.getResetCodeExpiry() == null || existingUser.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Confirmation code has expired. Please request a new one.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long.");
        }

        // Hash the new password and clear the one-time code
        existingUser.setPasswordHash(passwordEncoder.encode(newPassword));
        existingUser.setResetCode(null);
        existingUser.setResetCodeExpiry(null);
        userRepository.save(existingUser);
    }
    


}
