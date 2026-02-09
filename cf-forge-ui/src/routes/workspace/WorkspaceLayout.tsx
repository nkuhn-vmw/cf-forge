import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  PanelLeftClose, PanelLeftOpen, TerminalSquare, MessageSquare,
  Rocket, ArrowLeft, Package, Globe, Activity, Database, ChevronDown,
} from 'lucide-react'
import { useWorkspaceStore } from '../../store/workspace.ts'
import { useProject } from '../../api/queries.ts'
import { useTriggerBuild, useTriggerDeploy } from '../../api/queries.ts'
import { FileTree } from '../../components/filetree/FileTree.tsx'
import { EditorPanel } from '../../components/editor/EditorPanel.tsx'
import { Terminal } from '../../components/terminal/Terminal.tsx'
import { LogPanel } from '../../components/logs/LogPanel.tsx'
import { ChatPanel } from '../../components/chat/ChatPanel.tsx'
import { LivePreview } from '../../components/preview/LivePreview.tsx'
import { AppHealthDashboard } from '../../components/preview/AppHealthDashboard.tsx'
import { VcapExplorer } from '../../components/preview/VcapExplorer.tsx'

type BottomTab = 'terminal' | 'preview' | 'health' | 'services'
type DeployEnv = 'STAGING' | 'PRODUCTION'

