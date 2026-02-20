import { useState } from 'react'
import { X } from 'lucide-react'
import { useCreateProject } from '../../api/queries.ts'

const LANGUAGES = ['JAVA', 'NODEJS', 'PYTHON', 'GO', 'DOTNET', 'RUBY', 'STATICFILE']
const FRAMEWORKS: Record<string, string[]> = {
  JAVA: ['Spring Boot', 'Micronaut', 'Quarkus'],
  NODEJS: ['Express', 'Fastify', 'NestJS', 'Next.js'],
  PYTHON: ['Flask', 'Django', 'FastAPI'],
  GO: ['Gin', 'Echo', 'Fiber'],
  DOTNET: ['ASP.NET Core', 'Blazor'],
  RUBY: ['Rails', 'Sinatra'],
  STATICFILE: ['React', 'Vue', 'Angular', 'Static HTML'],
}

export function CreateProjectDialog({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState('')
  const [language, setLanguage] = useState('JAVA')
  const [framework, setFramework] = useState('')
  const createProject = useCreateProject()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    createProject.mutate(
      { name: name.trim(), language, framework },
      { onSuccess: () => onClose() }
    )
  }

  return (
    <div
      className="dialog-overlay"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="dialog">
        <div className="dialog-header">
          <h2 style={{ fontSize: '16px' }} className="font-semibold">New Project</h2>
          <button onClick={onClose} className="btn-icon">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="dialog-body">
          <div className="form-group">
            <label className="form-label">
              Project Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="my-cf-app"
              autoFocus
              className="form-input-full"
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Language
            </label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {LANGUAGES.map((lang) => (
                <button
                  key={lang}
                  type="button"
                  onClick={() => { setLanguage(lang); setFramework('') }}
                  className={`chip-toggle ${language === lang ? 'active' : ''}`}
                >
                  {lang}
                </button>
              ))}
            </div>
          </div>

          <div className="mb-24">
            <label className="form-label">
              Framework
            </label>
            <select
              value={framework}
              onChange={(e) => setFramework(e.target.value)}
              className="form-input-full"
            >
              <option value="">Select a framework...</option>
              {(FRAMEWORKS[language] ?? []).map((fw) => (
                <option key={fw} value={fw}>{fw}</option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={!name.trim() || createProject.isPending}
            className={`btn-primary ${!name.trim() || createProject.isPending ? 'btn-disabled' : ''}`}
            style={{ width: '100%', justifyContent: 'center' }}
          >
            {createProject.isPending ? 'Creating...' : 'Create Project'}
          </button>
        </form>
      </div>
    </div>
  )
}
