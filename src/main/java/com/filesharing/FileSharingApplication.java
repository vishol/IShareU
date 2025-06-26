package com.filesharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.filesharing.model.User;
import com.filesharing.repository.UserRepository;

@SpringBootApplication
@EnableScheduling
public class FileSharingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileSharingApplication.class, args);
    }

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository) {
        return args -> {
            String adminEmail = "admin@ishareu.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail(adminEmail);
                admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
                admin.setAdmin(true);
                userRepository.save(admin);
                System.out.println("Default admin user created: admin@ishareu.com / admin123");
            }
        };
    }

} 