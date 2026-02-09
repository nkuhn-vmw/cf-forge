import { useState, useRef, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Send, Bot, User, ArrowLeft, Sparkles } from 'lucide-react'
import { api } from '../../api/client.ts'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

export function ConversationalBuilder() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        'Welcome to the CF Forge Builder! Describe the application you want to create, and I\'ll help you build and deploy it to Cloud Foundry.\n\nFor example:\n- "Create a Spring Boot REST API with PostgreSQL"\n- "Build a React dashboard with authentication"\n- "Set up a Python Flask microservice"',
    },
  ])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
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
      }
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', backgroundColor: 'var(--bg-primary)' }}>
      <header
        style={{
          padding: '12px 24px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          backgroundColor: 'var(--bg-secondary)',
        }}
      >
        <Link to="/dashboard" style={{ color: 'var(--text-muted)', display: 'flex' }}>
          <ArrowLeft size={18} />
        </Link>
        <Sparkles size={20} color="var(--accent)" />
        <h1 style={{ fontSize: '16px', fontWeight: 600 }}>Conversational Builder</h1>
      </header>

      <div style={{ flex: 1, overflow: 'auto', maxWidth: '800px', width: '100%', margin: '0 auto', padding: '24px' }} ref={scrollRef}>
        {messages.map((msg, i) => (
          <div key={i} style={{ marginBottom: '20px', display: 'flex', gap: '12px' }}>
            <div
              style={{
                width: '32px', height: '32px', borderRadius: '8px', flexShrink: 0,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                backgroundColor: msg.role === 'user' ? 'var(--accent)' : 'var(--bg-tertiary)',
              }}
            >
              {msg.role === 'user' ? <User size={16} color="white" /> : <Bot size={16} color="var(--success)" />}
            </div>
            <div
              style={{
                padding: '12px 16px',
                backgroundColor: msg.role === 'user' ? 'var(--bg-tertiary)' : 'var(--bg-secondary)',
                borderRadius: '8px',
                fontSize: '14px',
                lineHeight: '1.7',
                color: 'var(--text-primary)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                maxWidth: '640px',
              }}
            >
              {msg.content}
              {streaming && i === messages.length - 1 && msg.role === 'assistant' && (
                <span style={{ color: 'var(--accent)' }}>|</span>
              )}
            </div>
          </div>
        ))}
      </div>

      <div style={{ padding: '16px 24px', borderTop: '1px solid var(--border)', backgroundColor: 'var(--bg-secondary)' }}>
        <div style={{ display: 'flex', gap: '12px', maxWidth: '800px', margin: '0 auto' }}>
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Describe the app you want to build..."
            disabled={streaming}
            style={{
              flex: 1, padding: '12px 16px', backgroundColor: 'var(--bg-primary)',
              border: '1px solid var(--border)', borderRadius: '8px',
              color: 'var(--text-primary)', fontSize: '14px', outline: 'none',
            }}
          />
          <button
            onClick={handleSend}
            disabled={streaming || !input.trim()}
            style={{
              padding: '12px 20px', backgroundColor: 'var(--accent)', border: 'none',
              borderRadius: '8px', color: 'white', display: 'flex', alignItems: 'center',
              gap: '6px', fontSize: '14px', fontWeight: 500,
              opacity: streaming || !input.trim() ? 0.5 : 1,
            }}
          >
            <Send size={16} /> Send
          </button>
        </div>
      </div>
    </div>
  )
}
