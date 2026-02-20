import { useState, useEffect } from 'react'
import { Activity, Cpu, HardDrive, Server, RefreshCcw } from 'lucide-react'
import '../../ui.css'

interface AppHealth {
  state: string
  instances: number
  memoryQuota: string
  diskQuota: string
  instanceDetails: InstanceDetail[]
}

interface InstanceDetail {
  index: number
  state: string
  cpuPercent: number
  memoryBytes: number
  memoryQuotaBytes: number
  diskBytes: number
  diskQuotaBytes: number
  uptime: number
}

function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(0)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

function formatUptime(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

export function AppHealthDashboard({ projectId }: { projectId: string }) {
  const [health, setHealth] = useState<AppHealth | null>(null)
  const [loading, setLoading] = useState(true)

  const fetchHealth = () => {
    setLoading(true)
    fetch(`/api/v1/projects/${projectId}/health`)
      .then((r) => r.json())
      .then((data) => { setHealth(data); setLoading(false) })
      .catch(() => { setHealth(null); setLoading(false) })
  }

  useEffect(() => { fetchHealth() }, [projectId])

  return (
    <div className="content-container-sm">
      <div className="row mb-16">
        <Activity size={16} color="var(--accent)" />
        <h3 className="text-md font-semibold">Application Health</h3>
        <button onClick={fetchHealth} className="btn-icon ml-auto">
          <RefreshCcw size={13} />
        </button>
      </div>

      {loading ? (
        <div className="text-muted text-base">Loading...</div>
      ) : !health ? (
        <div className="empty-state-sm">
          Deploy your app to see health metrics
        </div>
      ) : (
        <>
          <div className="grid-3 mb-16">
            <div className="metric-card">
              <Server size={16} color="var(--accent)" className="mb-4" />
              <div className="metric-card-value">{health.instances}</div>
              <div className="metric-card-label">Instances</div>
            </div>
            <div className="metric-card">
              <HardDrive size={16} color="var(--accent)" className="mb-4" />
              <div className="metric-card-value">{health.memoryQuota}</div>
              <div className="metric-card-label">Memory</div>
            </div>
            <div className="metric-card">
              <div className={`text-xl font-semibold mb-4 ${health.state === 'STARTED' ? 'status-running' : 'status-stopped'}`}>{health.state}</div>
              <div className="metric-card-label">State</div>
            </div>
          </div>

          {health.instanceDetails?.map((inst) => {
            const memPercent = inst.memoryQuotaBytes > 0 ? (inst.memoryBytes / inst.memoryQuotaBytes * 100) : 0
            const diskPercent = inst.diskQuotaBytes > 0 ? (inst.diskBytes / inst.diskQuotaBytes * 100) : 0
            return (
              <div key={inst.index} className="instance-card">
                <div className="instance-header">
                  <span className="font-semibold">Instance #{inst.index}</span>
                  <span className={inst.state === 'RUNNING' ? 'status-running' : 'status-stopped'}>{inst.state}</span>
                  <span className="text-muted">up {formatUptime(inst.uptime)}</span>
                </div>
                <div className="instance-metrics">
                  <div className="flex-1">
                    <div className="row-between text-muted mb-4">
                      <span><Cpu size={10} /> CPU</span><span>{inst.cpuPercent.toFixed(1)}%</span>
                    </div>
                    <div className="progress-track">
                      <div className="progress-fill progress-accent" style={{ width: `${Math.min(inst.cpuPercent, 100)}%` }} />
                    </div>
                  </div>
                  <div className="flex-1">
                    <div className="row-between text-muted mb-4">
                      <span>MEM</span><span>{formatBytes(inst.memoryBytes)}</span>
                    </div>
                    <div className="progress-track">
                      <div className={`progress-fill ${memPercent > 80 ? 'progress-warning' : 'progress-success'}`} style={{ width: `${Math.min(memPercent, 100)}%` }} />
                    </div>
                  </div>
                  <div className="flex-1">
                    <div className="row-between text-muted mb-4">
                      <span>DISK</span><span>{formatBytes(inst.diskBytes)}</span>
                    </div>
                    <div className="progress-track">
                      <div className={`progress-fill ${diskPercent > 80 ? 'progress-warning' : 'progress-success'}`} style={{ width: `${Math.min(diskPercent, 100)}%` }} />
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </>
      )}
    </div>
  )
}
