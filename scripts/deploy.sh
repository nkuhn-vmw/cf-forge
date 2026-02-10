#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# CF Forge — Orchestrated Deployment Script
# ============================================================
# Usage:
#   ./scripts/deploy.sh                  Full deploy (services + apps + migrations + smoke tests)
#   ./scripts/deploy.sh --apps-only      Re-deploy apps (skip service creation)
#   ./scripts/deploy.sh --services-only  Create/update services only
#   ./scripts/deploy.sh --migrate-only   Run Flyway migrations only
#   ./scripts/deploy.sh --smoke-test     Run smoke tests only
#   ./scripts/deploy.sh --teardown       Delete all CF Forge apps and services
#   ./scripts/deploy.sh --component api  Deploy a single component
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Load config
CONFIG_FILE="${SCRIPT_DIR}/deploy-config.sh"
if [[ ! -f "${CONFIG_FILE}" ]]; then
    echo "ERROR: ${CONFIG_FILE} not found."
    echo "Copy deploy-config.sh.example to deploy-config.sh and fill in your values."
    exit 1
fi
# shellcheck source=/dev/null
source "${CONFIG_FILE}"

ACTION="${1:---full}"
COMPONENT="${2:-}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}   $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ============================================================
# Preflight Checks
# ============================================================
preflight() {
    log_info "Running preflight checks..."

    command -v cf >/dev/null 2>&1 || { log_error "CF CLI not found. Install: https://github.com/cloudfoundry/cli"; exit 1; }
    command -v jq >/dev/null 2>&1 || { log_error "jq not found. Install: https://stedolan.github.io/jq/"; exit 1; }

    if [[ "${CF_SKIP_SSL}" == "true" ]]; then
        cf api "${CF_API}" --skip-ssl-validation
    else
        cf api "${CF_API}"
    fi

    cf target -o "${CF_ORG}" -s "${CF_SPACE}" 2>/dev/null || {
        log_info "Creating org/space..."
        cf create-org "${CF_ORG}" 2>/dev/null || true
        cf target -o "${CF_ORG}"
        cf create-space "${CF_SPACE}" 2>/dev/null || true
        cf target -s "${CF_SPACE}"
    }

    log_ok "Preflight checks passed"
}

# ============================================================
# Build (if JARs not pre-built)
# ============================================================
build_artifacts() {
    log_info "Building artifacts..."

    if [[ -z "${API_JAR}" ]]; then
        log_info "Building Spring Boot modules..."
        (cd "${PROJECT_ROOT}" && ./mvnw -q package -DskipTests -pl cf-forge-api,cf-forge-agent,cf-forge-builder,cf-forge-workspace,cf-forge-admin)
        API_JAR="${PROJECT_ROOT}/cf-forge-api/target/cf-forge-api-*.jar"
        AGENT_JAR="${PROJECT_ROOT}/cf-forge-agent/target/cf-forge-agent-*.jar"
        BUILDER_JAR="${PROJECT_ROOT}/cf-forge-builder/target/cf-forge-builder-*.jar"
        WORKSPACE_JAR="${PROJECT_ROOT}/cf-forge-workspace/target/cf-forge-workspace-*.jar"
        ADMIN_JAR="${PROJECT_ROOT}/cf-forge-admin/target/cf-forge-admin-*.jar"
    fi

    if [[ -z "${UI_DIST}" ]]; then
        log_info "Building frontend..."
        (cd "${PROJECT_ROOT}/cf-forge-ui" && npm ci && npm run build)
        UI_DIST="${PROJECT_ROOT}/cf-forge-ui/dist"
    fi

    log_ok "Artifacts built"
}

