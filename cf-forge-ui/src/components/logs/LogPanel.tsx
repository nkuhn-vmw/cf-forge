import { useRef, useEffect } from 'react'
import { useBuilds } from '../../api/queries.ts'

export function LogPanel({ projectId }: { projectId: string }) {
  const { data: builds, refetch } = useBuilds(projectId)
  const latestBuild = builds?.[0]
  const scrollRef = useRef<HTMLDivElement>(null)
  const isActive = latestBuild?.status === 'BUILDING' || latestBuild?.status === 'PENDING'

  // Poll during active builds
  useEffect(() => {
    if (!isActive) return
    const interval = setInterval(() => refetch(), 3000)
    return () => clearInterval(interval)
  }, [isActive, refetch])

  // Auto-scroll when log content changes
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [latestBuild?.buildLog])

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
      <div ref={scrollRef} className="log-panel-body">
        {latestBuild?.buildLog ? (
          latestBuild.buildLog
        ) : (
          <span className="text-muted">No build logs yet. Trigger a build to see output here.</span>
        )}
      </div>
    </div>
  )
}
