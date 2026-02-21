import { create } from 'zustand'

interface OpenFile {
  path: string
  name: string
  content: string
  language: string
  modified: boolean
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface WorkspaceState {
  projectId: string | null
  openFiles: OpenFile[]
  activeFilePath: string | null
  sidebarOpen: boolean
  terminalOpen: boolean
  chatOpen: boolean
  chatMessages: ChatMessage[]
  chatConversationId: string

  setProjectId: (id: string) => void
  openFile: (file: OpenFile) => void
  closeFile: (path: string) => void
  setActiveFile: (path: string) => void
  updateFileContent: (path: string, content: string) => void
  markFileSaved: (path: string) => void
  toggleSidebar: () => void
  toggleTerminal: () => void
  toggleChat: () => void
  addChatMessage: (msg: ChatMessage) => void
  updateLastChatMessage: (content: string) => void
  clearChatMessages: () => void
}

function getLanguage(path: string): string {
  const ext = path.split('.').pop()?.toLowerCase() ?? ''
  const map: Record<string, string> = {
    ts: 'typescript', tsx: 'typescript',
    js: 'javascript', jsx: 'javascript',
    java: 'java', py: 'python', go: 'go',
    rs: 'rust', rb: 'ruby', html: 'html',
    css: 'css', scss: 'scss', json: 'json',
    yml: 'yaml', yaml: 'yaml', xml: 'xml',
    md: 'markdown', sh: 'shell', bash: 'shell',
    sql: 'sql', dockerfile: 'dockerfile',
    toml: 'toml', properties: 'properties',
  }
  return map[ext] ?? 'plaintext'
}

export const useWorkspaceStore = create<WorkspaceState>((set) => ({
  projectId: null,
  openFiles: [],
  activeFilePath: null,
  sidebarOpen: true,
  terminalOpen: true,
  chatOpen: false,
  chatMessages: [],
  chatConversationId: crypto.randomUUID(),

  setProjectId: (id) => set({
    projectId: id,
    chatMessages: [],
    chatConversationId: crypto.randomUUID(),
  }),

  openFile: (file) =>
    set((state) => {
      const exists = state.openFiles.find((f) => f.path === file.path)
      if (exists) return { activeFilePath: file.path }
      return {
        openFiles: [...state.openFiles, { ...file, language: getLanguage(file.path) }],
        activeFilePath: file.path,
      }
    }),

  closeFile: (path) =>
    set((state) => {
      const files = state.openFiles.filter((f) => f.path !== path)
      const active =
        state.activeFilePath === path
          ? files[files.length - 1]?.path ?? null
          : state.activeFilePath
      return { openFiles: files, activeFilePath: active }
    }),

  setActiveFile: (path) => set({ activeFilePath: path }),

  updateFileContent: (path, content) =>
    set((state) => ({
      openFiles: state.openFiles.map((f) =>
        f.path === path ? { ...f, content, modified: true } : f
      ),
    })),

  markFileSaved: (path) =>
    set((state) => ({
      openFiles: state.openFiles.map((f) =>
        f.path === path ? { ...f, modified: false } : f
      ),
    })),

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  toggleTerminal: () => set((state) => ({ terminalOpen: !state.terminalOpen })),
  toggleChat: () => set((state) => ({ chatOpen: !state.chatOpen })),

  addChatMessage: (msg) =>
    set((state) => ({ chatMessages: [...state.chatMessages, msg] })),

  updateLastChatMessage: (content) =>
    set((state) => {
      const msgs = [...state.chatMessages]
      if (msgs.length > 0) {
        msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content }
      }
      return { chatMessages: msgs }
    }),

  clearChatMessages: () =>
    set({ chatMessages: [], chatConversationId: crypto.randomUUID() }),
}))
