# CF Forge

AI-powered application development platform for Cloud Foundry. Describe an app in natural language, and CF Forge generates the code, builds it, and deploys it to your CF foundation — all through a conversational chat interface.

**Stack:** Java 21, Spring Boot 3.4, Spring AI 1.1, React 19, GenAI on Tanzu Platform

**Deployed at:** `apps.tas-ndc.kuhn-labs.com`

## Architecture

```
                         ┌──────────────────────────────┐
                         │     cf-llama-chat (Chat UI)   │
                         │   MCP client + Skills system  │
                         └──────────────┬───────────────┘
                                        │ MCP (SSE)
                         ┌──────────────▼───────────────┐
┌──────────────┐         │      cf-forge-agent           │         ┌──────────────┐
│ cf-forge-ui  │         │  Spring AI + MCP Server       │────────▶│ cf-forge-    │
│ React SPA    │         │  24 tools, 4 skills           │         │ workspace    │
│ (optional)   │         │  AI advisors + RAG            │         │ file storage │
└──────┬───────┘         └──┬────────────────┬──────────┘         └──────────────┘
       │                    │                │
       │              ┌─────▼─────┐   ┌──────▼───────┐
       │              │  RabbitMQ  │   │  PostgreSQL   │
       │              │  (cf-     │   │  (cf-forge-   │
       │              │  forge-mq)│   │   db + pgvec) │
       │              └─────┬─────┘   └──────────────┘
       │                    │
┌──────▼───────┐     ┌─────▼──────┐
│ cf-forge-api │     │ cf-forge-  │
│ OAuth2 BFF   │     │ builder    │
│ REST gateway │     │ CI pipeline│
└──────────────┘     └────────────┘
```

## Services

| Service | Route | Memory | Purpose |
|---------|-------|--------|---------|
| **cf-forge-agent** | `cf-forge-agent.apps.internal` | 2G | AI engine — Spring AI chat, MCP server, tools, RAG |
| | `cf-forge-mcp.apps.tas-ndc.kuhn-labs.com` | | External MCP endpoint for cf-llama-chat |
| **cf-forge-api** | `cf-forge-api.apps.tas-ndc.kuhn-labs.com` | 1G | REST gateway — OAuth2 BFF, project CRUD, proxies to internal services |
| **cf-forge-workspace** | `cf-forge-workspace.apps.internal` | 1G | File storage — per-project workspace with read/write/list/delete |
| **cf-forge-builder** | `cf-forge-builder.apps.internal` | 1G | Build pipeline — compile, test, scan, package (via RabbitMQ) |
| **cf-forge-admin** | `cf-forge-admin.apps.tas-ndc.kuhn-labs.com` | 1G | Admin dashboard — metrics, health monitoring, audit logs |
| **cf-forge-ui** | `cf-forge-ui.apps.tas-ndc.kuhn-labs.com` | 64M | React SPA (optional — superseded by cf-llama-chat integration) |

### Backing Services

| Instance Name | Type | Used By |
|---------------|------|---------|
| `cf-forge-db` | PostgreSQL (pgvector) | api, agent, builder, workspace, admin |
| `cf-forge-mq` | RabbitMQ | api, agent, builder, admin |
| `cf-forge-cache` | Redis | api, workspace, admin |
| `cf-forge-genai` | GenAI on Tanzu Platform | agent |
| `cf-forge-sso` | p-identity (OAuth2/OIDC) | api, admin |

## Quick Start

### Prerequisites

- Java 21
- Maven (no wrapper — use system `mvn`)
- Node.js 18+ (for UI only)
- CF CLI v8+
- Docker & Docker Compose (for local dev)

### Local Development

Start backing services:

```bash
docker compose up -d
```

This starts PostgreSQL (pgvector), Redis, RabbitMQ, MinIO (S3), and Ollama.

Build and run a service:

```bash
# Build all modules
mvn package -DskipTests

# Build a single service
mvn package -DskipTests -pl cf-forge-agent -am

# Run locally
java -jar cf-forge-agent/target/cf-forge-agent-0.1.0-SNAPSHOT.jar
```

Build the UI:

```bash
cd cf-forge-ui
npm ci
npm run build   # produces dist/
npm run dev     # local dev server with HMR
```

### Deploy to TAS

#### Option A: Full automated deploy

```bash
cp scripts/deploy-config.sh.example scripts/deploy-config.sh
# Edit deploy-config.sh with your CF target, service plans, and credentials

./scripts/deploy.sh              # Full deploy (services + apps + migrations + smoke tests)
./scripts/deploy.sh --apps-only  # Redeploy apps only
./scripts/deploy.sh --component agent  # Deploy single service
./scripts/deploy.sh --teardown   # Delete everything
```

