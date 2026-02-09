package com.cfforge.api.interceptor;

import com.cfforge.api.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            if (!rateLimitService.isAllowed(userId)) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Max 100 requests per minute.\"}");
                return false;
            }
            response.setHeader("X-RateLimit-Remaining",
                String.valueOf(rateLimitService.getRemainingRequests(userId)));
        }
        return true;
    }
}
