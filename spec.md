# CF Forge — Technical Specification

> **Version:** 3.0 · **Date:** 2026-02-08 · **Status:** Draft
> **Purpose:** Implementation reference for GitHub Issues and Claude Code
> **Stack:** Java 21 · Spring Boot 3.4 · Spring AI 1.1 · GenAI on Tanzu Platform

---

## Table of Contents

- [1. Product Overview](#1-product-overview)
- [2. System Architecture](#2-system-architecture)
- [3. Component Specifications](#3-component-specifications)
- [4. Data Model](#4-data-model)
- [5. AI Agent Engine (Spring AI)](#5-ai-agent-engine-spring-ai)
- [6. GenAI on Tanzu Platform Integration](#6-genai-on-tanzu-platform-integration)
- [7. CF Manifest Generation Engine](#7-cf-manifest-generation-engine)
- [8. IDE / Workspace](#8-ide--workspace)
- [9. Deployment Pipeline](#9-deployment-pipeline)
- [10. CF Service Marketplace Integration](#10-cf-service-marketplace-integration)
- [11. Authentication & Authorization](#11-authentication--authorization)
- [12. Real-Time Collaboration](#12-real-time-collaboration)
- [13. Template & Starter Library](#13-template--starter-library)
- [14. Deployment Automation](#14-deployment-automation)
- [15. API Specification](#15-api-specification)
- [16. Configuration & Environment](#16-configuration--environment)
- [17. Security Requirements](#17-security-requirements)
- [18. Performance Targets](#18-performance-targets)
- [19. File & Directory Structure](#19-file--directory-structure)
- [20. Technology Stack](#20-technology-stack)
- [21. Implementation Phases & GitHub Milestones](#21-implementation-phases--github-milestones)
- [22. Issue Label Taxonomy](#22-issue-label-taxonomy)

---

## 1. Product Overview

### What is CF Forge?

CF Forge is an AI-powered application development platform built entirely on **Java 21 and Spring Boot 3.4**, leveraging **Spring AI 1.1** for all AI capabilities and **GenAI on Tanzu Platform** as the LLM provider. It:

1. **Runs on Cloud Foundry** — deployed via `cf push` with `java_buildpack`, uses CF service bindings, orchestrated by a single deploy script
2. **Deploys TO Cloud Foundry** — every generated app targets the CF runtime with proper manifests, buildpacks, and service bindings
3. **Combines two paradigms:**
   - **Lovable-style** prompt-to-app conversational builder for rapid prototyping (non-technical users)
   - **Replit-style** full browser IDE with terminal, editor, preview, and live logs (developers)
4. **Is 100% Spring ecosystem** — Spring Boot, Spring AI, Spring Security, Spring Data JPA, Spring WebSocket, Spring AMQP, Spring Cloud Stream

### Core Value Proposition

> From natural language prompt → production-deployed Cloud Foundry application in < 15 minutes, powered by GenAI on Tanzu Platform, with enterprise governance, Spring AI agent tooling, and CI/CD automation via GitHub Actions.

### Target Users

| Persona | Primary Mode | Key Need |
|---------|-------------|----------|
| Platform Engineer | IDE + CLI | Accelerate inner-loop dev, generate boilerplate, automate manifests |
| Enterprise Developer (new to CF) | IDE + Agent | Learn CF patterns while building real apps |
| Product Manager | Conversational Builder | Create working prototypes without coding |
| Citizen Developer | Conversational Builder | Build internal tools and dashboards |
| DevOps / SRE | IDE + Pipeline | Automate deployment workflows, generate autoscaler policies |

### Why Spring AI + GenAI on Tanzu Platform?

**Spring AI 1.1** provides the portable, enterprise-grade AI framework:
- `ChatClient` fluent API — idiomatically similar to `WebClient` and `RestClient`
- `@Tool` annotation — expose Java methods as LLM-callable functions
- Advisors API — encapsulate recurring GenAI patterns (memory, RAG, evaluation)
- Structured Output — map LLM responses directly to POJOs (manifests, plans, file trees)
- MCP (Model Context Protocol) support — standardized tool/data source integration
- Auto-configuration via Spring Boot starters
- Provider-agnostic — switch models via `application.yml` without code changes

**GenAI on Tanzu Platform** provides the LLM runtime:
- **Privacy** — privately hosted model inside the CF foundation
- **Accessibility** — discoverable in CF Marketplace via `cf marketplace`
- **Unlimited tokens** — no token limits; only bounded by hardware
- **OpenAI-compatible API** — `spring-ai-openai-spring-boot-starter` works out of the box
- **Managed lifecycle** — credential rotation, scaling via CF service updates
- **GPU support** — validated on NVIDIA Tesla T4 and A100 GPUs
- **Air-gap ready** — models hosted on-foundation with no external network dependency
- **Service binding** — credentials injected via `VCAP_SERVICES`, auto-configured by Spring AI

---

## 2. System Architecture

### High-Level Topology

```
┌──────────────────────────────────────────────────────────────────────┐
│                       Cloud Foundry Foundation                        │
│                                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐        │
│  │ cf-forge-ui  │  │ cf-forge-api │  │  cf-forge-agent      │        │
│  │ (React SPA)  │──│ (Spring Boot)│──│  (Spring Boot +      │        │
│  │ staticfile   │  │ java_buildpk │  │   Spring AI)         │        │
│  │ buildpack    │  │              │  │  java_buildpack      │        │
│  └──────────────┘  └──────┬───────┘  └──────────┬───────────┘        │
│                           │                      │                    │
│  ┌──────────────┐  ┌──────┴───────┐  ┌───────────┴──────────────┐    │
│  │ cf-forge-    │  │ cf-forge-    │  │  cf-forge-admin          │    │
│  │ workspace    │──│ builder      │  │  (Spring Boot)           │    │
│  │ (Spring Boot)│  │ (Spring Boot)│  │  Admin Dashboard +       │    │
│  │ java_buildpk │  │ java_buildpk │  │  Metrics + Health        │    │
│  └──────────────┘  └──────────────┘  │  java_buildpack          │    │
│                                       └─────────────────────────┘    │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐     │
│  │                    CF Service Bindings                        │     │
│  │  ┌─────┐ ┌───────┐ ┌───────┐ ┌──────────┐ ┌──────────────┐ │     │
│  │  │ DB  │ │ Redis │ │  MQ   │ │ Object   │ │ SSO          │ │     │
│  │  │(PG) │ │(cache)│ │(Rab-  │ │ Storage  │ │(p-identity)  │ │     │
│  │  │     │ │       │ │bitMQ) │ │(S3/Minio)│ │ CF SSO       │ │     │
│  │  └─────┘ └───────┘ └───────┘ └──────────┘ └──────────────┘ │     │
│  │                                ┌──────────────┐              │     │
│  │                                │ GenAI on     │              │     │
│  │                                │ Tanzu Platform│              │     │
│  │                                │ (LLM Service)│              │     │
│  │                                └──────────────┘              │     │
│  └──────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘
```

### Network Architecture

```
External Traffic
      │
      ▼
┌──────────────┐     CF External Routes (GoRouter)
│ cf-forge-ui  │ ◄── https://forge.apps.example.com
│ cf-forge-api │ ◄── https://forge-api.apps.example.com
│ cf-forge-    │ ◄── https://forge-admin.apps.example.com
│   admin      │
└──────┬───────┘
       │  CF Container-to-Container Networking (internal routes)
       ▼
┌─────────────────────────────────────────────┐
│ cf-forge-agent.apps.internal:8080           │
│ cf-forge-builder.apps.internal:8080         │
│ cf-forge-workspace.apps.internal:8080       │
└─────────────────────────────────────────────┘
```

**Key principle:** Only `cf-forge-ui`, `cf-forge-api`, and `cf-forge-admin` have external routes. All other Spring Boot backend services communicate exclusively via CF internal routes with network policies.

### Service Binding Map

```yaml
# All services bound via CF marketplace or CUPS
cf-forge-api:
  services:
    - cf-forge-db          # PostgreSQL (via Spring Data JPA)
    - cf-forge-cache       # Redis (via Spring Data Redis / Spring Session)
    - cf-forge-mq          # RabbitMQ (via Spring AMQP / Spring Cloud Stream)
    - cf-forge-sso         # CF SSO / p-identity (Spring Security OAuth2 Client)

cf-forge-agent:
  services:
    - cf-forge-db          # Read project context (Spring Data JPA)
    - cf-forge-mq          # Receive generation tasks (Spring Cloud Stream)
    - cf-forge-genai       # GenAI on Tanzu Platform (Spring AI OpenAI auto-config)
    - cf-forge-object-store # S3: generated artifacts

cf-forge-builder:
  services:
    - cf-forge-mq          # Receive build tasks (Spring Cloud Stream)
    - cf-forge-object-store # Store/retrieve build artifacts
    - cf-forge-db          # Update build status (Spring Data JPA)

cf-forge-workspace:
  services:
    - cf-forge-db          # Workspace metadata (Spring Data JPA)
    - cf-forge-cache       # Real-time collaboration state (Spring Data Redis)
    - cf-forge-object-store # File storage (project files, snapshots)
    - cf-forge-nfs         # Optional: NFS volume service

cf-forge-admin:
  services:
    - cf-forge-db          # Read all tables + admin-specific metrics tables
    - cf-forge-cache       # Dashboard session + cached aggregations
    - cf-forge-mq          # Consume metric events from all services
    - cf-forge-sso         # CF SSO (admin users must have OrgManager role)
```

### Inter-Service Communication

All inter-service calls use:
- **Synchronous:** `WebClient` over CF internal routes (`.apps.internal`)
- **Asynchronous:** Spring Cloud Stream / Spring AMQP over RabbitMQ
- **Events:** Spring Application Events (intra-process) + RabbitMQ (inter-process)

---

## 3. Component Specifications

### 3.1 cf-forge-ui

**Purpose:** Browser-based frontend — IDE, conversational builder, dashboard.

| Property | Value |
|----------|-------|
| Language | TypeScript |
| Framework | React 18+ with Vite |
| UI Library | shadcn/ui + Tailwind CSS |
| Editor | Monaco Editor (@monaco-editor/react) |
| Terminal | xterm.js with WebSocket backend |
| State Mgmt | Zustand + TanStack Query |
| Buildpack | `staticfile_buildpack` |
| Memory | 256 MB |
| Instances | 2+ (stateless) |

> **Note:** The UI is the only non-Java component. It is a static React SPA served by nginx. All API calls target `cf-forge-api` (Spring Boot).

**WebSocket Connections (from UI to cf-forge-api via Spring WebSocket/STOMP):**

```
wss://forge-api.apps.example.com/ws/terminal/{sessionId}
wss://forge-api.apps.example.com/ws/collab/{projectId}
wss://forge-api.apps.example.com/ws/logs/{appGuid}
wss://forge-api.apps.example.com/ws/agent/{conversationId}
wss://forge-api.apps.example.com/ws/build/{buildId}
```

### 3.2 cf-forge-api

**Purpose:** API gateway — auth, routing, WebSocket management, rate limiting.

| Property | Value |
|----------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Web | Spring WebMVC + Spring WebSocket (STOMP) |
| Security | Spring Security OAuth2 Resource Server (UAA) |
| Data | Spring Data JPA (Hibernate 6) + PostgreSQL |
| Cache | Spring Data Redis + Spring Session (Redis) |
| Messaging | Spring AMQP / Spring Cloud Stream (RabbitMQ) |
| Buildpack | `java_buildpack` or `java_buildpack_offline` |
| Memory | 1G |
| Instances | 3+ (stateless) |
| JDK | 21 |

**Key Spring Dependencies (pom.xml):**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.2</version>
</parent>

<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-websocket</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-resource-server</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.session</groupId><artifactId>spring-session-data-redis</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
</dependencies>
```

**Responsibilities:**

- OAuth2/OIDC auth via CF UAA (`spring-boot-starter-oauth2-resource-server`)
- Request routing to internal services via `WebClient`
- WebSocket management via Spring WebSocket + STOMP protocol
- Rate limiting via Spring Data Redis + custom `HandlerInterceptor`
- Request validation via Jakarta Bean Validation (`@Valid`)
- CORS/CSP via Spring Security `CorsConfigurationSource`
- Health/metrics via Spring Actuator (`/actuator/health`, `/actuator/prometheus`)
- Audit logging via Spring AOP `@Aspect`

### 3.3 cf-forge-agent

**Purpose:** AI engine — the core Spring AI component. Prompt processing, code generation, manifest creation, iterative refinement.

| Property | Value |
|----------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 + **Spring AI 1.1** |
| AI Provider | **GenAI on Tanzu Platform** (OpenAI-compatible) |
| AI Features | ChatClient, Advisors, @Tool Calling, Structured Output, MCP Client |
| Chat Memory | Spring AI JDBC ChatMemoryRepository (PostgreSQL) |
| Embeddings | GenAI on Tanzu Platform embedding model (for RAG) |
| Vector Store | PGVector (via spring-ai-pgvector-store) |
| Messaging | Spring Cloud Stream (RabbitMQ) |
| Buildpack | `java_buildpack` or `java_buildpack_offline` |
| Memory | 2G |
| Instances | 2+ (autoscaled on queue depth) |
| JDK | 21 |

**Spring AI Dependencies (pom.xml):**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Spring AI: OpenAI-compatible (works with GenAI on Tanzu Platform) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring AI: Advisors (memory, RAG, evaluation) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-advisors-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring AI: MCP Client -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring AI: JDBC Chat Memory (PostgreSQL) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-model-chat-memory-jdbc</artifactId>
    </dependency>

    <!-- Spring AI: PGVector Store (for RAG) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring Boot core -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-stream-binder-rabbit</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
</dependencies>
```

**This component is detailed in [Section 5: AI Agent Engine](#5-ai-agent-engine-spring-ai) and [Section 6: GenAI Integration](#6-genai-on-tanzu-platform-integration).**

### 3.4 cf-forge-builder

**Purpose:** Build service — compile, test, package, scan, store artifacts.

| Property | Value |
|----------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Messaging | Spring Cloud Stream (RabbitMQ) |
| Storage | AWS S3 SDK (S3-compatible object stores) |
| Buildpack | `java_buildpack` or `java_buildpack_offline` |
| Memory | 2G |
| Instances | 2+ (autoscaled on queue depth) |

**Build pipeline implemented as Spring Cloud Stream consumer:**

```java
@Component
public class BuildPipelineConsumer {

    @Bean
    public Consumer<BuildRequest> buildRequest() {
        return request -> {
            // 1. Pull project files from object storage
            // 2. Resolve dependencies (mvn/gradle/npm/pip/go mod)
            // 3. Lint source code
            // 4. Run unit tests
            // 5. Compile / build
            // 6. Scan for vulnerabilities (Trivy via ProcessBuilder)
            // 7. Generate SBOM (CycloneDX)
            // 8. Package artifact + manifest.yml
            // 9. Upload artifact to object storage
            // 10. Publish build result event
            // 11. Cleanup temp workspace
        };
    }
}
```

### 3.5 cf-forge-workspace

**Purpose:** Per-user workspace state, file management, project snapshots, checkpoint history.

| Property | Value |
|----------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Web | Spring WebMVC + Spring WebSocket |
| Data | Spring Data JPA + Spring Data Redis |
| Storage | AWS S3 SDK or NFS volume service |
| Buildpack | `java_buildpack` or `java_buildpack_offline` |
| Memory | 1G |
| Instances | 2+ |

### 3.6 cf-forge-admin

**Purpose:** Administrative dashboard — platform-wide metrics, user activity tracking, deployment analytics, service health monitoring, and operational insights. Provides operators and platform admins with full visibility into CF Forge usage, performance, and health.

| Property | Value |
|----------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Web | Spring WebMVC (REST API) + Thymeleaf (server-rendered admin UI) |
| Security | Spring Security OAuth2 Client (CF SSO / p-identity) |
| Data | Spring Data JPA (PostgreSQL) + Spring Data Redis (cached aggregations) |
| Messaging | Spring Cloud Stream (RabbitMQ) — consumes metric events |
| Scheduling | Spring `@Scheduled` tasks for metric aggregation |
| Observability | Micrometer + Spring Actuator + custom `MeterRegistry` gauges |
| Buildpack | `java_buildpack` or `java_buildpack_offline` |
| Memory | 1G |
| Instances | 2 |
| JDK | 21 |

**Key Spring Dependencies (pom.xml):**

```xml
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-client</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-stream-binder-rabbit</artifactId></dependency>
    <dependency><groupId>org.springframework.session</groupId><artifactId>spring-session-data-redis</artifactId></dependency>
    <dependency><groupId>io.pivotal.cfenv</groupId><artifactId>java-cfenv-boot-pivotal-sso</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
</dependencies>
```

**Dashboard Modules:**

| Module | Path | Description |
|--------|------|-------------|
| Overview | `/admin/` | Real-time system health, active users, deployments today, agent calls |
| User Metrics | `/admin/users` | Active users, sessions over time, top users by activity, sign-in history |
| AI Agent Metrics | `/admin/agent` | Prompts/day, avg response time, token usage, tool call frequency, error rates |
| Deployment Metrics | `/admin/deployments` | Deploys/day, success/fail rates, rollbacks, avg deploy time, by-language breakdown |
| Build Metrics | `/admin/builds` | Build success rate, avg duration, CVE findings, SBOM coverage |
| Service Health | `/admin/health` | All component health (actuator), GenAI latency, DB/Redis/RabbitMQ status |
| Audit Trail | `/admin/audit` | Searchable, filterable audit log with export (CSV) |
| GenAI Usage | `/admin/genai` | Model response times, request volume, queue depth, token throughput |

**Metric Event Consumer (Spring Cloud Stream):**

```java
@Component
public class MetricEventConsumer {

    private final MetricAggregationService aggregationService;

    @Bean
    public Consumer<MetricEvent> metricEvent() {
        return event -> {
            // All CF Forge services emit MetricEvents to RabbitMQ
            // Events: user.login, user.logout, agent.prompt, agent.response,
            //         agent.tool_call, build.started, build.completed, build.failed,
            //         deploy.started, deploy.completed, deploy.failed, deploy.rollback,
            //         service.created, service.bound, project.created, project.deleted
            aggregationService.record(event);
        };
    }
}
```

**Metric Aggregation Service:**

```java
@Service
public class MetricAggregationService {

    private final MetricSnapshotRepository snapshotRepo;
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, String> redis;

    // Real-time counters (Redis + Micrometer)
    public void record(MetricEvent event) {
        // 1. Increment Redis real-time counter
        redis.opsForValue().increment("metrics:" + event.type() + ":count");
        redis.opsForHash().increment("metrics:hourly:" + currentHour(), event.type(), 1);

        // 2. Update Micrometer gauge for Prometheus scraping
        meterRegistry.counter("cfforge." + event.type(), "source", event.source()).increment();

        // 3. Record timing for latency metrics
        if (event.durationMs() != null) {
            meterRegistry.timer("cfforge." + event.type() + ".duration")
                .record(Duration.ofMillis(event.durationMs()));
        }

        // 4. Persist raw event for historical queries
        // (batched via @Scheduled flush every 30s to avoid write amplification)
    }

    // Hourly aggregation job
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateHourly() {
        // Roll up Redis counters into metric_snapshots table
        // Granularity: HOURLY → DAILY → MONTHLY (retention: 90 days raw, 2 years aggregated)
    }
}
```

**Admin REST API:**

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('cfforge.admin')")
public class AdminMetricsController {

    @GetMapping("/overview")
    public OverviewMetrics getOverview() { ... }
    // Returns: activeUsers, totalProjects, deploysToday, buildsToday,
    //          agentPromptsToday, avgAgentResponseMs, systemHealth

    @GetMapping("/users")
    public Page<UserMetrics> getUserMetrics(@RequestParam Map<String, String> filters, Pageable p) { ... }

    @GetMapping("/users/{id}/activity")
    public List<UserActivity> getUserActivity(@PathVariable UUID id, @RequestParam String range) { ... }

    @GetMapping("/deployments")
    public DeploymentMetrics getDeploymentMetrics(@RequestParam String range) { ... }
    // Returns: totalDeploys, successRate, avgDurationSec, byStrategy, byLanguage, byEnvironment

    @GetMapping("/builds")
    public BuildMetrics getBuildMetrics(@RequestParam String range) { ... }
    // Returns: totalBuilds, successRate, avgDurationSec, cveFindings, byLanguage

    @GetMapping("/agent")
    public AgentMetrics getAgentMetrics(@RequestParam String range) { ... }
    // Returns: totalPrompts, avgResponseMs, toolCallBreakdown, errorRate, topTools

    @GetMapping("/genai")
    public GenAiMetrics getGenAiMetrics(@RequestParam String range) { ... }
    // Returns: requestVolume, avgLatencyMs, p95LatencyMs, queueDepth, modelBreakdown

    @GetMapping("/health")
    public Map<String, ComponentHealth> getSystemHealth() { ... }
    // Calls /actuator/health on all CF Forge components via internal routes

    @GetMapping("/audit")
    public Page<AuditLog> getAuditLog(@RequestParam Map<String, String> filters, Pageable p) { ... }

    @GetMapping("/audit/export")
    public ResponseEntity<StreamingResponseBody> exportAuditLog(
        @RequestParam String format, @RequestParam String range) { ... }
    // Export as CSV or JSON
}
```

---

### Primary Database (PostgreSQL via Spring Data JPA + Flyway)

All entities are Spring Data JPA `@Entity` classes. Migrations managed by Flyway.

```java
// ── Users (synced from CF UAA) ──

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uaa_user_id", unique = true, nullable = false)
    private String uaaUserId;

    @Column(nullable = false)
    private String email;
    private String displayName;
    private String avatarUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> preferences = new HashMap<>();

    private Instant createdAt;
    private Instant updatedAt;
}

// ── CF Targets ──

@Entity @Table(name = "cf_targets")
public class CfTarget {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    private String name;           // "Production TAS"
    private String apiEndpoint;    // https://api.sys.example.com
    private String orgGuid;
    private String orgName;
    private String spaceGuid;
    private String spaceName;
    private boolean isDefault;
    private Instant createdAt;
}

// ── Projects ──

@Entity @Table(name = "projects")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String slug;
    private String description;

    @Enumerated(EnumType.STRING) private Visibility visibility = Visibility.PRIVATE;
    @Enumerated(EnumType.STRING) private Language language;
    private String framework;
    private String buildpack;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cf_target_id")
    private CfTarget cfTarget;

    private String cfAppGuid;
    private UUID workspaceId;
    private UUID templateId;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> cfManifest;

    @Enumerated(EnumType.STRING) private ProjectStatus status = ProjectStatus.DRAFT;
    private Instant createdAt;
    private Instant updatedAt;
}

// ── Conversations ──
// Message persistence handled by Spring AI JDBC ChatMemoryRepository
// The conversation.id is used as the Spring AI conversationId key

@Entity @Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING) private ConversationMode mode = ConversationMode.BUILDER;
    @Enumerated(EnumType.STRING) private ConversationStatus status = ConversationStatus.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;
}

// ── Builds ──

@Entity @Table(name = "builds")
public class Build {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Enumerated(EnumType.STRING) private TriggerType triggerType;
    @Enumerated(EnumType.STRING) private BuildStatus status = BuildStatus.QUEUED;

    @Column(columnDefinition = "TEXT") private String buildLog;
    private String artifactPath;
    private String sbomPath;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> cveReport;

    private Integer durationMs;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
}

// ── Deployments ──

@Entity @Table(name = "deployments")
public class Deployment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "build_id")
    private Build build;

    @Enumerated(EnumType.STRING) private DeployStrategy strategy = DeployStrategy.ROLLING;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> manifestUsed;

    @Enumerated(EnumType.STRING) private DeployStatus status = DeployStatus.IN_PROGRESS;
    @Enumerated(EnumType.STRING) private DeployEnvironment environment = DeployEnvironment.STAGING;

    private String cfAppGuid;
    private String cfDropletGuid;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
}

// ── Audit Log ──

@Entity @Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false) private String action;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    private String ipAddress;
    private Instant createdAt;
}

// ── Templates ──

@Entity @Table(name = "templates")
public class Template {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String slug;
    private String description;
    private String category;

    @Enumerated(EnumType.STRING) private Language language;
    private String framework;
    private String buildpack;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> manifestTemplate;

    private String filesPath;
    private boolean isOfficial = true;
    private int downloadCount = 0;
    private Instant createdAt;
}
```

```java
// ============================================================
// ADMIN METRICS (cf-forge-admin)
// ============================================================

// ── Metric Snapshots (pre-aggregated time-series data) ──

@Entity @Table(name = "metric_snapshots")
public class MetricSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private String metricName;   // e.g. "agent.prompt", "deploy.completed"

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private MetricGranularity granularity;                  // HOURLY, DAILY, MONTHLY

    @Column(nullable = false) private Instant periodStart;
    @Column(nullable = false) private Instant periodEnd;

    private long count;
    private double sumDurationMs;
    private double avgDurationMs;
    private double p95DurationMs;
    private long errorCount;
    private long successCount;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> dimensions;                // e.g. {"language":"java","framework":"spring-boot"}

    private Instant createdAt;
}

// ── User Activity Log (high-frequency event stream for user metrics) ──

@Entity @Table(name = "user_activity")
public class UserActivity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false) private String activityType;  // login, logout, prompt, deploy, build, file_edit
    private String projectId;
    private String detail;                                   // prompt text summary, deploy target, etc.
    private Integer durationMs;
    private boolean success = true;
    private String errorMessage;

    private Instant createdAt;
}

// ── Component Health History (periodic health snapshots for SLA tracking) ──

@Entity @Table(name = "component_health_history")
public class ComponentHealthHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private String componentName;  // cf-forge-api, cf-forge-agent, etc.

    @Enumerated(EnumType.STRING)
    private HealthStatus status;                              // UP, DOWN, DEGRADED, UNKNOWN

    private double cpuPercent;
    private long memoryUsedMb;
    private long memoryTotalMb;
    private int instancesRunning;
    private int instancesDesired;
    private Double responseTimeMs;                            // health check response time

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;                     // actuator health details JSON

    private Instant recordedAt;
}

// ── Metric Event DTO (published by all services via RabbitMQ) ──

public record MetricEvent(
    String type,           // agent.prompt, deploy.completed, build.failed, user.login, etc.
    String source,         // cf-forge-api, cf-forge-agent, cf-forge-builder
    UUID userId,
    UUID projectId,
    Integer durationMs,
    boolean success,
    String errorMessage,
    Map<String, String> dimensions,  // language, framework, strategy, etc.
    Instant timestamp
) {}
```

### Spring Data JPA Repositories

```java
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    Optional<Project> findByOwnerIdAndSlug(UUID ownerId, String slug);
}

public interface BuildRepository extends JpaRepository<Build, UUID> {
    List<Build> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    List<Deployment> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findBySlug(String slug);
    List<Template> findByLanguageOrderByDownloadCountDesc(Language language);
}

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<AuditLog> findByActionContainingOrderByCreatedAtDesc(String action, Pageable pageable);
    long countByCreatedAtBetween(Instant start, Instant end);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);
}

// ── Admin Metrics Repositories ──

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, UUID> {
    List<MetricSnapshot> findByMetricNameAndGranularityAndPeriodStartBetween(
        String metricName, MetricGranularity granularity, Instant start, Instant end);
    List<MetricSnapshot> findByGranularityAndPeriodStartBetween(
        MetricGranularity granularity, Instant start, Instant end);
    @Query("SELECT m.metricName, SUM(m.count), AVG(m.avgDurationMs) FROM MetricSnapshot m " +
           "WHERE m.granularity = :gran AND m.periodStart BETWEEN :start AND :end " +
           "GROUP BY m.metricName")
    List<Object[]> aggregateByMetric(MetricGranularity gran, Instant start, Instant end);
}

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<UserActivity> findByActivityTypeAndCreatedAtBetween(String type, Instant start, Instant end);
    @Query("SELECT ua.user.id, COUNT(ua) FROM UserActivity ua " +
           "WHERE ua.createdAt BETWEEN :start AND :end GROUP BY ua.user.id ORDER BY COUNT(ua) DESC")
    List<Object[]> findTopActiveUsers(Instant start, Instant end, Pageable pageable);
    @Query("SELECT COUNT(DISTINCT ua.user.id) FROM UserActivity ua WHERE ua.createdAt BETWEEN :start AND :end")
    long countDistinctActiveUsers(Instant start, Instant end);
}

public interface ComponentHealthHistoryRepository extends JpaRepository<ComponentHealthHistory, UUID> {
    List<ComponentHealthHistory> findByComponentNameAndRecordedAtBetweenOrderByRecordedAtDesc(
        String componentName, Instant start, Instant end);
    @Query("SELECT h.componentName, h.status, h.recordedAt FROM ComponentHealthHistory h " +
           "WHERE h.recordedAt = (SELECT MAX(h2.recordedAt) FROM ComponentHealthHistory h2 " +
           "WHERE h2.componentName = h.componentName)")
    List<Object[]> findLatestHealthForAllComponents();
}
```

### RabbitMQ (Spring Cloud Stream)

```yaml
spring:
  cloud:
    stream:
      bindings:
        agentGenerate-in-0:
          destination: cf-forge.agent.generate
          group: agent-consumers
        agentRefine-in-0:
          destination: cf-forge.agent.refine
          group: agent-consumers
        buildRequest-in-0:
          destination: cf-forge.build.request
          group: builder-consumers
        deployRequest-in-0:
          destination: cf-forge.deploy.request
          group: deploy-consumers
        notification-out-0:
          destination: cf-forge.notifications
        # ── Admin metrics (consumed by cf-forge-admin) ──
        metricEvent-in-0:
          destination: cf-forge.metrics
          group: admin-consumers
        # ── Metric event publisher (used by all services via StreamBridge) ──
        metricEvent-out-0:
          destination: cf-forge.metrics
      rabbit:
        bindings:
          agentGenerate-in-0:
            consumer:
              auto-bind-dlq: true
              republish-to-dlq: true
          buildRequest-in-0:
            consumer:
              auto-bind-dlq: true
              republish-to-dlq: true
```

---

## 5. AI Agent Engine (Spring AI)

### ChatClient Configuration

The agent uses Spring AI's `ChatClient` fluent API with the `ToolCallAdvisor` and custom advisors:

```java
@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agentChatClient(
            ChatClient.Builder builder,
            ChatMemoryRepository chatMemoryRepository,
            List<Object> agentTools) {

        return builder
            // System prompt with CF platform knowledge
            .defaultSystem(ResourceUtils.getText("classpath:prompts/system.st"))

            // Chat memory advisor — persists conversation via JDBC
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                        .chatMemoryRepository(chatMemoryRepository)
                        .maxMessages(50)
                        .build()
                ).build(),

                // Tool calling advisor — enables @Tool method invocation
                new ToolCallAdvisor()
            )

            // Register all @Tool-annotated beans
            .defaultTools(agentTools.toArray())

            .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
            .dataSource(dataSource)
            .build();
    }
}
```

### Agent Service

```java
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ProjectRepository projectRepository;
    private final StreamBridge streamBridge;

    public Flux<String> generate(UUID conversationId, UUID projectId, String userMessage) {
        Project project = projectRepository.findById(projectId).orElseThrow();

        // Build dynamic system context from project state
        String projectContext = buildProjectContext(project);

        return chatClient.prompt()
            .system(s -> s.param("projectContext", projectContext)
                          .param("availableBuildpacks", getBuildpacks())
                          .param("availableServices", getMarketplaceServices())
                          .param("currentManifest", formatManifest(project.getCfManifest())))
            .user(userMessage)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
            .stream()
            .content();
    }

    public <T> T generateStructured(UUID projectId, String userMessage, Class<T> responseType) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String projectContext = buildProjectContext(project);

        return chatClient.prompt()
            .system(s -> s.param("projectContext", projectContext))
            .user(userMessage)
            .call()
            .entity(responseType);  // Spring AI Structured Output → POJO
    }
}
```

### System Prompt Architecture (StringTemplate .st files)

```
src/main/resources/prompts/
├── system.st                  # Base system prompt
├── planning.st                # Architecture planning
├── generation.st              # Code generation
├── refinement.st              # Iterative refinement
├── manifest.st                # CF manifest generation
└── debugging.st               # Error analysis
```

**Base System Prompt (system.st):**

```
You are CF Forge, an expert Cloud Foundry application architect and developer.
You generate production-ready applications that deploy to Cloud Foundry via `cf push`.

PLATFORM CONTEXT:
- Available buildpacks: {availableBuildpacks}
- Available marketplace services: {availableServices}
- Current project manifest: {currentManifest}

PROJECT CONTEXT:
{projectContext}

RULES:
1. Every app MUST include a valid manifest.yml
2. Use CF service bindings for all external dependencies (never hardcode credentials)
3. Include health check endpoints (Spring Actuator /actuator/health for Java apps)
4. Use Spring Boot profiles: application-cloud.yml for CF-specific config
5. Read database/cache/messaging config from VCAP_SERVICES via Spring auto-config
6. Include .cfignore to exclude build artifacts
7. Set appropriate memory, disk_quota, and instances for the app type
8. Use java_buildpack for Java apps, python_buildpack for Python, etc.

When generating code, use the provided tools to:
- Read and write workspace files
- Query the CF marketplace for available services
- Trigger builds and deployments
- Inspect CF app logs for debugging
```

### Agent Tool Definitions (@Tool annotation)

Spring AI's `@Tool` annotation exposes Java methods as LLM-callable functions:

```java
@Component
public class CfPlatformTools {

    private final CfClient cfClient;
    private final WorkspaceClient workspaceClient;

    @Tool(description = "List available services in the CF marketplace with their plans")
    public List<ServiceOffering> getMarketplaceServices() {
        return cfClient.listMarketplace();
    }

    @Tool(description = "List available buildpacks on the CF foundation")
    public List<BuildpackInfo> getAvailableBuildpacks() {
        return cfClient.listBuildpacks();
    }

    @Tool(description = "Get org quota limits (memory, routes, service instances)")
    public OrgQuota getOrgQuota(@ToolParam("CF org GUID") String orgGuid) {
        return cfClient.getOrgQuota(orgGuid);
    }

    @Tool(description = "List service instances bound in a CF space")
    public List<ServiceInstance> getSpaceServices(@ToolParam("CF space GUID") String spaceGuid) {
        return cfClient.getSpaceServices(spaceGuid);
    }

    @Tool(description = "Get recent logs from a deployed CF application")
    public String getRecentLogs(
            @ToolParam("CF app GUID") String appGuid,
            @ToolParam("Number of recent log lines") int lines) {
        return cfClient.getRecentLogs(appGuid, lines);
    }

    @Tool(description = "Get environment variables of a deployed CF app including VCAP_SERVICES")
    public Map<String, Object> getAppEnvironment(@ToolParam("CF app GUID") String appGuid) {
        return cfClient.getAppEnv(appGuid);
    }
}

@Component
public class WorkspaceTools {

    private final WorkspaceService workspaceService;

    @Tool(description = "Read the content of a file in the project workspace")
    public String readFile(
            @ToolParam("Project workspace ID") String workspaceId,
            @ToolParam("File path relative to project root") String path) {
        return workspaceService.readFile(UUID.fromString(workspaceId), path);
    }

    @Tool(description = "Write or create a file in the project workspace")
    public String writeFile(
            @ToolParam("Project workspace ID") String workspaceId,
            @ToolParam("File path relative to project root") String path,
            @ToolParam("File content") String content) {
        workspaceService.writeFile(UUID.fromString(workspaceId), path, content);
        return "File written: " + path;
    }

    @Tool(description = "List all files in the project workspace")
    public List<FileEntry> listFiles(
            @ToolParam("Project workspace ID") String workspaceId,
            @ToolParam(value = "Directory path, defaults to root", required = false) String dir) {
        return workspaceService.listFiles(UUID.fromString(workspaceId), dir);
    }

    @Tool(description = "Delete a file from the project workspace")
    public String deleteFile(
            @ToolParam("Project workspace ID") String workspaceId,
            @ToolParam("File path to delete") String path) {
        workspaceService.deleteFile(UUID.fromString(workspaceId), path);
        return "File deleted: " + path;
    }
}

@Component
public class BuildDeployTools {

    private final StreamBridge streamBridge;

    @Tool(description = "Trigger a build for the project (compile, test, scan, package)")
    public String triggerBuild(@ToolParam("Project ID") String projectId) {
        streamBridge.send("buildRequest-out-0",
            new BuildRequest(UUID.fromString(projectId), TriggerType.AGENT));
        return "Build triggered for project " + projectId;
    }

    @Tool(description = "Deploy the project to a CF space (staging or production)")
    public String triggerDeploy(
            @ToolParam("Project ID") String projectId,
            @ToolParam("Environment: staging or production") String environment) {
        streamBridge.send("deployRequest-out-0",
            new DeployRequest(UUID.fromString(projectId), DeployEnvironment.valueOf(environment.toUpperCase())));
        return "Deployment triggered to " + environment;
    }
}
```

### Spring AI Advisors

```java
// ── Custom Advisor: CF Context Injection ──
// Dynamically injects CF foundation capabilities into every prompt

@Component
public class CfContextAdvisor implements CallAdvisor, StreamAdvisor {

    private final CfClient cfClient;

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        // Inject current CF context into system prompt
        String cfContext = buildCfContext();
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nCF FOUNDATION STATUS:\n" + cfContext)
            .build();
        return chain.nextCall(augmented);
    }

    @Override
    public String getName() { return "CfContextAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 100; }
}

// ── Custom Advisor: Code Safety Validator ──
// Validates generated code before returning to user

@Component
public class CodeSafetyAdvisor implements CallAdvisor {

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        AdvisedResponse response = chain.nextCall(request);
        // Scan response for hardcoded secrets, dangerous patterns
        String validated = validateGeneratedCode(response.result().getOutput().getText());
        // Return sanitized response
        return response;
    }

    @Override
    public String getName() { return "CodeSafetyAdvisor"; }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 100; }
}
```

### Structured Output for Manifest Generation

```java
// Spring AI maps LLM output directly to this POJO
public record GeneratedAppPlan(
    String appName,
    String language,
    String framework,
    String buildpack,
    String memory,
    String diskQuota,
    int instances,
    HealthCheck healthCheck,
    List<ServiceRequirement> services,
    List<String> routes,
    Map<String, String> envVars,
    List<GeneratedFile> files
) {}

