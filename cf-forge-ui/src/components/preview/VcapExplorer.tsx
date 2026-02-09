import { useState, useEffect } from 'react'
import { Database, ChevronRight, ChevronDown, Key } from 'lucide-react'

interface VcapService {
  label: string
  name: string
  plan: string
  credentials: Record<string, string>
  tags: string[]
}

export function VcapExplorer({ projectId }: { projectId: string }) {
  const [services, setServices] = useState<VcapService[]>([])
  const [expanded, setExpanded] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch(`/api/v1/projects/${projectId}/vcap`)
      .then((r) => r.json())
      .then((data) => {
        setServices(data)
        setLoading(false)
      })
      .catch(() => {
        setServices([])
        setLoading(false)
      })
  }, [projectId])

  return (
    <div style={{ padding: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
        <Database size={16} color="var(--accent)" />
        <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Service Bindings</h3>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Loading...</div>
      ) : services.length === 0 ? (
        <div style={{ color: 'var(--text-muted)', fontSize: '13px', textAlign: 'center', padding: '20px' }}>
          No service bindings found
        </div>
      ) : (
        services.map((svc) => (
          <div key={svc.name} style={{ marginBottom: '8px', border: '1px solid var(--border)', borderRadius: '6px', overflow: 'hidden' }}>
            <div
              onClick={() => setExpanded(expanded === svc.name ? null : svc.name)}
              style={{
                display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px',
                cursor: 'pointer', backgroundColor: 'var(--bg-tertiary)', fontSize: '13px',
              }}
            >
              {expanded === svc.name ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              <Database size={14} color="var(--accent)" />
              <span style={{ fontWeight: 500 }}>{svc.label}</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>({svc.plan})</span>
              <span style={{ color: 'var(--text-muted)', fontSize: '11px', marginLeft: 'auto' }}>{svc.name}</span>
            </div>
            {expanded === svc.name && (
              <div style={{ padding: '8px 12px', fontSize: '12px', fontFamily: 'monospace' }}>
                {svc.tags.length > 0 && (
                  <div style={{ marginBottom: '8px', display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {svc.tags.map((tag) => (
                      <span key={tag} style={{ padding: '2px 6px', backgroundColor: 'var(--bg-hover)', borderRadius: '3px', fontSize: '11px', color: 'var(--text-muted)' }}>
                        {tag}
                      </span>
                    ))}
                  </div>
                )}
                <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Key size={11} /> Credentials
                </div>
                {Object.entries(svc.credentials).map(([key, val]) => (
                  <div key={key} style={{ display: 'flex', gap: '8px', padding: '2px 0', borderBottom: '1px solid var(--border)' }}>
                    <span style={{ color: 'var(--accent)', minWidth: '120px' }}>{key}</span>
                    <span style={{ color: 'var(--text-secondary)', wordBreak: 'break-all' }}>{val}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))
      )}
    </div>
  )
}
