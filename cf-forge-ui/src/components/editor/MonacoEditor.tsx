import Editor from '@monaco-editor/react'
import { useWorkspaceStore } from '../../store/workspace.ts'
import { api } from '../../api/client.ts'

export function MonacoEditor() {
  const { openFiles, activeFilePath, updateFileContent, markFileSaved, projectId } =
    useWorkspaceStore()

  const activeFile = openFiles.find((f) => f.path === activeFilePath)

  if (!activeFile) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%',
          color: 'var(--text-muted)',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
          <polyline points="13 2 13 9 20 9" />
        </svg>
        <span>Select a file to edit</span>
      </div>
    )
  }

  const handleChange = (value: string | undefined) => {
    if (value !== undefined) {
      updateFileContent(activeFile.path, value)
    }
  }

  const handleSave = () => {
    if (!projectId || !activeFile.modified) return
    api.files.write(projectId, activeFile.path, activeFile.content).then(() => {
      markFileSaved(activeFile.path)
    })
  }

  return (
    <Editor
      height="100%"
      language={activeFile.language}
      value={activeFile.content}
      theme="vs-dark"
      onChange={handleChange}
      onMount={(editor) => {
        editor.addAction({
          id: 'save-file',
          label: 'Save File',
          keybindings: [2048 | 49], // Ctrl+S
          run: handleSave,
        })
      }}
      options={{
        fontSize: 13,
        fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace",
        minimap: { enabled: true, maxColumn: 80 },
        scrollBeyondLastLine: false,
        wordWrap: 'off',
        tabSize: 2,
        renderWhitespace: 'selection',
        bracketPairColorization: { enabled: true },
        guides: { bracketPairs: true, indentation: true },
        smoothScrolling: true,
        cursorBlinking: 'smooth',
        cursorSmoothCaretAnimation: 'on',
        padding: { top: 8, bottom: 8 },
      }}
    />
  )
}
