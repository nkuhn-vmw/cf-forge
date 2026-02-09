package com.cfforge.api.service;

import com.cfforge.common.entity.AuditLog;
import com.cfforge.common.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(UUID userId, UUID projectId, String action,
                    Map<String, Object> details, String ipAddress) {
        var entry = AuditLog.builder()
            .user(userId != null ? com.cfforge.common.entity.User.builder().id(userId).build() : null)
            .projectId(projectId)
            .action(action)
            .details(details)
            .ipAddress(ipAddress)
            .build();
        auditLogRepository.save(entry);
    }
}