public record HealthCheck(String type, String httpEndpoint) {}
public record ServiceRequirement(String name, String type, String plan) {}
public record GeneratedFile(String path, String content, FileAction action) {}

// Usage in AgentService:
GeneratedAppPlan plan = chatClient.prompt()
    .system(ResourceUtils.getText("classpath:prompts/planning.st"))
    .user(userPrompt)
    .call()
    .entity(GeneratedAppPlan.class);  // Automatic JSON → POJO mapping
```

### MCP (Model Context Protocol) Integration

```java
// Spring AI MCP Client for external tool integration

@Configuration
public class McpConfig {

    @Bean
    public McpClient cfDocsMcpClient() {
        // Connect to CF documentation MCP server
        return McpClient.builder()
            .transportType(TransportType.SSE)
            .serverUrl("https://cf-docs-mcp.apps.internal:8080")
            .build();
    }

    @Bean
    public McpClient jiraMcpClient() {
        // Connect to Jira MCP server for ticket management
        return McpClient.builder()
            .transportType(TransportType.SSE)
            .serverUrl("https://jira-mcp.apps.internal:8080")
            .build();
    }
}
```

### RAG Pipeline (Spring AI + PGVector)

```java
@Configuration
public class RagConfig {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(768)               // nomic-embed-text dimension
            .distanceType(DistanceType.COSINE_DISTANCE)
            .indexType(IndexType.HNSW)
            .build();
    }

    // Advisor that injects relevant CF documentation into prompts
    @Bean
    public QuestionAnswerAdvisor cfDocsRagAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(
            SearchRequest.builder()
                .similarityThreshold(0.75)
                .topK(5)
                .build(),
            vectorStore
        ).build();
    }
}

