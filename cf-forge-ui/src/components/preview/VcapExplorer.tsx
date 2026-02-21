import { useState } from 'react'
import { Database, ChevronRight, ChevronDown, Key } from 'lucide-react'
import { useVcapServices } from '../../api/queries.ts'
import '../../ui.css'

export function VcapExplorer({ projectId }: { projectId: string }) {
  const { data: services, isLoading } = useVcapServices(projectId)
  const [expanded, setExpanded] = useState<string | null>(null)

  return (
    <div className="content-container-sm">
      <div className="row mb-16">
        <Database size={16} color="var(--accent)" />
        <h3 className="text-md font-semibold">Service Bindings</h3>
      </div>

      {isLoading ? (
        <div className="text-muted text-base">Loading...</div>
      ) : (services ?? []).length === 0 ? (
        <div className="empty-state-sm">
          No service bindings found
        </div>
      ) : (
        (services ?? []).map((svc) => (
          <div key={svc.name} className="vcap-service">
            <div
              onClick={() => setExpanded(expanded === svc.name ? null : svc.name)}
              className="vcap-service-header"
            >
              {expanded === svc.name ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              <Database size={14} color="var(--accent)" />
              <span className="font-medium">{svc.label}</span>
              <span className="text-muted text-sm">({svc.plan})</span>
              <span className="text-muted text-sm ml-auto">{svc.name}</span>
            </div>
            {expanded === svc.name && (
              <div className="vcap-service-body">
                {svc.tags.length > 0 && (
                  <div className="row mb-8 gap-4" style={{ flexWrap: 'wrap' }}>
                    {svc.tags.map((tag) => (
                      <span key={tag} className="badge-muted">
                        {tag}
                      </span>
                    ))}
                  </div>
                )}
                <div className="row text-muted text-sm mb-4 gap-4">
                  <Key size={11} /> Credentials
                </div>
                {Object.entries(svc.credentials).map(([key, val]) => (
                  <div key={key} className="vcap-cred-row">
                    <span className="vcap-cred-key">{key}</span>
                    <span className="vcap-cred-value">{val}</span>
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
