import { EditorTabs } from './EditorTabs.tsx'
import { MonacoEditor } from './MonacoEditor.tsx'

export function EditorPanel() {
  return (
    <div className="col-layout">
      <EditorTabs />
      <div className="flex-1 overflow-hidden">
        <MonacoEditor />
      </div>
    </div>
  )
}
