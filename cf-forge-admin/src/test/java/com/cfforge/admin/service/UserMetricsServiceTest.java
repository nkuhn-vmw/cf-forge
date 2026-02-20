package com.cfforge.admin.service;

import com.cfforge.common.repository.UserActivityRepository;
import com.cfforge.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMetricsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserActivityRepository userActivityRepository;

    private UserMetricsService service;

    @BeforeEach
    void setUp() {
        service = new UserMetricsService(userRepository, userActivityRepository);
    }

    @Test
    void getTotalUsers_returnsTotalCount() {
        when(userRepository.count()).thenReturn(42L);

        assertThat(service.getTotalUsers()).isEqualTo(42L);
    }

    @Test
    void getActiveUsers_returnsDistinctActiveCount() {
        when(userActivityRepository.countDistinctActiveUsers(any(Instant.class), any(Instant.class)))
            .thenReturn(15L);

        long result = service.getActiveUsers(LocalDateTime.now().minusDays(7));

        assertThat(result).isEqualTo(15L);
    }

    @Test
    void getTopActiveUsers_returnsTopUsers() {
        UUID userId = UUID.randomUUID();
        Object[] userRow = {userId, 25L};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(userRow);
        when(userActivityRepository.findTopActiveUsers(any(Instant.class), any(Instant.class), any()))
            .thenReturn(rows);

        List<Object[]> result = service.getTopActiveUsers(LocalDateTime.now().minusDays(7), 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)[0]).isEqualTo(userId);
        assertThat(result.get(0)[1]).isEqualTo(25L);
    }

    @Test
    void getUserMetrics_returnsAggregatedMetrics() {
        when(userRepository.count()).thenReturn(100L);
        when(userActivityRepository.countDistinctActiveUsers(any(Instant.class), any(Instant.class)))
            .thenReturn(30L);
        when(userActivityRepository.findTopActiveUsers(any(Instant.class), any(Instant.class), any()))
            .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.getUserMetrics(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(result.get("totalUsers")).isEqualTo(100L);
        assertThat(result.get("activeUsers")).isEqualTo(30L);
        assertThat(result).containsKey("topUsers");
    }
}
