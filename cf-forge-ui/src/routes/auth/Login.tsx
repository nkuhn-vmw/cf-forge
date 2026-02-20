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
    <div className="login-page">
      <div className="login-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }} className="mb-8">
          <Code2 size={32} color="var(--accent)" />
          <h1 style={{ fontSize: '24px', fontFamily: '"IBM Plex Mono", monospace' }} className="font-semibold">
            CF <span className="text-accent">Forge</span>
          </h1>
        </div>
        <p className="text-secondary" style={{ fontSize: '14px', marginBottom: '32px' }}>
          Sign in to manage your Cloud Foundry applications
        </p>
        <button onClick={handleLogin} className="btn-primary-dark">
          Sign in with SSO
        </button>
        {searchParams.get('error') && (
          <p className="text-danger" style={{ fontSize: '13px', marginTop: '16px' }}>
            Authentication failed. Please try again.
          </p>
        )}
      </div>
    </div>
  )
}
