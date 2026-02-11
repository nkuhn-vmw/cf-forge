import { useBuilds } from '../../api/queries.ts'

export function LogPanel({ projectId }: { projectId: string }) {
  const { data: builds } = useBuilds(projectId)
  const latestBuild = builds?.[0]

  return (
    <div
      style={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#0d1117',
        color: '#e6edf3',
        fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
        fontSize: '12px',
      }}
    >
      <div
        style={{
          padding: '6px 10px',
          borderBottom: '1px solid var(--border)',
          fontSize: '11px',
          color: 'var(--text-muted)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <span>Build Logs</span>
        {latestBuild && (
          <span style={{ color: latestBuild.status === 'SUCCESS' ? '#3fb950' : latestBuild.status === 'FAILED' ? '#f85149' : '#d29922' }}>
            {latestBuild.status}
          </span>
        )}
      </div>
      <div style={{ flex: 1, overflow: 'auto', padding: '8px 10px', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
        {latestBuild?.buildLog ? (
          latestBuild.buildLog
        ) : (
          <span style={{ color: '#484f58' }}>No build logs yet. Trigger a build to see output here.</span>
        )}
      </div>
    </div>
  )
}
