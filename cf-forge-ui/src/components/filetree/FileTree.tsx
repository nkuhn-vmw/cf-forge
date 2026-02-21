import { useState, useRef, useEffect, useCallback } from 'react'
import { ChevronRight, ChevronDown, File, Folder, FolderOpen, FilePlus, Trash2, Pencil } from 'lucide-react'
import { useFiles } from '../../api/queries.ts'
import { useWorkspaceStore } from '../../store/workspace.ts'
import { api } from '../../api/client.ts'
import type { FileEntry } from '../../api/client.ts'
import { useQueryClient } from '@tanstack/react-query'

interface ContextMenu {
  x: number
  y: number
  entry: FileEntry | null
  parentDir: string
}

function FileTreeNode({ entry, projectId, depth, onContextMenu }: {
  entry: FileEntry
  projectId: string
  depth: number
  onContextMenu: (e: React.MouseEvent, entry: FileEntry) => void
}) {
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
        onContextMenu={(e) => onContextMenu(e, entry)}
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
              <FileTreeNode
                key={child.path}
                entry={child}
                projectId={projectId}
                depth={depth + 1}
                onContextMenu={onContextMenu}
              />
            ))}
        </div>
      )}
    </div>
  )
}

export function FileTree({ projectId }: { projectId: string }) {
  const { data: files, isLoading } = useFiles(projectId)
  const queryClient = useQueryClient()
  const [contextMenu, setContextMenu] = useState<ContextMenu | null>(null)
  const [showNewFileInput, setShowNewFileInput] = useState(false)
  const [newFileParentDir, setNewFileParentDir] = useState('')
  const [showRenameInput, setShowRenameInput] = useState<FileEntry | null>(null)
  const [inputValue, setInputValue] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  const closeContextMenu = useCallback(() => setContextMenu(null), [])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        closeContextMenu()
      }
    }
    if (contextMenu) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [contextMenu, closeContextMenu])

  useEffect(() => {
    if ((showNewFileInput || showRenameInput) && inputRef.current) {
      inputRef.current.focus()
    }
  }, [showNewFileInput, showRenameInput])

  const handleContextMenu = (e: React.MouseEvent, entry: FileEntry) => {
    e.preventDefault()
    e.stopPropagation()
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      entry,
      parentDir: entry.directory ? entry.path : entry.path.split('/').slice(0, -1).join('/'),
    })
  }

  const handleRootContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      entry: null,
      parentDir: '',
    })
  }

  const handleNewFile = () => {
    setNewFileParentDir(contextMenu?.parentDir ?? '')
    setShowNewFileInput(true)
    setInputValue('')
    closeContextMenu()
  }

  const handleDelete = async () => {
    const entry = contextMenu?.entry
    closeContextMenu()
    if (!entry) return
    if (!confirm(`Delete "${entry.name}"?`)) return
    try {
      await api.files.delete(projectId, entry.path)
      queryClient.invalidateQueries({ queryKey: ['files', projectId] })
    } catch (err) {
      console.error('Delete failed:', err)
    }
  }

  const handleRename = () => {
    if (!contextMenu?.entry) return
    setShowRenameInput(contextMenu.entry)
    setInputValue(contextMenu.entry.name)
    closeContextMenu()
  }

  const submitNewFile = async () => {
    const name = inputValue.trim()
    if (!name) {
      setShowNewFileInput(false)
      return
    }
    const path = newFileParentDir ? `${newFileParentDir}/${name}` : name
    try {
      await api.files.write(projectId, path, '')
      queryClient.invalidateQueries({ queryKey: ['files', projectId] })
    } catch (err) {
      console.error('Create file failed:', err)
    }
    setShowNewFileInput(false)
    setInputValue('')
  }

  const submitRename = async () => {
    const newName = inputValue.trim()
    if (!newName || !showRenameInput) {
      setShowRenameInput(null)
      return
    }
    const oldPath = showRenameInput.path
    const parentDir = oldPath.split('/').slice(0, -1).join('/')
    const newPath = parentDir ? `${parentDir}/${newName}` : newName
    try {
      if (!showRenameInput.directory) {
        const result = await api.files.read(projectId, oldPath)
        await api.files.write(projectId, newPath, result.content)
        await api.files.delete(projectId, oldPath)
      }
      queryClient.invalidateQueries({ queryKey: ['files', projectId] })
    } catch (err) {
      console.error('Rename failed:', err)
    }
    setShowRenameInput(null)
    setInputValue('')
  }

  return (
    <div className="file-tree" onContextMenu={handleRootContextMenu}>
      <div className="panel-header">
        Explorer
        <button
          onClick={() => { setNewFileParentDir(''); setShowNewFileInput(true); setInputValue('') }}
          className="btn-icon ml-auto"
          title="New File"
          style={{ padding: '2px' }}
        >
          <FilePlus size={13} />
        </button>
      </div>

      {showNewFileInput && (
        <div style={{ padding: '4px 8px' }}>
          <input
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') submitNewFile()
              if (e.key === 'Escape') { setShowNewFileInput(false); setInputValue('') }
            }}
            onBlur={submitNewFile}
            placeholder="filename.ext"
            className="file-tree-input"
          />
        </div>
      )}

      {showRenameInput && (
        <div style={{ padding: '4px 8px' }}>
          <input
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') submitRename()
              if (e.key === 'Escape') { setShowRenameInput(null); setInputValue('') }
            }}
            onBlur={submitRename}
            placeholder="new name"
            className="file-tree-input"
          />
        </div>
      )}

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
            <FileTreeNode
              key={entry.path}
              entry={entry}
              projectId={projectId}
              depth={0}
              onContextMenu={handleContextMenu}
            />
          ))
      )}

      {contextMenu && (
        <div
          ref={menuRef}
          className="file-tree-context-menu"
          style={{ top: contextMenu.y, left: contextMenu.x }}
        >
          <button onClick={handleNewFile} className="file-tree-context-item">
            <FilePlus size={13} /> New File
          </button>
          {contextMenu.entry && !contextMenu.entry.directory && (
            <button onClick={handleRename} className="file-tree-context-item">
              <Pencil size={13} /> Rename
            </button>
          )}
          {contextMenu.entry && (
            <button onClick={handleDelete} className="file-tree-context-item text-danger">
              <Trash2 size={13} /> Delete
            </button>
          )}
        </div>
      )}
    </div>
  )
}
