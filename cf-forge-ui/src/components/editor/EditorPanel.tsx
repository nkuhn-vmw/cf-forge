import { EditorTabs } from './EditorTabs.tsx'
import { MonacoEditor } from './MonacoEditor.tsx'

export function EditorPanel() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <EditorTabs />
      <div style={{ flex: 1, overflow: 'hidden' }}>
        <MonacoEditor />
      </div>
    </div>
  )
}
