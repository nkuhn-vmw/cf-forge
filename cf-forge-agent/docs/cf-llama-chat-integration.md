# CF-Forge + CF-Llama-Chat Integration Guide

## Overview

CF-Forge exposes its Cloud Foundry development tools via the **MCP (Model Context Protocol)** standard. CF-Llama-Chat connects to CF-Forge as an MCP server, making all tools available to the LLM during chat sessions. **No code changes to CF-Llama-Chat are required.**

## Architecture

```
┌─────────────────────────────────────────────┐
│            cf-llama-chat (unchanged)         │
│  Chat UI + Admin Portal + Skills + MCP      │
│                                              │
│  Admin registers cf-forge MCP server URL ──┐ │
│  Skills created for CF workflows           │ │
│  Users select "CF App Builder" skill       │ │
│  LLM calls cf-forge tools via MCP ←───────┘ │
└──────────────────────┬──────────────────────┘
                       │ MCP Protocol (SSE/HTTP)
                       ↓
┌──────────────────────────────────────────────┐
│        cf-forge-agent (MCP Server)           │
│  Exposes 15+ tools via MCP protocol:        │
│                                              │
│  Project:  create, list, get, delete, update │
│  Files:    read, write, list, delete         │
│  Build:    trigger build, trigger deploy     │
│  Deploy:   list, status, rollback            │
│  Platform: marketplace, buildpacks, quotas   │
│  Services: recommend, list bound, provision  │
│  Docs:     search CF docs, buildpack docs    │
│                                              │
│  Proxies to: workspace, builder, api         │
└──────────────────────────────────────────────┘
```

## Prerequisites

1. **CF-Forge services deployed** on Tanzu Application Service:
   - `cf-forge-agent` — MCP server + AI agent
   - `cf-forge-workspace` — file storage service
   - `cf-forge-builder` — build pipeline service
   - `cf-forge-api` — project/data API

2. **CF-Llama-Chat deployed** with MCP server support enabled

3. **Shared services provisioned**:
   - `cf-forge-db` (PostgreSQL)
   - `cf-forge-mq` (RabbitMQ)
   - `cf-forge-genai` (GenAI on Tanzu Platform)

## Step 1: Deploy CF-Forge Agent with MCP

The agent is configured with two routes:
- **Internal**: `cf-forge-agent.apps.internal` (for inter-service communication)
- **External**: `cf-forge-mcp.apps.tas-ndc.kuhn-labs.com` (for MCP access from cf-llama-chat)

Build and push:

```bash
cd cf-forge
mvn package -DskipTests -pl cf-forge-agent -am
cf push cf-forge-agent -f cf-forge-agent/manifest.yml
```

Verify the MCP SSE endpoint is accessible:

```bash
curl -N https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse
```

Verify the setup endpoint:

```bash
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/mcp-config | jq .
```

## Step 2: Register MCP Server in CF-Llama-Chat

In the CF-Llama-Chat admin portal:

1. Navigate to **Admin > MCP Servers**
2. Click **Add New Server**
3. Fill in:
   - **Name**: `cf-forge`
   - **URL**: `https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/sse`
   - **Transport**: SSE
4. Click **Connect** and verify tools are discovered

Alternatively, if both apps are in the same CF space, use the internal route:
- **URL**: `http://cf-forge-agent.apps.internal:8080/sse`

## Step 3: Import Skills

Retrieve pre-configured skill definitions:

```bash
curl https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com/api/v1/setup/skills | jq .
```

In CF-Llama-Chat admin, create these skills:

### CF App Builder
- **Description**: Build and deploy CF apps from natural language
- **Tools**: createProject, writeFile, listFiles, readFile, triggerBuild, triggerDeploy, getMarketplaceServices, recommendServices, getAvailableBuildpacks, getBuildpackDocs
- **Use case**: "Build me a Java Spring Boot REST API with PostgreSQL"

