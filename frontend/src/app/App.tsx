import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '@/components/auth/RequireAuth'
import { RequireRole } from '@/components/auth/RequireRole'

const LoginPage        = lazy(() => import('@/pages/LoginPage'))
const RegisterPage     = lazy(() => import('@/pages/RegisterPage'))
const ContestListPage  = lazy(() => import('@/pages/ContestListPage'))
const UnauthorizedPage = lazy(() => import('@/pages/UnauthorizedPage'))

function Loading() {
  return <div className="min-h-screen flex items-center justify-center text-gray-400">Loading…</div>
}

export default function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Protected — any authenticated user */}
        <Route
          path="/contests"
          element={
            <RequireAuth>
              <ContestListPage />
            </RequireAuth>
          }
        />

        {/* Faculty+ protected routes (placeholders — filled in Week 4) */}
        <Route
          path="/problems/create"
          element={
            <RequireAuth>
              <RequireRole role="FACULTY">
                <div className="p-8">Create Problem — coming soon</div>
              </RequireRole>
            </RequireAuth>
          }
        />

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/contests" replace />} />
        <Route path="*" element={<Navigate to="/contests" replace />} />
      </Routes>
    </Suspense>
  )
}
