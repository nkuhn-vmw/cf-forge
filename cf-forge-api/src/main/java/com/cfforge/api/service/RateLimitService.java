package com.cfforge.api.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isAllowed(String userId) {
        String key = "rate_limit:" + userId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, WINDOW);
        }
        return count != null && count <= MAX_REQUESTS_PER_MINUTE;
    }

    public long getRemainingRequests(String userId) {
        String key = "rate_limit:" + userId;
        String val = redis.opsForValue().get(key);
        if (val == null) return MAX_REQUESTS_PER_MINUTE;
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - Long.parseLong(val));
    }
}
