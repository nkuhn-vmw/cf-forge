import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Code2 } from 'lucide-react'
import { useAuthStore } from '../../store/auth.ts'

export function Login() {
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const rawRedirect = searchParams.get('redirect') || '/dashboard'
  const redirect = rawRedirect.startsWith('/') && !rawRedirect.startsWith('//') ? rawRedirect : '/dashboard'

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirect, { replace: true })
    }
  }, [isAuthenticated, navigate, redirect])

  const handleLogin = () => {
    window.location.href = `/api/v1/auth/login?redirect_uri=${encodeURIComponent(redirect)}`
  }

  return (
    <div style={{
      height: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: 'var(--bg-primary)',
      background: 'radial-gradient(ellipse at 50% 0%, #0f1a2e 0%, #080b12 50%)',
    }}>
      <div style={{
        width: '420px',
        padding: '48px 40px',
        backgroundColor: 'var(--bg-secondary)',
        border: '1px solid var(--border)',
        borderRadius: '16px',
        textAlign: 'center',
        boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginBottom: '8px' }}>
          <Code2 size={32} color="var(--accent)" />
          <h1 style={{ fontSize: '24px', fontWeight: 600, fontFamily: '"IBM Plex Mono", monospace' }}>
            CF <span style={{ color: 'var(--accent)' }}>Forge</span>
          </h1>
        </div>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px', marginBottom: '32px' }}>
          Sign in to manage your Cloud Foundry applications
        </p>
        <button
          onClick={handleLogin}
          style={{
            width: '100%',
            padding: '12px 24px',
            backgroundColor: 'var(--accent)',
            color: '#080b12',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 600,
            fontFamily: '"IBM Plex Mono", monospace',
            cursor: 'pointer',
            transition: 'background-color 0.2s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--accent-hover)')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--accent)')}
        >
          Sign in with SSO
        </button>
        {searchParams.get('error') && (
          <p style={{ color: 'var(--danger)', fontSize: '13px', marginTop: '16px' }}>
            Authentication failed. Please try again.
          </p>
        )}
      </div>
    </div>
  )
}
