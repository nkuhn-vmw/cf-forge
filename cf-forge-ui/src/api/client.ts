const BASE_URL = '/api/v1'

let refreshPromise: Promise<boolean> | null = null

async function attemptRefresh(): Promise<boolean> {
  try {
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
    return res.ok
  } catch {
    return false
  }
}

function buildFetchOptions(options?: RequestInit): RequestInit {
  const { headers: optHeaders, ...rest } = options ?? {}
  return {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(optHeaders as Record<string, string>),
    },
    credentials: 'include',
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, buildFetchOptions(options))

  if (res.status === 401) {
    // Deduplicate concurrent refresh attempts
    if (!refreshPromise) {
      refreshPromise = attemptRefresh().finally(() => { refreshPromise = null })
    }
    const refreshed = await refreshPromise
    if (refreshed) {
      // Retry the original request
      const retryRes = await fetch(`${BASE_URL}${path}`, buildFetchOptions(options))
      if (retryRes.ok) {
        if (retryRes.status === 204) return undefined as T
        return retryRes.json()
      }
    }
    // Refresh failed or retry failed — redirect to login
    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
    throw new Error('Authentication required')
  }

  if (!res.ok) {
    const error = await res.text().catch(() => res.statusText)
    throw new Error(`API Error ${res.status}: ${error}`)
  }

  if (res.status === 204) return undefined as T
  return res.json()
}

export interface Project {
  id: string
  name: string
  slug: string
  language: string
  framework: string
  buildpack: string
  visibility: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface Build {
  id: string
  projectId: string
  triggerType: string
  status: string
  buildLog: string | null
  artifactPath: string | null
  durationMs: number | null
  createdAt: string
}

export interface Deployment {
  id: string
  projectId: string
  strategy: string
  environment: string
  status: string
  deploymentUrl: string | null
  createdAt: string
}

export interface FileEntry {
  name: string
  path: string
  directory: boolean
  size: number
  lastModified: string | null
}

export interface Template {
  id: string
  name: string
  slug: string
  language: string
  framework: string
  description: string
  downloadCount: number
}

export interface ServiceOffering {
  name: string
  description: string
  plans: string[]
  tags?: string[]
}

export interface ServiceRecommendation {
  serviceName: string
  plan: string
  reason: string
  bindingName: string
}

export interface ProvisionRequest {
  serviceName: string
  plan: string
  instanceName: string
}

export interface DeployStrategy {
  strategy: 'rolling' | 'blue-green' | 'canary'
  environment: string
}

export interface CfFoundation {
  id: string
  endpoint: string
  org: string
  space: string
  isDefault: boolean
}

export interface MigrationPlan {
  sourceStack: string
  targetStack: string
  complexityScore: string
  steps: { order: number; title: string; description: string; category: string; effort: string }[]
  recommendedServices: string[]
  risks: string[]
}

export interface AgentDefinition {
  name: string
  role: string
  capabilities: string[]
}

export const api = {
  projects: {
    list: () => request<Project[]>('/projects'),
    get: (id: string) => request<Project>(`/projects/${id}`),
    create: (data: Partial<Project>) =>
      request<Project>('/projects', { method: 'POST', body: JSON.stringify(data) }),
    delete: (id: string) =>
      request<void>(`/projects/${id}`, { method: 'DELETE' }),
  },

  builds: {
    list: (projectId: string) => request<Build[]>(`/projects/${projectId}/builds`),
    get: (projectId: string, buildId: string) =>
      request<Build>(`/projects/${projectId}/builds/${buildId}`),
    trigger: (projectId: string) =>
      request<Build>(`/projects/${projectId}/builds`, { method: 'POST' }),
  },

  deployments: {
    list: (projectId: string) => request<Deployment[]>(`/projects/${projectId}/deployments`),
    get: (projectId: string, deployId: string) =>
      request<Deployment>(`/projects/${projectId}/deployments/${deployId}`),
    trigger: (projectId: string, environment: string) =>
      request<Deployment>(`/projects/${projectId}/deployments`, {
        method: 'POST',
        body: JSON.stringify({ environment }),
      }),
  },

  files: {
    list: (projectId: string, dir?: string) => {
      const params = dir ? `?dir=${encodeURIComponent(dir)}` : ''
      return request<FileEntry[]>(`/projects/${projectId}/files${params}`)
    },
    read: (projectId: string, path: string) =>
      request<{ content: string }>(`/projects/${projectId}/files/${encodeURIComponent(path)}`),
    write: (projectId: string, path: string, content: string) =>
      request<void>(`/projects/${projectId}/files/${encodeURIComponent(path)}`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
      }),
    delete: (projectId: string, path: string) =>
      request<void>(`/projects/${projectId}/files/${encodeURIComponent(path)}`, {
        method: 'DELETE',
      }),
  },

  agent: {
    generate: (projectId: string, prompt: string): EventSource => {
      const params = new URLSearchParams({ projectId, prompt })
      return new EventSource(`${BASE_URL}/agent/generate?${params}`, { withCredentials: true })
    },
  },

  marketplace: {
    services: () => request<ServiceOffering[]>('/marketplace/services'),
    recommend: (projectId: string) =>
      request<ServiceRecommendation[]>(`/marketplace/recommend?projectId=${projectId}`),
    provision: (projectId: string, data: ProvisionRequest) =>
      request<void>(`/projects/${projectId}/services`, {
        method: 'POST',
        body: JSON.stringify(data),
      }),
  },

  templates: {
    list: () => request<Template[]>('/templates'),
    get: (slug: string) => request<Template>(`/templates/${slug}`),
    scaffold: (slug: string) =>
      request<Project>(`/templates/${slug}/scaffold`, { method: 'POST' }),
    community: (page = 0, sort = 'popular') =>
      request<{ content: Template[]; totalPages: number; totalElements: number }>(
        `/templates/community?page=${page}&sort=${sort}`),
    featured: () => request<Template[]>('/templates/featured'),
    rate: (slug: string, rating: number) =>
      request<Template>(`/templates/${slug}/rate`, {
        method: 'POST', body: JSON.stringify({ rating }),
      }),
  },

  foundations: {
    list: () => request<CfFoundation[]>('/targets'),
    create: (data: Partial<CfFoundation>) =>
      request<CfFoundation>('/targets', { method: 'POST', body: JSON.stringify(data) }),
    validate: (id: string) =>
      request<{ status: string; apiVersion: string }>(`/targets/${id}/validate`, { method: 'POST' }),
    setDefault: (id: string) =>
      request<CfFoundation>(`/targets/${id}/set-default`, { method: 'POST' }),
  },

  migration: {
    analyze: (data: { code: string; description: string; sourceStack: string }) =>
      request<MigrationPlan>('/migration/analyze', {
        method: 'POST', body: JSON.stringify(data),
      }),
  },

  agents: {
    list: () => request<AgentDefinition[]>('/agents'),
    workflow: (task: string, agents: string[], context = '') =>
      request<{ steps: { agentName: string; output: string }[]; durationMs: number }>(
        '/agents/workflow', {
          method: 'POST', body: JSON.stringify({ task, agents, context }),
        }),
  },

  bench: {
    evaluate: (code: string, prompt: string) =>
      request<{ overallScore: number; correctnessScore: number; securityScore: number }>(
        '/bench/evaluate', { method: 'POST', body: JSON.stringify({ code, prompt }) }),
  },
}
