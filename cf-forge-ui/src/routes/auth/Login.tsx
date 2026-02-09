import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Code2 } from 'lucide-react'
import { useAuthStore } from '../../store/auth.ts'

export function Login() {
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirect = searchParams.get('redirect') || '/dashboard'

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
    }}>
      <div style={{
        width: '360px',
        padding: '40px',
        backgroundColor: 'var(--bg-secondary)',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        textAlign: 'center',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', marginBottom: '24px' }}>
          <Code2 size={32} color="var(--accent)" />
          <h1 style={{ fontSize: '24px', fontWeight: 700 }}>CF Forge</h1>
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
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Sign in with SSO
        </button>
        {searchParams.get('error') && (
          <p style={{ color: '#ef4444', fontSize: '13px', marginTop: '16px' }}>
            Authentication failed. Please try again.
          </p>
        )}
      </div>
    </div>
  )
}
