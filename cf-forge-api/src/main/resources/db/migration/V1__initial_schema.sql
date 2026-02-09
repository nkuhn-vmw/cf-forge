CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uaa_user_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    avatar_url VARCHAR(512),
    preferences JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cf_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    api_endpoint VARCHAR(512) NOT NULL,
    org_guid VARCHAR(255),
    org_name VARCHAR(255),
    space_guid VARCHAR(255),
    space_name VARCHAR(255),
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(50) NOT NULL,
    framework VARCHAR(100),
    buildpack VARCHAR(100),
    cf_manifest JSONB,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    cf_target_id UUID REFERENCES cf_targets(id),
    cf_app_guid VARCHAR(255),
    workspace_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(owner_id, slug)
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) NOT NULL,
    user_id UUID REFERENCES users(id) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE builds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) NOT NULL,
    triggered_by UUID REFERENCES users(id),
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'QUEUED',
    build_log TEXT,
    artifact_path VARCHAR(512),
    sbom_path VARCHAR(512),
    cve_report JSONB,
    duration_ms INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) NOT NULL,
    build_id UUID REFERENCES builds(id),
    triggered_by UUID REFERENCES users(id),
    strategy VARCHAR(20) DEFAULT 'ROLLING',
    manifest_used JSONB,
    cf_app_guid VARCHAR(255),
    environment VARCHAR(20) DEFAULT 'STAGING',
    status VARCHAR(20) DEFAULT 'PENDING',
    deployment_url VARCHAR(512),
    error_message TEXT,
    duration_ms INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(100),
    language VARCHAR(50) NOT NULL,
    framework VARCHAR(100),
    buildpack VARCHAR(100),
    manifest_template JSONB,
    source_url VARCHAR(512),
    download_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_projects_slug ON projects(owner_id, slug);
CREATE INDEX idx_builds_project ON builds(project_id);
CREATE INDEX idx_builds_created ON builds(created_at);
CREATE INDEX idx_deployments_project ON deployments(project_id);
CREATE INDEX idx_deployments_created ON deployments(created_at);
CREATE INDEX idx_conversations_project ON conversations(project_id);
CREATE INDEX idx_cf_targets_user ON cf_targets(user_id);
