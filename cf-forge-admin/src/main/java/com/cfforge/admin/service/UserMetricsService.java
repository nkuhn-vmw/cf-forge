package com.cfforge.admin.service;

import com.cfforge.common.repository.UserActivityRepository;
import com.cfforge.common.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserMetricsService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;

    public UserMetricsService(UserRepository userRepository, UserActivityRepository userActivityRepository) {
        this.userRepository = userRepository;
        this.userActivityRepository = userActivityRepository;
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getActiveUsers(LocalDateTime since) {
        return userActivityRepository.countDistinctActiveUsers(since);
    }

    public List<Object[]> getTopActiveUsers(LocalDateTime since, int limit) {
        return userActivityRepository.findTopActiveUsers(since,
            org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public Map<String, Object> getUserMetrics(LocalDateTime from, LocalDateTime to) {
        return Map.of(
            "totalUsers", getTotalUsers(),
            "activeUsers", getActiveUsers(from),
            "topUsers", getTopActiveUsers(from, 10)
        );
    }
}
