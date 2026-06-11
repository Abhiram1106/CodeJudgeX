export type UserRole = 'STUDENT' | 'FACULTY' | 'ADMIN' | 'SUPER_ADMIN'

export interface AuthUser {
  id: string
  name: string
  email: string
  roles: UserRole[]
  status: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: AuthUser
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}
