import { useState, useRef, useEffect } from 'react'
import { Send, Bot, User } from 'lucide-react'
import { api } from '../../api/client.ts'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

export function ChatPanel({ projectId }: { projectId: string }) {
  const [messages, setMessages] = useState<Message[]>([])
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

    const eventSource = api.agent.generate(projectId, prompt)
    let assistantMsg = ''

    setMessages((prev) => [...prev, { role: 'assistant', content: '' }])

    eventSource.onmessage = (event) => {
      assistantMsg += event.data
      setMessages((prev) => {
        const updated = [...prev]
        updated[updated.length - 1] = { role: 'assistant', content: assistantMsg }
        return updated
      })
    }

    eventSource.onerror = () => {
      eventSource.close()
      setStreaming(false)
      if (!assistantMsg) {
        setMessages((prev) => {
          const updated = [...prev]
          updated[updated.length - 1] = {
            role: 'assistant',
            content: 'Connection error. Please try again.',
          }
          return updated
        })
      }
    }
  }

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        backgroundColor: 'var(--bg-secondary)',
        borderLeft: '1px solid var(--border)',
      }}
    >
      <div
        style={{
          padding: '8px 12px',
          fontSize: '11px',
          fontWeight: 600,
          color: 'var(--text-muted)',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
        }}
      >
        <Bot size={14} /> AI Assistant
      </div>

      <div ref={scrollRef} style={{ flex: 1, overflow: 'auto', padding: '12px' }}>
        {messages.length === 0 && (
          <div style={{ color: 'var(--text-muted)', fontSize: '12px', textAlign: 'center', padding: '20px' }}>
            Ask the AI to help build, debug, or deploy your application.
          </div>
        )}
        {messages.map((msg, i) => (
          <div
            key={i}
            style={{
              marginBottom: '12px',
              display: 'flex',
              gap: '8px',
              alignItems: 'flex-start',
            }}
          >
            <div
              style={{
                width: '24px',
                height: '24px',
                borderRadius: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: msg.role === 'user' ? 'var(--accent)' : 'var(--success)',
                flexShrink: 0,
              }}
            >
              {msg.role === 'user' ? <User size={14} color="white" /> : <Bot size={14} color="white" />}
            </div>
            <div
              style={{
                fontSize: '13px',
                lineHeight: '1.6',
                color: 'var(--text-primary)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}
            >
              {msg.content}
              {streaming && i === messages.length - 1 && msg.role === 'assistant' && (
                <span style={{ animation: 'blink 1s infinite', color: 'var(--accent)' }}>|</span>
              )}
            </div>
          </div>
        ))}
      </div>

      <div style={{ padding: '8px', borderTop: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Ask the AI assistant..."
            disabled={streaming}
            style={{
              flex: 1,
              padding: '8px 12px',
              backgroundColor: 'var(--bg-primary)',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              color: 'var(--text-primary)',
              fontSize: '13px',
              outline: 'none',
            }}
          />
          <button
            onClick={handleSend}
            disabled={streaming || !input.trim()}
            style={{
              padding: '8px 12px',
              backgroundColor: 'var(--accent)',
              border: 'none',
              borderRadius: '6px',
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              opacity: streaming || !input.trim() ? 0.5 : 1,
            }}
          >
            <Send size={14} />
          </button>
        </div>
      </div>
    </div>
  )
}
