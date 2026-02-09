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
      style={{
        position: 'fixed', inset: 0, display: 'flex', alignItems: 'center',
        justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 100,
      }}
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div style={{ backgroundColor: 'var(--bg-secondary)', borderRadius: '12px', border: '1px solid var(--border)', width: '420px', maxHeight: '90vh', overflow: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
          <h2 style={{ fontSize: '16px', fontWeight: 600 }}>New Project</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', display: 'flex' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ padding: '20px' }}>
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: '6px' }}>
              Project Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="my-cf-app"
              autoFocus
              style={{
                width: '100%', padding: '8px 12px', backgroundColor: 'var(--bg-primary)',
                border: '1px solid var(--border)', borderRadius: '6px', color: 'var(--text-primary)',
                fontSize: '13px', outline: 'none',
              }}
            />
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: '6px' }}>
              Language
            </label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {LANGUAGES.map((lang) => (
                <button
                  key={lang}
                  type="button"
                  onClick={() => { setLanguage(lang); setFramework('') }}
                  style={{
                    padding: '6px 12px', border: '1px solid var(--border)', borderRadius: '6px',
                    fontSize: '12px', backgroundColor: language === lang ? 'var(--accent)' : 'var(--bg-tertiary)',
                    color: language === lang ? 'white' : 'var(--text-secondary)',
                  }}
                >
                  {lang}
                </button>
              ))}
            </div>
          </div>

          <div style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: '6px' }}>
              Framework
            </label>
            <select
              value={framework}
              onChange={(e) => setFramework(e.target.value)}
              style={{
                width: '100%', padding: '8px 12px', backgroundColor: 'var(--bg-primary)',
                border: '1px solid var(--border)', borderRadius: '6px', color: 'var(--text-primary)',
                fontSize: '13px', outline: 'none',
              }}
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
            style={{
              width: '100%', padding: '10px', backgroundColor: 'var(--accent)', color: 'white',
              border: 'none', borderRadius: '6px', fontSize: '14px', fontWeight: 500,
              opacity: !name.trim() || createProject.isPending ? 0.5 : 1,
            }}
          >
            {createProject.isPending ? 'Creating...' : 'Create Project'}
          </button>
        </form>
      </div>
    </div>
  )
}
