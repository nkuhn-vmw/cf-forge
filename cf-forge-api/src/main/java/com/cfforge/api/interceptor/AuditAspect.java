package com.cfforge.api.interceptor;

import com.cfforge.api.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AOP audit aspect that logs all state-changing HTTP operations.
 * Writes structured JSON audit entries to both database and SLF4J
 * for CF syslog drain consumption.
 */
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
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object auditMutatingOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String action = joinPoint.getSignature().toShortString();
        String uaaUserId = getCurrentUaaUserId();
        String username = getCurrentUsername();
        String ipAddress = getClientIp();
        String method = getHttpMethod();
        String path = getRequestPath();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - startTime;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", "success");
            details.put("method", method);
            details.put("path", path);
            details.put("durationMs", durationMs);

            auditService.log(uaaUserId, extractProjectId(joinPoint), action, details, ipAddress);

            // Structured JSON log for CF syslog drain
            log.info("AUDIT action={} user={} ip={} method={} path={} status=success durationMs={}",
                action, username, ipAddress, method, path, durationMs);

            return result;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", "error");
            details.put("method", method);
            details.put("path", path);
            details.put("error", e.getMessage());
            details.put("durationMs", durationMs);

            auditService.log(uaaUserId, extractProjectId(joinPoint), action, details, ipAddress);

            log.warn("AUDIT action={} user={} ip={} method={} path={} status=error error=\"{}\" durationMs={}",
                action, username, ipAddress, method, path, e.getMessage(), durationMs);

            throw e;
        }
    }

    private String getCurrentUaaUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("user_name");
            return name != null ? name : jwt.getSubject();
        }
        return "anonymous";
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

    private String getHttpMethod() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getMethod();
        }
        return "UNKNOWN";
    }

    private String getRequestPath() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getRequestURI();
        }
        return "unknown";
    }

    private UUID extractProjectId(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("projectId".equals(paramNames[i]) && args[i] instanceof UUID uuid) {
                    return uuid;
                }
            }
        }
        return null;
    }
}
