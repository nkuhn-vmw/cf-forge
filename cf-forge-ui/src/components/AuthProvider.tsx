import { useEffect } from 'react'
import { useAuthStore } from '../store/auth.ts'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { isLoading, checkAuth } = useAuthStore()

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  if (isLoading) {
    return (
      <div className="loading-screen">
        Loading...
      </div>
    )
  }

  return <>{children}</>
}
