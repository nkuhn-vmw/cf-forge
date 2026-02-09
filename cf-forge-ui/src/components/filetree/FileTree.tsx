import { useState } from 'react'
import { ChevronRight, ChevronDown, File, Folder, FolderOpen } from 'lucide-react'
import { useFiles } from '../../api/queries.ts'
import { useWorkspaceStore } from '../../store/workspace.ts'
import { api } from '../../api/client.ts'
import type { FileEntry } from '../../api/client.ts'

function FileTreeNode({ entry, projectId, depth }: { entry: FileEntry; projectId: string; depth: number }) {
  const [expanded, setExpanded] = useState(false)
  const { data: children } = useFiles(projectId, expanded ? entry.path : undefined)
  const openFile = useWorkspaceStore((s) => s.openFile)

  const handleClick = async () => {
    if (entry.directory) {
      setExpanded(!expanded)
    } else {
      try {
        const result = await api.files.read(projectId, entry.path)
        openFile({
          path: entry.path,
          name: entry.name,
          content: result.content,
          language: '',
          modified: false,
        })
      } catch (err) {
        console.error('Failed to read file:', err)
      }
    }
  }

  return (
    <div>
      <div
        onClick={handleClick}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          padding: '3px 8px',
          paddingLeft: `${depth * 16 + 8}px`,
          cursor: 'pointer',
          color: 'var(--text-secondary)',
          fontSize: '13px',
          userSelect: 'none',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--bg-hover)')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        {entry.directory ? (
          <>
            {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            {expanded ? <FolderOpen size={14} color="var(--warning)" /> : <Folder size={14} color="var(--warning)" />}
          </>
        ) : (
          <>
            <span style={{ width: '14px' }} />
            <File size={14} color="var(--accent)" />
          </>
        )}
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {entry.name}
        </span>
      </div>
      {expanded && children && (
        <div>
          {children
            .sort((a, b) => {
              if (a.directory !== b.directory) return a.directory ? -1 : 1
              return a.name.localeCompare(b.name)
            })
            .map((child) => (
              <FileTreeNode key={child.path} entry={child} projectId={projectId} depth={depth + 1} />
            ))}
        </div>
      )}
    </div>
  )
}

export function FileTree({ projectId }: { projectId: string }) {
  const { data: files, isLoading } = useFiles(projectId)

  return (
    <div
      style={{
        height: '100%',
        overflow: 'auto',
        backgroundColor: 'var(--bg-secondary)',
        borderRight: '1px solid var(--border)',
      }}
    >
      <div
        style={{
          padding: '8px 12px',
          fontSize: '11px',
          fontWeight: 600,
          color: 'var(--text-muted)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          borderBottom: '1px solid var(--border)',
        }}
      >
        Explorer
      </div>
      {isLoading ? (
        <div style={{ padding: '12px', color: 'var(--text-muted)', fontSize: '12px' }}>
          Loading...
        </div>
      ) : (
        files
          ?.sort((a, b) => {
            if (a.directory !== b.directory) return a.directory ? -1 : 1
            return a.name.localeCompare(b.name)
          })
          .map((entry) => (
            <FileTreeNode key={entry.path} entry={entry} projectId={projectId} depth={0} />
          ))
      )}
    </div>
  )
}
