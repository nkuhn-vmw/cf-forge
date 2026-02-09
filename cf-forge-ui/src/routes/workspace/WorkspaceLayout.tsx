import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  PanelLeftClose, PanelLeftOpen, TerminalSquare, MessageSquare,
  Rocket, ArrowLeft, Package,
} from 'lucide-react'
import { useWorkspaceStore } from '../../store/workspace.ts'
import { useProject } from '../../api/queries.ts'
import { useTriggerBuild, useTriggerDeploy } from '../../api/queries.ts'
import { FileTree } from '../../components/filetree/FileTree.tsx'
import { EditorPanel } from '../../components/editor/EditorPanel.tsx'
import { Terminal } from '../../components/terminal/Terminal.tsx'
import { LogPanel } from '../../components/logs/LogPanel.tsx'
import { ChatPanel } from '../../components/chat/ChatPanel.tsx'

export function WorkspaceLayout() {
  const { projectId } = useParams<{ projectId: string }>()
  const { sidebarOpen, terminalOpen, chatOpen, toggleSidebar, toggleTerminal, toggleChat, setProjectId } =
    useWorkspaceStore()
  const { data: project } = useProject(projectId ?? '')
  const triggerBuild = useTriggerBuild(projectId ?? '')
  const triggerDeploy = useTriggerDeploy(projectId ?? '')

  useEffect(() => {
    if (projectId) setProjectId(projectId)
  }, [projectId, setProjectId])

  if (!projectId) return null

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
        <button
          onClick={() => triggerDeploy.mutate('STAGING')}
          disabled={triggerDeploy.isPending}
          style={{
            padding: '4px 12px', backgroundColor: 'var(--success)', border: 'none',
            borderRadius: '4px', color: 'white', display: 'flex', alignItems: 'center',
            gap: '4px', fontSize: '12px',
          }}
        >
          <Rocket size={13} /> Deploy
        </button>
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

          {/* Bottom panel: terminal or logs */}
          {terminalOpen && (
            <div style={{ height: '250px', borderTop: '1px solid var(--border)', display: 'flex' }}>
              <div style={{ flex: 1 }}>
                <Terminal projectId={projectId} />
              </div>
              <div style={{ width: '300px', borderLeft: '1px solid var(--border)' }}>
                <LogPanel projectId={projectId} />
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
