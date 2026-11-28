package com.hsis.registrarapi.config;

import com.hsis.registrarapi.entity.Course;
import com.hsis.registrarapi.entity.Role;
import com.hsis.registrarapi.entity.User;
import com.hsis.registrarapi.repository.CourseRepository;
import com.hsis.registrarapi.repository.RoleRepository;
import com.hsis.registrarapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class DataSeederConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSeederConfig.class);

    private static final List<String> DEFAULT_ROLES = List.of(
            "ROLE_STUDENT",
            "ROLE_INSTRUCTOR",
            "ROLE_ADVISOR",
            "ROLE_REGISTRAR",
            "ROLE_ADMIN"
    );

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin.email:admin@university.edu}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.seed.demo-users:true}")
    private boolean seedDemoUsers;

    @Bean
    public CommandLineRunner seedInitialData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (!seedEnabled) {
                log.info("Startup data seeding is disabled (app.seed.enabled=false).");
                return;
            }
            initializeCatalogAndAccounts(roleRepository, userRepository, courseRepository, passwordEncoder);
        };
    }

    @Transactional
    protected void initializeCatalogAndAccounts(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder) {

        // 1. Ensure all 5 system roles exist
        for (String roleName : DEFAULT_ROLES) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = new Role();
                role.setName(roleName);
                log.info("Seeded security role: {}", roleName);
                return roleRepository.save(role);
            });
        }

        Role adminRole = getRoleOrThrow(roleRepository, "ROLE_ADMIN");
        Role registrarRole = getRoleOrThrow(roleRepository, "ROLE_REGISTRAR");
        Role advisorRole = getRoleOrThrow(roleRepository, "ROLE_ADVISOR");
        Role instructorRole = getRoleOrThrow(roleRepository, "ROLE_INSTRUCTOR");
        Role studentRole = getRoleOrThrow(roleRepository, "ROLE_STUDENT");

        // 2. Ensure bootstrap Admin account exists
        ensureUser(
                userRepository,
                passwordEncoder,
                adminEmail,
                "System",
                "Administrator",
                adminPassword,
                Set.of(adminRole, registrarRole)
        );

        // 3. Optionally seed demo Faculty, Advisor, and Student accounts
        User demoInstructor = null;
        if (seedDemoUsers) {
            demoInstructor = ensureUser(
                    userRepository,
                    passwordEncoder,
                    "instructor@university.edu",
                    "Samuel",
                    "Bekele",
                    "Instructor123!",
                    Set.of(instructorRole)
            );

            ensureUser(
                    userRepository,
                    passwordEncoder,
                    "advisor@university.edu",
                    "Hanna",
                    "Tadesse",
                    "Advisor123!",
                    Set.of(advisorRole)
            );

            ensureUser(
                    userRepository,
                    passwordEncoder,
                    "student@university.edu",
                    "Dawit",
                    "Alemu",
                    "Student123!",
                    Set.of(studentRole)
            );
        }

        // 4. Seed starter catalog courses only if the catalog is currently empty
        if (courseRepository.count() == 0) {
            Course cs101 = new Course();
            cs101.setCourseCode("CS101");
            cs101.setTitle("Introduction to Computer Science");
            cs101.setCredits(3);
            cs101.setMaxCapacity(30);
            cs101.setMeetingDays("MON,WED");
            cs101.setStartTime(LocalTime.of(9, 0));
            cs101.setEndTime(LocalTime.of(10, 30));
            cs101.setRoom("Main Hall 101");
            cs101.setInstructor(demoInstructor);
            cs101 = courseRepository.save(cs101);

            Course cs201 = new Course();
            cs201.setCourseCode("CS201");
            cs201.setTitle("Data Structures & Algorithms");
            cs201.setCredits(4);
            cs201.setMaxCapacity(25);
            cs201.setPrerequisite(cs101);
            cs201.setMeetingDays("TUE,THU");
            cs201.setStartTime(LocalTime.of(10, 0));
            cs201.setEndTime(LocalTime.of(11, 45));
            cs201.setRoom("Science Bldg 204");
            cs201.setInstructor(demoInstructor);
            courseRepository.save(cs201);

            Course cs301 = new Course();
            cs301.setCourseCode("CS301");
            cs301.setTitle("Database Management Systems");
            cs301.setCredits(3);
            cs301.setMaxCapacity(25);
            cs301.setMeetingDays("MON,WED");
            cs301.setStartTime(LocalTime.of(14, 0));
            cs301.setEndTime(LocalTime.of(15, 30));
            cs301.setRoom("Lab Complex 3B");
            cs301.setInstructor(demoInstructor);
            courseRepository.save(cs301);

            log.info("Seeded 3 starter Computer Science courses (CS101, CS201, CS301).");
        }
    }

    private User ensureUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String firstName,
            String lastName,
            String rawPassword,
            Set<Role> roles) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setActive(true);
            user.setRoles(new HashSet<>(roles));
            log.info("Seeded default user account: {} ({} {})", email, firstName, lastName);
            return userRepository.save(user);
        });
    }

    private Role getRoleOrThrow(RoleRepository roleRepository, String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Missing required role: " + roleName));
    }
}