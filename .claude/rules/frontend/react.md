# React Rules — CodeJudgeX Frontend

> Vite + React 18 + TypeScript 5.4 + React Router v6.
> NOT Next.js. No SSR. No server components. No app/ directory.
> Load alongside code-style.md — this file adds React-specific depth.

---

## Project structure contract

```
src/
  app/          App.tsx, globals.css, route definitions
  components/   Shared, reusable, domain-agnostic UI
  features/     Domain modules — one folder per bounded context
    submission/
      components/   Submission-specific components
      hooks/        useSubmission*, useLeaderboard*
      services/     submission.service.ts
      types/        submission.types.ts
  pages/        Route-level components — thin wrappers only
  lib/          axios.ts, queryClient.ts, utils
  types/        Global shared types (not feature-specific)
  stores/       Zustand stores
```

- Pages are thin: they compose features, pass route params, render layout. No business logic.
- Features own their own components, hooks, services, and types.
- `components/` is for genuinely reusable, domain-agnostic UI only (Button, Badge, Modal wrappers).
- Never import from a feature into another feature — go through `types/` or lift to `components/`.

---

## Component rules

- **Named exports only** — no default exports except `App.tsx` and page-level route components.
- **No `React.FC`** — plain function with typed props interface above the component.
- Props interface is local unless shared across features, then it belongs in `types/`.
- Hooks at top, derived values next, handlers below derived values, JSX last.
- One component per file. File name matches component name exactly (PascalCase).
- **No inline styles** — Tailwind utility classes only. Exception: pixel values that Tailwind can't express.
- **No `className` string concatenation** — use `cn()` from `@/lib/utils` (shadcn/ui convention).

```typescript
interface SubmissionCardProps {
  submissionId: string
  status: SubmissionStatus
  score: number
  language: string
}

export function SubmissionCard({ submissionId, status, score, language }: SubmissionCardProps) {
  const { data: details } = useSubmissionDetails(submissionId)
  const isTerminal = TERMINAL_STATUSES.has(status)
  const handleViewCode = () => { /* ... */ }

  return (
    <div className={cn('rounded-lg border p-4', isTerminal ? 'opacity-100' : 'opacity-70')}>
      {/* JSX */}
    </div>
  )
}
```

---

## TanStack Query v5 patterns

### Standard query

```typescript
export function useSubmissions(contestId: string) {
  return useQuery({
    queryKey: ['submissions', contestId],
    queryFn: () => submissionService.getByContest(contestId),
    staleTime: 30_000,
  })
}
```

### Submission status polling (critical pattern)

Polling MUST stop when a terminal status is reached. Never poll indefinitely.

```typescript
const TERMINAL_STATUSES = new Set<SubmissionStatus>([
  'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED', 'RUNTIME_ERROR', 'COMPILATION_ERROR',
])

export function useSubmissionStatus(submissionId: string) {
  return useQuery({
    queryKey: ['submission', submissionId, 'status'],
    queryFn: () => submissionService.getStatus(submissionId),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status && TERMINAL_STATUSES.has(status) ? false : 2000
    },
  })
}
```

### Mutations

```typescript
export function useSubmitCode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: submissionService.submit,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['submissions'] })
    },
  })
}
```

- `queryKey` arrays: `[resource, id, sub-resource]` — consistent shape enables targeted invalidation.
- `staleTime` set explicitly — never rely on the default 0 for data that doesn't change frequently.
- Never call `queryClient.setQueryData` unless the server returns the full updated object.

---

## React Hook Form + Zod

```typescript
const submitCodeSchema = z.object({
  problemId: z.string().uuid(),
  languageId: z.number().int().positive(),
  sourceCode: z.string().min(1, 'Code cannot be empty').max(65_536, 'Exceeds size limit'),
})

type SubmitCodeForm = z.infer<typeof submitCodeSchema>

export function SubmitCodeForm({ problemId }: { problemId: string }) {
  const { register, handleSubmit, formState: { errors } } = useForm<SubmitCodeForm>({
    resolver: zodResolver(submitCodeSchema),
    defaultValues: { problemId },
  })

  const { mutate: submitCode, isPending } = useSubmitCode()

  return (
    <form onSubmit={handleSubmit((data) => submitCode(data))}>
      {/* fields */}
    </form>
  )
}
```

- Schema defined above the component, not inline.
- `z.infer<typeof schema>` — no manual type duplication.
- Always display `errors.field?.message` — never silent validation failures.

---

## Monaco Editor

- Wrap `@monaco-editor/react` in a local `CodeEditor` component — never use it directly in pages.
- The wrapper handles: language prop mapping to Monaco language id, theme, read-only mode, size constraints.
- Pass `defaultValue` not `value` for uncontrolled mode (submission viewing).
- Pass `onChange` with debounce (300ms) for controlled mode (code writing).
- Always specify `height` — Monaco with no height collapses to 0px.

