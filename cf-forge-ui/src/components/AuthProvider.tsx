import { useEffect } from 'react'
import { useAuthStore } from '../store/auth.ts'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { isLoading, checkAuth } = useAuthStore()

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  if (isLoading) {
    return (
      <div style={{
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-primary)',
        color: 'var(--text-secondary)',
        fontSize: '14px',
      }}>
        Loading...
      </div>
    )
  }

  return <>{children}</>
}
