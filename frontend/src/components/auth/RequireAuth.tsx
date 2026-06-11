import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useIsAuthenticated } from '@/stores/auth.store'

interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const isAuthenticated = useIsAuthenticated()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}
