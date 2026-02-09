import { X } from 'lucide-react'
import { useWorkspaceStore } from '../../store/workspace.ts'

export function EditorTabs() {
  const { openFiles, activeFilePath, setActiveFile, closeFile } = useWorkspaceStore()

  if (openFiles.length === 0) return null

  return (
    <div
      style={{
        display: 'flex',
        backgroundColor: 'var(--bg-secondary)',
        borderBottom: '1px solid var(--border)',
        overflow: 'auto hidden',
        minHeight: '35px',
      }}
    >
      {openFiles.map((file) => (
        <div
          key={file.path}
          onClick={() => setActiveFile(file.path)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: '6px 12px',
            cursor: 'pointer',
            borderRight: '1px solid var(--border)',
            backgroundColor:
              file.path === activeFilePath ? 'var(--bg-primary)' : 'transparent',
            color:
              file.path === activeFilePath ? 'var(--text-primary)' : 'var(--text-secondary)',
            fontSize: '12px',
            whiteSpace: 'nowrap',
            userSelect: 'none',
          }}
        >
          <span>
            {file.modified && (
              <span style={{ color: 'var(--warning)', marginRight: '4px' }}>&#9679;</span>
            )}
            {file.name}
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation()
              closeFile(file.path)
            }}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              padding: '2px',
              display: 'flex',
              alignItems: 'center',
              borderRadius: '3px',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
          >
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  )
}
