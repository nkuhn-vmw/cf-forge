import { X } from 'lucide-react'
import { useWorkspaceStore } from '../../store/workspace.ts'

export function EditorTabs() {
  const { openFiles, activeFilePath, setActiveFile, closeFile } = useWorkspaceStore()

  if (openFiles.length === 0) return null

  return (
    <div className="editor-tabs">
      {openFiles.map((file) => (
        <div
          key={file.path}
          onClick={() => setActiveFile(file.path)}
          className={`editor-tab ${file.path === activeFilePath ? 'active' : ''}`}
        >
          <span>
            {file.modified && (
              <span className="editor-tab-modified">&#9679;</span>
            )}
            {file.name}
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation()
              closeFile(file.path)
            }}
            className="editor-tab-close"
          >
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  )
}