// Document ingestion (run as cf run-task or deploy script step)
@Component
public class CfDocsIngestionService {

    private final VectorStore vectorStore;

    public void ingestCfDocumentation(Path docsDirectory) {
        var reader = new TextReader(docsDirectory);
        var splitter = new TokenTextSplitter(800, 200, 5, 10000, true);
        List<Document> documents = splitter.apply(reader.get());
        vectorStore.add(documents);
    }
}
```

---

## 6. GenAI on Tanzu Platform Integration

### Service Binding Flow

```
1. Platform operator provisions the GenAI on Tanzu Platform service
2. Operator configures LLM models (e.g., Llama 3, Mistral, Gemma)
3. Models appear in CF Marketplace:
   $ cf marketplace
   service    plans          description
   genai      llama3-8b      GenAI LLM Service - Llama 3 8B
   genai      mistral-7b     GenAI LLM Service - Mistral 7B

4. CF Forge agent binds to the GenAI service:
   $ cf create-service genai llama3-8b cf-forge-genai
   $ cf bind-service cf-forge-agent cf-forge-genai

5. VCAP_SERVICES is injected with OpenAI-compatible credentials:
   {
     "genai": [{
       "name": "cf-forge-genai",
       "credentials": {
         "api_base": "https://genai-api.sys.example.com/v1",
         "api_key": "genai-token-xxx",
         "model": "llama3-8b",
         "wire_format": "openai"
       }
     }]
   }

6. Spring AI auto-configuration reads these credentials via application.yml:
   spring.ai.openai.base-url=${vcap.services.cf-forge-genai.credentials.api_base}
   spring.ai.openai.api-key=${vcap.services.cf-forge-genai.credentials.api_key}
   spring.ai.openai.chat.options.model=${vcap.services.cf-forge-genai.credentials.model}
```

### Spring Cloud Connectors for GenAI

```java
@Configuration
@Profile("cloud")
public class GenAiCloudConfig {

    /**
     * Custom VCAP_SERVICES parser for GenAI on Tanzu Platform.
     * Maps the GenAI service binding to Spring AI OpenAI properties.
     */
    @Bean
    public BeanFactoryPostProcessor genAiPropertySource(Environment env) {
        return beanFactory -> {
            String vcapServices = env.getProperty("VCAP_SERVICES", "{}");
            JsonNode vcap = new ObjectMapper().readTree(vcapServices);

            // Find genai service binding
            JsonNode genaiBindings = vcap.path("genai");
            if (genaiBindings.isEmpty()) {
                // Fallback: check user-provided services
                genaiBindings = StreamSupport.stream(
                    vcap.path("user-provided").spliterator(), false)
                    .filter(s -> s.path("name").asText().contains("genai"))
                    .collect(Collectors.toList());
            }

            if (!genaiBindings.isEmpty()) {
                JsonNode creds = genaiBindings.get(0).path("credentials");
                System.setProperty("spring.ai.openai.base-url", creds.path("api_base").asText());
                System.setProperty("spring.ai.openai.api-key", creds.path("api_key").asText());
                System.setProperty("spring.ai.openai.chat.options.model", creds.path("model").asText());

                // Set embedding model if available
                if (creds.has("embedding_model")) {
                    System.setProperty("spring.ai.openai.embedding.options.model",
                        creds.path("embedding_model").asText());
                }
            }
        };
    }
}
```

### GenAI Configuration in application.yml

```yaml
# ── cf-forge-agent application.yml ──