#### Option B: Manual deploy

```bash
# Build
mvn package -DskipTests

# Push each service using its manifest
cf push cf-forge-api       -f cf-forge-api/manifest.yml
cf push cf-forge-agent     -f cf-forge-agent/manifest.yml
cf push cf-forge-workspace -f cf-forge-workspace/manifest.yml
cf push cf-forge-builder   -f cf-forge-builder/manifest.yml
cf push cf-forge-admin     -f cf-forge-admin/manifest.yml

# UI
cd cf-forge-ui && npm ci && npm run build
cf push cf-forge-ui -f manifest.yml
```

## MCP Server Integration (cf-llama-chat)

CF Forge exposes all its capabilities via the **Model Context Protocol (MCP)**, allowing any MCP-compatible chat UI (like cf-llama-chat) to use CF Forge tools. No code changes to the chat UI are required.

### How It Works

1. cf-forge-agent runs an MCP server at `/sse` (auto-configured by Spring AI)
2. A chat UI (like cf-llama-chat) registers cf-forge as an MCP server
3. The LLM discovers and calls cf-forge tools during conversations
4. Users select a **Skill** (curated bundle of tools + system prompt) for their workflow

### MCP Endpoint

```
SSE:  https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse
```

### Setup Endpoints

Get the full MCP configuration (for registering in a chat UI admin):

```bash
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/mcp-config
```

Get skill definitions:

```bash
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/skills
```

### Available Tools (24)

**Project Management**
| Tool | Description |
|------|-------------|
| `createProject` | Create a new project with workspace, language, framework |
| `listProjects` | List all projects with status |
| `getProject` | Get project details by ID |
| `updateProject` | Update project name, description, framework |
| `deleteProject` | Soft-delete a project |

**File Operations**
| Tool | Description |
|------|-------------|
| `readFile` | Read file content from workspace |
| `writeFile` | Create or overwrite a file in workspace |
| `listFiles` | List all files in a workspace directory |
| `deleteFile` | Delete a file from workspace |

**Build & Deploy**
| Tool | Description |
|------|-------------|
| `triggerBuild` | Queue a build (compile, test, scan, package) |
| `triggerDeploy` | Deploy to staging or production |
| `listDeployments` | List deployment history for a project |
| `getDeploymentStatus` | Get deployment details and error messages |
| `rollbackDeployment` | Rollback a failed deployment |

**CF Platform**
| Tool | Description |
|------|-------------|
| `getMarketplaceServices` | List available marketplace services |
| `getAvailableBuildpacks` | List enabled buildpacks |
| `getOrgQuota` | Get org memory, routes, service instance limits |
| `getRecentLogs` | Get recent app logs |
| `getAppEnvironment` | Get VCAP_SERVICES and environment variables |

**Services**
| Tool | Description |
|------|-------------|
| `recommendServices` | AI-powered service recommendations for your stack |
| `listBoundServices` | List services bound to a CF app |
| `getServiceProvisioningGuide` | Step-by-step guide to provision and bind a service |

**Documentation (RAG)**
| Tool | Description |
|------|-------------|
| `searchDocumentation` | Vector similarity search across CF docs |
| `getBuildpackDocs` | Language-specific buildpack configuration docs |

### Skills

Skills bundle tools with specialized system prompts so the LLM knows how to orchestrate multi-step workflows.

**CF App Builder** — End-to-end app creation from a natural language description.
> "Build me a Java Spring Boot REST API with PostgreSQL"
>
> The LLM will: create a project, generate source files, write a manifest.yml, trigger a build, and deploy to staging.

**CF Migration Analyst** — Assess and plan migrations to Cloud Foundry.
> "Analyze my Node.js Express app for CF migration"
>
> The LLM will: identify the buildpack, map dependencies to CF services, check 12-factor compliance, and produce a migration plan.

**CF Deployment Manager** — Monitor and manage running deployments.
> "Show me my deployments and rollback the last failed one"
>
> The LLM will: list projects, check deployment status, view logs, and trigger a rollback.

**CF Platform Explorer** — Discover and understand your CF environment.
> "What services are available in the marketplace?"
>
> The LLM will: query the CF API for marketplace services, buildpacks, quotas, and link to relevant documentation.

### Connecting cf-llama-chat

1. In cf-llama-chat admin, go to **MCP Servers** and add:
   - **Name:** `cf-forge`
   - **URL:** `https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse`
   - **Transport:** SSE