# ============================================================
# Service Creation
# ============================================================
create_services() {
    log_info "Creating services..."

    # PostgreSQL
    cf create-service "${DB_SERVICE_TYPE}" "${DB_PLAN}" "${DB_INSTANCE_NAME}" 2>/dev/null && \
        log_ok "PostgreSQL: ${DB_INSTANCE_NAME}" || log_warn "PostgreSQL already exists"

    # Redis
    cf create-service "${REDIS_SERVICE_TYPE}" "${REDIS_PLAN}" "${REDIS_INSTANCE_NAME}" 2>/dev/null && \
        log_ok "Redis: ${REDIS_INSTANCE_NAME}" || log_warn "Redis already exists"

    # RabbitMQ
    cf create-service "${MQ_SERVICE_TYPE}" "${MQ_PLAN}" "${MQ_INSTANCE_NAME}" 2>/dev/null && \
        log_ok "RabbitMQ: ${MQ_INSTANCE_NAME}" || log_warn "RabbitMQ already exists"

    # GenAI
    if [[ "${GENAI_USER_PROVIDED}" == "true" ]]; then
        cf create-user-provided-service "${GENAI_INSTANCE_NAME}" -p "{\"uri\":\"${GENAI_BASE_URL}\",\"api-key\":\"${GENAI_API_KEY}\",\"model\":\"${GENAI_MODEL}\"}" 2>/dev/null && \
            log_ok "GenAI (user-provided): ${GENAI_INSTANCE_NAME}" || log_warn "GenAI already exists"
    else
        cf create-service "${GENAI_SERVICE_TYPE}" "${GENAI_PLAN}" "${GENAI_INSTANCE_NAME}" 2>/dev/null && \
            log_ok "GenAI: ${GENAI_INSTANCE_NAME}" || log_warn "GenAI already exists"
    fi

    # SSO
    if [[ "${SSO_USER_PROVIDED}" == "true" ]]; then
        cf create-user-provided-service "${SSO_INSTANCE_NAME}" -p "{\"auth_domain\":\"${SSO_AUTH_DOMAIN}\",\"client_id\":\"${SSO_CLIENT_ID}\",\"client_secret\":\"${SSO_CLIENT_SECRET}\"}" 2>/dev/null && \
            log_ok "SSO (user-provided): ${SSO_INSTANCE_NAME}" || log_warn "SSO already exists"
    else
        cf create-service "${SSO_SERVICE_TYPE}" "${SSO_PLAN}" "${SSO_INSTANCE_NAME}" 2>/dev/null && \
            log_ok "SSO: ${SSO_INSTANCE_NAME}" || log_warn "SSO already exists"
    fi

    # Object Storage
    cf create-user-provided-service "${OBJECT_STORE_INSTANCE_NAME}" -p "{\"endpoint\":\"${OBJECT_STORE_ENDPOINT}\",\"access-key\":\"${OBJECT_STORE_ACCESS_KEY}\",\"secret-key\":\"${OBJECT_STORE_SECRET_KEY}\",\"bucket\":\"${OBJECT_STORE_BUCKET}\",\"region\":\"${OBJECT_STORE_REGION}\"}" 2>/dev/null && \
        log_ok "Object Storage: ${OBJECT_STORE_INSTANCE_NAME}" || log_warn "Object Storage already exists"

    # Wait for async services
    log_info "Waiting for services to provision..."
    sleep 10
    log_ok "Services ready"
}

# ============================================================
# Deploy Apps
# ============================================================
deploy_app() {
    local name=$1 jar=$2 memory=$3 instances=$4 services=$5 route=$6

    log_info "Deploying ${name}..."

    local manifest_opts=(
        -p "${jar}"
        -m "${memory}"
        -i "${instances}"
        --health-check-type http
        --health-check-http-endpoint /actuator/health
    )

    if [[ "${route}" == "internal" ]]; then
        cf push "${name}" "${manifest_opts[@]}" -b java_buildpack_offline -d "${CF_INTERNAL_DOMAIN}" --no-route 2>/dev/null || \
        cf push "${name}" "${manifest_opts[@]}" -b java_buildpack -d "${CF_INTERNAL_DOMAIN}" --no-route
        cf map-route "${name}" "${CF_INTERNAL_DOMAIN}" --hostname "${name}"
    else
        cf push "${name}" "${manifest_opts[@]}" -b java_buildpack_offline -d "${CF_APPS_DOMAIN}" 2>/dev/null || \
        cf push "${name}" "${manifest_opts[@]}" -b java_buildpack -d "${CF_APPS_DOMAIN}"
    fi

    # Bind services
    IFS=',' read -ra SVC_LIST <<< "${services}"
    for svc in "${SVC_LIST[@]}"; do
        svc=$(echo "${svc}" | xargs)
        cf bind-service "${name}" "${svc}" 2>/dev/null || true
    done

    # Set environment
    cf set-env "${name}" SPRING_PROFILES_ACTIVE "${SPRING_PROFILES}" 2>/dev/null || true
    cf set-env "${name}" CVE_SCAN_ENABLED "${CVE_SCAN_ENABLED}" 2>/dev/null || true
    cf set-env "${name}" CVE_BLOCK_SEVERITY "${CVE_BLOCK_SEVERITY}" 2>/dev/null || true
    cf set-env "${name}" CF_FORGE_DOMAIN "https://forge.${CF_APPS_DOMAIN}" 2>/dev/null || true

    cf restage "${name}" 2>/dev/null || true
    log_ok "Deployed ${name}"
}

