import { describe, it, expect, beforeEach } from 'vitest'
import { useWorkspaceStore } from './workspace'

describe('workspace store', () => {
  beforeEach(() => {
    useWorkspaceStore.setState({
      projectId: null,
      openFiles: [],
      activeFilePath: null,
      sidebarOpen: true,
      terminalOpen: true,
      chatOpen: false,
      chatMessages: [],
      chatConversationId: 'initial-uuid',
    })
  })

  describe('chat persistence', () => {
    it('starts with empty chat messages', () => {
      expect(useWorkspaceStore.getState().chatMessages).toEqual([])
    })

    it('adds a chat message', () => {
      useWorkspaceStore.getState().addChatMessage({ role: 'user', content: 'Hello' })
      expect(useWorkspaceStore.getState().chatMessages).toHaveLength(1)
      expect(useWorkspaceStore.getState().chatMessages[0]).toEqual({ role: 'user', content: 'Hello' })
    })

    it('updates the last chat message content', () => {
      const store = useWorkspaceStore.getState()
      store.addChatMessage({ role: 'user', content: 'Hello' })
      store.addChatMessage({ role: 'assistant', content: 'Hi' })
      store.updateLastChatMessage('Hi there!')
      const msgs = useWorkspaceStore.getState().chatMessages
      expect(msgs[1].content).toBe('Hi there!')
      expect(msgs[0].content).toBe('Hello') // first message unchanged
    })

    it('updateLastChatMessage is a no-op when no messages', () => {
      useWorkspaceStore.getState().updateLastChatMessage('ghost')
      expect(useWorkspaceStore.getState().chatMessages).toEqual([])
    })

    it('clears chat messages and generates new conversationId', () => {
      const store = useWorkspaceStore.getState()
      store.addChatMessage({ role: 'user', content: 'Hello' })
      const oldId = useWorkspaceStore.getState().chatConversationId
      store.clearChatMessages()
      expect(useWorkspaceStore.getState().chatMessages).toEqual([])
      expect(useWorkspaceStore.getState().chatConversationId).not.toBe(oldId)
    })

    it('setProjectId resets chat messages and conversationId', () => {
      const store = useWorkspaceStore.getState()
      store.addChatMessage({ role: 'user', content: 'Hello' })
      const oldConvId = useWorkspaceStore.getState().chatConversationId
      store.setProjectId('project-123')
      expect(useWorkspaceStore.getState().chatMessages).toEqual([])
      expect(useWorkspaceStore.getState().chatConversationId).not.toBe(oldConvId)
      expect(useWorkspaceStore.getState().projectId).toBe('project-123')
    })
  })

  describe('file management', () => {
    it('opens a file and sets it as active', () => {
      useWorkspaceStore.getState().openFile({
        path: '/src/main.ts',
        name: 'main.ts',
        content: 'console.log("hi")',
        language: 'typescript',
        modified: false,
      })
      const state = useWorkspaceStore.getState()
      expect(state.openFiles).toHaveLength(1)
      expect(state.activeFilePath).toBe('/src/main.ts')
    })

    it('does not duplicate already-open files', () => {
      const file = {
        path: '/src/main.ts',
        name: 'main.ts',
        content: 'x',
        language: 'typescript',
        modified: false,
      }
      useWorkspaceStore.getState().openFile(file)
      useWorkspaceStore.getState().openFile(file)
      expect(useWorkspaceStore.getState().openFiles).toHaveLength(1)
    })

    it('detects language from file extension', () => {
      useWorkspaceStore.getState().openFile({
        path: '/src/App.tsx',
        name: 'App.tsx',
        content: '',
        language: '', // will be overridden
        modified: false,
      })
      expect(useWorkspaceStore.getState().openFiles[0].language).toBe('typescript')
    })

    it('closes a file and selects the last remaining file', () => {
      const store = useWorkspaceStore.getState()
      store.openFile({ path: '/a.ts', name: 'a.ts', content: '', language: '', modified: false })
      store.openFile({ path: '/b.ts', name: 'b.ts', content: '', language: '', modified: false })
      expect(useWorkspaceStore.getState().activeFilePath).toBe('/b.ts')
      store.closeFile('/b.ts')
      expect(useWorkspaceStore.getState().activeFilePath).toBe('/a.ts')
    })

    it('marks file content as modified', () => {
      useWorkspaceStore.getState().openFile({
        path: '/x.ts',
        name: 'x.ts',
        content: 'old',
        language: 'typescript',
        modified: false,
      })
      useWorkspaceStore.getState().updateFileContent('/x.ts', 'new')
      expect(useWorkspaceStore.getState().openFiles[0].modified).toBe(true)
      expect(useWorkspaceStore.getState().openFiles[0].content).toBe('new')
    })

    it('markFileSaved resets modified flag', () => {
      useWorkspaceStore.getState().openFile({
        path: '/x.ts',
        name: 'x.ts',
        content: 'old',
        language: 'typescript',
        modified: false,
      })
      useWorkspaceStore.getState().updateFileContent('/x.ts', 'new')
      useWorkspaceStore.getState().markFileSaved('/x.ts')
      expect(useWorkspaceStore.getState().openFiles[0].modified).toBe(false)
    })
  })

  describe('toggles', () => {
    it('terminalOpen defaults to true', () => {
      expect(useWorkspaceStore.getState().terminalOpen).toBe(true)
    })

    it('toggles terminal', () => {
      useWorkspaceStore.getState().toggleTerminal()
      expect(useWorkspaceStore.getState().terminalOpen).toBe(false)
      useWorkspaceStore.getState().toggleTerminal()
      expect(useWorkspaceStore.getState().terminalOpen).toBe(true)
    })

    it('toggles chat', () => {
      expect(useWorkspaceStore.getState().chatOpen).toBe(false)
      useWorkspaceStore.getState().toggleChat()
      expect(useWorkspaceStore.getState().chatOpen).toBe(true)
    })

    it('toggles sidebar', () => {
      expect(useWorkspaceStore.getState().sidebarOpen).toBe(true)
      useWorkspaceStore.getState().toggleSidebar()
      expect(useWorkspaceStore.getState().sidebarOpen).toBe(false)
    })
  })
})
