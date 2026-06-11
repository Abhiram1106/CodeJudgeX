import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthUser, UserRole } from '@/types/auth.types'

interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  refreshToken: string | null
  setAuth: (user: AuthUser, accessToken: string, refreshToken: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      setAuth: (user, accessToken, refreshToken) =>
        set({ user, accessToken, refreshToken }),
      clearAuth: () =>
        set({ user: null, accessToken: null, refreshToken: null }),
    }),
    {
      name: 'codejudgex-auth',
      // Only persist refresh token + user — access token is short-lived
      partialize: (state) => ({
        user: state.user,
        refreshToken: state.refreshToken,
        accessToken: state.accessToken,
      }),
    }
  )
)

// Selector hooks

export function useCurrentUser() {
  return useAuthStore((s) => s.user)
}

export function useIsAuthenticated() {
  return useAuthStore((s) => s.user !== null)
}

export function useHasRole(role: UserRole) {
  return useAuthStore((s) => s.user?.roles.includes(role) ?? false)
}

export function useIsAtLeastFaculty() {
  return useAuthStore(
    (s) =>
      s.user?.roles.some((r) =>
        (['FACULTY', 'ADMIN', 'SUPER_ADMIN'] as UserRole[]).includes(r)
      ) ?? false
  )
}