spring:
  application:
    name: cf-forge-agent
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  ai:
    openai:
      # Overridden by VCAP_SERVICES in cloud profile
      base-url: ${GENAI_BASE_URL:http://localhost:11434/v1}
      api-key: ${GENAI_API_KEY:not-needed-for-ollama}
      chat:
        options:
          model: ${GENAI_MODEL:llama3}
          temperature: 0.2
          max-tokens: 8192
      embedding:
        options:
          model: ${GENAI_EMBEDDING_MODEL:nomic-embed-text}
    chat:
      memory:
        repository:
          jdbc:
            enabled: true
    vectorstore:
      pgvector:
        dimensions: 768
        distance-type: cosine_distance
        index-type: hnsw

---
# Cloud profile (activated on CF)
spring:
  config:
    activate:
      on-profile: cloud
  ai:
    openai:
      base-url: ${vcap.services.cf-forge-genai.credentials.api_base}
      api-key: ${vcap.services.cf-forge-genai.credentials.api_key}
      chat:
        options:
          model: ${vcap.services.cf-forge-genai.credentials.model}
```

### Key GenAI on Tanzu Platform Properties

| Property | Value |
|----------|-------|
| Wire format | OpenAI-compatible (`/v1/chat/completions`) |
| Token limits | None (bounded by hardware only) |
| Embedding support | Yes (via configured embedding model) |
| Tool calling | Yes (automatic tool calling configurable per model) |
| GPU support | NVIDIA Tesla T4, V100, A100 |
| CPU fallback | Yes (slower, 10-30s response for PoC) |
| Air-gap | Yes (models hosted on-foundation, HuggingFace or MinIO) |
| Model providers | Ollama, vLLM, VMware Private AI, AWS Bedrock, Azure OpenAI |
| Scaling | Per-model vertical and horizontal scaling via platform operator |

---

## 7. CF Manifest Generation Engine

### Manifest POJOs (for Spring AI Structured Output)

```java
// Spring AI maps LLM output directly to these records

public record CfManifest(List<CfApplication> applications) {}

public record CfApplication(
    String name,
    String memory,
    @JsonProperty("disk_quota") String diskQuota,
    int instances,
    String buildpack,
    List<String> buildpacks,
    String path,
    String command,
    @JsonProperty("health_check_type") String healthCheckType,
    @JsonProperty("health_check_http_endpoint") String healthCheckHttpEndpoint,
    @JsonProperty("health_check_invocation_timeout") Integer healthCheckInvocationTimeout,
    Integer timeout,
    @JsonProperty("no_route") Boolean noRoute,
    @JsonProperty("random_route") Boolean randomRoute,
    List<RouteEntry> routes,
    List<String> services,
    Map<String, String> env,
    String stack,
    List<CfProcess> processes,
    List<CfSidecar> sidecars
) {}

public record RouteEntry(String route) {}
public record CfProcess(String type, String command, String memory, int instances) {}
public record CfSidecar(String name, String command, @JsonProperty("process_types") List<String> processTypes) {}
```

### Manifest Generation Rules

```java
@Component
public class ManifestGenerationRules {

    // Memory defaults by language/framework
    public static final Map<String, String> MEMORY_DEFAULTS = Map.ofEntries(
        entry("java:spring-boot", "1G"),
        entry("java:micronaut", "512M"),
        entry("python:flask", "256M"),
        entry("python:django", "512M"),
        entry("nodejs:express", "256M"),
        entry("nodejs:nextjs", "512M"),
        entry("go:gin", "128M"),
        entry("dotnet:aspnet", "512M"),
        entry("ruby:rails", "512M")
    );

    // Health check defaults
    public static final Map<String, HealthCheck> HEALTH_CHECK_DEFAULTS = Map.ofEntries(
        entry("java:spring-boot", new HealthCheck("http", "/actuator/health")),
        entry("python:flask", new HealthCheck("http", "/health")),
        entry("python:django", new HealthCheck("http", "/health/")),
        entry("nodejs:express", new HealthCheck("http", "/healthz")),
        entry("go:gin", new HealthCheck("http", "/health"))
    );

    // Required env vars by framework
    public static final Map<String, Map<String, String>> REQUIRED_ENV = Map.ofEntries(
        entry("java:spring-boot", Map.of(
            "JBP_CONFIG_OPEN_JDK_JRE", "{ jre: { version: 21.+ } }",
            "SPRING_PROFILES_ACTIVE", "cloud"
        )),
        entry("python:flask", Map.of("FLASK_ENV", "production")),
        entry("nodejs:express", Map.of("NODE_ENV", "production"))
    );

    // Buildpack mapping
    public static final Map<String, String> BUILDPACK_MAP = Map.of(
        "java", "java_buildpack_offline",
        "python", "python_buildpack",
        "nodejs", "nodejs_buildpack",
        "go", "go_buildpack",
        "dotnet", "dotnet_core_buildpack",
        "ruby", "ruby_buildpack",
        "staticfile", "staticfile_buildpack"
    );
}
```

### Autoscaler Policy Generation (Spring AI Structured Output)

```java
public record AutoscalerPolicy(
    @JsonProperty("instance_min_count") int instanceMinCount,
    @JsonProperty("instance_max_count") int instanceMaxCount,
    @JsonProperty("scaling_rules") List<ScalingRule> scalingRules
) {}

public record ScalingRule(
    @JsonProperty("metric_type") String metricType,
    @JsonProperty("breach_duration_secs") int breachDurationSecs,
    int threshold,
    String operator,
    @JsonProperty("cool_down_secs") int coolDownSecs,
    String adjustment
) {}

@Service
public class AutoscalerService {
    private final ChatClient chatClient;

    public AutoscalerPolicy generatePolicy(String appType, String expectedLoad) {
        return chatClient.prompt()
            .system("Generate a CF Autoscaler policy for the described application.")
            .user("App type: " + appType + ", Expected load: " + expectedLoad)
            .call()
            .entity(AutoscalerPolicy.class);
    }
}
```

---

## 8. IDE / Workspace

### IDE Layout

```
┌─────────────────────────────────────────────────────────────┐
│ IDE Layout                                                   │
│                                                               │
│ ┌────────┬───────────────────────┬────────────────────────┐  │
│ │ File   │    Code Editor        │   Preview Panel        │  │
│ │ Tree   │    (Monaco)           │   ┌──────────────────┐ │  │
│ │        │                       │   │ Staging URL:     │ │  │
│ │ src/   │  [manifest.yml]       │   │ https://app-     │ │  │
│ │  ├ app │  [Application.java]   │   │ staging.apps.    │ │  │
│ │  ├ cfg │  [pom.xml]            │   │ example.com      │ │  │
│ │  └ tst │                       │   │                  │ │  │
│ │        │                       │   │ [iframe/preview] │ │  │
│ ├────────┤                       │   └──────────────────┘ │  │
│ │Services│                       │   ┌──────────────────┐ │  │
│ │ ┌────┐ │                       │   │ App Health:      │ │  │
│ │ │ DB │ │                       │   │ CPU: 23%  ██░░░  │ │  │
│ │ │Redis│ │                       │   │ MEM: 450/1024MB  │ │  │
│ │ └────┘ │                       │   │ Inst: 2/3        │ │  │
│ ├────────┴───────────────────────┴────────────────────────┤  │
│ │ Terminal (xterm.js)              │ CF Logs (streaming)   │  │
│ │ $ cf target                      │ [APP/PROC/WEB/0      │  │
│ │ api: api.sys.example.com         │  Started on port 8080]│  │
│ │ $ _                              │                       │  │
│ └──────────────────────────────────┴──────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Terminal WebSocket Handler (Spring WebSocket)

```java
@Configuration
@EnableWebSocket
public class TerminalWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalWebSocketHandler(), "/ws/terminal/{sessionId}")
            .setAllowedOrigins("${cf.forge.domain}")
            .addInterceptors(new AuthHandshakeInterceptor());
    }

    @Bean
    public WebSocketHandler terminalWebSocketHandler() {
        return new TerminalWebSocketHandler();
    }
}

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    // Pre-installed tools in terminal: cf CLI, git, java, mvn, gradle
    // Terminal auto-configures: cf target -o {org} -s {space}
    // cf-forge-cli shortcuts available:
    //   $ forge build          → trigger build pipeline
    //   $ forge deploy         → deploy to staging
    //   $ forge deploy --prod  → deploy to production
    //   $ forge services       → list bound services
    //   $ forge logs           → stream CF logs
    //   $ forge manifest       → regenerate manifest

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Route terminal input to workspace container process
    }
}
```

---

## 9. Deployment Pipeline

### Pipeline State Machine

```
  ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
  │  IDLE   │───▶│ BUILDING │───▶│ SCANNING │───▶│ STAGING  │
  └─────────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
                      │               │               │
                      ▼               ▼               ▼
                 ┌─────────┐    ┌──────────┐    ┌──────────┐
                 │  FAILED  │    │  BLOCKED │    │ REVIEWING│
                 └─────────┘    │ (CVE)    │    └────┬─────┘
                                └──────────┘         │
                      ┌──────────────────────────────┘
                      ▼
                 ┌──────────┐    ┌──────────┐    ┌──────────┐
                 │DEPLOYING │───▶│VALIDATING│───▶│ DEPLOYED │
                 │(cf push) │    │(smoke)   │    │          │
                 └────┬─────┘    └────┬─────┘    └──────────┘
                      │               │
                      ▼               ▼
                 ┌──────────┐    ┌──────────┐
                 │  FAILED  │    │ ROLLBACK │
                 └──────────┘    └──────────┘
```

### Deployment Strategies (Spring Services)

```java
public interface DeployStrategy {
    DeployResult execute(DeployContext ctx);
    void rollback(DeployContext ctx);
}

@Service("rollingDeploy")
public class RollingDeployStrategy implements DeployStrategy {
    // cf push --strategy rolling
}

@Service("blueGreenDeploy")
public class BlueGreenDeployStrategy implements DeployStrategy {
    // 1. cf push {app}-green
    // 2. cf map-route {app}-green {domain} --hostname {app}
    // 3. cf unmap-route {app}-blue {domain}
    // 4. cf delete {app}-blue
    // 5. cf rename {app}-green {app}
}

@Service("canaryDeploy")
public class CanaryDeployStrategy implements DeployStrategy {
    // 1. cf push {app}-canary -i 1
    // 2. cf map-route {app}-canary → monitor error rates
    // 3. If healthy: scale up canary, scale down original
}
```

### CF API Client (Spring WebClient)

```java
@Service
public class CfClient {

    private final WebClient cfApiClient;

    public CfClient(@Value("${cf.api.endpoint}") String apiEndpoint,
                     @Value("${cf.api.token}") String token) {
        this.cfApiClient = WebClient.builder()
            .baseUrl(apiEndpoint + "/v3")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    }

    public Mono<CfApp> getApp(String appGuid) {
        return cfApiClient.get().uri("/apps/{guid}", appGuid)
            .retrieve().bodyToMono(CfApp.class);
    }

    public Flux<ServiceOffering> listMarketplace() {
        return cfApiClient.get().uri("/service_offerings")
            .retrieve().bodyToMono(CfPaginatedResponse.class)
            .flatMapMany(r -> Flux.fromIterable(r.resources()));
    }

    public Flux<BuildpackInfo> listBuildpacks() {
        return cfApiClient.get().uri("/buildpacks")
            .retrieve().bodyToMono(CfPaginatedResponse.class)
            .flatMapMany(r -> Flux.fromIterable(r.resources()));
    }

    public Mono<Void> scaleApp(String appGuid, int instances, String memory) {
        return cfApiClient.patch().uri("/apps/{guid}/processes/web", appGuid)
            .bodyValue(Map.of("instances", instances, "memory_in_mb", parseMemory(memory)))
            .retrieve().toBodilessEntity().then();
    }

    // ... pushApp, createRoute, mapRoute, bindService, getRecentLogs, streamLogs
}
```

---

## 10. CF Service Marketplace Integration

### Marketplace Browser Service

```java
@Service
public class MarketplaceService {

    private final CfClient cfClient;
    private final ChatClient chatClient;

    // Spring AI: recommend services based on app description
    public List<ServiceRecommendation> recommendServices(String appDescription) {
        return chatClient.prompt()
            .system("Given the CF marketplace services, recommend which services this app needs.")
            .user(appDescription + "\n\nAvailable services:\n" + cfClient.listMarketplace().collectList().block())
            .call()
            .entity(new ParameterizedTypeReference<List<ServiceRecommendation>>() {});
    }
}

public record ServiceRecommendation(
    String serviceName,
    String plan,
    String reason,
    String bindingName
) {}
```

---

## 11. Authentication & Authorization

### CF Single Sign-On Service (p-identity)

CF Forge authenticates users via the **Single Sign-On for VMware Tanzu** service (marketplace label: `p-identity`). This provides enterprise SSO with support for SAML, OIDC, and LDAP identity providers — managed through the SSO operator and developer dashboards.

**SSO Flow:**

```
1. Platform operator provisions the SSO service and creates an SSO service plan
2. Operator configures identity providers (UAA internal, LDAP, Okta OIDC, SAML, etc.)
3. The deploy script creates and binds the SSO service instance:

   $ cf create-service p-identity cf-forge-sso-plan cf-forge-sso
   $ cf bind-service cf-forge-api cf-forge-sso
   $ cf bind-service cf-forge-admin cf-forge-sso

5. VCAP_SERVICES injects OAuth2 client credentials:
   {
     "p-identity": [{
       "name": "cf-forge-sso",
       "credentials": {
         "client_id": "0402c634-67de-4c0b-bb7d-1be3f7310fab",
         "client_secret": "00cf5e1c-dea3-43af-9b2d-c0ad6319b206",
         "auth_domain": "https://cf-forge-plan.login.sys.example.com",
         "issuer_uri": "https://cf-forge-plan.login.sys.example.com/oauth/token"
       }
     }]
   }

6. java-cfenv-boot-pivotal-sso auto-maps to Spring Security properties:
   spring.security.oauth2.client.registration.sso.client-id=...
   spring.security.oauth2.client.registration.sso.client-secret=...
   spring.security.oauth2.client.provider.sso.issuer-uri=...
```

### SSO Dependencies

```xml
<!-- pom.xml (cf-forge-api and cf-forge-admin) -->
<dependencies>
    <!-- Spring Security OAuth2 Client (authorization code flow) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- Spring Security OAuth2 Resource Server (JWT validation for API calls) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- CF SSO auto-configuration: maps VCAP_SERVICES to Spring Security props -->
    <dependency>
        <groupId>io.pivotal.cfenv</groupId>
        <artifactId>java-cfenv-boot-pivotal-sso</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>
```

### Spring Security Configuration (cf-forge-api)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfig()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/ws/**").authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            // JWT resource server: validates tokens issued by SSO service
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(ssoJwtDecoder())
                    .jwtAuthenticationConverter(cfForgeJwtConverter())
                )
            )
            .build();
    }

    @Bean
    public JwtDecoder ssoJwtDecoder(
            @Value("${spring.security.oauth2.client.provider.sso.issuer-uri:}") String issuerUri) {
        // SSO service issues JWTs — auto-discovered from issuer-uri
        // java-cfenv-boot-pivotal-sso maps VCAP_SERVICES → this property
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public JwtAuthenticationConverter cfForgeJwtConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract CF roles and SSO scopes from JWT claims
            List<GrantedAuthority> authorities = new ArrayList<>();

            // SSO scopes → Spring authorities
            List<String> scopes = jwt.getClaimAsStringList("scope");
            if (scopes != null) {
                scopes.stream()
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .forEach(authorities::add);
            }

            // Map cfforge.admin scope to ROLE_ADMIN
            if (scopes != null && scopes.contains("cfforge.admin")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("${cf.forge.domain}"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### Spring Security Configuration (cf-forge-admin)

```java
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/api/v1/admin/**").authenticated()
                .anyRequest().denyAll()
            )
            // OAuth2 login flow via CF SSO service (authorization code grant)
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/admin/", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(cfForgeOidcUserService())
                )
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutSuccessUrl("/admin/")
                .invalidateHttpSession(true)
            )
            .build();
    }

    @Bean
    public OidcUserService cfForgeOidcUserService() {
        // Custom OIDC user service to enforce admin access
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) {
                OidcUser oidcUser = super.loadUser(userRequest);
                // Verify user has cfforge.admin scope or OrgManager role
                Set<String> scopes = userRequest.getAccessToken().getScopes();
                if (!scopes.contains("cfforge.admin")) {
                    throw new OAuth2AuthenticationException("Insufficient permissions for admin dashboard");
                }
                return oidcUser;
            }
        };
    }
}
```

### SSO application.yml

```yaml
# ── cf-forge-api application.yml (SSO properties auto-set by java-cfenv-boot-pivotal-sso) ──
spring:
  security:
    oauth2:
      client:
        registration:
          sso:
            # These are auto-populated from VCAP_SERVICES p-identity binding
            # by java-cfenv-boot-pivotal-sso library
            client-id: ${sso.client-id:local-dev-client}
            client-secret: ${sso.client-secret:local-dev-secret}
            scope: openid,profile,email,roles,cfforge.admin
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          sso:
            issuer-uri: ${sso.issuer-uri:http://localhost:8080/uaa/oauth/token}
            user-name-attribute: user_name
```

### SSO Identity Provider Configuration

The SSO service supports multiple identity providers. CF Forge requires the following scopes/resources configured in the SSO developer dashboard:

| Resource | Scopes | Description |
|----------|--------|-------------|
| `cfforge` | `cfforge.user` | Standard CF Forge user access |
| `cfforge` | `cfforge.admin` | Admin dashboard access (metrics, audit, health) |
| `cfforge` | `cfforge.deploy` | Deploy to production (staging is default) |
| `cloud_controller` | `cloud_controller.read`, `cloud_controller.write` | CF API access (token passthrough) |

**Supported Identity Providers:**
- **UAA Internal** — CF platform users (default)
- **LDAP** — Enterprise Active Directory / LDAP
- **SAML 2.0** — Okta, Azure AD, PingFederate, ADFS
- **OIDC** — Okta, Azure AD, Google Workspace, Keycloak

### RBAC (CF Role Mapping)

```java
@Component
public class CfRbacService {

    private final CfClient cfClient;

    // CF Forge permissions map to CF org/space roles + SSO scopes
    private static final Map<String, Set<String>> PERMISSIONS = Map.ofEntries(
        entry("project.create",    Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("project.edit",      Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("project.view",      Set.of("SpaceDeveloper", "SpaceManager", "SpaceAuditor", "OrgManager")),
        entry("project.delete",    Set.of("SpaceDeveloper", "OrgManager")),
        entry("deploy.staging",    Set.of("SpaceDeveloper", "SpaceManager", "OrgManager")),
        entry("deploy.production", Set.of("SpaceDeveloper", "OrgManager")),
        entry("service.create",    Set.of("SpaceDeveloper", "OrgManager")),
        entry("service.bind",      Set.of("SpaceDeveloper", "OrgManager")),
        entry("audit.view",        Set.of("SpaceManager", "SpaceAuditor", "OrgManager")),
        entry("admin.access",      Set.of("OrgManager"))  // Admin dashboard
    );

    public boolean hasPermission(String uaaToken, String action, String spaceGuid) {
        Set<String> userRoles = cfClient.getUserSpaceRoles(uaaToken, spaceGuid);
        Set<String> requiredRoles = PERMISSIONS.getOrDefault(action, Set.of());
        return userRoles.stream().anyMatch(requiredRoles::contains);
    }

    public boolean isAdmin(JwtAuthenticationToken auth) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("SCOPE_cfforge.admin"));
    }
}
```

---

## 12. Real-Time Collaboration

### Spring WebSocket + STOMP

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/collab")
            .setAllowedOrigins("${cf.forge.domain}")
            .withSockJS();
    }
}

@Controller
public class CollaborationController {

    @MessageMapping("/collab/{projectId}/edit")
    @SendTo("/topic/collab/{projectId}/edits")
    public EditOperation handleEdit(@DestinationVariable String projectId,
                                     EditOperation operation,
                                     Principal principal) {
        operation.setUserId(principal.getName());
        return operation;
    }

    @MessageMapping("/collab/{projectId}/cursor")
    @SendTo("/topic/collab/{projectId}/cursors")
    public CursorPosition handleCursor(@DestinationVariable String projectId,
                                        CursorPosition position,
                                        Principal principal) {
        position.setUserId(principal.getName());
        return position;
    }
}
```

---

## 13. Template & Starter Library

### Template Registry (Spring Boot API template example)

```yaml
# templates/spring-boot-api/metadata.yml
name: Spring Boot REST API
slug: spring-boot-api
description: >
  Production-ready Spring Boot 3.4 REST API with Spring Security (OAuth2/UAA),
  Spring Data JPA, Actuator health, and CF service bindings.
category: microservice
language: java
framework: spring-boot
buildpack: java_buildpack_offline

required_services:
  - type: postgresql
    plan_hint: small

manifest:
  memory: 1G
  disk_quota: 512M
  instances: 2
  health_check_type: http
  health_check_http_endpoint: /actuator/health
  env:
    JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
    SPRING_PROFILES_ACTIVE: cloud
```

### Template List

| Template | Language | Category | Services |
|----------|----------|----------|----------|
| Spring Boot REST API | Java | Microservice | PostgreSQL |
| Spring Boot + React Full-Stack | Java + JS | Full-Stack | PostgreSQL |
| Flask REST API | Python | Microservice | PostgreSQL |
| Express.js API | Node.js | Microservice | PostgreSQL |
| Go Gin Microservice | Go | Microservice | PostgreSQL |
| Spring Cloud Gateway | Java | Infrastructure | Redis |
| Spring Boot + Spring AI Chatbot | Java | AI | PostgreSQL, GenAI |
| RabbitMQ Worker (Spring AMQP) | Java | Worker | RabbitMQ |
| CF Service Broker (OSBAPI) | Java | Infrastructure | PostgreSQL |
| Static React SPA | JavaScript | Frontend | None |
| Spring Boot + Spring AI RAG | Java | AI | PostgreSQL, GenAI, PGVector |

---

## 14. Deployment Automation

CF Forge is deployed via an orchestrated shell script (`scripts/deploy.sh`) that can be run manually from a workstation or triggered automatically via a GitHub Actions workflow. There is no dependency on BOSH releases, Tanzu Operations Manager tiles, or any tile-based packaging.

### 14.1 Deployment Configuration File

All deployment parameters are centralized in a single configuration file that the deploy script sources. Operators copy the example and fill in their environment-specific values.

```bash
# ── scripts/deploy-config.sh.example ──
# Copy to scripts/deploy-config.sh and fill in values for your foundation.
# This file is .gitignored — never commit secrets.

# ── CF Target ──
CF_API="https://api.sys.example.com"
CF_ORG="cf-forge"
CF_SPACE="production"
CF_SKIP_SSL=false                      # Set true for self-signed certs

# ── App Domains ──
CF_APPS_DOMAIN="apps.example.com"      # Route domain for external apps
CF_INTERNAL_DOMAIN="apps.internal"     # Internal route domain (C2C)

# ── GenAI on Tanzu Platform ──
GENAI_SERVICE_TYPE="genai"             # Marketplace label (or "user-provided")
GENAI_PLAN="llama3-8b"                 # Service plan name
GENAI_INSTANCE_NAME="cf-forge-genai"
# For user-provided GenAI (non-marketplace):
# GENAI_USER_PROVIDED=true
# GENAI_BASE_URL="https://genai-api.example.com/v1"
# GENAI_API_KEY="sk-xxx"
# GENAI_MODEL="llama3-8b"

# ── CF SSO (p-identity) ──
SSO_SERVICE_TYPE="p-identity"          # Marketplace label
SSO_PLAN="cf-forge-sso-plan"           # SSO plan name
SSO_INSTANCE_NAME="cf-forge-sso"
# For UAA-direct (no SSO service):
# SSO_USER_PROVIDED=true
# SSO_AUTH_DOMAIN="https://uaa.sys.example.com"
# SSO_CLIENT_ID="cf-forge-client"
# SSO_CLIENT_SECRET="xxx"

# ── PostgreSQL ──
DB_SERVICE_TYPE="postgresql"           # e.g. postgresql, p.postgresql, aws-rds
DB_PLAN="small"
DB_INSTANCE_NAME="cf-forge-db"

# ── Redis ──
REDIS_SERVICE_TYPE="p-redis"           # e.g. p-redis, p.redis, aws-elasticache
REDIS_PLAN="shared-vm"
REDIS_INSTANCE_NAME="cf-forge-cache"

# ── RabbitMQ ──
MQ_SERVICE_TYPE="p-rabbitmq"           # e.g. p-rabbitmq, p.rabbitmq
MQ_PLAN="standard"
MQ_INSTANCE_NAME="cf-forge-mq"

# ── Object Storage (user-provided S3-compatible) ──
OBJECT_STORE_ENDPOINT="https://minio.example.com"
OBJECT_STORE_ACCESS_KEY="xxx"
OBJECT_STORE_SECRET_KEY="xxx"
OBJECT_STORE_BUCKET="cf-forge-artifacts"
OBJECT_STORE_REGION="us-east-1"
OBJECT_STORE_INSTANCE_NAME="cf-forge-object-store"

# ── Resource Allocation ──
API_INSTANCES=3
API_MEMORY="1G"
AGENT_INSTANCES=2
AGENT_MEMORY="2G"
BUILDER_INSTANCES=2
BUILDER_MEMORY="2G"
WORKSPACE_INSTANCES=2
WORKSPACE_MEMORY="1G"
ADMIN_INSTANCES=2
ADMIN_MEMORY="1G"
UI_INSTANCES=2
UI_MEMORY="256M"

# ── Security ──
CVE_SCAN_ENABLED=true
CVE_BLOCK_SEVERITY="critical"          # critical, high, medium, low, none

# ── Admin Dashboard ──
ADMIN_ENABLED=true
METRIC_RETENTION_DAYS=90
HEALTH_CHECK_INTERVAL_SECONDS=60

# ── Spring Profiles ──
SPRING_PROFILES="cloud,production"

# ── Build Artifacts (paths to pre-built JARs or dist/) ──
# If empty, the script will run `mvn package` first
API_JAR=""                             # e.g. cf-forge-api/target/cf-forge-api-0.0.1-SNAPSHOT.jar
AGENT_JAR=""
BUILDER_JAR=""
WORKSPACE_JAR=""
ADMIN_JAR=""
UI_DIST=""                             # e.g. cf-forge-ui/dist/
```

### 14.2 Deploy Script

```bash
#!/usr/bin/env bash
# ── scripts/deploy.sh ──
# Orchestrated deployment of all CF Forge components to Cloud Foundry.
# Usage:
#   ./scripts/deploy.sh                    # Full deploy (services + apps)
#   ./scripts/deploy.sh --apps-only        # Skip service creation (re-deploy apps)
#   ./scripts/deploy.sh --services-only    # Create/update services only
#   ./scripts/deploy.sh --migrate-only     # Run Flyway migrations only
#   ./scripts/deploy.sh --smoke-test       # Run smoke tests only
#   ./scripts/deploy.sh --teardown         # Delete all CF Forge apps and services
#   ./scripts/deploy.sh --component api    # Deploy a single component

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="${SCRIPT_DIR}/deploy-config.sh"

# ── Color output ──
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log()  { echo -e "${BLUE}[CF Forge]${NC} $1"; }
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1" >&2; }

# ── Load configuration ──
if [[ ! -f "$CONFIG_FILE" ]]; then
    err "Config file not found: $CONFIG_FILE"
    err "Copy scripts/deploy-config.sh.example → scripts/deploy-config.sh and fill in values."
    exit 1
fi
source "$CONFIG_FILE"

# ── Parse arguments ──
ACTION="full"
COMPONENT=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --apps-only)     ACTION="apps" ;;
        --services-only) ACTION="services" ;;
        --migrate-only)  ACTION="migrate" ;;
        --smoke-test)    ACTION="smoke" ;;
        --teardown)      ACTION="teardown" ;;
        --component)     ACTION="component"; COMPONENT="$2"; shift ;;
        --help|-h)
            echo "Usage: $0 [--apps-only|--services-only|--migrate-only|--smoke-test|--teardown|--component NAME]"
            exit 0 ;;
        *) err "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

