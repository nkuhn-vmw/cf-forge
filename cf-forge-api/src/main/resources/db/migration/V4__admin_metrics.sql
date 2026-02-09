CREATE TABLE metric_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name VARCHAR(255) NOT NULL,
    granularity VARCHAR(20) NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    count BIGINT DEFAULT 0,
    sum_duration_ms DOUBLE PRECISION DEFAULT 0,
    avg_duration_ms DOUBLE PRECISION DEFAULT 0,
    p95_duration_ms DOUBLE PRECISION DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    dimensions JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    activity_type VARCHAR(100) NOT NULL,
    project_id VARCHAR(255),
    detail TEXT,
    duration_ms INTEGER,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE component_health_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cpu_percent DOUBLE PRECISION,
    memory_used_mb BIGINT,
    memory_total_mb BIGINT,
    instances_running INTEGER,
    instances_desired INTEGER,
    response_time_ms DOUBLE PRECISION,
    details JSONB,
    recorded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_metric_snapshots_name ON metric_snapshots(metric_name, granularity, period_start);
CREATE INDEX idx_user_activity_user ON user_activity(user_id);
CREATE INDEX idx_user_activity_type ON user_activity(activity_type, created_at);
CREATE INDEX idx_component_health ON component_health_history(component_name, recorded_at);
