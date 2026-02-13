# CF Forge

**AI-powered application development platform for Cloud Foundry.** Describe an app in natural language, and CF Forge generates the code, builds it, and deploys it to your CF foundation — all through a conversational chat interface.

```
 "Build me a Python Flask API with PostgreSQL"
                    │
                    ▼
    ┌──────────────────────────────┐
    │       cf-llama-chat          │
    │    (Chat UI + Skills)        │
    └──────────────┬───────────────┘
                   │ MCP Protocol (SSE)
                   ▼
    ┌──────────────────────────────┐
    │       cf-forge-agent         │  ← 24 tools, 4 skills
    │    (MCP Server + Spring AI)  │
    └───┬──────────┬───────────┬───┘
        │          │           │
        ▼          ▼           ▼
   workspace    builder    CF API
   (files)    (CI pipeline) (platform)
```

**Stack:** Java 21 · Spring Boot 3.4 · Spring AI 1.1 · MCP Protocol · GenAI on Tanzu Platform

**Deployed at:** `apps.tas-ndc.kuhn-labs.com`

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [How It Works](#how-it-works)
- [Quick Start](#quick-start)
- [MCP Tools Reference](#mcp-tools-reference)
- [Skills](#skills)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        USER LAYER                                       │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    cf-llama-chat                                 │    │
│  │          Production Chat UI + Admin Portal                      │    │
│  │                                                                 │    │
│  │  ┌─────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐  │    │
│  │  │  Chat   │  │  Skills  │  │  MCP Client │  │    Admin     │  │    │
│  │  │  UI     │  │  System  │  │  (SSE)      │  │    Portal    │  │    │
│  │  └─────────┘  └──────────┘  └──────┬─────┘  └──────────────┘  │    │
│  └────────────────────────────────────┼──────────────────────────┘    │
│                                        │                               │
└────────────────────────────────────────┼───────────────────────────────┘
                                         │ MCP Protocol (SSE/HTTP)
┌────────────────────────────────────────┼───────────────────────────────┐
│                       CF FORGE SERVICES                                │
│                                        │                               │
│  ┌─────────────────────────────────────▼──────────────────────────┐    │
│  │                    cf-forge-agent (2G)                          │    │
│  │            MCP Server + Spring AI + RAG                        │    │
│  │                                                                │    │
│  │  ┌──────────────┐  ┌───────────────┐  ┌───────────────────┐  │    │
│  │  │  24 MCP      │  │  4 Skills     │  │  AI Advisors      │  │    │
│  │  │  Tools       │  │  (bundled     │  │  (safety, RAG,    │  │    │
│  │  │  (auto-      │  │   prompts)    │  │   memory)         │  │    │
│  │  │   discovered)│  │               │  │                   │  │    │
│  │  └──────┬───────┘  └───────────────┘  └───────────────────┘  │    │
│  │         │                                                      │    │
│  │         │  Proxies tool calls to:                              │    │
│  └─────────┼──────────────────────────────────────────────────────┘    │
│            │                                                           │
│    ┌───────┼──────────────┬────────────────────┐                      │
│    │       │              │                    │                      │
│    ▼       ▼              ▼                    ▼                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ cf-forge-    │  │ cf-forge-    │  │ cf-forge-api │  │ cf-forge-│ │
│  │ workspace    │  │ builder      │  │              │  │ admin    │ │
│  │ (1G)         │  │ (1G)         │  │ (1G)         │  │ (1G)     │ │
│  │              │  │              │  │              │  │          │ │
│  │ File storage │  │ CI pipeline  │  │ OAuth2 BFF   │  │ Health   │ │
│  │ per-project  │  │ build/test/  │  │ REST gateway │  │ monitor  │ │
│  │ CRUD         │  │ package      │  │ cookie auth  │  │ metrics  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────┬─────┘ │
│         │                 │                 │               │        │
│  ┌──────┴─────────────────┴─────────────────┴───────────────┘        │
│  │                                                                    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────┐  │
│  │  │ PostgreSQL   │  │  RabbitMQ    │  │  Redis   │  │  GenAI   │  │
│  │  │ (pgvector)   │  │              │  │          │  │  on      │  │
│  │  │ cf-forge-db  │  │ cf-forge-mq  │  │ cf-forge │  │  Tanzu   │  │
│  │  │              │  │              │  │ -cache   │  │          │  │
│  │  └──────────────┘  └──────────────┘  └──────────┘  └──────────┘  │
│  │                    BACKING SERVICES                                │
│  └────────────────────────────────────────────────────────────────────┘
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
                         Tanzu Application Service
```

### Data Flow: "Build me a Flask API"

```
User ──► cf-llama-chat ──► cf-forge-agent (MCP)
                                │
                                ├─► createProject     → PostgreSQL (project record)
                                ├─► writeFile (x3)    → cf-forge-workspace (app.py, requirements.txt, manifest.yml)
                                ├─► triggerBuild      → RabbitMQ → cf-forge-builder (compile + package)
                                └─► triggerDeploy     → CF API (cf push to staging)
                                │
User ◄── formatted response ◄──┘
```

---

## Services

| Service | Route | Mem | Purpose |
|---------|-------|-----|---------|
| **cf-forge-agent** | `cf-forge-mcp.apps.tas-ndc.kuhn-labs.com` | 2G | AI engine — MCP server, 24 tools, Spring AI chat, RAG |
| | `cf-forge-agent.apps.internal` | | Internal route for inter-service calls |
| **cf-forge-workspace** | `cf-forge-workspace.apps.tas-ndc.kuhn-labs.com` | 1G | File storage — per-project workspace CRUD |
| | `cf-forge-workspace.apps.internal` | | Internal route |
| **cf-forge-builder** | `cf-forge-builder.apps.tas-ndc.kuhn-labs.com` | 1G | CI pipeline — compile, test, scan, package (via RabbitMQ) |
| | `cf-forge-builder.apps.internal` | | Internal route |
| **cf-forge-api** | `cf-forge-api.apps.tas-ndc.kuhn-labs.com` | 1G | REST gateway — OAuth2 BFF, project CRUD, proxies |
| **cf-forge-admin** | `cf-forge-admin.apps.tas-ndc.kuhn-labs.com` | 1G | Admin dashboard — health monitoring, metrics, audit |
| **cf-forge-ui** | `cf-forge-ui.apps.tas-ndc.kuhn-labs.com` | 64M | React SPA (optional — superseded by cf-llama-chat) |

### Backing Services

| Instance | Type | Bound To |
|----------|------|----------|
| `cf-forge-db` | PostgreSQL (pgvector) | api, agent, builder, workspace, admin |
| `cf-forge-mq` | RabbitMQ | api, agent, builder, admin |
| `cf-forge-cache` | Redis | api, workspace, admin |
| `cf-forge-genai` | GenAI on Tanzu Platform | agent |
| `cf-forge-sso` | p-identity (OAuth2/OIDC) | api, admin |

---

## How It Works

CF Forge uses the **Model Context Protocol (MCP)** to expose its capabilities as tools that any LLM can call. The primary UI is **cf-llama-chat**, an existing production chat application — zero code changes required.

### Integration Architecture

```
┌────────────────────────────────────────────────────────┐
│                   cf-llama-chat                         │
│                                                        │
│  1. Admin registers cf-forge MCP server URL            │
│  2. Tools are auto-discovered (24 tools)               │
│  3. Admin creates Skills (curated tool bundles)         │
│  4. User selects a Skill in chat sidebar               │
│  5. LLM calls cf-forge tools during conversation       │
│                                                        │
│  No code changes to cf-llama-chat needed!              │
└───────────────────────┬────────────────────────────────┘
                        │ SSE stream + JSON-RPC
                        ▼
┌───────────────────────────────────────────────────────┐
│                   cf-forge-agent                       │
│                                                       │
│  Spring AI auto-registers @Tool methods via MCP:      │
│                                                       │
│  ProjectTools ──► createProject, listProjects, ...    │
│  WorkspaceTools ─► readFile, writeFile, listFiles     │
│  BuildDeployTools ► triggerBuild, triggerDeploy        │
│  DeploymentTools ─► listDeployments, rollback         │
│  CfPlatformTools ─► marketplace, buildpacks, logs     │
│  ServiceTools ───► recommend, provision guide         │
│  DocumentationTools ► search docs, buildpack docs     │
│                                                       │
│  MethodToolCallbackProvider auto-discovers all tools  │
│  MCP SSE endpoint at /sse (Spring AI auto-config)     │
└───────────────────────────────────────────────────────┘
```

### Setup Steps

1. **Deploy cf-forge services** to TAS (see [Quick Start](#quick-start))
2. **Register MCP server** in cf-llama-chat admin:
   - Name: `cf-forge`
   - URL: `https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse`
   - Transport: SSE
3. **Connect** — 24 tools are auto-discovered
4. **Import Skills** from `GET /api/v1/setup/skills`
5. **Chat** — select a skill and start building apps

---

## Quick Start

### Prerequisites

- Java 21
- Maven (system `mvn`, no wrapper)
- CF CLI v8+
- Node.js 18+ (for UI only)

### Build

```bash
# Build all modules
mvn package -DskipTests

# Build a single service
mvn package -DskipTests -pl cf-forge-agent -am
```

### Deploy to TAS

```bash
# Create backing services (one-time)
cf create-service postgres on-demand-postgres-db cf-forge-db
cf create-service p.rabbitmq on-demand-plan cf-forge-mq
cf create-service p-redis shared-vm cf-forge-cache
cf create-service genai tanzu-all-models cf-forge-genai
cf create-service p-identity uaa cf-forge-sso

# Wait for services to provision
watch cf services

# Build and push all services
mvn package -DskipTests

cf push cf-forge-agent     -f cf-forge-agent/manifest.yml
cf push cf-forge-workspace -f cf-forge-workspace/manifest.yml
cf push cf-forge-builder   -f cf-forge-builder/manifest.yml
cf push cf-forge-api       -f cf-forge-api/manifest.yml
cf push cf-forge-admin     -f cf-forge-admin/manifest.yml
```

### Verify

```bash
# Health checks
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/actuator/health
curl https://cf-forge-workspace.apps.tas-ndc.kuhn-labs.com/actuator/health
curl https://cf-forge-builder.apps.tas-ndc.kuhn-labs.com/actuator/health

# MCP endpoint (should stream SSE events)
curl -N https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse

# Setup info
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/mcp-config | jq .
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/skills | jq .
```

### Local Development

```bash
docker compose up -d   # PostgreSQL, Redis, RabbitMQ, MinIO, Ollama
java -jar cf-forge-agent/target/cf-forge-agent-0.1.0-SNAPSHOT.jar
```

---

## MCP Tools Reference

### Project Management (5 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `createProject` | name, language, framework, description | Create a new project with workspace for file storage |
| `listProjects` | — | List all projects with ID, language, status |
| `getProject` | projectId | Get full project details |
| `updateProject` | projectId, name?, description?, framework? | Update project metadata |
| `deleteProject` | projectId | Soft-delete a project |

**Supported languages:** JAVA, PYTHON, NODEJS, GO, DOTNET, RUBY, STATICFILE

### File Operations (4 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `writeFile` | workspaceId, path, content | Create or overwrite a file |
| `readFile` | workspaceId, path | Read file content |
| `listFiles` | workspaceId, dir? | List files in workspace |
| `deleteFile` | workspaceId, path | Delete a file |

### Build & Deploy (5 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `triggerBuild` | projectId | Queue build (compile, test, scan, package) |
| `triggerDeploy` | projectId, environment | Deploy to staging or production |
| `listDeployments` | projectId | Deployment history (most recent first) |
| `getDeploymentStatus` | deploymentId | Deployment details + error messages |
| `rollbackDeployment` | projectId, environment | Rollback to last successful deployment |

### CF Platform (5 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `getMarketplaceServices` | — | List CF marketplace services with plans |
| `getAvailableBuildpacks` | — | List enabled buildpacks |
| `getOrgQuota` | orgGuid | Memory, routes, service instance limits |
| `getRecentLogs` | appGuid, lines | Recent app logs |
| `getAppEnvironment` | appGuid | VCAP_SERVICES and env vars |

### Service Advisory (3 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `recommendServices` | language, framework, requirements | AI-powered service recommendations |
| `listBoundServices` | appGuid | Services bound to a CF app |
| `getServiceProvisioningGuide` | serviceType, language | Step-by-step provisioning guide |

### Documentation / RAG (2 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `searchDocumentation` | query | Vector similarity search across CF docs |
| `getBuildpackDocs` | language | Language-specific buildpack configuration |

---

## Skills

Skills bundle tools with specialized system prompts so the LLM orchestrates multi-step workflows automatically.

### CF App Builder

> *"Build me a Java Spring Boot REST API with PostgreSQL"*

The LLM will: create project → generate source files → write manifest.yml → trigger build → deploy to staging.

**Tools:** createProject, writeFile, listFiles, readFile, triggerBuild, triggerDeploy, getMarketplaceServices, recommendServices, getAvailableBuildpacks, getBuildpackDocs

### CF Migration Analyst

> *"Analyze my Node.js Express app for CF migration"*

The LLM will: identify buildpack → map dependencies to CF services → check 12-factor compliance → produce migration plan.

**Tools:** getMarketplaceServices, getAvailableBuildpacks, searchDocumentation, getBuildpackDocs, recommendServices, getServiceProvisioningGuide

### CF Deployment Manager

> *"Show me my deployments and rollback the last failed one"*

The LLM will: list projects → check deployment status → view logs → trigger rollback.

**Tools:** listProjects, getProject, listDeployments, getDeploymentStatus, triggerDeploy, rollbackDeployment, getRecentLogs, getAppEnvironment

### CF Platform Explorer

> *"What services are available in the marketplace?"*

The LLM will: query CF API → list services/buildpacks/quotas → link to documentation.

**Tools:** getMarketplaceServices, getAvailableBuildpacks, getOrgQuota, searchDocumentation, getBuildpackDocs, recommendServices, getServiceProvisioningGuide

---

## API Reference

### cf-forge-agent (MCP + Setup)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sse` | MCP SSE stream (auto-configured by Spring AI) |
| GET | `/api/v1/setup/mcp-config` | MCP server configuration for registration |
| GET | `/api/v1/setup/skills` | Skill definitions for import |
| POST | `/api/v1/agent/generate` | Stream AI response (SSE) |
| POST | `/api/v1/agent/structured` | Structured AI response |

### cf-forge-workspace (File Storage)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/workspace/{id}/files` | List files (optional `?dir=`) |
| GET | `/workspace/{id}/files/{path}` | Read file content |
| PUT | `/workspace/{id}/files/{path}` | Write file (`{"content": "..."}`) |
| DELETE | `/workspace/{id}/files/{path}` | Delete file |

### cf-forge-api (REST Gateway)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/login` | Initiate OAuth2 login |
| GET | `/api/v1/auth/me` | Get current user |
| GET | `/api/v1/projects` | List user's projects |
| POST | `/api/v1/projects` | Create project |
| GET | `/api/v1/projects/{id}` | Get project details |
| POST | `/api/v1/projects/{id}/builds` | Trigger build |
| POST | `/api/v1/projects/{id}/deployments` | Trigger deployment |
| GET | `/api/v1/marketplace/services` | List marketplace services |

### cf-forge-admin (Dashboard)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin` | Admin dashboard (HTML) |
| GET | `/api/v1/admin/overview` | Platform overview metrics |
| GET | `/api/v1/admin/health` | All services health status |
| GET | `/api/v1/admin/audit` | Audit logs |

---

## Project Structure

```
cf-forge/
├── cf-forge-common/              # Shared module
│   └── src/main/java/com/cfforge/common/
│       ├── entity/               # JPA entities (Project, Build, Deployment, User)
│       ├── dto/                  # BuildRequest, DeployRequest, FileEntry
│       ├── enums/                # Language, ProjectStatus, BuildStatus, DeployStatus
│       └── repository/           # Spring Data JPA repositories
│
├── cf-forge-agent/               # AI engine + MCP server
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/agent/
│       ├── config/
│       │   ├── McpServerConfig.java      # Registers all tools for MCP
│       │   ├── WebClientConfig.java      # Workspace + CF API clients
│       │   └── GenAiCloudConfig.java     # VCAP_SERVICES → Spring AI
│       ├── tools/
│       │   ├── ProjectTools.java         # 5 project CRUD tools
│       │   ├── WorkspaceTools.java       # 4 file operation tools
│       │   ├── BuildDeployTools.java     # 2 build/deploy tools
│       │   ├── DeploymentTools.java      # 3 deployment management tools
│       │   ├── CfPlatformTools.java      # 5 CF platform query tools
│       │   ├── ServiceTools.java         # 3 service advisory tools
│       │   └── DocumentationTools.java   # 2 RAG documentation tools
│       ├── controller/
│       │   └── SetupController.java      # MCP config + skill endpoints
│       ├── advisor/                      # Spring AI advisors
│       └── service/                      # Agent, migration, evaluation
│
├── cf-forge-workspace/           # File storage service
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/workspace/
│       ├── controller/WorkspaceController.java
│       └── service/FileStorageService.java
│
├── cf-forge-builder/             # Build pipeline service
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/builder/
│       └── consumer/             # RabbitMQ build request consumer
│
├── cf-forge-api/                 # REST gateway + OAuth2 BFF
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/api/
│       ├── controller/           # REST endpoints
│       ├── service/              # Business logic
│       └── config/               # Security, CORS
│
├── cf-forge-admin/               # Admin dashboard
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/admin/
│       ├── controller/           # Dashboard + API
│       └── scheduler/            # HealthCheckScheduler
│
├── cf-forge-ui/                  # React 19 SPA (optional)
│   ├── manifest.yml
│   ├── src/
│   └── package.json
│
├── docker-compose.yml            # Local dev: Postgres, Redis, RabbitMQ, Ollama
├── pom.xml                       # Parent POM (Maven multi-module)
└── scripts/
    ├── deploy.sh                 # Orchestrated deploy script
    └── deploy-config.sh.example
```

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Set to `cloud` for TAS |
| `CFFORGE_CF_API_URL` | `https://api.sys.local` | CF API endpoint |
| `CFFORGE_WORKSPACE_URL` | `http://cf-forge-workspace.apps.internal:8080` | Workspace service URL |
| `GENAI_BASE_URL` | `http://localhost:11434/v1` | GenAI/Ollama API |
| `GENAI_MODEL` | `llama3` | Chat model name |
| `GENAI_EMBEDDING_MODEL` | `nomic-embed-text` | Embedding model |

### Cloud Profile (`SPRING_PROFILES_ACTIVE=cloud`)

Services auto-configure from `VCAP_SERVICES`:
- **PostgreSQL** — DataSource via java-cfenv
- **RabbitMQ** — Connection factory auto-configured
- **GenAI** — Parsed from VCAP, creates OpenAI-compatible client
- **SSO** — OAuth2 client from p-identity binding

### MCP Server Configuration (`application.yml`)

```yaml
spring:
  ai:
    mcp:
      server:
        name: cf-forge
        version: 0.1.0
        type: SYNC
        sse-message-endpoint: /mcp/messages
```

---

## Data Model

```
User ──────┬── Project ──┬── Build ──── Deployment
           │             │
           │             ├── Conversation
           │             │
           ├── CfTarget  └── (workspace files via workspace service)
           │
           └── UserActivity

Template (community marketplace)
AuditLog (all operations)
MetricSnapshot (time-series)
ComponentHealthHistory (service monitoring)
```

### Key Enums

| Enum | Values |
|------|--------|
| `Language` | JAVA, PYTHON, NODEJS, GO, DOTNET, RUBY, STATICFILE |
| `ProjectStatus` | ACTIVE, ARCHIVED, DELETED |
| `BuildStatus` | QUEUED, BUILDING, SUCCESS, FAILED, BLOCKED |
| `DeployStatus` | PENDING, IN_PROGRESS, DEPLOYED, FAILED, ROLLED_BACK |
| `DeployStrategy` | ROLLING, BLUE_GREEN, CANARY |

---

## Troubleshooting

### MCP connection fails from cf-llama-chat

```bash
# Verify agent is running
cf app cf-forge-agent

# Test SSE endpoint
curl -N https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse

# Check setup endpoint
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/mcp-config | jq .
```

### "Session not found" errors

The MCP SSE session expired (e.g., after agent restart). In cf-llama-chat admin:
1. Disconnect the MCP server
2. Reconnect — a new SSE session is established

### Tool calls return errors

```bash
# Check agent logs
cf logs cf-forge-agent --recent | grep ERROR

# Verify workspace is reachable from agent
cf ssh cf-forge-agent -c "curl -s https://cf-forge-workspace.apps.tas-ndc.kuhn-labs.com/actuator/health"

# Verify builder is running
cf app cf-forge-builder
```

### Container-to-container networking

Internal routes (`.apps.internal`) require network policies. If your CF user lacks `network.admin` scope, use external routes instead:

```bash
# Set workspace URL to external route
cf set-env cf-forge-agent CFFORGE_WORKSPACE_URL https://cf-forge-workspace.apps.tas-ndc.kuhn-labs.com
cf restart cf-forge-agent
```

---

## License

Proprietary. Internal use only.
