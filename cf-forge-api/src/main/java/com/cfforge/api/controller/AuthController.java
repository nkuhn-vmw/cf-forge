package com.cfforge.api.controller;

import com.cfforge.api.config.SsoProperties;
import com.cfforge.api.service.OidcDiscoveryService;
import com.cfforge.common.entity.User;
import com.cfforge.common.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String STATE_COOKIE = "cf_forge_oauth_state";
    private static final String ACCESS_COOKIE = "cf_forge_access";
    private static final String REFRESH_COOKIE = "cf_forge_refresh";

    private final SsoProperties ssoProperties;
    private final OidcDiscoveryService oidcDiscovery;
    private final UserRepository userRepository;
    private final RestClient restClient;
    private final String domain;

    public AuthController(SsoProperties ssoProperties,
                          OidcDiscoveryService oidcDiscovery,
                          UserRepository userRepository,
                          @Value("${cf.forge.domain}") String domain) {
        this.ssoProperties = ssoProperties;
        this.oidcDiscovery = oidcDiscovery;
        this.userRepository = userRepository;
        this.restClient = RestClient.create();
        this.domain = domain;
    }

    @GetMapping("/login")
    public void login(@RequestParam(defaultValue = "/dashboard") String redirect_uri,
                      HttpServletResponse response) throws Exception {
        // Validate redirect_uri is a relative path only (prevent open redirect)
        if (redirect_uri.contains("://") || redirect_uri.startsWith("//")) {
            redirect_uri = "/dashboard";
        }

        String nonce = generateNonce();
        String state = Base64.getUrlEncoder().withoutPadding()
            .encodeToString((nonce + "|" + redirect_uri).getBytes(StandardCharsets.UTF_8));

        // Set state cookie for CSRF validation
        ResponseCookie stateCookie = ResponseCookie.from(STATE_COOKIE, state)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(600)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        String callbackUrl = domain + "/api/v1/auth/callback";
        String authUrl = oidcDiscovery.getAuthorizationEndpoint()
            + "?response_type=code"
            + "&client_id=" + URLEncoder.encode(ssoProperties.clientId(), StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode(ssoProperties.scopes().replace(",", " "), StandardCharsets.UTF_8)
            + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        // Validate state against cookie
        String stateCookieValue = getCookieValue(request, STATE_COOKIE);
        if (stateCookieValue == null || !stateCookieValue.equals(state)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid OAuth state");
            return;
        }

        // Clear state cookie
        ResponseCookie clearState = ResponseCookie.from(STATE_COOKIE, "")
            .httpOnly(true).secure(true).sameSite("Lax")
            .path("/api/v1/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearState.toString());

        // Decode redirect_uri from state
        String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        String redirectPath = "/dashboard";
        int pipeIdx = decoded.indexOf('|');
        if (pipeIdx >= 0) {
            redirectPath = decoded.substring(pipeIdx + 1);
        }

        // Exchange code for tokens
        String callbackUrl = domain + "/api/v1/auth/callback";
        String credentials = Base64.getEncoder()
            .encodeToString((ssoProperties.clientId() + ":" + ssoProperties.clientSecret())
                .getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", callbackUrl);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                .uri(oidcDiscovery.getTokenEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

            if (tokenResponse == null) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Empty token response");
                return;
            }

            setTokenCookies(response, tokenResponse);
            response.sendRedirect(redirectPath);
        } catch (Exception e) {
            log.error("Token exchange failed", e);
            response.sendRedirect("/login?error=token_exchange_failed");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        // Auto-create/update user
        String uaaUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String userName = jwt.getClaimAsString("user_name");
        List<String> scopes = jwt.getClaimAsStringList("scope");

        userRepository.findByUaaUserId(uaaUserId)
            .orElseGet(() -> userRepository.save(User.builder()
                .uaaUserId(uaaUserId)
                .email(email != null ? email : uaaUserId)
                .displayName(userName)
                .build()));

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("sub", uaaUserId);
        userInfo.put("email", email);
        userInfo.put("userName", userName);
        userInfo.put("scopes", scopes != null ? scopes : List.of());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValue(request, REFRESH_COOKIE);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String credentials = Base64.getEncoder()
            .encodeToString((ssoProperties.clientId() + ":" + ssoProperties.clientSecret())
                .getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                .uri(oidcDiscovery.getTokenEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

            if (tokenResponse == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            setTokenCookies(response, tokenResponse);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Token refresh failed", e);
            clearAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    private void setTokenCookies(HttpServletResponse response, Map<String, Object> tokenResponse) {
        String accessToken = (String) tokenResponse.get("access_token");
        Object expiresInObj = tokenResponse.get("expires_in");
        long expiresIn = expiresInObj instanceof Number n ? n.longValue() : 3600;

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE, accessToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api")
            .maxAge(expiresIn)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        String refreshToken = (String) tokenResponse.get("refresh_token");
        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(604800) // 7 days
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from(ACCESS_COOKIE, "")
            .httpOnly(true).secure(true).sameSite("Lax")
            .path("/api").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true).secure(true).sameSite("Strict")
            .path("/api/v1/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
