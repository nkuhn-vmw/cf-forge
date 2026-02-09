const BASE_URL = '/api/v1'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

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
      return new EventSource(`${BASE_URL}/agent/generate?${params}`)
    },
  },

  marketplace: {
    services: () => request<ServiceOffering[]>('/marketplace/services'),
  },

  templates: {
    list: () => request<Template[]>('/templates'),
    get: (slug: string) => request<Template>(`/templates/${slug}`),
  },
}
