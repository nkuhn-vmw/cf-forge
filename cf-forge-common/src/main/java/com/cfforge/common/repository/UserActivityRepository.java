package com.cfforge.common.repository;

import com.cfforge.common.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<UserActivity> findByActivityTypeAndCreatedAtBetween(String type, Instant start, Instant end);

    @Query("SELECT ua.user.id, COUNT(ua) FROM UserActivity ua " +
           "WHERE ua.createdAt BETWEEN :start AND :end GROUP BY ua.user.id ORDER BY COUNT(ua) DESC")
    List<Object[]> findTopActiveUsers(Instant start, Instant end, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT ua.user.id) FROM UserActivity ua WHERE ua.createdAt BETWEEN :start AND :end")
    long countDistinctActiveUsers(Instant start, Instant end);
}
