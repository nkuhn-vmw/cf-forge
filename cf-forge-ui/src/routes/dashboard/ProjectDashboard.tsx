import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Plus, Code2, Rocket, Settings, Trash2, Store, BookTemplate } from 'lucide-react'
import { useProjects, useDeleteProject } from '../../api/queries.ts'
import { CreateProjectDialog } from './CreateProjectDialog.tsx'

export function ProjectDashboard() {
  const [showCreate, setShowCreate] = useState(false)
  const { data: projects, isLoading } = useProjects()
  const deleteProject = useDeleteProject()
  const navigate = useNavigate()

  const languageColors: Record<string, string> = {
    JAVA: '#b07219',
    NODEJS: '#f1e05a',
    PYTHON: '#3572A5',
    GO: '#00ADD8',
    DOTNET: '#178600',
    RUBY: '#701516',
    STATICFILE: '#e34c26',
  }

  return (
    <div style={{ height: '100%', overflow: 'auto', backgroundColor: 'var(--bg-primary)' }}>
      <header
        style={{
          padding: '16px 24px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: 'var(--bg-secondary)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Code2 size={24} color="var(--accent)" />
          <h1 style={{ fontSize: '18px', fontWeight: 600 }}>CF Forge</h1>
        </div>
        <nav style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <Link to="/builder" style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)', fontSize: '13px' }}>
            <Rocket size={14} /> Builder
          </Link>
          <Link to="/marketplace" style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)', fontSize: '13px' }}>
            <Store size={14} /> Marketplace
          </Link>
          <Link to="/templates" style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)', fontSize: '13px' }}>
            <BookTemplate size={14} /> Templates
          </Link>
        </nav>
      </header>

      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 600 }}>Projects</h2>
          <button
            onClick={() => setShowCreate(true)}
            style={{
              display: 'flex', alignItems: 'center', gap: '6px',
              padding: '8px 16px', backgroundColor: 'var(--accent)', color: 'white',
              border: 'none', borderRadius: '6px', fontSize: '13px', fontWeight: 500,
            }}
          >
            <Plus size={14} /> New Project
          </button>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading projects...</div>
        ) : !projects?.length ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            <Code2 size={48} style={{ marginBottom: '16px', opacity: 0.3 }} />
            <p style={{ fontSize: '16px', marginBottom: '8px' }}>No projects yet</p>
            <p style={{ fontSize: '13px' }}>Create your first Cloud Foundry application</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
            {projects.map((project) => (
              <div
                key={project.id}
                onClick={() => navigate(`/workspace/${project.id}`)}
                style={{
                  padding: '16px',
                  backgroundColor: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  transition: 'border-color 0.2s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--accent)')}
                onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border)')}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{project.name}</h3>
                  <div style={{ display: 'flex', gap: '4px' }}>
                    <button
                      onClick={(e) => { e.stopPropagation(); navigate(`/workspace/${project.id}`) }}
                      style={{ padding: '4px', background: 'none', border: 'none', color: 'var(--text-muted)' }}
                    >
                      <Settings size={14} />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        if (confirm('Delete this project?')) deleteProject.mutate(project.id)
                      }}
                      style={{ padding: '4px', background: 'none', border: 'none', color: 'var(--text-muted)' }}
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <span
                    style={{
                      display: 'inline-block',
                      width: '10px',
                      height: '10px',
                      borderRadius: '50%',
                      backgroundColor: languageColors[project.language] ?? 'var(--text-muted)',
                    }}
                  />
                  <span>{project.language}</span>
                  {project.framework && <span>/ {project.framework}</span>}
                </div>
                <div style={{ marginTop: '8px', fontSize: '11px', color: 'var(--text-muted)' }}>
                  Updated {new Date(project.updatedAt).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {showCreate && <CreateProjectDialog onClose={() => setShowCreate(false)} />}
    </div>
  )
}