2. Click **Connect** — tools are auto-discovered
3. Create Skills from the definitions at `/api/v1/setup/skills`
4. In chat, select a skill (e.g., "CF App Builder") and start building

For detailed instructions, see [`cf-forge-agent/docs/cf-llama-chat-integration.md`](cf-forge-agent/docs/cf-llama-chat-integration.md).

## API Reference

### cf-forge-api (REST Gateway)

Base URL: `https://cf-forge-api.apps.tas-ndc.kuhn-labs.com`

**Authentication** — OAuth2 BFF pattern with cookie-based JWT tokens.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/login` | Initiate OAuth2 login |
| GET | `/api/v1/auth/callback` | OAuth2 callback |
| GET | `/api/v1/auth/me` | Get current user |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout |

**Projects**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/projects` | List user's projects |
| POST | `/api/v1/projects` | Create project |
| GET | `/api/v1/projects/{id}` | Get project details |
| PUT | `/api/v1/projects/{id}` | Update project |
| DELETE | `/api/v1/projects/{id}` | Delete project |
| GET | `/api/v1/projects/{id}/health` | Get app health |
| GET | `/api/v1/projects/{id}/vcap` | Get bound services |

**Files**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/projects/{id}/files` | List files |
| GET | `/api/v1/projects/{id}/files/**` | Read file |
| PUT | `/api/v1/projects/{id}/files/**` | Write file |
| DELETE | `/api/v1/projects/{id}/files/**` | Delete file |

**Builds & Deployments**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/projects/{id}/builds` | Trigger build |
| GET | `/api/v1/projects/{id}/builds` | List builds |
| GET | `/api/v1/projects/{id}/builds/{buildId}` | Get build details |
| GET | `/api/v1/projects/{id}/builds/{buildId}/sbom` | Download SBOM |
| GET | `/api/v1/projects/{id}/builds/{buildId}/cve-report` | Get CVE report |
| POST | `/api/v1/projects/{id}/deployments` | Trigger deployment |
| GET | `/api/v1/projects/{id}/deployments` | List deployments |
| GET | `/api/v1/projects/{id}/deployments/{deployId}` | Get deployment details |
| POST | `/api/v1/projects/{id}/deployments/{deployId}/rollback` | Rollback |

**Templates**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/templates` | List templates |
| GET | `/api/v1/templates/{slug}` | Get template |
| POST | `/api/v1/templates/{slug}/use` | Create project from template |
| GET | `/api/v1/templates/community` | Browse community templates |
| GET | `/api/v1/templates/featured` | Get featured templates |

**Marketplace**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/marketplace/services` | List services |
| GET | `/api/v1/marketplace/buildpacks` | List buildpacks |
| GET | `/api/v1/marketplace/recommend` | Get recommendations |

**AI Agent**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/agent/generate` | Stream AI response (SSE) |
| POST | `/api/v1/agent/structured` | Structured AI response |
| GET | `/api/v1/agents` | List available agents |
| POST | `/api/v1/agents/workflow` | Execute multi-agent workflow |

### cf-forge-agent (MCP + AI)

Base URL: `https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sse` | MCP SSE stream (auto-configured) |
| GET | `/api/v1/setup/mcp-config` | MCP server configuration |
| GET | `/api/v1/setup/skills` | Skill definitions |
| POST | `/api/v1/agent/generate` | Stream AI response (SSE) |
| POST | `/api/v1/agent/structured` | Structured AI response |
| POST | `/api/v1/migration/analyze` | Analyze migration |
| GET | `/api/v1/mcp/servers` | List MCP servers |
| GET | `/api/v1/mcp/tools` | List available tools |

### cf-forge-admin (Dashboard)

Base URL: `https://cf-forge-admin.apps.tas-ndc.kuhn-labs.com`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin` | Admin dashboard (HTML) |
| GET | `/api/v1/admin/overview` | Platform overview metrics |
| GET | `/api/v1/admin/health` | All services health status |
| GET | `/api/v1/admin/health/history` | Health history |
| GET | `/api/v1/admin/users` | User metrics |
| GET | `/api/v1/admin/builds` | Build metrics |
| GET | `/api/v1/admin/deployments` | Deployment metrics |
| GET | `/api/v1/admin/agent` | Agent usage metrics |
| GET | `/api/v1/admin/genai` | GenAI usage metrics |
| GET | `/api/v1/admin/audit` | Audit logs |
| GET | `/api/v1/admin/audit/export/csv` | Export audit logs as CSV |

## Project Structure

```
cf-forge/
├── cf-forge-common/          # Shared entities, DTOs, enums, repositories
│   └── src/main/java/com/cfforge/common/
│       ├── entity/           # JPA entities (Project, Build, Deployment, User, ...)
│       ├── dto/              # BuildRequest, DeployRequest, FileEntry, MetricEvent
│       ├── enums/            # Language, ProjectStatus, DeployStatus, BuildStatus, ...
│       └── repository/       # Spring Data JPA repositories
│
├── cf-forge-api/             # REST gateway + OAuth2 BFF
│   └── src/main/java/com/cfforge/api/
│       ├── controller/       # ProjectController, AuthController, BuildController, ...
│       ├── service/          # Business logic, CF API proxy
│       └── config/           # Security, WebSocket, CORS
│
├── cf-forge-agent/           # AI engine + MCP server
│   └── src/main/java/com/cfforge/agent/
│       ├── config/           # McpServerConfig, AgentConfig, GenAiCloudConfig, WebClientConfig
│       ├── tools/            # @Tool classes (Project, Workspace, Build, Deploy, Platform, Service, Docs)
│       ├── advisor/          # Spring AI advisors (safety, context, memory, RAG)
│       ├── controller/       # SetupController, AgentController
│       ├── service/          # AgentService, MigrationService, EvaluationService
│       └── mcp/              # MCP client registry
│
├── cf-forge-workspace/       # File storage service
│   └── src/main/java/com/cfforge/workspace/
│       └── controller/       # WorkspaceController (CRUD on project files)
│
├── cf-forge-builder/         # Build pipeline service
│   └── src/main/java/com/cfforge/builder/
│       └── consumer/         # RabbitMQ build request consumer
│
├── cf-forge-admin/           # Admin dashboard
│   └── src/main/java/com/cfforge/admin/
│       ├── controller/       # AdminApiController, AuditLogApiController
│       └── scheduler/        # HealthCheckScheduler
│
├── cf-forge-ui/              # React 19 SPA (optional)
│   ├── src/
│   └── package.json
│
├── scripts/
│   ├── deploy.sh             # Orchestrated deploy script
│   └── deploy-config.sh.example
│
├── docker-compose.yml        # Local dev: Postgres, Redis, RabbitMQ, MinIO, Ollama
└── pom.xml                   # Parent POM (Maven multi-module)
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Set to `cloud` for TAS deployment |
| `GENAI_BASE_URL` | `http://localhost:11434/v1` | GenAI/Ollama API endpoint |
| `GENAI_API_KEY` | `not-needed-for-ollama` | API key for GenAI service |
| `GENAI_MODEL` | `llama3` | Chat model name |
| `GENAI_EMBEDDING_MODEL` | `nomic-embed-text` | Embedding model name |
| `CFFORGE_CF_API_URL` | `https://api.sys.local` | CF API endpoint for platform tools |

### Cloud Profile

When `SPRING_PROFILES_ACTIVE=cloud`, services auto-configure from `VCAP_SERVICES`:

- **PostgreSQL** — DataSource auto-configured via java-cfenv
- **RabbitMQ** — Connection factory auto-configured
- **GenAI** — Parsed from VCAP_SERVICES, creates OpenAI-compatible client
- **SSO** — OAuth2 client credentials from p-identity binding

## Deploy Script Reference

```bash
./scripts/deploy.sh                      # Full deploy (services + apps + migrations + smoke tests)
./scripts/deploy.sh --apps-only          # Redeploy apps only
./scripts/deploy.sh --services-only      # Create backing services only
./scripts/deploy.sh --component agent    # Deploy a single service
./scripts/deploy.sh --component ui       # Deploy UI only
./scripts/deploy.sh --migrate-only       # Run Flyway migrations
./scripts/deploy.sh --smoke-test         # Run smoke tests
./scripts/deploy.sh --teardown           # Delete all apps and services
```

## Data Model

```
User ─────┬── Project ──┬── Build ──── Deployment
          │             │
          │             ├── Conversation
          │             │
          ├── CfTarget  └── (Workspace files via workspace service)
          │
          └── UserActivity

Template (community marketplace)
AuditLog (all actions)
MetricSnapshot (time-series aggregation)
ComponentHealthHistory (service monitoring)
```

**Key Enums:** `Language` (JAVA, PYTHON, NODEJS, GO, DOTNET, RUBY, STATICFILE), `ProjectStatus` (ACTIVE, ARCHIVED, DELETED), `BuildStatus` (QUEUED, BUILDING, SUCCESS, FAILED, BLOCKED), `DeployStatus` (PENDING, IN_PROGRESS, DEPLOYED, FAILED, ROLLED_BACK), `DeployStrategy` (ROLLING, BLUE_GREEN, CANARY)

## License

Proprietary. Internal use only.