### CF Migration Analyst
- **Description**: Analyze apps and plan CF migrations
- **Tools**: getMarketplaceServices, getAvailableBuildpacks, searchDocumentation, getBuildpackDocs, recommendServices, getServiceProvisioningGuide
- **Use case**: "Analyze my Node.js app for CF migration"

### CF Deployment Manager
- **Description**: Manage deployments, monitoring, and rollbacks
- **Tools**: listProjects, getProject, listDeployments, getDeploymentStatus, triggerDeploy, rollbackDeployment, getRecentLogs, getAppEnvironment
- **Use case**: "Show me deployment status and rollback the last failed deploy"

### CF Platform Explorer
- **Description**: Explore CF platform capabilities
- **Tools**: getMarketplaceServices, getAvailableBuildpacks, getOrgQuota, searchDocumentation, getBuildpackDocs, recommendServices, getServiceProvisioningGuide
- **Use case**: "What services are available in the marketplace?"

## Step 4: Verify Integration

1. Open CF-Llama-Chat
2. In the chat sidebar, select the **"CF App Builder"** skill
3. Send: "Build me a Python Flask hello world app"
4. Verify the LLM:
   - Calls `createProject` to create the project
   - Calls `writeFile` to create app.py, requirements.txt, Procfile, manifest.yml
   - Calls `triggerBuild` to build the project
   - Calls `triggerDeploy` to deploy to staging

## Available MCP Tools

| Tool | Description |
|------|-------------|
| `createProject` | Create a new project with workspace |
| `listProjects` | List all projects |
| `getProject` | Get project details by ID |
| `updateProject` | Update project metadata |
| `deleteProject` | Soft-delete a project |
| `readFile` | Read file from workspace |
| `writeFile` | Write/create file in workspace |
| `listFiles` | List files in workspace |
| `deleteFile` | Delete file from workspace |
| `triggerBuild` | Queue a build (compile, test, package) |
| `triggerDeploy` | Deploy to staging or production |
| `listDeployments` | List deployment history |
| `getDeploymentStatus` | Get deployment details |
| `rollbackDeployment` | Rollback a failed deployment |
| `getMarketplaceServices` | List CF marketplace services |
| `getAvailableBuildpacks` | List CF buildpacks |
| `getOrgQuota` | Get org quota limits |
| `getRecentLogs` | Get app logs |
| `getAppEnvironment` | Get app env vars |
| `searchDocumentation` | Search CF docs (RAG) |
| `getBuildpackDocs` | Get buildpack-specific docs |
| `recommendServices` | AI-powered service recommendations |
| `listBoundServices` | List services bound to an app |
| `getServiceProvisioningGuide` | Step-by-step service setup guide |

## Networking Options

### Option A: External Route (Default)
The agent manifest includes an external route `cf-forge-mcp.apps.tas-ndc.kuhn-labs.com`. This works regardless of where cf-llama-chat is deployed.

### Option B: Container-to-Container Networking
If both apps are in the same CF foundation, use internal routing for lower latency:

```bash
cf add-network-policy cf-llama-chat cf-forge-agent --port 8080 --protocol tcp
```

Then register the MCP server URL as: `http://cf-forge-agent.apps.internal:8080/sse`

## Troubleshooting

### MCP connection fails
- Check that cf-forge-agent is running: `cf app cf-forge-agent`
- Verify the SSE endpoint: `curl -N <url>/sse`
- Check agent logs: `cf logs cf-forge-agent --recent`

### Tools not discovered
- Verify MCP server config: `curl <url>/api/v1/setup/mcp-config`
- Check that the `spring.ai.mcp.server` properties are set in application.yml
- Ensure all tool beans are properly registered in McpServerConfig

### Tool calls return errors
- Check workspace service is running: `cf app cf-forge-workspace`
- Check builder service is running: `cf app cf-forge-builder`
- Verify database connectivity: check cf-forge-agent logs for JPA errors
- Verify RabbitMQ connectivity: check for stream binding errors in logs
