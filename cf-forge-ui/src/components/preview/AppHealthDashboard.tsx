import { useState, useEffect } from 'react'
import { Activity, Cpu, HardDrive, Server, RefreshCcw } from 'lucide-react'

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
    <div style={{ padding: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
        <Activity size={16} color="var(--accent)" />
        <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Application Health</h3>
        <button onClick={fetchHealth} style={{ marginLeft: 'auto', padding: '2px 6px', background: 'none', border: 'none', color: 'var(--text-muted)', display: 'flex' }}>
          <RefreshCcw size={13} />
        </button>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Loading...</div>
      ) : !health ? (
        <div style={{ color: 'var(--text-muted)', fontSize: '13px', textAlign: 'center', padding: '20px' }}>
          Deploy your app to see health metrics
        </div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', marginBottom: '16px' }}>
            <div style={{ padding: '10px', backgroundColor: 'var(--bg-tertiary)', borderRadius: '6px', textAlign: 'center' }}>
              <Server size={16} color="var(--accent)" style={{ marginBottom: '4px' }} />
              <div style={{ fontSize: '18px', fontWeight: 600 }}>{health.instances}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Instances</div>
            </div>
            <div style={{ padding: '10px', backgroundColor: 'var(--bg-tertiary)', borderRadius: '6px', textAlign: 'center' }}>
              <HardDrive size={16} color="var(--accent)" style={{ marginBottom: '4px' }} />
              <div style={{ fontSize: '18px', fontWeight: 600 }}>{health.memoryQuota}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Memory</div>
            </div>
            <div style={{ padding: '10px', backgroundColor: 'var(--bg-tertiary)', borderRadius: '6px', textAlign: 'center' }}>
              <div style={{ fontSize: '16px', fontWeight: 600, color: health.state === 'STARTED' ? 'var(--success)' : 'var(--danger)', marginBottom: '4px' }}>{health.state}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>State</div>
            </div>
          </div>

          {health.instanceDetails?.map((inst) => {
            const memPercent = inst.memoryQuotaBytes > 0 ? (inst.memoryBytes / inst.memoryQuotaBytes * 100) : 0
            const diskPercent = inst.diskQuotaBytes > 0 ? (inst.diskBytes / inst.diskQuotaBytes * 100) : 0
            return (
              <div key={inst.index} style={{ marginBottom: '8px', padding: '10px', border: '1px solid var(--border)', borderRadius: '6px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '12px' }}>
                  <span style={{ fontWeight: 600 }}>Instance #{inst.index}</span>
                  <span style={{ color: inst.state === 'RUNNING' ? 'var(--success)' : 'var(--danger)' }}>{inst.state}</span>
                  <span style={{ color: 'var(--text-muted)' }}>up {formatUptime(inst.uptime)}</span>
                </div>
                <div style={{ display: 'flex', gap: '12px', fontSize: '11px' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)', marginBottom: '2px' }}>
                      <span><Cpu size={10} /> CPU</span><span>{inst.cpuPercent.toFixed(1)}%</span>
                    </div>
                    <div style={{ height: '4px', backgroundColor: 'var(--bg-hover)', borderRadius: '2px' }}>
                      <div style={{ height: '100%', width: `${Math.min(inst.cpuPercent, 100)}%`, backgroundColor: 'var(--accent)', borderRadius: '2px' }} />
                    </div>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)', marginBottom: '2px' }}>
                      <span>MEM</span><span>{formatBytes(inst.memoryBytes)}</span>
                    </div>
                    <div style={{ height: '4px', backgroundColor: 'var(--bg-hover)', borderRadius: '2px' }}>
                      <div style={{ height: '100%', width: `${Math.min(memPercent, 100)}%`, backgroundColor: memPercent > 80 ? 'var(--warning)' : 'var(--success)', borderRadius: '2px' }} />
                    </div>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)', marginBottom: '2px' }}>
                      <span>DISK</span><span>{formatBytes(inst.diskBytes)}</span>
                    </div>
                    <div style={{ height: '4px', backgroundColor: 'var(--bg-hover)', borderRadius: '2px' }}>
                      <div style={{ height: '100%', width: `${Math.min(diskPercent, 100)}%`, backgroundColor: diskPercent > 80 ? 'var(--warning)' : 'var(--success)', borderRadius: '2px' }} />
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