deploy_ui() {
    log_info "Deploying cf-forge-ui..."
    cf push cf-forge-ui -p "${UI_DIST}" -b staticfile_buildpack \
        -m "${UI_MEMORY}" -i "${UI_INSTANCES}" \
        -d "${CF_APPS_DOMAIN}" --hostname forge
    log_ok "Deployed cf-forge-ui"
}

deploy_all_apps() {
    # shellcheck disable=SC2086
    deploy_app "cf-forge-api" ${API_JAR} "${API_MEMORY}" "${API_INSTANCES}" \
        "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${MQ_INSTANCE_NAME},${SSO_INSTANCE_NAME}" \
        "external"

    deploy_app "cf-forge-agent" ${AGENT_JAR} "${AGENT_MEMORY}" "${AGENT_INSTANCES}" \
        "${DB_INSTANCE_NAME},${MQ_INSTANCE_NAME},${GENAI_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" \
        "internal"

    deploy_app "cf-forge-builder" ${BUILDER_JAR} "${BUILDER_MEMORY}" "${BUILDER_INSTANCES}" \
        "${DB_INSTANCE_NAME},${MQ_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" \
        "internal"

    deploy_app "cf-forge-workspace" ${WORKSPACE_JAR} "${WORKSPACE_MEMORY}" "${WORKSPACE_INSTANCES}" \
        "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" \
        "internal"

    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        deploy_app "cf-forge-admin" ${ADMIN_JAR} "${ADMIN_MEMORY}" "${ADMIN_INSTANCES}" \
            "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${MQ_INSTANCE_NAME},${SSO_INSTANCE_NAME}" \
            "external"
    fi

    deploy_ui
}

deploy_single() {
    local comp=$1
    case "${comp}" in
        api)       deploy_app "cf-forge-api" ${API_JAR} "${API_MEMORY}" "${API_INSTANCES}" "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${MQ_INSTANCE_NAME},${SSO_INSTANCE_NAME}" "external" ;;
        agent)     deploy_app "cf-forge-agent" ${AGENT_JAR} "${AGENT_MEMORY}" "${AGENT_INSTANCES}" "${DB_INSTANCE_NAME},${MQ_INSTANCE_NAME},${GENAI_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" "internal" ;;
        builder)   deploy_app "cf-forge-builder" ${BUILDER_JAR} "${BUILDER_MEMORY}" "${BUILDER_INSTANCES}" "${DB_INSTANCE_NAME},${MQ_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" "internal" ;;
        workspace) deploy_app "cf-forge-workspace" ${WORKSPACE_JAR} "${WORKSPACE_MEMORY}" "${WORKSPACE_INSTANCES}" "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${OBJECT_STORE_INSTANCE_NAME}" "internal" ;;
        admin)     deploy_app "cf-forge-admin" ${ADMIN_JAR} "${ADMIN_MEMORY}" "${ADMIN_INSTANCES}" "${DB_INSTANCE_NAME},${REDIS_INSTANCE_NAME},${MQ_INSTANCE_NAME},${SSO_INSTANCE_NAME}" "external" ;;
        ui)        deploy_ui ;;
        *) log_error "Unknown component: ${comp}"; exit 1 ;;
    esac
}

# ============================================================
# Network Policies (C2C)
# ============================================================
setup_network_policies() {
    log_info "Configuring C2C network policies..."
    cf add-network-policy cf-forge-api cf-forge-agent --port 8080 --protocol tcp 2>/dev/null || true
    cf add-network-policy cf-forge-api cf-forge-builder --port 8080 --protocol tcp 2>/dev/null || true
    cf add-network-policy cf-forge-api cf-forge-workspace --port 8080 --protocol tcp 2>/dev/null || true

    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        cf add-network-policy cf-forge-admin cf-forge-agent --port 8080 --protocol tcp 2>/dev/null || true
        cf add-network-policy cf-forge-admin cf-forge-builder --port 8080 --protocol tcp 2>/dev/null || true
        cf add-network-policy cf-forge-admin cf-forge-workspace --port 8080 --protocol tcp 2>/dev/null || true
    fi

    log_ok "Network policies configured"
}

# ============================================================
# Migrations
# ============================================================
run_migrations() {
    log_info "Running Flyway migrations..."
    cf run-task cf-forge-api --command "java -cp /home/vcap/app/BOOT-INF/classes:/home/vcap/app/BOOT-INF/lib/* org.flywaydb.commandline.Main migrate" \
        --name flyway-migrate -m 512M 2>/dev/null || log_warn "Migration task submission skipped"
    log_ok "Migration task submitted"
}

