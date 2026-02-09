package com.cfforge.api.interceptor;

import com.cfforge.api.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object auditMutatingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String action = joinPoint.getSignature().toShortString();
        UUID userId = getCurrentUserId();
        String ipAddress = getClientIp();

        try {
            Object result = joinPoint.proceed();
            auditService.log(userId, null, action, Map.of("status", "success"), ipAddress);
            return result;
        } catch (Exception e) {
            auditService.log(userId, null, action,
                Map.of("status", "error", "message", e.getMessage()), ipAddress);
            throw e;
        }
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getClientIp() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
        }
        return "unknown";
    }
}
