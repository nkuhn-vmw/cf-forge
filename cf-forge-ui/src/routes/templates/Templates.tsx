import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, BookTemplate, Download, Code2 } from 'lucide-react'
import { useTemplates, useScaffoldTemplate } from '../../api/queries.ts'

export function Templates() {
  const { data: templates, isLoading } = useTemplates()
  const scaffoldTemplate = useScaffoldTemplate()
  const navigate = useNavigate()

  const handleUseTemplate = (template: { slug: string }) => {
    scaffoldTemplate.mutate(template.slug, {
      onSuccess: (project) => navigate(`/workspace/${project.id}`),
    })
  }

  return (
    <div className="page">
      <header className="page-header">
        <Link to="/dashboard" className="btn-icon">
          <ArrowLeft size={18} />
        </Link>
        <BookTemplate size={20} color="var(--accent)" />
        <h1 className="page-header-title">Templates</h1>
      </header>

      <div className="content-container">
        <p className="text-md text-secondary mb-24">
          Start quickly with pre-configured Cloud Foundry application templates.
        </p>

        {isLoading ? (
          <div className="empty-state">Loading templates...</div>
        ) : !templates?.length ? (
          <div className="empty-state">
            <BookTemplate size={48} className="empty-state-icon" />
            <p className="empty-state-title">No templates available</p>
          </div>
        ) : (
          <div className="grid-auto-280">
            {templates.map((template) => (
              <div
                key={template.id}
                className="card-lg"
              >
                <div className="row gap-10 mb-10">
                  <Code2 size={18} color="var(--accent)" />
                  <h3 className="text-lg font-semibold">{template.name}</h3>
                </div>
                <p className="text-base text-secondary flex-1 mb-12" style={{ lineHeight: 1.5 }}>
                  {template.description || `${template.language} / ${template.framework} template`}
                </p>
                <div className="row-between">
                  <span className="row gap-4 text-sm text-muted">
                    <Download size={12} /> {template.downloadCount}
                  </span>
                  <button
                    onClick={() => handleUseTemplate(template)}
                    disabled={scaffoldTemplate.isPending}
                    className="btn-primary btn-sm"
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