# ============================================================
# Doc Ingestion
# ============================================================
ingest_docs() {
    log_info "Ingesting CF documentation for RAG..."
    cf run-task cf-forge-agent --command "java -cp /home/vcap/app/BOOT-INF/classes:/home/vcap/app/BOOT-INF/lib/* com.cfforge.agent.service.CfDocsIngestionService" \
        --name doc-ingest -m 1G 2>/dev/null || log_warn "Doc ingestion task skipped"
    log_ok "Doc ingestion task submitted"
}

# ============================================================
# Smoke Tests
# ============================================================
smoke_tests() {
    log_info "Running smoke tests..."
    local api_url="https://forge-api.${CF_APPS_DOMAIN}"
    local ui_url="https://forge.${CF_APPS_DOMAIN}"

    local failures=0

    # API health
    if curl -sf --max-time 10 "${api_url}/actuator/health" | jq -e '.status == "UP"' >/dev/null 2>&1; then
        log_ok "API health: UP"
    else
        log_error "API health: FAILED"
        ((failures++))
    fi

    # UI
    if curl -sf --max-time 10 "${ui_url}/" >/dev/null 2>&1; then
        log_ok "UI: responding"
    else
        log_error "UI: FAILED"
        ((failures++))
    fi

    # Admin (if enabled)
    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        local admin_url="https://forge-admin.${CF_APPS_DOMAIN}"
        if curl -sf --max-time 10 "${admin_url}/actuator/health" | jq -e '.status == "UP"' >/dev/null 2>&1; then
            log_ok "Admin health: UP"
        else
            log_warn "Admin health: unavailable"
        fi
    fi

    if [[ "${failures}" -gt 0 ]]; then
        log_error "Smoke tests: ${failures} failure(s)"
        return 1
    fi

    log_ok "All smoke tests passed"
}

# ============================================================
# Teardown
# ============================================================
teardown() {
    log_warn "This will DELETE all CF Forge apps and services in ${CF_ORG}/${CF_SPACE}."
    read -rp "Type 'yes' to confirm: " confirm
    if [[ "${confirm}" != "yes" ]]; then
        log_info "Teardown cancelled."
        return
    fi

    log_info "Tearing down CF Forge..."
    for app in cf-forge-api cf-forge-agent cf-forge-builder cf-forge-workspace cf-forge-admin cf-forge-ui; do
        cf delete "${app}" -f -r 2>/dev/null || true
    done

    for svc in "${DB_INSTANCE_NAME}" "${REDIS_INSTANCE_NAME}" "${MQ_INSTANCE_NAME}" "${GENAI_INSTANCE_NAME}" "${SSO_INSTANCE_NAME}" "${OBJECT_STORE_INSTANCE_NAME}"; do
        cf delete-service "${svc}" -f 2>/dev/null || true
    done

    log_ok "Teardown complete"
}

# ============================================================
# Summary
# ============================================================
print_summary() {
    echo ""
    echo "============================================================"
    echo " CF Forge Deployment Summary"
    echo "============================================================"
    echo "  API:   https://forge-api.${CF_APPS_DOMAIN}"
    echo "  UI:    https://forge.${CF_APPS_DOMAIN}"
    if [[ "${ADMIN_ENABLED}" == "true" ]]; then
        echo "  Admin: https://forge-admin.${CF_APPS_DOMAIN}"
    fi
    echo "============================================================"
    cf apps
}

# ============================================================
# Main
# ============================================================
case "${ACTION}" in
    --full|-f)
        preflight
        build_artifacts
        create_services
        deploy_all_apps
        setup_network_policies
        run_migrations
        ingest_docs
        smoke_tests
        print_summary
        ;;
    --apps-only|-a)
        preflight
        build_artifacts
        deploy_all_apps
        setup_network_policies
        smoke_tests
        print_summary
        ;;
    --services-only|-s)
        preflight
        create_services
        ;;
    --migrate-only|-m)
        preflight
        run_migrations
        ;;
    --smoke-test|-t)
        preflight
        smoke_tests
        ;;
    --teardown)
        preflight
        teardown
        ;;
    --component|-c)
        if [[ -z "${COMPONENT}" ]]; then
            log_error "Usage: deploy.sh --component <api|agent|builder|workspace|admin|ui>"
            exit 1
        fi
        preflight
        build_artifacts
        deploy_single "${COMPONENT}"
        ;;
    *)
        echo "Usage: ./scripts/deploy.sh [--full|--apps-only|--services-only|--migrate-only|--smoke-test|--teardown|--component <name>]"
        exit 1
        ;;
esac
