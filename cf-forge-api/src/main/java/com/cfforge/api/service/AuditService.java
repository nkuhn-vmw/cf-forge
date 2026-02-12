package com.cfforge.api.service;

import com.cfforge.common.entity.AuditLog;
import com.cfforge.common.entity.User;
import com.cfforge.common.repository.AuditLogRepository;
import com.cfforge.common.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(String uaaUserId, UUID projectId, String action,
                    Map<String, Object> details, String ipAddress) {
        User user = null;
        if (uaaUserId != null) {
            user = userRepository.findByUaaUserId(uaaUserId).orElseGet(() -> {
                // Auto-create user if not yet in DB (first request before /me is called)
                return userRepository.save(User.builder()
                    .uaaUserId(uaaUserId)
                    .email(uaaUserId)
                    .displayName(uaaUserId)
                    .build());
            });
        }

        try {
            var entry = AuditLog.builder()
                .user(user)
                .projectId(projectId)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }
}