# ── Preflight checks ──
preflight() {
    log "Running preflight checks..."
    command -v cf >/dev/null 2>&1 || { err "cf CLI not found. Install: https://docs.cloudfoundry.org/cf-cli/install-go-cli.html"; exit 1; }
    command -v jq >/dev/null 2>&1 || { err "jq not found. Install: https://jqlang.github.io/jq/download/"; exit 1; }

    # Verify CF login
    if ! cf target >/dev/null 2>&1; then
        log "Logging into CF..."
        if [[ "${CF_SKIP_SSL}" == "true" ]]; then
            cf api "$CF_API" --skip-ssl-validation
        else
            cf api "$CF_API"
        fi
        cf auth  # Uses CF_USERNAME/CF_PASSWORD env vars, or interactive login
    fi

    # Target org/space (create if needed)
    cf target -o "$CF_ORG" -s "$CF_SPACE" 2>/dev/null || {
        log "Creating org and space..."
        cf create-org "$CF_ORG" 2>/dev/null || true
        cf create-space "$CF_SPACE" -o "$CF_ORG" 2>/dev/null || true
        cf target -o "$CF_ORG" -s "$CF_SPACE"
    }
    ok "CF target: $CF_API → $CF_ORG / $CF_SPACE"
}

# ── Build (if JARs not pre-built) ──
build_all() {
    if [[ -z "${API_JAR}" ]]; then
        log "Building all Spring Boot modules (mvn package -DskipTests)..."
        cd "$PROJECT_ROOT"
        ./mvnw -q package -DskipTests -pl cf-forge-common,cf-forge-api,cf-forge-agent,cf-forge-builder,cf-forge-workspace,cf-forge-admin
        API_JAR="cf-forge-api/target/cf-forge-api-0.0.1-SNAPSHOT.jar"
        AGENT_JAR="cf-forge-agent/target/cf-forge-agent-0.0.1-SNAPSHOT.jar"
        BUILDER_JAR="cf-forge-builder/target/cf-forge-builder-0.0.1-SNAPSHOT.jar"
        WORKSPACE_JAR="cf-forge-workspace/target/cf-forge-workspace-0.0.1-SNAPSHOT.jar"
        ADMIN_JAR="cf-forge-admin/target/cf-forge-admin-0.0.1-SNAPSHOT.jar"
        ok "Maven build complete"
    fi
    if [[ -z "${UI_DIST}" ]]; then
        log "Building frontend (npm run build)..."
        cd "$PROJECT_ROOT/cf-forge-ui"
        npm ci --silent && npm run build
        UI_DIST="cf-forge-ui/dist/"
        ok "Frontend build complete"
    fi
}

# ── Service creation ──
create_services() {
    log "Creating marketplace services..."

    # PostgreSQL
    cf service "$DB_INSTANCE_NAME" >/dev/null 2>&1 || {
        cf create-service "$DB_SERVICE_TYPE" "$DB_PLAN" "$DB_INSTANCE_NAME"
        log "Waiting for $DB_INSTANCE_NAME to provision..."
        until cf service "$DB_INSTANCE_NAME" | grep -q "succeeded"; do sleep 5; done
    }
    ok "PostgreSQL: $DB_INSTANCE_NAME"

    # Redis
    cf service "$REDIS_INSTANCE_NAME" >/dev/null 2>&1 || {
        cf create-service "$REDIS_SERVICE_TYPE" "$REDIS_PLAN" "$REDIS_INSTANCE_NAME"
        until cf service "$REDIS_INSTANCE_NAME" | grep -q "succeeded"; do sleep 5; done
    }
    ok "Redis: $REDIS_INSTANCE_NAME"

    # RabbitMQ
    cf service "$MQ_INSTANCE_NAME" >/dev/null 2>&1 || {
        cf create-service "$MQ_SERVICE_TYPE" "$MQ_PLAN" "$MQ_INSTANCE_NAME"
        until cf service "$MQ_INSTANCE_NAME" | grep -q "succeeded"; do sleep 5; done
    }
    ok "RabbitMQ: $MQ_INSTANCE_NAME"

    # GenAI on Tanzu Platform
    if [[ "${GENAI_USER_PROVIDED:-false}" == "true" ]]; then
        cf service "$GENAI_INSTANCE_NAME" >/dev/null 2>&1 || \
            cf create-user-provided-service "$GENAI_INSTANCE_NAME" -p "{
                \"api_base\": \"${GENAI_BASE_URL}\",
                \"api_key\": \"${GENAI_API_KEY}\",
                \"model\": \"${GENAI_MODEL}\",
                \"wire_format\": \"openai\"
            }"
    else
        cf service "$GENAI_INSTANCE_NAME" >/dev/null 2>&1 || {
            cf create-service "$GENAI_SERVICE_TYPE" "$GENAI_PLAN" "$GENAI_INSTANCE_NAME"
            until cf service "$GENAI_INSTANCE_NAME" | grep -q "succeeded"; do sleep 5; done
        }
    fi
    ok "GenAI: $GENAI_INSTANCE_NAME"

    # CF SSO (p-identity)
    if [[ "${SSO_USER_PROVIDED:-false}" == "true" ]]; then
        cf service "$SSO_INSTANCE_NAME" >/dev/null 2>&1 || \
            cf create-user-provided-service "$SSO_INSTANCE_NAME" -p "{
                \"auth_domain\": \"${SSO_AUTH_DOMAIN}\",
                \"client_id\": \"${SSO_CLIENT_ID}\",
                \"client_secret\": \"${SSO_CLIENT_SECRET}\"
            }"
    else
        cf service "$SSO_INSTANCE_NAME" >/dev/null 2>&1 || {
            cf create-service "$SSO_SERVICE_TYPE" "$SSO_PLAN" "$SSO_INSTANCE_NAME"
            until cf service "$SSO_INSTANCE_NAME" | grep -q "succeeded"; do sleep 5; done
        }
    fi
    ok "SSO: $SSO_INSTANCE_NAME"

    # Object Storage (always user-provided / S3-compatible)
    cf service "$OBJECT_STORE_INSTANCE_NAME" >/dev/null 2>&1 || \
        cf create-user-provided-service "$OBJECT_STORE_INSTANCE_NAME" -p "{
            \"endpoint\": \"${OBJECT_STORE_ENDPOINT}\",
            \"access_key\": \"${OBJECT_STORE_ACCESS_KEY}\",
            \"secret_key\": \"${OBJECT_STORE_SECRET_KEY}\",
            \"bucket\": \"${OBJECT_STORE_BUCKET}\",
            \"region\": \"${OBJECT_STORE_REGION}\"
        }"
    ok "Object Storage: $OBJECT_STORE_INSTANCE_NAME"

    log "All services ready."
}

# ── App deployment ──
deploy_app() {
    local name=$1 jar=$2 memory=$3 instances=$4 services=$5 routes=$6 extra_env=${7:-}

    log "Deploying $name..."
    local push_args=(
        "$name"
        -p "$jar"
        -m "$memory"
        -i "$instances"
        -b java_buildpack_offline
        --health-check-type http
        --health-check-http-endpoint /actuator/health
        --no-start
    )

    cf push "${push_args[@]}"

    # Bind services
    IFS=',' read -ra SVC_LIST <<< "$services"
    for svc in "${SVC_LIST[@]}"; do
        cf bind-service "$name" "$(echo "$svc" | xargs)" 2>/dev/null || true
    done

    # Set routes
    if [[ "$routes" == "internal" ]]; then
        cf unmap-route "$name" "$CF_APPS_DOMAIN" --hostname "$name" 2>/dev/null || true
        cf map-route "$name" "$CF_INTERNAL_DOMAIN" --hostname "$name" 2>/dev/null || true
    fi

    # Set environment
    cf set-env "$name" JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 21.+ } }'
    cf set-env "$name" SPRING_PROFILES_ACTIVE "$SPRING_PROFILES"
    cf set-env "$name" JAVA_OPTS "-Djava.security.egd=file:///dev/urandom"

    if [[ -n "$extra_env" ]]; then
        eval "$extra_env"
    fi

    cf start "$name"
    ok "$name deployed (${instances}x${memory})"
}

deploy_ui() {
    log "Deploying cf-forge-ui..."
    cf push cf-forge-ui \
        -p "$PROJECT_ROOT/$UI_DIST" \
        -m "$UI_MEMORY" \
        -i "$UI_INSTANCES" \
        -b staticfile_buildpack \
        --health-check-type http \
        --health-check-http-endpoint /
    cf map-route cf-forge-ui "$CF_APPS_DOMAIN" --hostname forge 2>/dev/null || true
    ok "cf-forge-ui deployed"
}

deploy_all_apps() {
    # 1. API Gateway (external route)
    deploy_app "cf-forge-api" "$PROJECT_ROOT/$API_JAR" "$API_MEMORY" "$API_INSTANCES" \
        "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$MQ_INSTANCE_NAME,$SSO_INSTANCE_NAME" \
        "external"

    # 2. AI Agent (internal route)
    deploy_app "cf-forge-agent" "$PROJECT_ROOT/$AGENT_JAR" "$AGENT_MEMORY" "$AGENT_INSTANCES" \
        "$DB_INSTANCE_NAME,$MQ_INSTANCE_NAME,$GENAI_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" \
        "internal"

    # 3. Builder (internal route)
    deploy_app "cf-forge-builder" "$PROJECT_ROOT/$BUILDER_JAR" "$BUILDER_MEMORY" "$BUILDER_INSTANCES" \
        "$DB_INSTANCE_NAME,$MQ_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" \
        "internal"

    # 4. Workspace (internal route)
    deploy_app "cf-forge-workspace" "$PROJECT_ROOT/$WORKSPACE_JAR" "$WORKSPACE_MEMORY" "$WORKSPACE_INSTANCES" \
        "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" \
        "internal"

    # 5. Admin Dashboard (external route)
    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        deploy_app "cf-forge-admin" "$PROJECT_ROOT/$ADMIN_JAR" "$ADMIN_MEMORY" "$ADMIN_INSTANCES" \
            "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$MQ_INSTANCE_NAME,$SSO_INSTANCE_NAME" \
            "external" \
            "cf set-env cf-forge-admin CFFORGE_ADMIN_COMPONENTS 'cf-forge-api,cf-forge-agent,cf-forge-builder,cf-forge-workspace';
             cf set-env cf-forge-admin CFFORGE_HEALTH_CHECK_INTERVAL_SECONDS '${HEALTH_CHECK_INTERVAL_SECONDS}';"
    fi

    # 6. UI (external route, staticfile buildpack)
    deploy_ui

    # Set up C2C network policies
    log "Configuring network policies..."
    for internal_app in cf-forge-agent cf-forge-builder cf-forge-workspace; do
        cf add-network-policy cf-forge-api "$internal_app" --port 8080 --protocol tcp 2>/dev/null || true
    done
    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        for internal_app in cf-forge-agent cf-forge-builder cf-forge-workspace; do
            cf add-network-policy cf-forge-admin "$internal_app" --port 8080 --protocol tcp 2>/dev/null || true
        done
    fi
    ok "Network policies configured"
}

# ── Post-deploy tasks ──
run_migrations() {
    log "Running Flyway database migrations..."
    cf run-task cf-forge-api \
        --command "java -cp /home/vcap/app/BOOT-INF/classes:/home/vcap/app/BOOT-INF/lib/* org.flywaydb.commandline.Main migrate" \
        --name flyway-migrate \
        -m 512M
    ok "Migrations submitted (check: cf tasks cf-forge-api)"
}

ingest_docs() {
    log "Ingesting CF documentation into PGVector for RAG..."
    cf run-task cf-forge-agent \
        --command "java -jar app.jar --spring.main.web-application-type=none --ingest-docs" \
        --name ingest-docs \
        -m 1G
    ok "Doc ingestion submitted (check: cf tasks cf-forge-agent)"
}

# ── Smoke tests ──
smoke_test() {
    log "Running smoke tests..."
    local failed=0

    for endpoint in \
        "https://forge-api.${CF_APPS_DOMAIN}/actuator/health" \
        "https://forge.${CF_APPS_DOMAIN}/"
    do
        if curl -sf --max-time 10 "$endpoint" >/dev/null 2>&1; then
            ok "  $endpoint"
        else
            err "  $endpoint FAILED"
            failed=1
        fi
    done

    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        if curl -sf --max-time 10 "https://forge-admin.${CF_APPS_DOMAIN}/actuator/health" >/dev/null 2>&1; then
            ok "  https://forge-admin.${CF_APPS_DOMAIN}/actuator/health"
        else
            err "  Admin health check FAILED"
            failed=1
        fi
    fi

    # Check internal services health via API actuator
    local api_health
    api_health=$(curl -sf "https://forge-api.${CF_APPS_DOMAIN}/actuator/health" 2>/dev/null || echo '{"status":"DOWN"}')
    local status
    status=$(echo "$api_health" | jq -r '.status')
    if [[ "$status" == "UP" ]]; then
        ok "API health: UP"
    else
        err "API health: $status"
        failed=1
    fi

    if [[ $failed -eq 0 ]]; then
        ok "All smoke tests passed!"
    else
        err "Some smoke tests failed."
        exit 1
    fi
}

