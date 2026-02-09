import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, BookTemplate, Download, Code2 } from 'lucide-react'
import { useTemplates, useCreateProject } from '../../api/queries.ts'

export function Templates() {
  const { data: templates, isLoading } = useTemplates()
  const createProject = useCreateProject()
  const navigate = useNavigate()

  const handleUseTemplate = (template: { name: string; language: string; framework: string }) => {
    createProject.mutate(
      { name: `${template.name}-project`, language: template.language, framework: template.framework },
      { onSuccess: (project) => navigate(`/workspace/${project.id}`) }
    )
  }

  return (
    <div style={{ height: '100%', overflow: 'auto', backgroundColor: 'var(--bg-primary)' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          backgroundColor: 'var(--bg-secondary)',
        }}
      >
        <Link to="/dashboard" style={{ color: 'var(--text-muted)', display: 'flex' }}>
          <ArrowLeft size={18} />
        </Link>
        <BookTemplate size={20} color="var(--accent)" />
        <h1 style={{ fontSize: '16px', fontWeight: 600 }}>Templates</h1>
      </header>

      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '24px' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px', marginBottom: '24px' }}>
          Start quickly with pre-configured Cloud Foundry application templates.
        </p>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>Loading templates...</div>
        ) : !templates?.length ? (
          <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
            <BookTemplate size={48} style={{ marginBottom: '16px', opacity: 0.3 }} />
            <p style={{ fontSize: '16px', marginBottom: '8px' }}>No templates available</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
            {templates.map((template) => (
              <div
                key={template.id}
                style={{
                  padding: '20px',
                  backgroundColor: 'var(--bg-secondary)',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  display: 'flex',
                  flexDirection: 'column',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                  <Code2 size={18} color="var(--accent)" />
                  <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{template.name}</h3>
                </div>
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.5', marginBottom: '12px', flex: 1 }}>
                  {template.description || `${template.language} / ${template.framework} template`}
                </p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Download size={12} /> {template.downloadCount}
                  </span>
                  <button
                    onClick={() => handleUseTemplate(template)}
                    disabled={createProject.isPending}
                    style={{
                      padding: '6px 14px', backgroundColor: 'var(--accent)', color: 'white',
                      border: 'none', borderRadius: '6px', fontSize: '12px', fontWeight: 500,
                    }}
                  >
                    Use Template
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
