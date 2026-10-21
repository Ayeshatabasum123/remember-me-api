package com.rememberme.api.communityfund.security;

import com.rememberme.api.communityfund.exception.CommunityFundAccessDeniedException;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommunityFundSecurityHelper {

    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new ApiException("Authentication required to access this resource", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Authenticated user not found", HttpStatus.UNAUTHORIZED));
    }

    public Optional<User> getOptionalAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(auth.getName());
    }

    public boolean isAdminOrSuperAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN;
    }

    public boolean isSuperAdmin(User user) {
        return user != null && user.getRole() == User.Role.SUPER_ADMIN;
    }

    public void verifyOwnerOrStaff(Long resourceOwnerUserId) {
        User current = getCurrentAuthenticatedUser();
        if (isAdminOrSuperAdmin(current)) {
            return;
        }
        if (resourceOwnerUserId == null || !resourceOwnerUserId.equals(current.getId())) {
            log.warn("User ID {} attempted to access resource belonging to user ID {}", current.getId(), resourceOwnerUserId);
            throw new CommunityFundAccessDeniedException("You do not have permission to view or modify this resource");
        }
    }
}
