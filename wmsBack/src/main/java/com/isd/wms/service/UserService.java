package com.isd.wms.service;

import com.isd.wms.dto.user.UserCreateRequest;
import com.isd.wms.enums.Role;
import com.isd.wms.entity.User;
import com.isd.wms.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User registerUser(UserCreateRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("This username is already taken.");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("This email is already registered.");
        }

        String formattedRole = request.userRole().toUpperCase();
        if (!formattedRole.startsWith("ROLE_")) {
            formattedRole = "ROLE_" + formattedRole;
        }

        String verificationToken = UUID.randomUUID().toString();

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setUserRole(Role.valueOf(formattedRole));
        newUser.setEmailVerified(false);
        newUser.setVerificationToken(verificationToken);
        newUser.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        userRepository.save(newUser);

        emailService.sendVerificationEmail(request.email(), request.username(), verificationToken);
        log.info("User '{}' registered, verification email sent to {}", request.username(), request.email());

        return newUser;
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Verification token has expired. Please ask your supervisor to resend the invitation.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user '{}'", user.getUsername());
    }
}