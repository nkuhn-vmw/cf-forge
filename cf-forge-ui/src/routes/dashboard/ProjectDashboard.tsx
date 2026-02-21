import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Plus, Code2, Rocket, Trash2, Store, BookTemplate, LogOut, FileCode, AlertTriangle } from 'lucide-react'
import { useProjects, useDeleteProject } from '../../api/queries.ts'
import { CreateProjectDialog } from './CreateProjectDialog.tsx'
import { useAuthStore } from '../../store/auth.ts'
import { notify } from '../../store/notifications.ts'

export function ProjectDashboard() {
  const [showCreate, setShowCreate] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string } | null>(null)
  const { data: projects, isLoading, isError } = useProjects()
  const deleteProject = useDeleteProject()
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  const languageColors: Record<string, string> = {
    JAVA: '#b07219',
    NODEJS: '#f1e05a',
    PYTHON: '#3572A5',
    GO: '#00ADD8',
    DOTNET: '#178600',
    RUBY: '#701516',
    STATICFILE: '#e34c26',
  }

  const handleDelete = () => {
    if (!deleteTarget) return
    deleteProject.mutate(deleteTarget.id, {
      onSuccess: () => {
        notify.success(`Deleted "${deleteTarget.name}"`)
        setDeleteTarget(null)
      },
      onError: (err: any) => {
        notify.error('Delete failed: ' + (err?.message ?? err))
        setDeleteTarget(null)
      },
    })
  }

  return (
    <div className="page">
      <header className="page-header-lg">
        <div className="row gap-12">
          <Code2 size={24} color="var(--accent)" />
          <h1 className="text-2xl font-semibold">CF Forge</h1>
        </div>
        <nav className="row gap-16">
          <Link to="/builder" className="nav-link">
            <Rocket size={14} /> Builder
          </Link>
          <Link to="/marketplace" className="nav-link">
            <Store size={14} /> Marketplace
          </Link>
          <Link to="/templates" className="nav-link">
            <BookTemplate size={14} /> Templates
          </Link>
          <Link to="/migration" className="nav-link">
            <FileCode size={14} /> Migration
          </Link>
          <div className="row gap-8" style={{ marginLeft: '8px', paddingLeft: '16px', borderLeft: '1px solid var(--border)' }}>
            <span className="text-base text-secondary">{user?.userName || user?.email}</span>
            <button
              onClick={() => { logout(); navigate('/login') }}
              className="btn-icon"
              title="Sign out"
            >
              <LogOut size={14} />
            </button>
          </div>
        </nav>
      </header>

      <div className="content-container">
        <div className="row-between mb-24">
          <h2 className="text-3xl font-semibold">Projects</h2>
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary"
          >
            <Plus size={14} /> New Project
          </button>
        </div>

        {isError ? (
          <div className="empty-state">
            <AlertTriangle size={48} className="empty-state-icon" />
            <p className="empty-state-title">Failed to load projects</p>
            <p className="empty-state-text">Check your connection and try refreshing the page</p>
          </div>
        ) : isLoading ? (
          <div className="empty-state">Loading projects...</div>
        ) : !projects?.length ? (
          <div className="empty-state">
            <Code2 size={48} className="empty-state-icon" />
            <p className="empty-state-title">No projects yet</p>
            <p className="empty-state-text">Create your first Cloud Foundry application</p>
          </div>
        ) : (
          <div className="grid-auto-300">
            {projects.map((project) => (
              <div
                key={project.id}
                onClick={() => navigate(`/workspace/${project.id}`)}
                className="card-clickable"
              >
                <div className="row-between mb-8">
                  <h3 className="text-lg font-semibold">{project.name}</h3>
                  <div className="row gap-4">
                    <button
                      onClick={(e) => { e.stopPropagation(); navigate(`/workspace/${project.id}`) }}
                      className="btn-icon"
                      title="Open workspace"
                    >
                      <Code2 size={14} />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        setDeleteTarget({ id: project.id, name: project.name })
                      }}
                      disabled={deleteProject.isPending}
                      className="btn-icon"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
                <div className="row gap-8 text-base text-secondary mb-8">
                  <span
                    className="lang-dot"
                    style={{ backgroundColor: languageColors[project.language] ?? 'var(--text-muted)' }}
                  />
                  <span>{project.language}</span>
                  {project.framework && <span>/ {project.framework}</span>}
                </div>
                <div className="text-sm text-muted">
                  Updated {new Date(project.updatedAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {showCreate && <CreateProjectDialog onClose={() => setShowCreate(false)} />}

      {deleteTarget && (
        <div className="dialog-overlay" onClick={() => setDeleteTarget(null)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-8">Delete Project</h3>
            <p className="text-secondary mb-16">
              Are you sure you want to delete &ldquo;{deleteTarget.name}&rdquo;? This action cannot be undone.
            </p>
            <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
              <button onClick={() => setDeleteTarget(null)} className="btn-secondary">
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={deleteProject.isPending}
                className="btn-danger"
              >
                {deleteProject.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
