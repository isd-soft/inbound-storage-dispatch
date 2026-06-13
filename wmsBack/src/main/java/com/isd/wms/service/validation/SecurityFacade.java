package com.isd.wms.service.validation;

import com.isd.wms.entity.User;
import com.isd.wms.enums.Role;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFacade {

    private final UserRepository userRepository;

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Security alert: Attempt to retrieve username from unauthenticated session");
            throw new RuntimeException("No authenticated user found");
        }

        log.trace("Retrieved current username from SecurityContext: '{}'", authentication.getName());
        return authentication.getName();
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("CRITICAL: Authenticated user '{}' found in JWT, but does not exist in the database!", username);
                return new UserNotFoundException(username);
            });
    }

    public boolean hasRole(Role roleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.debug("Role check failed: No authenticated user found in context");
            return false;
        }

        boolean hasAuthority = auth.getAuthorities().stream()
            .anyMatch(a -> Objects.equals(a.getAuthority(), roleName.name()));

        log.debug("Role check for user '{}': requires Role={}, result={}", auth.getName(), roleName, hasAuthority);
        return hasAuthority;
    }
}
