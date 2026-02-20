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
        className="file-tree-node"
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
      >
        {entry.directory ? (
          <>
            {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            {expanded ? <FolderOpen size={14} color="var(--warning)" /> : <Folder size={14} color="var(--warning)" />}
          </>
        ) : (
          <>
            <span className="file-tree-spacer" />
            <File size={14} color="var(--accent)" />
          </>
        )}
        <span className="file-tree-name">
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
    <div className="file-tree">
      <div className="panel-header">
        Explorer
      </div>
      {isLoading ? (
        <div className="empty-state-sm text-muted text-xs">
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
