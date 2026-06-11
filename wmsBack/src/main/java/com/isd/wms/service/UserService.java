package com.isd.wms.service;

import com.isd.wms.dto.user.UserCreateRequest;
import com.isd.wms.dto.user.UserResponse;
import com.isd.wms.enums.Role;
import com.isd.wms.entity.User;
import com.isd.wms.mapper.UserMapper;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final SecurityFacade securityFacade;

    @Transactional
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

        if (securityFacade.hasRole("ROLE_SUPERVISOR") && formattedRole.equals("ROLE_DEV")) {
            log.warn("Security breach attempt: SUPERVISOR tried to create a DEV account.");
            throw new AccessDeniedException("Supervisors are not allowed to create developer accounts.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User newUser = new User(
            request.username(),
            request.email(),
            passwordEncoder.encode(request.password()),
            Role.valueOf(formattedRole),
            false,
            verificationToken,
            Instant.now().plus(24, ChronoUnit.HOURS)
        );
        userRepository.save(newUser);

        emailService.sendVerificationEmail(request.email(), request.username(), verificationToken);
        log.info("User '{}' registered, verification email sent to {}", request.username(), request.email());

        return newUser;
    }

    @Transactional
    public User verifyEmail(String rawToken) {
        String cleanToken = rawToken != null ? rawToken.trim() : "";

        log.info("Attempting to verify email with token: [{}]", cleanToken);

        User user = userRepository.findByVerificationToken(cleanToken)
            .orElseThrow(() -> {
                log.error("Token not found in database: [{}]", cleanToken);
                return new RuntimeException("Invalid verification token");
            });

        if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Verification token has expired. Please ask your supervisor to resend the invitation.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user '{}'", user.getUsername());

        return user;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(userMapper::toResponse)
            .toList();
    }
}