# ── Teardown ──
teardown() {
    warn "This will DELETE all CF Forge apps and services in $CF_ORG/$CF_SPACE."
    read -rp "Type 'yes' to confirm: " confirm
    [[ "$confirm" != "yes" ]] && { log "Aborted."; exit 0; }

    for app in cf-forge-ui cf-forge-admin cf-forge-workspace cf-forge-builder cf-forge-agent cf-forge-api; do
        cf delete "$app" -f -r 2>/dev/null || true
    done
    for svc in "$OBJECT_STORE_INSTANCE_NAME" "$SSO_INSTANCE_NAME" "$GENAI_INSTANCE_NAME" \
               "$MQ_INSTANCE_NAME" "$REDIS_INSTANCE_NAME" "$DB_INSTANCE_NAME"; do
        cf delete-service "$svc" -f 2>/dev/null || true
    done
    ok "Teardown complete."
}

# ── Main ──
preflight

case "$ACTION" in
    full)
        build_all
        create_services
        deploy_all_apps
        run_migrations
        smoke_test
        log "🎉 CF Forge deployed successfully!"
        log "  UI:    https://forge.${CF_APPS_DOMAIN}"
        log "  API:   https://forge-api.${CF_APPS_DOMAIN}"
        log "  Admin: https://forge-admin.${CF_APPS_DOMAIN}"
        ;;
    apps)      build_all; deploy_all_apps ;;
    services)  create_services ;;
    migrate)   run_migrations ;;
    smoke)     smoke_test ;;
    teardown)  teardown ;;
    component)
        build_all
        case "$COMPONENT" in
            api)       deploy_app "cf-forge-api" "$PROJECT_ROOT/$API_JAR" "$API_MEMORY" "$API_INSTANCES" \
                           "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$MQ_INSTANCE_NAME,$SSO_INSTANCE_NAME" "external" ;;
            agent)     deploy_app "cf-forge-agent" "$PROJECT_ROOT/$AGENT_JAR" "$AGENT_MEMORY" "$AGENT_INSTANCES" \
                           "$DB_INSTANCE_NAME,$MQ_INSTANCE_NAME,$GENAI_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" "internal" ;;
            builder)   deploy_app "cf-forge-builder" "$PROJECT_ROOT/$BUILDER_JAR" "$BUILDER_MEMORY" "$BUILDER_INSTANCES" \
                           "$DB_INSTANCE_NAME,$MQ_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" "internal" ;;
            workspace) deploy_app "cf-forge-workspace" "$PROJECT_ROOT/$WORKSPACE_JAR" "$WORKSPACE_MEMORY" "$WORKSPACE_INSTANCES" \
                           "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$OBJECT_STORE_INSTANCE_NAME" "internal" ;;
            admin)     deploy_app "cf-forge-admin" "$PROJECT_ROOT/$ADMIN_JAR" "$ADMIN_MEMORY" "$ADMIN_INSTANCES" \
                           "$DB_INSTANCE_NAME,$REDIS_INSTANCE_NAME,$MQ_INSTANCE_NAME,$SSO_INSTANCE_NAME" "external" ;;
            ui)        deploy_ui ;;
            *) err "Unknown component: $COMPONENT"; exit 1 ;;
        esac
        ;;
esac
```

### 14.3 GitHub Actions Workflow

```yaml
# ── .github/workflows/deploy.yml ──
name: Deploy CF Forge

on:
  workflow_dispatch:
    inputs:
      environment:
        description: "Target environment"
        required: true
        type: choice
        options: [staging, production]
      action:
        description: "Deploy action"
        required: true
        type: choice
        options: [full, apps-only, services-only, migrate-only, smoke-test]
        default: full
      component:
        description: "Single component (leave empty for all)"
        required: false
        type: choice
        options: ["", api, agent, builder, workspace, admin, ui]
  push:
    branches: [main]
    paths:
      - "cf-forge-*/src/**"
      - "cf-forge-ui/src/**"
      - "pom.xml"

concurrency:
  group: deploy-${{ github.event.inputs.environment || 'staging' }}
  cancel-in-progress: false

env:
  JAVA_VERSION: "21"
  NODE_VERSION: "20"

