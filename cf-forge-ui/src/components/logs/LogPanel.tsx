import { useBuilds } from '../../api/queries.ts'

export function LogPanel({ projectId }: { projectId: string }) {
  const { data: builds } = useBuilds(projectId)
  const latestBuild = builds?.[0]

  return (
    <div className="log-panel">
      <div className="log-panel-header">
        <span>Build Logs</span>
        {latestBuild && (
          <span className={latestBuild.status === 'SUCCESS' ? 'text-success' : latestBuild.status === 'FAILED' ? 'text-danger' : 'text-warning'}>
            {latestBuild.status}
          </span>
        )}
      </div>
      <div className="log-panel-body">
        {latestBuild?.buildLog ? (
          latestBuild.buildLog
        ) : (
          <span className="text-muted">No build logs yet. Trigger a build to see output here.</span>
        )}
      </div>
    </div>
  )
}
