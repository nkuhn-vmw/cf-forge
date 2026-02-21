import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './client.ts'
import type { Project } from './client.ts'

export function useProjects() {
  return useQuery({
    queryKey: ['projects'],
    queryFn: api.projects.list,
  })
}

export function useProject(id: string) {
  return useQuery({
    queryKey: ['projects', id],
    queryFn: () => api.projects.get(id),
    enabled: !!id,
  })
}

export function useCreateProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: Partial<Project>) => api.projects.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useDeleteProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.projects.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useBuilds(projectId: string) {
  return useQuery({
    queryKey: ['builds', projectId],
    queryFn: () => api.builds.list(projectId),
    enabled: !!projectId,
  })
}

export function useTriggerBuild(projectId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => api.builds.trigger(projectId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['builds', projectId] }),
  })
}

export function useDeployments(projectId: string) {
  return useQuery({
    queryKey: ['deployments', projectId],
    queryFn: () => api.deployments.list(projectId),
    enabled: !!projectId,
  })
}

export function useTriggerDeploy(projectId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (environment: string) => api.deployments.trigger(projectId, environment),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['deployments', projectId] }),
  })
}

export function useFiles(projectId: string, dir?: string) {
  return useQuery({
    queryKey: ['files', projectId, dir],
    queryFn: () => api.files.list(projectId, dir),
    enabled: !!projectId,
  })
}

export function useFileContent(projectId: string, path: string) {
  return useQuery({
    queryKey: ['file-content', projectId, path],
    queryFn: () => api.files.read(projectId, path),
    enabled: !!projectId && !!path,
  })
}

export function useTemplates() {
  return useQuery({
    queryKey: ['templates'],
    queryFn: api.templates.list,
  })
}

export function useMarketplace() {
  return useQuery({
    queryKey: ['marketplace'],
    queryFn: api.marketplace.services,
  })
}

export function useServiceRecommendations(projectId: string) {
  return useQuery({
    queryKey: ['marketplace', 'recommend', projectId],
    queryFn: () => api.marketplace.recommend(projectId),
    enabled: !!projectId,
  })
}

export function useProvisionService(projectId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: { serviceName: string; plan: string; instanceName: string }) =>
      api.marketplace.provision(projectId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['marketplace'] }),
  })
}

export function useScaffoldTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (slug: string) => api.templates.scaffold(slug),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useAppHealth(projectId: string) {
  return useQuery({
    queryKey: ['health', projectId],
    queryFn: () => api.health.get(projectId),
    enabled: !!projectId,
    refetchInterval: 30_000,
  })
}

export function useVcapServices(projectId: string) {
  return useQuery({
    queryKey: ['vcap', projectId],
    queryFn: () => api.vcap.get(projectId),
    enabled: !!projectId,
  })
}