export function WorkspaceLayout() {
  const { projectId } = useParams<{ projectId: string }>()
  const { sidebarOpen, terminalOpen, chatOpen, toggleSidebar, toggleTerminal, toggleChat, setProjectId } =
    useWorkspaceStore()
  const { data: project } = useProject(projectId ?? '')
  const triggerBuild = useTriggerBuild(projectId ?? '')
  const triggerDeploy = useTriggerDeploy(projectId ?? '')
  const [bottomTab, setBottomTab] = useState<BottomTab>('terminal')
  const [deployMenuOpen, setDeployMenuOpen] = useState(false)

  useEffect(() => {
    if (projectId) setProjectId(projectId)
  }, [projectId, setProjectId])

  if (!projectId) return null

  const handleDeploy = (env: DeployEnv) => {
    triggerDeploy.mutate(env)
    setDeployMenuOpen(false)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Toolbar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '6px 12px',
          backgroundColor: 'var(--bg-secondary)',
          borderBottom: '1px solid var(--border)',
          fontSize: '13px',
        }}
      >
        <Link to="/dashboard" style={{ display: 'flex', alignItems: 'center', color: 'var(--text-muted)' }}>
          <ArrowLeft size={16} />
        </Link>
        <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>
          {project?.name ?? 'Loading...'}
        </span>
        <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>
          {project?.language} {project?.framework ? `/ ${project.framework}` : ''}
        </span>
        <div style={{ flex: 1 }} />

        <button
          onClick={() => toggleSidebar()}
          style={{ padding: '4px 8px', background: 'none', border: 'none', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px' }}
          title="Toggle Explorer"
        >
          {sidebarOpen ? <PanelLeftClose size={14} /> : <PanelLeftOpen size={14} />}
        </button>
        <button
          onClick={() => toggleTerminal()}
          style={{
            padding: '4px 8px', background: 'none', border: 'none', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px',
            color: terminalOpen ? 'var(--accent)' : 'var(--text-secondary)',
          }}
          title="Toggle Terminal"
        >
          <TerminalSquare size={14} />
        </button>
        <button
          onClick={() => toggleChat()}
          style={{
            padding: '4px 8px', background: 'none', border: 'none', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px',
            color: chatOpen ? 'var(--accent)' : 'var(--text-secondary)',
          }}
          title="Toggle AI Chat"
        >
          <MessageSquare size={14} />
        </button>

        <div style={{ width: '1px', height: '20px', backgroundColor: 'var(--border)', margin: '0 4px' }} />

        <button
          onClick={() => triggerBuild.mutate()}
          disabled={triggerBuild.isPending}
          style={{
            padding: '4px 12px', backgroundColor: 'var(--bg-tertiary)', border: '1px solid var(--border)',
            borderRadius: '4px', color: 'var(--text-primary)', display: 'flex', alignItems: 'center',
            gap: '4px', fontSize: '12px',
          }}
        >
          <Package size={13} /> Build
        </button>

        {/* Deploy with strategy dropdown */}
        <div style={{ position: 'relative' }}>
          <div style={{ display: 'flex' }}>
            <button
              onClick={() => handleDeploy('STAGING')}
              disabled={triggerDeploy.isPending}
              style={{
                padding: '4px 12px', backgroundColor: 'var(--success)', border: 'none',
                borderRadius: '4px 0 0 4px', color: 'white', display: 'flex', alignItems: 'center',
                gap: '4px', fontSize: '12px',
              }}
            >
              <Rocket size={13} /> Deploy
            </button>
            <button
              onClick={() => setDeployMenuOpen(!deployMenuOpen)}
              style={{
                padding: '4px 4px', backgroundColor: 'var(--success)', border: 'none',
                borderRadius: '0 4px 4px 0', color: 'white', display: 'flex', alignItems: 'center',
                borderLeft: '1px solid rgba(255,255,255,0.3)',
              }}
            >
              <ChevronDown size={12} />
            </button>
          </div>
          {deployMenuOpen && (
            <div
              style={{
                position: 'absolute', top: '100%', right: 0, marginTop: '4px',
                backgroundColor: 'var(--bg-secondary)', border: '1px solid var(--border)',
                borderRadius: '6px', overflow: 'hidden', zIndex: 10, minWidth: '180px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
              }}
            >
              <button
                onClick={() => handleDeploy('STAGING')}
                style={{ width: '100%', padding: '8px 12px', background: 'none', border: 'none', color: 'var(--text-primary)', fontSize: '12px', textAlign: 'left', display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <Rocket size={12} color="var(--warning)" /> Deploy to Staging
              </button>
              <button
                onClick={() => handleDeploy('PRODUCTION')}
                style={{ width: '100%', padding: '8px 12px', background: 'none', border: 'none', color: 'var(--text-primary)', fontSize: '12px', textAlign: 'left', display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <Rocket size={12} color="var(--success)" /> Deploy to Production
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main content */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Sidebar - File tree */}
        {sidebarOpen && (
          <div style={{ width: '250px', flexShrink: 0 }}>
            <FileTree projectId={projectId} />
          </div>
        )}

        {/* Editor + bottom panels */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <EditorPanel />
          </div>

          {/* Bottom panel with tabs */}
          {terminalOpen && (
            <div style={{ height: '280px', borderTop: '1px solid var(--border)', display: 'flex', flexDirection: 'column' }}>
              {/* Bottom tab bar */}
              <div style={{ display: 'flex', borderBottom: '1px solid var(--border)', backgroundColor: 'var(--bg-secondary)' }}>
                {([
                  { key: 'terminal' as BottomTab, icon: TerminalSquare, label: 'Terminal' },
                  { key: 'preview' as BottomTab, icon: Globe, label: 'Preview' },
                  { key: 'health' as BottomTab, icon: Activity, label: 'Health' },
                  { key: 'services' as BottomTab, icon: Database, label: 'Services' },
                ]).map(({ key, icon: Icon, label }) => (
                  <button
                    key={key}
                    onClick={() => setBottomTab(key)}
                    style={{
                      padding: '6px 14px', background: 'none', border: 'none', fontSize: '11px',
                      color: bottomTab === key ? 'var(--accent)' : 'var(--text-muted)',
                      borderBottom: bottomTab === key ? '2px solid var(--accent)' : '2px solid transparent',
                      display: 'flex', alignItems: 'center', gap: '4px',
                    }}
                  >
                    <Icon size={12} /> {label}
                  </button>
                ))}
              </div>

              {/* Bottom tab content */}
              <div style={{ flex: 1, overflow: 'hidden', display: 'flex' }}>
                {bottomTab === 'terminal' && (
                  <>
                    <div style={{ flex: 1 }}>
                      <Terminal projectId={projectId} />
                    </div>
                    <div style={{ width: '300px', borderLeft: '1px solid var(--border)' }}>
                      <LogPanel projectId={projectId} />
                    </div>
                  </>
                )}
                {bottomTab === 'preview' && (
                  <div style={{ flex: 1 }}>
                    <LivePreview projectId={projectId} />
                  </div>
                )}
                {bottomTab === 'health' && (
                  <div style={{ flex: 1, overflow: 'auto' }}>
                    <AppHealthDashboard projectId={projectId} />
                  </div>
                )}
                {bottomTab === 'services' && (
                  <div style={{ flex: 1, overflow: 'auto' }}>
                    <VcapExplorer projectId={projectId} />
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Chat panel */}
        {chatOpen && (
          <div style={{ width: '350px', flexShrink: 0 }}>
            <ChatPanel projectId={projectId} />
          </div>
        )}
      </div>
    </div>
  )
}
