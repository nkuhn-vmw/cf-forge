package com.cfforge.api.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TokenPassthroughService {

    public Optional<String> getCurrentToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt.getTokenValue());
        }
        return Optional.empty();
    }

    public String requireToken() {
        return getCurrentToken()
            .orElseThrow(() -> new SecurityException("No JWT token available in security context"));
    }
}