```typescript
interface CodeEditorProps {
  value?: string
  defaultValue?: string
  language: 'java' | 'cpp' | 'python' | 'javascript' | 'c'
  readOnly?: boolean
  onChange?: (value: string) => void
  height?: string
}

export function CodeEditor({ language, readOnly = false, height = '400px', ...props }: CodeEditorProps) {
  const monacoLanguage = LANGUAGE_TO_MONACO[language] // 'java' | 'cpp' | 'python' | 'javascript' | 'c'
  return (
    <Editor
      height={height}
      language={monacoLanguage}
      theme="vs-dark"
      options={{ readOnly, minimap: { enabled: false } }}
      {...props}
    />
  )
}
```

---

## Zustand auth store

```typescript
interface AuthState {
  user: AuthUser | null
  accessToken: string | null
  setAuth: (user: AuthUser, token: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  setAuth: (user, accessToken) => set({ user, accessToken }),
  clearAuth: () => set({ user: null, accessToken: null }),
}))
```

- One store for auth state (`stores/auth.store.ts`). No other global Zustand stores unless justified.
- Derive `isAuthenticated` and `hasRole()` from the store via selector hooks — don't store derived booleans.
- The axios interceptor in `lib/axios.ts` reads `useAuthStore.getState().accessToken` — not a hook call.

---

## Route guards and role guards

```typescript
export function RequireAuth({ children }: { children: ReactNode }) {
  const user = useAuthStore((s) => s.user)
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

export function RequireRole({ role, children }: { role: UserRole; children: ReactNode }) {
  const user = useAuthStore((s) => s.user)
  if (!user || !hasRole(user, role)) return <Navigate to="/unauthorized" replace />
  return <>{children}</>
}
```

- Guards live in `components/auth/`. They render `<Navigate>` — never `null`, never throw.
- Role hierarchy enforced: `STUDENT < FACULTY < ADMIN < SUPER_ADMIN`.
- `hasRole(user, role)` checks the hierarchy — a SUPER_ADMIN has all lower roles automatically.
- Never conditionally render admin UI based on role alone — always use `RequireRole` at the route level.

---

## shadcn/ui conventions

- Import from `@/components/ui/` — never from `shadcn/ui` directly.
- Do not modify files in `components/ui/` — they are generated. Customize via `className` prop.
- Compose shadcn primitives into domain-specific components in `components/` or `features/`.
- Use `cn()` for conditional class merging — not template literals, not `classnames` package.

---

## Service layer

```typescript
// features/submission/services/submission.service.ts
export const submissionService = {
  submit: (data: CreateSubmissionRequest): Promise<SubmissionResponse> =>
    apiClient.post('/submissions', data).then((r) => r.data.data),

  getStatus: (id: string): Promise<SubmissionStatusResponse> =>
    apiClient.get(`/submissions/${id}/status`).then((r) => r.data.data),

  getByContest: (contestId: string): Promise<SubmissionResponse[]> =>
    apiClient.get(`/submissions?contestId=${contestId}`).then((r) => r.data.data),
}
```

- All API calls go through `apiClient` from `@/lib/axios` — never raw `fetch`.
- `.then((r) => r.data.data)` unwraps the `ApiResponse<T>` envelope — services return the inner `data` type.
- Service functions are plain objects (not classes). No `new SubmissionService()`.
- Types come from `features/submission/types/submission.types.ts` or `src/types/`.

---

## Type rules

```typescript
// submission.types.ts
export type SubmissionStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT_EXCEEDED'
  | 'MEMORY_LIMIT_EXCEEDED'
  | 'RUNTIME_ERROR'
  | 'COMPILATION_ERROR'

export type UserRole = 'STUDENT' | 'FACULTY' | 'ADMIN' | 'SUPER_ADMIN'
```

- Status unions defined once in `types/` — never re-declared in components.
- No `enum` — use union types. Enums serialize inconsistently across TS/JSON boundary.
- No `any` — use `unknown` with a type guard, or define the exact type.
- API response shape: `{ success: boolean, data: T, message?: string, timestamp: string }`.

---

## Error handling

- Query errors surface via `isError` + `error` from `useQuery` — display with an error state component.
- Mutation errors surface via `onError` callback or `mutation.error` — show inline form error or toast.
- Axios 401 interceptor clears auth store and redirects to `/login` — do not handle 401 in components.
- Never swallow errors silently in `.catch(() => {})` — at minimum log to console.error.

---

## What never goes here

- No `getServerSideProps`, `getStaticProps`, `use server`, `use client` — this is not Next.js.
- No `React.lazy` without a `<Suspense>` boundary wrapping it.
- No `useEffect` for data fetching — use TanStack Query.
- No direct DOM manipulation (`document.getElementById`) — use refs.
- No `index.tsx` barrel files that re-export everything — explicit imports only.
