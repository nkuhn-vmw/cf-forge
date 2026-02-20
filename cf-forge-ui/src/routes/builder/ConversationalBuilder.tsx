import { useState, useRef, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Send, Bot, User, ArrowLeft, Sparkles, FolderPlus, Loader2 } from 'lucide-react'
import { api } from '../../api/client.ts'
import { useCreateProject } from '../../api/queries.ts'

interface Message {
  role: 'user' | 'assistant'
  content: string
  projectReady?: boolean
}

export function ConversationalBuilder() {
  const navigate = useNavigate()
  const createProject = useCreateProject()
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        'Welcome to the CF Forge Builder! Describe the application you want to create, and I\'ll help you build and deploy it to Cloud Foundry.\n\nFor example:\n- "Create a Spring Boot REST API with PostgreSQL"\n- "Build a React dashboard with authentication"\n- "Set up a Python Flask microservice with Redis caching"',
    },
  ])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [generatedProject, setGeneratedProject] = useState<{ name: string; language: string; framework: string } | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  const handleSend = () => {
    const prompt = input.trim()
    if (!prompt || streaming) return

    setMessages((prev) => [...prev, { role: 'user', content: prompt }])
    setInput('')
    setStreaming(true)

    const eventSource = api.agent.generate('builder', prompt)
    let response = ''

    setMessages((prev) => [...prev, { role: 'assistant', content: '' }])

    eventSource.onmessage = (event) => {
      response += event.data
      setMessages((prev) => {
        const updated = [...prev]
        updated[updated.length - 1] = { role: 'assistant', content: response }
        return updated
      })
    }

    eventSource.onerror = () => {
      eventSource.close()
      setStreaming(false)

      if (!response) {
        setMessages((prev) => {
          const updated = [...prev]
          updated[updated.length - 1] = { role: 'assistant', content: 'Connection error. Please try again.' }
          return updated
        })
      } else {
        // Parse project info from response if it looks like a generation result
        const nameMatch = response.match(/project[:\s]+["']?([a-zA-Z0-9-]+)["']?/i)
        const langMatch = response.match(/language[:\s]+["']?(\w+)["']?/i)
        const fwMatch = response.match(/framework[:\s]+["']?([a-zA-Z0-9.]+)["']?/i)
        if (nameMatch) {
          setGeneratedProject({
            name: nameMatch[1],
            language: langMatch?.[1] ?? 'java',
            framework: fwMatch?.[1] ?? '',
          })
          setMessages((prev) => {
            const updated = [...prev]
            updated[updated.length - 1] = { ...updated[updated.length - 1], projectReady: true }
            return updated
          })
        }
      }
    }
  }

  const handleCreateProject = async () => {
    if (!generatedProject) return
    try {
      const project = await createProject.mutateAsync({
        name: generatedProject.name,
        language: generatedProject.language,
        framework: generatedProject.framework,
      })
      navigate(`/workspace/${project.id}`)
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', content: 'Failed to create project. Please try again.' }])
    }
  }

  return (
    <div className="col-layout">
      <header className="page-header">
        <Link to="/dashboard" className="btn-icon">
          <ArrowLeft size={18} />
        </Link>
        <Sparkles size={20} color="var(--accent)" />
        <h1 className="page-header-title">Conversational Builder</h1>
        <span className="badge-muted">AI-Powered</span>
      </header>

      <div className="chat-messages content-container-md" style={{ padding: '24px' }} ref={scrollRef}>
        {messages.map((msg, i) => (
          <div key={i} className="mb-20">
            <div className="chat-message gap-12">
              <div className={`chat-avatar-lg ${msg.role === 'user' ? 'chat-avatar-user' : 'chat-avatar-bot'}`}>
                {msg.role === 'user' ? <User size={16} color="white" /> : <Bot size={16} color="white" />}
              </div>
              <div className={msg.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-bot'}>
                {msg.content}
                {streaming && i === messages.length - 1 && msg.role === 'assistant' && (
                  <span className="chat-cursor">|</span>
                )}
              </div>
            </div>
            {msg.projectReady && (
              <div style={{ marginLeft: '44px', marginTop: '8px' }}>
                <button
                  onClick={handleCreateProject}
                  disabled={createProject.isPending}
                  className={`btn-success${createProject.isPending ? ' btn-disabled' : ''}`}
                >
                  {createProject.isPending ? <Loader2 size={14} className="spin" /> : <FolderPlus size={14} />}
                  Create Project & Open Workspace
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="chat-input-bar-lg">
        <div className="row gap-12 content-container-md">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Describe the app you want to build..."
            disabled={streaming}
            className="form-input flex-1 btn-lg"
          />
          <button
            onClick={handleSend}
            disabled={streaming || !input.trim()}
            className={`btn-primary btn-lg${streaming || !input.trim() ? ' btn-disabled' : ''}`}
          >
            <Send size={16} /> Send
          </button>
        </div>
      </div>
    </div>
  )
}
