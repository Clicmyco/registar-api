package com.hsis.registrarapi.config;

import com.hsis.registrarapi.entity.AcademicProgram;
import com.hsis.registrarapi.repository.AcademicProgramRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ProgramDataSeeder {

    @Bean
    CommandLineRunner seedAcademicPrograms(AcademicProgramRepository programRepository) {
        return args -> {
            if (programRepository.count() == 0) {
                programRepository.saveAll(List.of(
                    new AcademicProgram(
                        "BSC-CS",
                        "B.Sc. in Computer Science",
                        "Undergraduate",
                        "School of Computer Science",
                        4,
                        120,
                        "Core foundations in algorithms, operating systems, databases, and full-stack software architecture."
                    ),
                    new AcademicProgram(
                        "BSC-SE",
                        "B.Sc. in Software Engineering",
                        "Undergraduate",
                        "School of Computer Science",
                        4,
                        128,
                        "Applied software design, testing, DevOps pipelines, and enterprise system development."
                    ),
                    new AcademicProgram(
                        "MSC-AI",
                        "M.Sc. in Artificial Intelligence",
                        "Graduate",
                        "School of Computer Science",
                        2,
                        48,
                        "Advanced machine learning, natural language processing, and AI governance."
                    ),
                    new AcademicProgram(
                        "MSC-CYB",
                        "M.Sc. in Cybersecurity",
                        "Graduate",
                        "Department of Information Security",
                        2,
                        45,
                        "Network defense, cryptography, access control systems, and security auditing."
                    )
                ));
            }
        };
    }
}