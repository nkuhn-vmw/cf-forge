import { useState } from 'react'
import { ExternalLink, RefreshCcw, Globe } from 'lucide-react'
import { useDeployments } from '../../api/queries.ts'

export function LivePreview({ projectId }: { projectId: string }) {
  const { data: deployments } = useDeployments(projectId)
  const [key, setKey] = useState(0)

  const latestDeployed = deployments?.find((d) => d.status === 'DEPLOYED')
  const url = latestDeployed?.deploymentUrl

  return (
    <div className="col-layout" style={{ backgroundColor: 'var(--bg-secondary)' }}>
      <div className="ws-toolbar">
        <Globe size={14} color="var(--accent)" />
        <span className="font-semibold text-secondary">Live Preview</span>
        <div className="flex-1" />
        {url && (
          <>
            <button
              onClick={() => setKey((k) => k + 1)}
              className="btn-icon"
              title="Refresh"
            >
              <RefreshCcw size={13} />
            </button>
            <a href={url} target="_blank" rel="noopener noreferrer" className="btn-icon" title="Open in new tab">
              <ExternalLink size={13} />
            </a>
          </>
        )}
      </div>
      <div className="flex-1 overflow-hidden">
        {url ? (
          <iframe
            key={key}
            src={url}
            style={{ width: '100%', height: '100%', border: 'none', backgroundColor: 'white' }}
            title="App Preview"
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
        ) : (
          <div className="editor-empty">
            <Globe size={32} className="empty-state-icon" />
            <span className="empty-state-text">Deploy your app to see a live preview</span>
          </div>
        )}
      </div>
    </div>
  )
}
