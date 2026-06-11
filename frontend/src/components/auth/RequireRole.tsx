import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { UserRole } from '@/types/auth.types'
import { useAuthStore } from '@/stores/auth.store'

const ROLE_HIERARCHY: Record<UserRole, number> = {
  STUDENT: 0,
  FACULTY: 1,
  ADMIN: 2,
  SUPER_ADMIN: 3,
}

function hasRole(userRoles: UserRole[], required: UserRole): boolean {
  const requiredLevel = ROLE_HIERARCHY[required]
  return userRoles.some((r) => ROLE_HIERARCHY[r] >= requiredLevel)
}

interface RequireRoleProps {
  role: UserRole
  children: ReactNode
}

export function RequireRole({ role, children }: RequireRoleProps) {
  const user = useAuthStore((s) => s.user)

  if (!user || !hasRole(user.roles, role)) {
    return <Navigate to="/unauthorized" replace />
  }

  return <>{children}</>
}
