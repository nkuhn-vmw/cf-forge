import { useState } from 'react'
import { ExternalLink, RefreshCcw, Globe } from 'lucide-react'
import { useDeployments } from '../../api/queries.ts'

export function LivePreview({ projectId }: { projectId: string }) {
  const { data: deployments } = useDeployments(projectId)
  const [key, setKey] = useState(0)

  const latestDeployed = deployments?.find((d) => d.status === 'DEPLOYED')
  const url = latestDeployed?.deploymentUrl

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', backgroundColor: 'var(--bg-secondary)' }}>
      <div
        style={{
          display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px',
          borderBottom: '1px solid var(--border)', fontSize: '12px',
        }}
      >
        <Globe size={14} color="var(--accent)" />
        <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Live Preview</span>
        <div style={{ flex: 1 }} />
        {url && (
          <>
            <button
              onClick={() => setKey((k) => k + 1)}
              style={{ padding: '2px 6px', background: 'none', border: 'none', color: 'var(--text-muted)', display: 'flex' }}
              title="Refresh"
            >
              <RefreshCcw size={13} />
            </button>
            <a href={url} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', color: 'var(--text-muted)' }} title="Open in new tab">
              <ExternalLink size={13} />
            </a>
          </>
        )}
      </div>
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {url ? (
          <iframe
            key={key}
            src={url}
            style={{ width: '100%', height: '100%', border: 'none', backgroundColor: 'white' }}
            title="App Preview"
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)', flexDirection: 'column', gap: '8px' }}>
            <Globe size={32} style={{ opacity: 0.3 }} />
            <span style={{ fontSize: '13px' }}>Deploy your app to see a live preview</span>
          </div>
        )}
      </div>
    </div>
  )
}
