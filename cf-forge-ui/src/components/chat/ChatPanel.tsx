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

    let assistantMsg = ''
    setMessages((prev) => [...prev, { role: 'assistant', content: '' }])

    api.agent.generate(
      { conversationId: projectId, projectId, message: prompt },
      {
        onChunk: (data) => {
          assistantMsg += data
          setMessages((prev) => {
            const updated = [...prev]
            updated[updated.length - 1] = { role: 'assistant', content: assistantMsg }
            return updated
          })
        },
        onDone: () => {
          setStreaming(false)
        },
        onError: () => {
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
        },
      },
    )
  }

  return (
    <div className="chat-panel">
      <div className="panel-header">
        <Bot size={14} /> AI Assistant
      </div>

      <div ref={scrollRef} className="chat-messages">
        {messages.length === 0 && (
          <div className="empty-state-sm text-sm">
            Ask the AI to help build, debug, or deploy your application.
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className="chat-message">
            <div className={msg.role === 'user' ? 'chat-avatar chat-avatar-user' : 'chat-avatar chat-avatar-bot'}>
              {msg.role === 'user' ? <User size={14} color="white" /> : <Bot size={14} color="white" />}
            </div>
            <div className="chat-bubble">
              {msg.content}
              {streaming && i === messages.length - 1 && msg.role === 'assistant' && (
                <span className="chat-cursor">|</span>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="chat-input-bar">
        <div className="row">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Ask the AI assistant..."
            disabled={streaming}
            className="form-input flex-1"
          />
          <button
            onClick={handleSend}
            disabled={streaming || !input.trim()}
            className={`btn-primary${streaming || !input.trim() ? ' btn-disabled' : ''}`}
          >
            <Send size={14} />
          </button>
        </div>
      </div>
    </div>
  )
}
