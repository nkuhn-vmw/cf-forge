package com.cfforge.admin.controller;

import com.cfforge.common.entity.AuditLog;
import com.cfforge.common.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for querying and exporting audit logs.
 * Supports pagination, filtering by action/user/project/date range,
 * and export to CSV or JSON.
 */
@RestController
@RequestMapping("/api/v1/admin/audit")
@Slf4j
public class AuditLogApiController {

    private final AuditLogRepository auditLogRepo;

    public AuditLogApiController(AuditLogRepository auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    @GetMapping
    public Map<String, Object> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLog> result;

        if (action != null && !action.isBlank()) {
            result = auditLogRepo.findByActionContainingOrderByCreatedAtDesc(action, pageable);
        } else if (userId != null) {
            result = auditLogRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (projectId != null) {
            result = auditLogRepo.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        } else {
            result = auditLogRepo.findAll(pageable);
        }

        List<Map<String, Object>> entries = result.getContent().stream()
            .map(this::toMap)
            .collect(Collectors.toList());

        return Map.of(
            "content", entries,
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        );
    }

    @GetMapping("/export/json")
    public ResponseEntity<List<Map<String, Object>>> exportJson(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String action) {

        List<AuditLog> logs = fetchForExport(days, action);
        List<Map<String, Object>> data = logs.stream()
            .map(this::toMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.json")
            .contentType(MediaType.APPLICATION_JSON)
            .body(data);
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String action) {

        List<AuditLog> logs = fetchForExport(days, action);
        StringBuilder csv = new StringBuilder();
        csv.append("id,timestamp,user_id,project_id,action,ip_address,details\n");

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

        for (AuditLog entry : logs) {
            csv.append(entry.getId()).append(',');
            csv.append(fmt.format(entry.getCreatedAt())).append(',');
            csv.append(entry.getUser() != null ? entry.getUser().getId() : "").append(',');
            csv.append(entry.getProjectId() != null ? entry.getProjectId() : "").append(',');
            csv.append(escapeCsv(entry.getAction())).append(',');
            csv.append(entry.getIpAddress() != null ? entry.getIpAddress() : "").append(',');
            csv.append(entry.getDetails() != null ? escapeCsv(entry.getDetails().toString()) : "");
            csv.append('\n');
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.csv")
            .body(csv.toString());
    }

    private List<AuditLog> fetchForExport(int days, String action) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to = Instant.now();

        List<AuditLog> logs = auditLogRepo.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        if (action != null && !action.isBlank()) {
            logs = logs.stream()
                .filter(l -> l.getAction().contains(action))
                .collect(Collectors.toList());
        }
        return logs;
    }

    private Map<String, Object> toMap(AuditLog entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entry.getId());
        map.put("timestamp", entry.getCreatedAt().toString());
        map.put("userId", entry.getUser() != null ? entry.getUser().getId() : null);
        map.put("projectId", entry.getProjectId());
        map.put("action", entry.getAction());
        map.put("ipAddress", entry.getIpAddress());
        map.put("details", entry.getDetails());
        return map;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
