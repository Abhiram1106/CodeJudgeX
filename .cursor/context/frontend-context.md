# Frontend Context — CodeJudgeX

> @-include this file when working on frontend/ code.

## Stack

- React 18 + Vite 5
- TypeScript 5.4 (strict mode)
- Tailwind CSS 3 + shadcn/ui (Radix primitives)
- Monaco Editor (`@monaco-editor/react`) — the in-browser code editor
- TanStack Query v5 — all server state (queries, mutations, caching, polling)
- Zustand v4 — client-side global state (auth user, UI preferences)
- Axios v1 — HTTP client, single instance in `src/lib/axios.ts`
- React Router v6 — routing with role guards
- React Hook Form v7 + Zod v3 — forms and validation
- Recharts v2 — analytics charts

## Directory structure

```
frontend/src/
├── app/
│   ├── App.tsx           Routes + role guards
│   ├── globals.css       Tailwind base + CSS variables
│   └── providers.tsx     QueryClient, Toaster, etc. (to be created)
├── pages/
│   ├── auth/             Login, Register
│   ├── dashboard/        Role-based home dashboards
│   ├── contest/          Browse contests, contest detail, live contest view
│   ├── problem/          Problem statement + Monaco editor
│   ├── submission/       Submission history, submission detail + verdict
│   ├── leaderboard/      Live leaderboard table
│   ├── faculty/          Problem management, contest management, plagiarism review
│   └── admin/            User management, platform analytics, audit logs
├── components/
│   ├── ui/               shadcn/ui generated components — do not modify directly
│   ├── editor/           Monaco editor wrapper (language, value, onChange, readOnly)
│   ├── layout/           AppShell, Sidebar, Navbar, RoleGuard
│   └── shared/           DataTable, StatusBadge, LoadingSkeleton, ConfirmDialog
├── features/
│   ├── auth/             useAuth, useLogin, useRegister, auth store slice
│   ├── contest/          useContests, useContest, useJoinContest
│   ├── problem/          useProblems, useProblem
│   ├── submission/       useSubmit, useSubmissionStatus (with polling), useMySubmissions
│   └── leaderboard/      useLeaderboard
├── hooks/                useDebounce, usePagination, useRole
├── lib/
│   └── axios.ts          Single axios instance with JWT Bearer interceptor
├── services/             Typed API functions — one file per resource domain
│   ├── auth.service.ts
│   ├── contest.service.ts
│   ├── problem.service.ts
│   ├── submission.service.ts
│   └── leaderboard.service.ts
├── store/
│   └── auth.store.ts     Zustand: currentUser, token, isAuthenticated
├── types/
│   ├── api.types.ts      ApiResponse<T>, PaginatedResponse<T>, ApiError
│   ├── auth.types.ts     User, Role, LoginRequest, RegisterRequest, AuthResponse
│   ├── contest.types.ts  Contest, ContestStatus, ContestProblem
│   ├── problem.types.ts  Problem, Difficulty, TestCase
│   ├── submission.types.ts Submission, SubmissionStatus, SubmissionVerdict
│   └── leaderboard.types.ts LeaderboardEntry
└── utils/
    ├── format.ts         Date formatting, score formatting
    └── status.ts         Submission status → label, colour, icon mapping
```

## Key patterns

### Axios instance (src/lib/axios.ts)

```typescript
import axios from 'axios'

export const api = axios.create({ baseURL: '/api', headers: { 'Content-Type': 'application/json' } })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 → clear token + redirect to /login
api.interceptors.response.use(null, (error) => {
  if (error.response?.status === 401) { localStorage.removeItem('token'); window.location.href = '/login' }
  return Promise.reject(error)
})
```

### Service function pattern (src/services/)

```typescript
// src/services/submission.service.ts
export const submitCode = (body: SubmitRequest): Promise<ApiResponse<SubmissionResponse>> =>
  api.post('/v1/submissions', body).then(r => r.data)

export const getSubmission = (id: string): Promise<ApiResponse<SubmissionResponse>> =>
  api.get(`/v1/submissions/${id}`).then(r => r.data)
```

### Submission polling (TanStack Query)

```typescript
// src/features/submission/useSubmissionStatus.ts
const TERMINAL_STATUSES = ['ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','MEMORY_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILATION_ERROR','EVALUATION_ERROR']

export const useSubmissionStatus = (submissionId: string | null) =>
  useQuery({
    queryKey: ['submission', submissionId],
    queryFn: () => getSubmission(submissionId!),
    enabled: !!submissionId,
    refetchInterval: (query) =>
      TERMINAL_STATUSES.includes(query.state.data?.data.status ?? '') ? false : 2000,
  })
```

### Role guard (React Router)

```typescript
// src/components/layout/RoleGuard.tsx
// Wraps routes that require specific roles — redirects to /dashboard if unauthorized
```

## Submission status types

```typescript
type SubmissionStatus =
  | 'QUEUED' | 'RUNNING'                          // in-progress
  | 'ACCEPTED'                                     // success
  | 'WRONG_ANSWER' | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED' | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR' | 'EVALUATION_ERROR'       // failure types
```

## Current implementation status

No source files exist yet beyond `src/main.tsx`, `src/app/App.tsx`, `src/app/globals.css`, `src/lib/axios.ts`. Build from types → services → features → pages.
