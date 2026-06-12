package com.isd.wms.service;

import com.isd.wms.dto.user.UserCreateRequest;
import com.isd.wms.dto.user.UserResponse;
import com.isd.wms.dto.user.UserUpdateRequest;
import com.isd.wms.enums.Role;
import com.isd.wms.entity.User;
import com.isd.wms.mapper.UserMapper;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void registerUser(UserCreateRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("This username is already taken.");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("This email is already registered.");
        }

        if (request.userRole() == Role.ROLE_DEV) {
            log.warn("Security block: Attempt to create a DEV account via API.");
            throw new AccessDeniedException("Creating DEV accounts via API is strictly prohibited.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User newUser = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.userRole(),
                false,
                verificationToken,
                LocalDateTime.now().plusHours(24)
        );
        userRepository.save(newUser);

        emailService.sendVerificationEmail(request.email(), request.username(), verificationToken);
        log.info("User '{}' registered, verification email sent to {}", request.username(), request.email());

    }

    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request) {

        if (!securityFacade.hasRole(Role.ROLE_DEV)) {
            log.warn("Security block: Non-DEV user attempted to update user ID {}", userId);
            throw new AccessDeniedException("Only Developers are allowed to update user information.");
        }

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!existingUser.getUsername().equals(request.username()) &&
                userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("This username is already taken by another user.");
        }

        if (request.userRole() == Role.ROLE_DEV && existingUser.getUserRole() != Role.ROLE_DEV) {
            log.warn("Security block: Attempt to escalate a user to ROLE_DEV.");
            throw new AccessDeniedException("Promoting an account to ROLE_DEV is strictly prohibited.");
        }

        existingUser.setUsername(request.username());
        existingUser.setUserRole(request.userRole());

        log.info("User ID {} successfully updated. New username: {}, New role: {}",
                userId, request.username(), request.userRole());

        userRepository.save(existingUser);
    }

    @Transactional
    public boolean verifyEmail(String rawToken) {
        String cleanToken = rawToken != null ? rawToken.trim() : "";

        log.info("Attempting to verify email with token: [{}]", cleanToken);

        User user = userRepository.findByVerificationToken(cleanToken)
            .orElseThrow(() -> {
                log.error("Token not found in database: [{}]", cleanToken);
                return new RuntimeException("Invalid verification token.");
            });

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            String username = user.getUsername();
            userRepository.delete(user);
            log.info("Expired token. Deleted unverified user '{}'", username);
            return false;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user '{}'", user.getUsername());

        return true;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpExpiredUnverifiedUsers() {
        List<User> expiredUsers = userRepository.findAllByEmailVerifiedFalseAndVerificationTokenExpiresAtBefore(LocalDateTime.now());

        if (!expiredUsers.isEmpty()) {
            userRepository.deleteAll(expiredUsers);

            log.info("Background Job: Successfully deleted {} expired unverified accounts.", expiredUsers.size());
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (targetUser.getUsername().equals(securityFacade.getCurrentUsername())) {
            throw new RuntimeException("You cannot ban your own account.");
        }

        boolean isDev = securityFacade.hasRole(Role.ROLE_DEV);
        boolean isSupervisor = securityFacade.hasRole(Role.ROLE_SUPERVISOR);

        if (!isDev && !isSupervisor) {
            log.warn("Security block: Unauthorized user attempted to ban user ID {}", userId);
            throw new AccessDeniedException("You do not have permission to ban users.");
        }

        if (isSupervisor && !isDev && targetUser.getUserRole() != Role.ROLE_OPERATOR) {
            log.warn("Security block: SUPERVISOR attempted to ban a non-OPERATOR account.");
            throw new AccessDeniedException("Supervisors are only allowed to ban operator accounts.");
        }

        targetUser.setIsActive(false);
        userRepository.save(targetUser);

        log.info("User '{}' (ID: {}) was successfully deactivated by {}",
                targetUser.getUsername(), userId, securityFacade.getCurrentUsername());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByIsActiveTrue().stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
