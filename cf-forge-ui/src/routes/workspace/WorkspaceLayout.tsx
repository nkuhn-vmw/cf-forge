import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  PanelLeftClose, PanelLeftOpen, TerminalSquare, MessageSquare,
  Rocket, ArrowLeft, Package, Globe, Activity, Database, ChevronDown, FileCode,
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
import { ManifestValidator } from '../../components/preview/ManifestValidator.tsx'
import { notify } from '../../store/notifications.ts'

type BottomTab = 'terminal' | 'preview' | 'health' | 'services' | 'manifest'
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
    triggerDeploy.mutate(env, {
      onSuccess: () => notify.success(`Deploy to ${env} started`),
      onError: (err: any) => notify.error('Deploy failed: ' + (err?.message ?? err)),
    })
    setDeployMenuOpen(false)
  }

  return (
    <div className="col-layout">
      {/* Toolbar */}
      <div className="ws-toolbar">
        <Link to="/dashboard" className="btn-icon text-muted">
          <ArrowLeft size={16} />
        </Link>
        <span className="text-primary font-semibold">
          {project?.name ?? 'Loading...'}
        </span>
        <span className="text-muted text-sm">
          {project?.language} {project?.framework ? `/ ${project.framework}` : ''}
        </span>
        <div className="flex-1" />

        <button
          onClick={() => toggleSidebar()}
          className="btn-ghost"
          title="Toggle Explorer"
        >
          {sidebarOpen ? <PanelLeftClose size={14} /> : <PanelLeftOpen size={14} />}
        </button>
        <button
          onClick={() => toggleTerminal()}
          className={`btn-ghost${terminalOpen ? ' active' : ''}`}
          title="Toggle Terminal"
        >
          <TerminalSquare size={14} />
        </button>
        <button
          onClick={() => toggleChat()}
          className={`btn-ghost${chatOpen ? ' active' : ''}`}
          title="Toggle AI Chat"
        >
          <MessageSquare size={14} />
        </button>

        <div className="divider-v" />

        <button
          onClick={() => triggerBuild.mutate(undefined, {
            onSuccess: () => notify.success('Build started'),
            onError: (err: any) => notify.error('Build failed: ' + (err?.message ?? err)),
          })}
          disabled={triggerBuild.isPending}
          className="btn-secondary"
        >
          <Package size={13} /> Build
        </button>

        {/* Deploy with strategy dropdown */}
        <div style={{ position: 'relative' }}>
          <div style={{ display: 'flex' }}>
            <button
              onClick={() => handleDeploy('STAGING')}
              disabled={triggerDeploy.isPending}
              className="btn-ghost"
              style={{
                padding: '4px 12px', backgroundColor: 'var(--success)',
                borderRadius: '4px 0 0 4px', color: 'white', fontSize: '12px',
              }}
            >
              <Rocket size={13} /> Deploy
            </button>
            <button
              onClick={() => setDeployMenuOpen(!deployMenuOpen)}
              className="btn-ghost"
              style={{
                padding: '4px 4px', backgroundColor: 'var(--success)',
                borderRadius: '0 4px 4px 0', color: 'white',
                borderLeft: '1px solid rgba(255,255,255,0.3)',
              }}
            >
              <ChevronDown size={12} />
            </button>
          </div>
          {deployMenuOpen && (
            <div className="deploy-dropdown">
              <button
                onClick={() => handleDeploy('STAGING')}
                className="deploy-dropdown-item"
              >
                <Rocket size={12} color="var(--warning)" /> Deploy to Staging
              </button>
              <button
                onClick={() => handleDeploy('PRODUCTION')}
                className="deploy-dropdown-item"
              >
                <Rocket size={12} color="var(--success)" /> Deploy to Production
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main content */}
      <div style={{ display: 'flex' }} className="flex-1 overflow-hidden">
        {/* Sidebar - File tree */}
        {sidebarOpen && (
          <div className="ws-sidebar">
            <FileTree projectId={projectId} />
          </div>
        )}

        {/* Editor + bottom panels */}
        <div className="col-layout flex-1 overflow-hidden">
          <div className="flex-1 overflow-hidden">
            <EditorPanel />
          </div>

          {/* Bottom panel with tabs */}
          {terminalOpen && (
            <div className="ws-bottom">
              {/* Bottom tab bar */}
              <div className="ws-tab-bar">
                {([
                  { key: 'terminal' as BottomTab, icon: TerminalSquare, label: 'Terminal' },
                  { key: 'preview' as BottomTab, icon: Globe, label: 'Preview' },
                  { key: 'health' as BottomTab, icon: Activity, label: 'Health' },
                  { key: 'services' as BottomTab, icon: Database, label: 'Services' },
                  { key: 'manifest' as BottomTab, icon: FileCode, label: 'Manifest' },
                ]).map(({ key, icon: Icon, label }) => (
                  <button
                    key={key}
                    onClick={() => setBottomTab(key)}
                    className={`ws-tab${bottomTab === key ? ' active' : ''}`}
                  >
                    <Icon size={12} /> {label}
                  </button>
                ))}
              </div>

              {/* Bottom tab content */}
              <div style={{ display: 'flex' }} className="flex-1 overflow-hidden">
                {bottomTab === 'terminal' && (
                  <>
                    <div className="flex-1">
                      <Terminal projectId={projectId} />
                    </div>
                    <div style={{ width: '300px', borderLeft: '1px solid var(--border)' }}>
                      <LogPanel projectId={projectId} />
                    </div>
                  </>
                )}
                {bottomTab === 'preview' && (
                  <div className="flex-1">
                    <LivePreview projectId={projectId} />
                  </div>
                )}
                {bottomTab === 'health' && (
                  <div className="flex-1 overflow-auto">
                    <AppHealthDashboard projectId={projectId} />
                  </div>
                )}
                {bottomTab === 'services' && (
                  <div className="flex-1 overflow-auto">
                    <VcapExplorer projectId={projectId} />
                  </div>
                )}
                {bottomTab === 'manifest' && (
                  <div className="flex-1 overflow-auto">
                    <ManifestValidator />
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Chat panel */}
        {chatOpen && (
          <div className="ws-chat">
            <ChatPanel projectId={projectId} />
          </div>
        )}
      </div>
    </div>
  )
}
