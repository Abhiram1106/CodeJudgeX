import { apiClient } from '@/lib/axios'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth.types'

export const authService = {
  register: (data: RegisterRequest): Promise<AuthResponse> =>
    apiClient.post('/auth/register', data).then((r) => r.data.data),

  login: (data: LoginRequest): Promise<AuthResponse> =>
    apiClient.post('/auth/login', data).then((r) => r.data.data),

  refresh: (refreshToken: string): Promise<AuthResponse> =>
    apiClient.post('/auth/refresh', { refreshToken }).then((r) => r.data.data),

  logout: (): Promise<void> =>
    apiClient.post('/auth/logout').then(() => undefined),

  me: (): Promise<AuthResponse['user']> =>
    apiClient.get('/auth/me').then((r) => r.data.data),
}
