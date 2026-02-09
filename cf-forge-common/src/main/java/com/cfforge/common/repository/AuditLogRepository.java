package com.cfforge.common.repository;

import com.cfforge.common.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<AuditLog> findByActionContainingOrderByCreatedAtDesc(String action, Pageable pageable);
    long countByCreatedAtBetween(Instant start, Instant end);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);
}
