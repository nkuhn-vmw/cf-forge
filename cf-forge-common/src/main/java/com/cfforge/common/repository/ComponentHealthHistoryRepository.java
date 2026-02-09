package com.cfforge.common.repository;

import com.cfforge.common.entity.ComponentHealthHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ComponentHealthHistoryRepository extends JpaRepository<ComponentHealthHistory, UUID> {

    List<ComponentHealthHistory> findByComponentNameAndRecordedAtBetweenOrderByRecordedAtDesc(
        String componentName, Instant start, Instant end);

    @Query("SELECT h.componentName, h.status, h.recordedAt FROM ComponentHealthHistory h " +
           "WHERE h.recordedAt = (SELECT MAX(h2.recordedAt) FROM ComponentHealthHistory h2 " +
           "WHERE h2.componentName = h.componentName)")
    List<Object[]> findLatestHealthForAllComponents();
}