jobs:
  build:
    name: Build Artifacts
    runs-on: ubuntu-latest
    if: github.event.inputs.action != 'smoke-test' && github.event.inputs.action != 'services-only'
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven

      - name: Build Spring Boot modules
        run: ./mvnw -q package -DskipTests

      - name: Set up Node.js ${{ env.NODE_VERSION }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: npm
          cache-dependency-path: cf-forge-ui/package-lock.json

      - name: Build frontend
        working-directory: cf-forge-ui
        run: npm ci && npm run build

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: cf-forge-artifacts
          retention-days: 1
          path: |
            cf-forge-api/target/*.jar
            cf-forge-agent/target/*.jar
            cf-forge-builder/target/*.jar
            cf-forge-workspace/target/*.jar
            cf-forge-admin/target/*.jar
            cf-forge-ui/dist/

  test:
    name: Run Tests
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin
          cache: maven

      - name: Run unit tests
        run: ./mvnw verify

      - name: Publish test results
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: "**/target/surefire-reports/TEST-*.xml"
          reporter: java-junit

  deploy:
    name: Deploy to ${{ github.event.inputs.environment || 'staging' }}
    runs-on: ubuntu-latest
    needs: [build]
    if: always() && (needs.build.result == 'success' || needs.build.result == 'skipped')
    environment:
      name: ${{ github.event.inputs.environment || 'staging' }}
      url: https://forge.${{ vars.CF_APPS_DOMAIN }}

    steps:
      - uses: actions/checkout@v4

      - name: Download build artifacts
        if: github.event.inputs.action != 'smoke-test' && github.event.inputs.action != 'services-only'
        uses: actions/download-artifact@v4
        with:
          name: cf-forge-artifacts

      - name: Install CF CLI
        run: |
          wget -q -O cf-cli.deb "https://packages.cloudfoundry.org/stable?release=debian64&version=v8&source=github"
          sudo dpkg -i cf-cli.deb

      - name: Write deploy config
        run: |
          cat > scripts/deploy-config.sh << 'DEPLOY_EOF'
          CF_API="${{ vars.CF_API }}"
          CF_ORG="${{ vars.CF_ORG }}"
          CF_SPACE="${{ vars.CF_SPACE }}"
          CF_SKIP_SSL=${{ vars.CF_SKIP_SSL || 'false' }}
          CF_APPS_DOMAIN="${{ vars.CF_APPS_DOMAIN }}"
          CF_INTERNAL_DOMAIN="${{ vars.CF_INTERNAL_DOMAIN || 'apps.internal' }}"

          GENAI_SERVICE_TYPE="${{ vars.GENAI_SERVICE_TYPE }}"
          GENAI_PLAN="${{ vars.GENAI_PLAN }}"
          GENAI_INSTANCE_NAME="${{ vars.GENAI_INSTANCE_NAME || 'cf-forge-genai' }}"
          GENAI_USER_PROVIDED="${{ vars.GENAI_USER_PROVIDED || 'false' }}"
          GENAI_BASE_URL="${{ secrets.GENAI_BASE_URL || '' }}"
          GENAI_API_KEY="${{ secrets.GENAI_API_KEY || '' }}"
          GENAI_MODEL="${{ vars.GENAI_MODEL || '' }}"

          SSO_SERVICE_TYPE="${{ vars.SSO_SERVICE_TYPE }}"
          SSO_PLAN="${{ vars.SSO_PLAN }}"
          SSO_INSTANCE_NAME="${{ vars.SSO_INSTANCE_NAME || 'cf-forge-sso' }}"
          SSO_USER_PROVIDED="${{ vars.SSO_USER_PROVIDED || 'false' }}"
          SSO_AUTH_DOMAIN="${{ secrets.SSO_AUTH_DOMAIN || '' }}"
          SSO_CLIENT_ID="${{ secrets.SSO_CLIENT_ID || '' }}"
          SSO_CLIENT_SECRET="${{ secrets.SSO_CLIENT_SECRET || '' }}"

          DB_SERVICE_TYPE="${{ vars.DB_SERVICE_TYPE }}"
          DB_PLAN="${{ vars.DB_PLAN }}"
          DB_INSTANCE_NAME="${{ vars.DB_INSTANCE_NAME || 'cf-forge-db' }}"
          REDIS_SERVICE_TYPE="${{ vars.REDIS_SERVICE_TYPE }}"
          REDIS_PLAN="${{ vars.REDIS_PLAN }}"
          REDIS_INSTANCE_NAME="${{ vars.REDIS_INSTANCE_NAME || 'cf-forge-cache' }}"
          MQ_SERVICE_TYPE="${{ vars.MQ_SERVICE_TYPE }}"
          MQ_PLAN="${{ vars.MQ_PLAN }}"
          MQ_INSTANCE_NAME="${{ vars.MQ_INSTANCE_NAME || 'cf-forge-mq' }}"

          OBJECT_STORE_ENDPOINT="${{ secrets.OBJECT_STORE_ENDPOINT }}"
          OBJECT_STORE_ACCESS_KEY="${{ secrets.OBJECT_STORE_ACCESS_KEY }}"
          OBJECT_STORE_SECRET_KEY="${{ secrets.OBJECT_STORE_SECRET_KEY }}"
          OBJECT_STORE_BUCKET="${{ vars.OBJECT_STORE_BUCKET || 'cf-forge-artifacts' }}"
          OBJECT_STORE_REGION="${{ vars.OBJECT_STORE_REGION || 'us-east-1' }}"
          OBJECT_STORE_INSTANCE_NAME="${{ vars.OBJECT_STORE_INSTANCE_NAME || 'cf-forge-object-store' }}"

          API_INSTANCES=${{ vars.API_INSTANCES || 3 }}
          API_MEMORY="${{ vars.API_MEMORY || '1G' }}"
          AGENT_INSTANCES=${{ vars.AGENT_INSTANCES || 2 }}
          AGENT_MEMORY="${{ vars.AGENT_MEMORY || '2G' }}"
          BUILDER_INSTANCES=${{ vars.BUILDER_INSTANCES || 2 }}
          BUILDER_MEMORY="${{ vars.BUILDER_MEMORY || '2G' }}"
          WORKSPACE_INSTANCES=${{ vars.WORKSPACE_INSTANCES || 2 }}
          WORKSPACE_MEMORY="${{ vars.WORKSPACE_MEMORY || '1G' }}"
          ADMIN_INSTANCES=${{ vars.ADMIN_INSTANCES || 2 }}
          ADMIN_MEMORY="${{ vars.ADMIN_MEMORY || '1G' }}"
          UI_INSTANCES=${{ vars.UI_INSTANCES || 2 }}
          UI_MEMORY="${{ vars.UI_MEMORY || '256M' }}"

          CVE_SCAN_ENABLED=${{ vars.CVE_SCAN_ENABLED || 'true' }}
          CVE_BLOCK_SEVERITY="${{ vars.CVE_BLOCK_SEVERITY || 'critical' }}"
          ADMIN_ENABLED=${{ vars.ADMIN_ENABLED || 'true' }}
          METRIC_RETENTION_DAYS=${{ vars.METRIC_RETENTION_DAYS || 90 }}
          HEALTH_CHECK_INTERVAL_SECONDS=${{ vars.HEALTH_CHECK_INTERVAL_SECONDS || 60 }}

          SPRING_PROFILES="${{ vars.SPRING_PROFILES || 'cloud,production' }}"

          # Pre-built artifacts (downloaded by actions/download-artifact)
          API_JAR="cf-forge-api/target/cf-forge-api-0.0.1-SNAPSHOT.jar"
          AGENT_JAR="cf-forge-agent/target/cf-forge-agent-0.0.1-SNAPSHOT.jar"
          BUILDER_JAR="cf-forge-builder/target/cf-forge-builder-0.0.1-SNAPSHOT.jar"
          WORKSPACE_JAR="cf-forge-workspace/target/cf-forge-workspace-0.0.1-SNAPSHOT.jar"
          ADMIN_JAR="cf-forge-admin/target/cf-forge-admin-0.0.1-SNAPSHOT.jar"
          UI_DIST="cf-forge-ui/dist/"
          DEPLOY_EOF

      - name: CF Login
        run: |
          cf api "${{ vars.CF_API }}" ${{ vars.CF_SKIP_SSL == 'true' && '--skip-ssl-validation' || '' }}
          cf auth "${{ secrets.CF_USERNAME }}" "${{ secrets.CF_PASSWORD }}"
          cf target -o "${{ vars.CF_ORG }}" -s "${{ vars.CF_SPACE }}"

      - name: Deploy
        run: |
          ACTION="${{ github.event.inputs.action || 'full' }}"
          COMPONENT="${{ github.event.inputs.component }}"

          if [[ -n "$COMPONENT" ]]; then
            bash scripts/deploy.sh --component "$COMPONENT"
          elif [[ "$ACTION" == "full" ]]; then
            bash scripts/deploy.sh
          else
            bash scripts/deploy.sh "--${ACTION//-only/}-only" 2>/dev/null || \
            bash scripts/deploy.sh "--${ACTION}"
          fi

      - name: Smoke Test
        if: github.event.inputs.action == 'full' || github.event.inputs.action == 'apps-only'
        run: bash scripts/deploy.sh --smoke-test

      - name: Post deploy summary
        if: always()
        run: |
          echo "### CF Forge Deployment Summary" >> $GITHUB_STEP_SUMMARY
          echo "| Property | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|----------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| Environment | ${{ github.event.inputs.environment || 'staging' }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Action | ${{ github.event.inputs.action || 'full' }} |" >> $GITHUB_STEP_SUMMARY
          echo "| CF API | ${{ vars.CF_API }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Org/Space | ${{ vars.CF_ORG }} / ${{ vars.CF_SPACE }} |" >> $GITHUB_STEP_SUMMARY
          echo "| UI URL | https://forge.${{ vars.CF_APPS_DOMAIN }} |" >> $GITHUB_STEP_SUMMARY
          echo "| API URL | https://forge-api.${{ vars.CF_APPS_DOMAIN }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Admin URL | https://forge-admin.${{ vars.CF_APPS_DOMAIN }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Commit | ${{ github.sha }} |" >> $GITHUB_STEP_SUMMARY
```

### 14.4 GitHub Action Environment Configuration

Configure these in GitHub → Repository Settings → Environments (create `staging` and `production`):

**Variables (non-secret):**

| Variable | Example | Required |
|----------|---------|----------|
| `CF_API` | `https://api.sys.example.com` | ✅ |
| `CF_ORG` | `cf-forge` | ✅ |
| `CF_SPACE` | `production` | ✅ |
| `CF_APPS_DOMAIN` | `apps.example.com` | ✅ |
| `CF_INTERNAL_DOMAIN` | `apps.internal` | ✅ |
| `CF_SKIP_SSL` | `false` | |
| `DB_SERVICE_TYPE` | `postgresql` | ✅ |
| `DB_PLAN` | `small` | ✅ |
| `REDIS_SERVICE_TYPE` | `p-redis` | ✅ |
| `REDIS_PLAN` | `shared-vm` | ✅ |
| `MQ_SERVICE_TYPE` | `p-rabbitmq` | ✅ |
| `MQ_PLAN` | `standard` | ✅ |
| `GENAI_SERVICE_TYPE` | `genai` | ✅ |
| `GENAI_PLAN` | `llama3-8b` | ✅ |
| `SSO_SERVICE_TYPE` | `p-identity` | ✅ |
| `SSO_PLAN` | `cf-forge-sso-plan` | ✅ |
| `API_INSTANCES` | `3` | |
| `AGENT_INSTANCES` | `2` | |
| `ADMIN_ENABLED` | `true` | |
| `SPRING_PROFILES` | `cloud,production` | |

**Secrets:**

| Secret | Description | Required |
|--------|-------------|----------|
| `CF_USERNAME` | CF API username (or client ID) | ✅ |
| `CF_PASSWORD` | CF API password (or client secret) | ✅ |
| `OBJECT_STORE_ENDPOINT` | S3-compatible endpoint URL | ✅ |
| `OBJECT_STORE_ACCESS_KEY` | S3 access key | ✅ |
| `OBJECT_STORE_SECRET_KEY` | S3 secret key | ✅ |
| `GENAI_BASE_URL` | GenAI API URL (only if user-provided) | |
| `GENAI_API_KEY` | GenAI API key (only if user-provided) | |
| `SSO_AUTH_DOMAIN` | SSO auth domain (only if user-provided) | |
| `SSO_CLIENT_ID` | SSO client ID (only if user-provided) | |
| `SSO_CLIENT_SECRET` | SSO client secret (only if user-provided) | |

### 14.5 Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/your-org/cf-forge.git && cd cf-forge

# 2. Copy and configure deployment settings
cp scripts/deploy-config.sh.example scripts/deploy-config.sh
$EDITOR scripts/deploy-config.sh    # Fill in your CF foundation values

# 3. Login to CF (if not already)
cf login -a https://api.sys.example.com --sso

# 4. Full deploy (build + services + apps + migrations + smoke test)
./scripts/deploy.sh

# 5. Re-deploy a single component after code changes
./scripts/deploy.sh --component agent

# 6. View status
cf apps                              # All CF Forge apps
cf services                          # All bound services
curl https://forge-api.apps.example.com/actuator/health | jq
```

---

## 15. API Specification

### REST API Routes (Spring @RestController)

```
BASE: /api/v1

# ─── Authentication ───
POST   /auth/login                    → Initiate OAuth2 flow
GET    /auth/callback                 → OAuth2 callback
POST   /auth/refresh                  → Refresh token
GET    /auth/me                       → Current user + CF roles

# ─── CF Targets ───
GET    /cf-targets                    → List user's CF targets
POST   /cf-targets                    → Add CF target
PUT    /cf-targets/{id}               → Update CF target
DELETE /cf-targets/{id}               → Remove CF target
GET    /cf-targets/{id}/info          → CF foundation info (buildpacks, services)

# ─── Projects ───
GET    /projects                      → List projects
POST   /projects                      → Create project (blank or from template)
GET    /projects/{id}                 → Get project details
PUT    /projects/{id}                 → Update project
DELETE /projects/{id}                 → Delete project + CF app

# ─── Conversations (Spring AI Agent) ───
POST   /projects/{id}/conversations                → Start conversation
GET    /projects/{id}/conversations                → List conversations
GET    /projects/{id}/conversations/{cid}          → Get conversation + messages
POST   /projects/{id}/conversations/{cid}/messages → Send message (SSE stream response)

# ─── Workspace / Files ───
GET    /projects/{id}/files              → List files
GET    /projects/{id}/files/**           → Read file
PUT    /projects/{id}/files/**           → Write file
DELETE /projects/{id}/files/**           → Delete file

# ─── Checkpoints ───
GET    /projects/{id}/checkpoints                → List
POST   /projects/{id}/checkpoints                → Create
POST   /projects/{id}/checkpoints/{cid}/restore  → Restore

# ─── Builds ───
POST   /projects/{id}/builds              → Trigger build
GET    /projects/{id}/builds              → List builds
GET    /projects/{id}/builds/{bid}        → Build details + log
GET    /projects/{id}/builds/{bid}/sbom   → Download SBOM

# ─── Deployments ───
POST   /projects/{id}/deployments              → Deploy
GET    /projects/{id}/deployments              → List deployments
POST   /projects/{id}/deployments/{did}/rollback → Rollback

# ─── CF Services ───
GET    /marketplace                       → List marketplace services
POST   /projects/{id}/services            → Create + bind service
GET    /projects/{id}/services            → List bound services
GET    /projects/{id}/services/vcap       → Parsed VCAP_SERVICES

# ─── Templates ───
GET    /templates                         → List templates
GET    /templates/{slug}                  → Template details
POST   /templates/{slug}/create-project   → Create from template

# ─── Admin (cf-forge-admin service — requires cfforge.admin scope) ───
GET    /admin/overview                → System overview (active users, deploys today, health)
GET    /admin/users                   → User metrics (paginated, filterable)
GET    /admin/users/{id}/activity     → User activity timeline
GET    /admin/deployments             → Deployment metrics (success rate, by strategy/language)
GET    /admin/builds                  → Build metrics (success rate, CVE findings, duration)
GET    /admin/agent                   → AI agent metrics (prompts/day, latency, tool usage)
GET    /admin/genai                   → GenAI on Tanzu Platform usage (latency, queue depth)
GET    /admin/health                  → All component health (calls each actuator endpoint)
GET    /admin/health/history          → Component health over time (uptime/SLA tracking)
GET    /admin/audit                   → Searchable, paginated audit log
GET    /admin/audit/export            → Export audit log (CSV or JSON)
GET    /admin/metrics/timeseries      → Generic time-series query (metric, granularity, range)
```

---

## 16. Configuration & Environment

### CF Manifests for All Spring Boot Components

```yaml
# ── cf-forge-api/manifest.yml ──
applications:
  - name: cf-forge-api
    memory: 1G
    instances: 3
    buildpack: java_buildpack_offline
    path: target/cf-forge-api-0.0.1-SNAPSHOT.jar
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    services:
      - cf-forge-db
      - cf-forge-cache
      - cf-forge-mq
      - cf-forge-sso           # CF SSO (p-identity)
    routes:
      - route: forge-api.apps.example.com
    env:
      JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
      SPRING_PROFILES_ACTIVE: cloud
      JAVA_OPTS: "-Djava.security.egd=file:///dev/urandom"

# ── cf-forge-agent/manifest.yml ──
applications:
  - name: cf-forge-agent
    memory: 2G
    instances: 2
    buildpack: java_buildpack_offline
    path: target/cf-forge-agent-0.0.1-SNAPSHOT.jar
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    no-route: true
    services:
      - cf-forge-db
      - cf-forge-mq
      - cf-forge-genai          # GenAI on Tanzu Platform binding
      - cf-forge-object-store
    routes:
      - route: cf-forge-agent.apps.internal
    env:
      JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
      SPRING_PROFILES_ACTIVE: cloud
      JAVA_OPTS: "-Xmx1536m -Djava.security.egd=file:///dev/urandom"

# ── cf-forge-builder/manifest.yml ──
applications:
  - name: cf-forge-builder
    memory: 2G
    instances: 2
    buildpack: java_buildpack_offline
    path: target/cf-forge-builder-0.0.1-SNAPSHOT.jar
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    no-route: true
    services:
      - cf-forge-db
      - cf-forge-mq
      - cf-forge-object-store
    routes:
      - route: cf-forge-builder.apps.internal
    env:
      JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
      SPRING_PROFILES_ACTIVE: cloud

# ── cf-forge-workspace/manifest.yml ──
applications:
  - name: cf-forge-workspace
    memory: 1G
    instances: 2
    buildpack: java_buildpack_offline
    path: target/cf-forge-workspace-0.0.1-SNAPSHOT.jar
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    no-route: true
    services:
      - cf-forge-db
      - cf-forge-cache
      - cf-forge-object-store
    routes:
      - route: cf-forge-workspace.apps.internal
    env:
      JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
      SPRING_PROFILES_ACTIVE: cloud

# ── cf-forge-admin/manifest.yml ──
applications:
  - name: cf-forge-admin
    memory: 1G
    instances: 2
    buildpack: java_buildpack_offline
    path: target/cf-forge-admin-0.0.1-SNAPSHOT.jar
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    services:
      - cf-forge-db
      - cf-forge-cache
      - cf-forge-mq
      - cf-forge-sso           # CF SSO (p-identity)
    routes:
      - route: forge-admin.apps.example.com
    env:
      JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"
      SPRING_PROFILES_ACTIVE: cloud
      CFFORGE_ADMIN_COMPONENTS: cf-forge-api,cf-forge-agent,cf-forge-builder,cf-forge-workspace
      CFFORGE_HEALTH_CHECK_INTERVAL_SECONDS: 60

# ── cf-forge-ui/manifest.yml ──
applications:
  - name: cf-forge-ui
    memory: 256M
    instances: 2
    buildpack: staticfile_buildpack
    path: dist/
    health-check-type: http
    health-check-http-endpoint: /
    routes:
      - route: forge.apps.example.com
```

### Services Creation Script

```bash
#!/bin/bash
# scripts/create-services.sh

# PostgreSQL
cf create-service postgresql small cf-forge-db

# Redis
cf create-service p-redis shared-vm cf-forge-cache

# RabbitMQ
cf create-service p-rabbitmq standard cf-forge-mq

# GenAI on Tanzu Platform (THE LLM PROVIDER)
cf create-service genai llama3-8b cf-forge-genai

# CF SSO (Single Sign-On for VMware Tanzu)
cf create-service p-identity cf-forge-sso-plan cf-forge-sso

# Object Storage (CUPS for S3-compatible)
cf create-user-provided-service cf-forge-object-store -p '{
  "endpoint": "https://minio.example.com",
  "access_key": "xxx",
  "secret_key": "xxx",
  "bucket": "cf-forge-artifacts",
  "region": "us-east-1"
}'
```

---

## 17. Security Requirements

### Security Checklist

- [ ] All API endpoints require valid UAA JWT (Spring Security OAuth2 Resource Server)
- [ ] CF API calls use user's token (token passthrough, never elevated)
- [ ] WebSocket connections authenticated on STOMP CONNECT frame
- [ ] Rate limiting on all endpoints (Spring Data Redis + `HandlerInterceptor`)
- [ ] Input validation via Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`)
- [ ] Generated code scanned for hardcoded secrets before deployment
- [ ] CVE scanning of all dependencies before deployment
- [ ] SBOM generation (CycloneDX) for every deployed app
- [ ] No backend services exposed via external routes (C2C networking only)
- [ ] All secrets via CF service bindings or CredHub (never in env vars or code)
- [ ] Audit logging via Spring AOP `@Aspect` → CF syslog drain
- [ ] CSP/CORS via Spring Security
- [ ] Spring AI prompt injection detection via custom `CodeSafetyAdvisor`

---

## 18. Performance Targets

| Metric | Target |
|--------|--------|
| Concurrent dev sessions | 1,000 |
| AI plan generation (Spring AI ChatClient call) | < 5s |
| Full code generation (CRUD app) | < 60s |
| Build time (Java Spring Boot, Maven) | < 90s |
| Build time (Node.js/Python) | < 30s |
| Deployment time (cf push) | < 120s |
| API response (non-AI, Spring MVC) | < 200ms p95 |
| WebSocket connections per API instance | 5,000+ |
| Spring Boot startup time (with Spring AI) | < 15s |
| GenAI on Tanzu Platform response (GPU) | < 5s |
| GenAI on Tanzu Platform response (CPU) | < 30s |

---

## 19. File & Directory Structure

```
cf-forge/
├── README.md
├── CF_FORGE_TECHNICAL_SPEC.md
├── pom.xml                            # Maven multi-module parent POM
│
├── cf-forge-common/                   # Shared library (DTOs, enums, utils)
│   ├── pom.xml
│   └── src/main/java/com/cfforge/common/
│       ├── dto/                        # Shared DTOs
│       ├── enums/                      # Language, BuildStatus, DeployStrategy, etc.
│       ├── events/                     # Spring Cloud Stream message types
│       └── util/                       # ManifestUtils, VcapParser, etc.
│
├── cf-forge-api/                      # API Gateway (Spring Boot)
│   ├── pom.xml
│   ├── manifest.yml
│   └── src/main/
│       ├── java/com/cfforge/api/
│       │   ├── CfForgeApiApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── WebSocketConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   └── WebClientConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── ProjectController.java
│       │   │   ├── ConversationController.java
│       │   │   ├── FileController.java
│       │   │   ├── BuildController.java
│       │   │   ├── DeploymentController.java
│       │   │   ├── MarketplaceController.java
│       │   │   └── TemplateController.java
│       │   ├── service/
│       │   │   ├── CfClient.java
│       │   │   ├── RateLimitService.java
│       │   │   └── AuditService.java
│       │   ├── ws/
│       │   │   ├── TerminalWebSocketHandler.java
│       │   │   ├── CollaborationController.java
│       │   │   ├── LogStreamingHandler.java
│       │   │   └── AgentStreamHandler.java
│       │   └── interceptor/
│       │       ├── RateLimitInterceptor.java
│       │       └── AuditAspect.java
│       └── resources/
│           ├── application.yml
│           ├── application-cloud.yml
│           └── db/migration/           # Flyway migrations
│               ├── V1__initial_schema.sql
│               ├── V2__audit_log.sql
│               └── V3__spring_ai_chat_memory.sql
│
├── cf-forge-agent/                    # AI Engine (Spring Boot + Spring AI)
│   ├── pom.xml
│   ├── manifest.yml
│   └── src/main/
│       ├── java/com/cfforge/agent/
│       │   ├── CfForgeAgentApplication.java
│       │   ├── config/
│       │   │   ├── AgentConfig.java         # ChatClient + Advisors setup
│       │   │   ├── GenAiCloudConfig.java     # VCAP_SERVICES → Spring AI props
│       │   │   ├── McpConfig.java            # MCP client setup
│       │   │   └── RagConfig.java            # PGVector + embedding setup
│       │   ├── service/
│       │   │   ├── AgentService.java          # Core agent logic
│       │   │   ├── ManifestGeneratorService.java
│       │   │   └── AutoscalerService.java
│       │   ├── tools/                         # Spring AI @Tool beans
│       │   │   ├── CfPlatformTools.java
│       │   │   ├── WorkspaceTools.java
│       │   │   ├── BuildDeployTools.java
│       │   │   └── DocumentationTools.java
│       │   ├── advisor/                       # Custom Spring AI Advisors
│       │   │   ├── CfContextAdvisor.java
│       │   │   ├── CodeSafetyAdvisor.java
│       │   │   └── OperatorInstructionsAdvisor.java
│       │   ├── model/                         # Structured Output POJOs
│       │   │   ├── GeneratedAppPlan.java
│       │   │   ├── CfManifest.java
│       │   │   ├── AutoscalerPolicy.java
│       │   │   └── GeneratedFile.java
│       │   └── consumer/
│       │       ├── GenerateConsumer.java       # Spring Cloud Stream
│       │       └── RefineConsumer.java
│       └── resources/
│           ├── application.yml
│           ├── application-cloud.yml
│           └── prompts/                       # StringTemplate .st files
│               ├── system.st
│               ├── planning.st
│               ├── generation.st
│               ├── refinement.st
│               ├── manifest.st
│               └── debugging.st
│
├── cf-forge-builder/                  # Build Service (Spring Boot)
│   ├── pom.xml
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/builder/
│       ├── CfForgeBuilderApplication.java
│       ├── consumer/
│       │   └── BuildPipelineConsumer.java
│       ├── pipeline/
│       │   ├── JavaBuildPipeline.java
│       │   ├── NodeBuildPipeline.java
│       │   ├── PythonBuildPipeline.java
│       │   └── GoBuildPipeline.java
│       └── scanner/
│           ├── CveScanner.java
│           └── SbomGenerator.java
│
├── cf-forge-workspace/                # Workspace Service (Spring Boot)
│   ├── pom.xml
│   ├── manifest.yml
│   └── src/main/java/com/cfforge/workspace/
│       ├── CfForgeWorkspaceApplication.java
│       ├── controller/
│       │   └── WorkspaceController.java
│       ├── service/
│       │   ├── FileStorageService.java
│       │   └── CheckpointService.java
│       └── storage/
│           ├── S3StorageBackend.java
│           └── NfsStorageBackend.java
│
├── cf-forge-admin/                    # Admin Dashboard (Spring Boot + Thymeleaf)
│   ├── pom.xml
│   ├── manifest.yml
│   └── src/main/
│       ├── java/com/cfforge/admin/
│       │   ├── CfForgeAdminApplication.java
│       │   ├── config/
│       │   │   ├── AdminSecurityConfig.java       # Spring Security + CF SSO OAuth2 Client
│       │   │   └── MetricsConfig.java              # Micrometer + Prometheus registry
│       │   ├── controller/
│       │   │   ├── AdminDashboardController.java   # Thymeleaf pages (overview, users, etc.)
│       │   │   └── AdminMetricsController.java     # REST API for admin metrics
│       │   ├── service/
│       │   │   ├── MetricAggregationService.java   # Hourly/daily roll-ups
│       │   │   ├── UserMetricsService.java          # Active users, sessions, top users
│       │   │   ├── DeploymentMetricsService.java    # Deploy success rate, by strategy
│       │   │   ├── BuildMetricsService.java         # Build duration, CVE findings
│       │   │   ├── AgentMetricsService.java         # Prompt count, latency, tool calls
│       │   │   ├── GenAiMetricsService.java         # GenAI queue depth, model latency
│       │   │   ├── ComponentHealthService.java      # Periodic health checks → history
│       │   │   └── AuditExportService.java          # CSV/JSON audit log export
│       │   ├── consumer/
│       │   │   └── MetricEventConsumer.java         # Spring Cloud Stream: metric events
│       │   └── scheduler/
│       │       ├── MetricAggregationScheduler.java  # @Scheduled hourly/daily roll-ups
│       │       └── HealthCheckScheduler.java        # @Scheduled component health polling
│       └── resources/
│           ├── application.yml
│           ├── application-cloud.yml
│           ├── templates/                           # Thymeleaf HTML templates
│           │   ├── admin/
│           │   │   ├── layout.html                  # Base layout (nav, footer)
│           │   │   ├── overview.html                # System overview dashboard
│           │   │   ├── users.html                   # User metrics
│           │   │   ├── deployments.html             # Deployment metrics
│           │   │   ├── builds.html                  # Build metrics
│           │   │   ├── agent.html                   # AI agent metrics
│           │   │   ├── genai.html                   # GenAI usage
│           │   │   ├── health.html                  # Component health
│           │   │   └── audit.html                   # Audit log viewer
│           └── static/                              # Admin UI static assets
│               ├── css/admin.css
│               └── js/charts.js                     # Chart.js for dashboard charts
│
├── cf-forge-ui/                       # Frontend (React SPA — only non-Java component)
│   ├── package.json
│   ├── vite.config.ts
│   ├── manifest.yml
│   ├── Staticfile
│   └── src/
│       ├── main.tsx
│       ├── routes/
│       │   ├── builder/               # Conversational builder
│       │   ├── workspace/             # IDE workspace
│       │   ├── dashboard/             # Project dashboard
│       │   ├── marketplace/           # CF service marketplace
│       │   └── templates/             # Template library
│       ├── components/
│       │   ├── editor/                # Monaco editor
│       │   ├── terminal/              # xterm.js
│       │   ├── chat/                  # AI conversation
│       │   ├── preview/               # App preview iframe
│       │   └── logs/                  # CF log viewer
│       └── api/                       # TanStack Query API client
│
├── templates/                         # Starter app templates
│   ├── spring-boot-api/
│   ├── spring-boot-ai-chatbot/
│   ├── flask-api/
│   ├── express-api/
│   └── ...
│
├── scripts/                           # Deployment automation
│   ├── deploy.sh                      # Orchestrated deploy script (see §14.2)
│   ├── deploy-config.sh.example       # Configuration template (see §14.1)
│   ├── create-services.sh             # Standalone service creation helper
│   └── smoke-test.sh                  # Standalone smoke test helper
│
├── .github/
│   └── workflows/
│       └── deploy.yml                 # GitHub Actions workflow (see §14.3)
│
├── docs/
│   ├── architecture.md
│   ├── deployment-guide.md
│   └── spring-ai-integration.md
│
├── docker-compose.yml                 # Local dev: PG, Redis, RabbitMQ, MinIO, Ollama
│
└── .github/
    ├── ISSUE_TEMPLATE/
    │   ├── feature.md
    │   ├── bug.md
    │   └── template-request.md
    └── workflows/
        ├── ci.yml                     # Maven build, test, lint
        ├── deploy-staging.yml
        └── deploy-prod.yml
```

---

## 20. Technology Stack

### Runtime Dependencies (All Spring Boot / Java)

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Runtime | Java (OpenJDK) | 21 LTS | All backend services |
| Framework | Spring Boot | 3.4.x | Application framework |
| AI Framework | Spring AI | 1.1.x | LLM integration, tools, advisors |
| LLM Provider | GenAI on Tanzu Platform | 10.x | OpenAI-compatible LLM |
| Web | Spring WebMVC | 6.2.x | REST APIs |
| WebSocket | Spring WebSocket + STOMP | 6.2.x | Real-time communication |
| Security | Spring Security | 6.4.x | OAuth2/UAA auth |
| ORM | Spring Data JPA + Hibernate | 6.6.x | Database access |
| Migrations | Flyway | 10.x | Schema management |
| Cache | Spring Data Redis | 3.4.x | Sessions, rate limiting |
| Messaging | Spring Cloud Stream + Spring AMQP | 4.2.x | Async messaging |
| Object Storage | AWS SDK v2 (S3) | 2.x | S3-compatible storage |
| AI Memory | Spring AI JDBC ChatMemoryRepository | 1.1.x | Conversation persistence |
| Vector Store | Spring AI PGVector Store | 1.1.x | RAG embeddings |
| MCP | Spring AI MCP Client | 1.1.x | Model Context Protocol |
| Observability | Micrometer + Spring Actuator | 1.14.x | Metrics, health |
| Logging | SLF4J + Logback | 1.5.x | Structured logging |
| Validation | Jakarta Bean Validation (Hibernate Validator) | 8.0 | Input validation |
| Build | Maven | 3.9.x | Multi-module build |
| UI Framework | React | 18+ | Frontend SPA |
| UI Build | Vite | 5+ | Frontend bundling |
| Code Editor | Monaco Editor | latest | IDE editing |
| Terminal | xterm.js | 5+ | Browser terminal |

### Infrastructure (CF Services)

| Service | Version | CF Marketplace Label | Spring Integration |
|---------|---------|---------------------|-------------------|
| PostgreSQL | 14+ | `postgresql`, `p.postgresql` | Spring Data JPA (auto-configured) |
| Redis | 6+ | `redis`, `p.redis` | Spring Data Redis (auto-configured) |
| RabbitMQ | 3.9+ | `rabbitmq`, `p.rabbitmq` | Spring AMQP (auto-configured) |
| GenAI | 10.x | `genai` | Spring AI OpenAI (auto-configured) |
| S3-Compatible | Any | CUPS | Manual config |
| NFS (optional) | Any | `nfs` | Volume mount |

---

## 21. Implementation Phases & GitHub Milestones

### Phase 1: Foundation (Milestone: `v0.1.0`)

**Epic: Core Infrastructure**
- [ ] `INFRA-001` — Maven multi-module parent POM (cf-forge-common, api, agent, builder, workspace)
- [ ] `INFRA-002` — Docker Compose local dev (PostgreSQL, Redis, RabbitMQ, MinIO, Ollama)
- [ ] `INFRA-003` — GitHub Actions CI (mvn verify, checkstyle, spotbugs)
- [ ] `INFRA-004` — CF manifests for all 5 components (java_buildpack_offline)
- [ ] `INFRA-005` — VCAP_SERVICES auto-configuration (Spring Cloud Connectors pattern)
- [ ] `INFRA-006` — Flyway database migrations
- [ ] `INFRA-007` — Spring Cloud Stream RabbitMQ topology
- [ ] `INFRA-008` — S3-compatible object storage service (AWS SDK v2)
- [ ] `INFRA-009` — SLF4J/Logback structured logging (CF syslog format)
- [ ] `INFRA-010` — Spring Actuator health/metrics for all components

**Epic: Authentication**
- [ ] `AUTH-001` — Spring Security OAuth2 Resource Server (CF UAA JWT)
- [ ] `AUTH-002` — Token passthrough to CF API calls via WebClient
- [ ] `AUTH-003` — Spring Session + Redis (session management)
- [ ] `AUTH-004` — CF RBAC middleware (`CfRbacService`)
- [ ] `AUTH-005` — Rate limiting interceptor (Spring Data Redis)

**Epic: Spring AI + GenAI Integration**
- [ ] `AI-001` — Spring AI OpenAI starter + GenAI on Tanzu Platform binding
- [ ] `AI-002` — GenAI VCAP_SERVICES → Spring AI property mapping (`GenAiCloudConfig`)
- [ ] `AI-003` — `ChatClient` bean with `ToolCallAdvisor` + memory advisor
- [ ] `AI-004` — JDBC `ChatMemoryRepository` (PostgreSQL)
- [ ] `AI-005` — System prompt templates (`.st` files)
- [ ] `AI-006` — `CfPlatformTools` @Tool beans (marketplace, buildpacks, logs)
- [ ] `AI-007` — `WorkspaceTools` @Tool beans (read/write/list/delete files)
- [ ] `AI-008` — `BuildDeployTools` @Tool beans (trigger build, deploy)
- [ ] `AI-009` — Structured Output for `GeneratedAppPlan` and `CfManifest`
- [ ] `AI-010` — Agent response streaming (SSE via Spring WebFlux)

**Epic: IDE Foundation**
- [ ] `IDE-001` — React app scaffold with Vite, shadcn/ui, routing
- [ ] `IDE-002` — Monaco editor with multi-file tabs
- [ ] `IDE-003` — File tree component (calls workspace REST API)
- [ ] `IDE-004` — Terminal (xterm.js + Spring WebSocket handler)
- [ ] `IDE-005` — CF CLI pre-configuration in terminal sessions
- [ ] `IDE-006` — Live CF log streaming panel
- [ ] `IDE-007` — Project dashboard (list, create, delete)
- [ ] `IDE-008` — manifest.yml syntax highlighting and validation

**Epic: Basic Deployment**
- [ ] `DEPLOY-001` — CF API client (`CfClient` via `WebClient`)
- [ ] `DEPLOY-002` — Direct deploy (cf push) from workspace
- [ ] `DEPLOY-003` — Java/Maven build pipeline (Spring Cloud Stream consumer)
- [ ] `DEPLOY-004` — Node.js build pipeline
- [ ] `DEPLOY-005` — Build status streaming via WebSocket
- [ ] `DEPLOY-006` — Deployment history tracking (Spring Data JPA)

### Phase 2: AI Enhancement (Milestone: `v0.2.0`)

- [ ] `AI-020` — Conversational builder UI (Lovable-style full-screen chat)
- [ ] `AI-021` — `CfContextAdvisor` (dynamic CF foundation injection)
- [ ] `AI-022` — `CodeSafetyAdvisor` (generated code validation)
- [ ] `AI-023` — `OperatorInstructionsAdvisor` (custom forge-instructions.md)
- [ ] `AI-024` — Iterative refinement (diff-based updates via agent tools)
- [ ] `AI-025` — Error analysis: parse build/deploy logs → auto-fix agent call
- [ ] `AI-026` — Spring AI MCP Client integration (CF docs, Jira)
- [ ] `AI-027` — PGVector RAG pipeline for CF documentation
- [ ] `AI-028` — Spring AI LLM-as-a-Judge (response quality evaluation)
- [ ] `IDE-020` — Live preview panel (staging URL iframe)
- [ ] `IDE-021` — VCAP_SERVICES explorer / service binding viewer
- [ ] `IDE-022` — Application health dashboard (CPU, memory, instances)
- [ ] `DEPLOY-020` — Rolling deployment strategy
- [ ] `DEPLOY-021` — Blue-green deployment strategy
- [ ] `DEPLOY-022` — Autoscaler policy generation (Spring AI structured output)
- [ ] `DEPLOY-023` — Python build pipeline
- [ ] `DEPLOY-024` — Go build pipeline
- [ ] `TMPL-001` — Template registry and metadata format
- [ ] `TMPL-002` — Spring Boot REST API template
- [ ] `TMPL-003` — Spring Boot + Spring AI Chatbot template
- [ ] `TMPL-004` — Flask API template
- [ ] `TMPL-005` — Express.js API template
- [ ] `MKT-001` — CF marketplace browser UI
- [ ] `MKT-002` — AI-recommended services (Spring AI `ChatClient.entity()`)
- [ ] `MKT-003` — One-click service provisioning

### Phase 3: Enterprise (Milestone: `v0.3.0`)

- [ ] `COLLAB-001` — Spring WebSocket STOMP collaboration
- [ ] `COLLAB-002` — User presence and cursor tracking
- [ ] `COLLAB-003` — Shared terminal sessions
- [ ] `SEC-001` — CVE scanning (Trivy via `ProcessBuilder`)
- [ ] `SEC-002` — SBOM generation (CycloneDX Maven plugin)
- [ ] `SEC-003` — Configurable CVE severity gate
- [ ] `SEC-004` — Spring AI prompt injection detection
- [ ] `SEC-005` — Audit logging via Spring AOP → CF syslog drain
- [ ] `DEPLOY-001` — Orchestrated deploy.sh script (services + apps + migrations + smoke)
- [ ] `DEPLOY-002` — deploy-config.sh.example with all configurable parameters
- [ ] `DEPLOY-003` — GitHub Actions workflow (build → test → deploy with environments)
- [ ] `DEPLOY-004` — Air-gapped deployment support (offline buildpacks, on-foundation GenAI)
- [ ] `AI-030` — Spring AI Tool Argument Augmenter (capture LLM reasoning)
- [ ] `AI-031` — Spring AI Recursive Advisors (self-improving generation)
- [ ] `TMPL-010` — CF Service Broker (OSBAPI) template
- [ ] `TMPL-011` — Spring AMQP Worker template
- [ ] `TMPL-012` — Spring Boot + Spring AI RAG template

### Phase 4: Ecosystem (Milestone: `v1.0.0`)

- [ ] `ECO-001` — Public template marketplace
- [ ] `ECO-002` — MCP server support (Jira, Confluence, ServiceNow)
- [ ] `ECO-003` — Multi-foundation deployment support
- [ ] `ECO-004` — AI migration assistant (legacy → CF + Spring Boot)
- [ ] `ECO-005` — Mobile PWA
- [ ] `ECO-006` — TAP supply chain integration
- [ ] `ECO-007` — CF Weekly showcase integration
- [ ] `ECO-008` — Spring AI Agents framework integration (Spring AI Agents 1.0)
- [ ] `ECO-009` — Spring AI Bench evaluation suite for generated code quality

---

## 22. Issue Label Taxonomy

```
# Component labels
component/ui              — Frontend (React SPA)
component/api             — API Gateway (Spring Boot)
component/agent           — AI Agent (Spring Boot + Spring AI)
component/builder         — Build Service (Spring Boot)
component/workspace       — Workspace Service (Spring Boot)
component/deploy          — Deployment Automation
component/templates       — Starter Templates
component/common          — Shared library (cf-forge-common)

# Type labels
type/feature              — New feature
type/bug                  — Bug fix
type/chore                — Maintenance, deps, refactoring
type/security             — Security fix
type/performance          — Performance improvement

# Priority labels
priority/critical         — Blocks release
priority/high             — Must have for milestone
priority/medium           — Should have
priority/low              — Nice to have

# Phase labels
phase/1-foundation        — Phase 1
phase/2-ai-enhancement    — Phase 2
phase/3-enterprise        — Phase 3
phase/4-ecosystem         — Phase 4

# Area labels
area/spring-ai            — Spring AI integration
area/genai-tanzu          — GenAI on Tanzu Platform
area/auth                 — Authentication / Authorization (Spring Security)
area/cf-api               — CF Cloud Controller integration
area/manifest             — Manifest generation
area/deploy               — Deployment pipeline
area/collab               — Real-time collaboration
area/marketplace          — CF service marketplace
area/security             — CVE scanning, SBOM, audit
area/air-gap              — Air-gapped deployment
area/mcp                  — Model Context Protocol
area/rag                  — Retrieval Augmented Generation
```

---

## Appendix: Quick Reference for Claude Code

### When generating code for CF Forge, follow these rules:

1. **Everything is Spring Boot 3.4 + Java 21** — no Node.js, Python, or Go in backend services
2. **Spring AI 1.1 for all AI** — use `ChatClient`, `@Tool`, Advisors, Structured Output
3. **GenAI on Tanzu Platform is the LLM** — OpenAI-compatible, bound via CF service binding
4. **Spring AI auto-config** — `spring-ai-openai-spring-boot-starter` with `VCAP_SERVICES` mapping
5. **All config from VCAP_SERVICES** — never hardcode credentials; use Spring auto-configuration
6. **CF internal routes** for backend-to-backend (`.apps.internal`); use `WebClient`
7. **Spring Data JPA + Flyway** for all database access and migrations
8. **Spring Cloud Stream + RabbitMQ** for async messaging between services
9. **Spring Session + Redis** for session management
10. **Spring Security OAuth2 Resource Server** for UAA JWT authentication
11. **Jakarta Bean Validation** (`@Valid`, `@NotBlank`) for input validation
12. **Spring Actuator** for health checks (`/actuator/health`) and metrics
13. **SLF4J + Logback** for all logging (structured JSON for CF syslog)
14. **Flyway** for schema migrations (not Liquibase, not JPA ddl-auto)
15. **Spring AI Chat Memory** via `JdbcChatMemoryRepository` for conversation persistence
16. **`java_buildpack_offline`** preferred over `java_buildpack` for air-gap
17. **`SPRING_PROFILES_ACTIVE: cloud`** in all CF manifests
18. **`JBP_CONFIG_OPEN_JDK_JRE: "{ jre: { version: 21.+ } }"`** in all CF manifests
19. **Maven multi-module** — parent POM in root, shared lib in `cf-forge-common`
20. **All state-changing operations** must write to the audit log (Spring AOP `@Aspect`)

### Key Spring AI Patterns Used:

```java
// 1. ChatClient fluent API
chatClient.prompt().system(systemPrompt).user(userMessage).call().content();

// 2. Streaming response
chatClient.prompt().user(msg).stream().content();  // Returns Flux<String>

// 3. Structured Output → POJO
chatClient.prompt().user(msg).call().entity(CfManifest.class);

// 4. Tool Calling
@Tool(description = "...") public String myTool(@ToolParam("...") String arg) { ... }

// 5. Chat Memory (JDBC-backed)
.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))

// 6. Custom Advisor
public class MyAdvisor implements CallAdvisor { ... }

// 7. RAG (PGVector)
QuestionAnswerAdvisor.builder(searchRequest, vectorStore).build()

// 8. MCP Client
McpClient.builder().transportType(TransportType.SSE).serverUrl(url).build()
```

### CF CLI Commands the Agent Should Know:

```bash
cf push                              # Deploy app
cf push --strategy rolling           # Rolling deploy
cf logs APP --recent                 # Recent logs
cf services                          # List bound services
cf marketplace                       # Available services (including GenAI)
cf create-service genai PLAN NAME    # Provision GenAI model
cf create-service postgresql PLAN N  # Provision database
cf bind-service APP SERVICE          # Bind service
cf restage APP                       # Restage after binding
cf scale APP -i N -m MEMORY         # Scale app
cf env APP                           # View VCAP_SERVICES
cf network-policies                  # View C2C policies
cf add-network-policy APP --destination-app DST --port 8080 --protocol tcp
```
