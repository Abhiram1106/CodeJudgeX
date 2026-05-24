# Code Style Rules — CodeJudgeX

> Polyglot style guide. Applies to all languages in this monorepo.
> Claude Code loads this automatically via `.claude/` rules discovery.

---

## Universal conventions

- **One concern per file, one concern per commit.** Don't bundle unrelated changes.
- **Names describe intent, not implementation.** `evaluateSubmission()` not `doTheThing()`.
- **No abbreviations** unless universally understood (`id`, `url`, `dto`, `jwt`, `api`).
- **No commented-out code** in committed files. Delete it — git history exists.
- **No `TODO` comments** without a linked issue or a date. Undated TODOs are permanent lies.
- **No magic numbers.** Extract to named constants with units in the name: `TIME_LIMIT_MS`, `MAX_PAGE_SIZE`.

---

## Java (backend/)

### Naming

| Construct | Convention | Example |
|---|---|---|
| Class | PascalCase | `SubmissionService`, `EvaluationWorker` |
| Interface | PascalCase, no `I` prefix | `SubmissionRepository`, not `ISubmissionRepository` |
| Method | camelCase, verb-first | `evaluateSubmission()`, `findByContestId()` |
| Variable | camelCase | `submissionId`, `totalScore` |
| Constant | SCREAMING_SNAKE_CASE | `MAX_RETRY_ATTEMPTS`, `EVALUATION_QUEUE` |
| Package | lowercase, singular | `com.codejudgex.submission`, not `submissions` |
| DTO class | Suffix with purpose | `CreateSubmissionRequest`, `SubmissionResponse` |
| Entity class | Plain noun | `Submission`, `Problem`, `Contest` |
| Exception class | Suffix with `Exception` | `SubmissionNotFoundException`, `ContestNotLiveException` |

### Structure rules

```java
// Controller method shape — validate → delegate → return. Nothing else.
@PostMapping
@ResponseStatus(HttpStatus.ACCEPTED)
public ApiResponse<SubmissionResponse> submit(
    @Valid @RequestBody CreateSubmissionRequest request,
    @AuthenticationPrincipal UserDetails user) {
    return ApiResponse.success(submissionService.submit(request, user.getUsername()));
}

// Service method shape — business logic, @Transactional where needed
@Transactional
public SubmissionResponse submit(CreateSubmissionRequest request, String userEmail) {
    // validate → persist → publish → return DTO
}
```

- `@Transactional` goes on the **service** method, never the controller or repository
- `@PreAuthorize` goes on the **controller** method or class
- `@Valid` on every controller parameter that accepts a request body
- Spring Data repository method names must be self-explanatory: `findByContestIdAndUserId()`, not `find()`
- Use `Optional<T>` returns from repositories — never return `null`
- Throw typed exceptions: `throw new SubmissionNotFoundException(submissionId)` — not `RuntimeException`

### Formatting

- 4-space indentation (no tabs)
- Opening brace on same line: `public void foo() {`
- Max line length: 120 characters
- One blank line between methods
- `@Override` always present when overriding
- Lombok `@Data` / `@Builder` / `@RequiredArgsConstructor` on entities and DTOs — no manual getters/setters

### Imports

- No wildcard imports (`import com.codejudgex.submission.*` is banned)
- Group: Java stdlib → third-party → project internal
- Remove unused imports before committing

---

## TypeScript / React (frontend/)

### Naming

| Construct | Convention | Example |
|---|---|---|
| Component | PascalCase | `SubmissionStatusBadge`, `ContestCard` |
| Hook | camelCase, `use` prefix | `useSubmissionStatus`, `useContests` |
| Service function | camelCase, verb-first | `submitCode()`, `getLeaderboard()` |
| Type / Interface | PascalCase | `SubmissionResponse`, `ContestStatus` |
| Enum-like union | PascalCase | `type SubmissionStatus = 'ACCEPTED' | 'QUEUED' | ...` |
| Constant | SCREAMING_SNAKE_CASE | `TERMINAL_STATUSES`, `DEFAULT_PAGE_SIZE` |
| File (component) | PascalCase | `SubmissionStatusBadge.tsx` |
| File (hook/util/service) | camelCase | `useSubmissionStatus.ts`, `submission.service.ts` |

### Structure rules

```typescript
// Component shape — props typed inline or as interface above component
interface SubmissionCardProps {
  submissionId: string
  status: SubmissionStatus
  score: number
}

export function SubmissionCard({ submissionId, status, score }: SubmissionCardProps) {
  // hooks at top
  // derived values
  // handlers
  // return JSX
}
```

- **Named exports** for components — no default exports (except `App.tsx` and page files)
- **No inline styles** — Tailwind classes only. Exception: dynamic pixel values that can't be expressed in Tailwind
- **Props interfaces** above the component, not in a separate types file (unless shared)
- **Shared types** in `src/types/` — not scattered across feature files
- **No `React.FC`** — use plain function declarations with typed props
- **`const` over `let`** — always. `let` only when reassignment is necessary
- **No `any`** — use `unknown` with a type guard, or define the type properly

### Import order

```typescript
// 1. React
import { useState, useEffect } from 'react'
// 2. Third-party libraries
import { useQuery } from '@tanstack/react-query'
// 3. Internal — absolute (@/ alias)
import { Button } from '@/components/ui/button'
import { useSubmissionStatus } from '@/features/submission/useSubmissionStatus'
// 4. Types
import type { SubmissionStatus } from '@/types/submission.types'
```

---

## SQL / Flyway migrations

- Table names: `snake_case`, plural — `users`, `contest_problems`, `submission_results`
- Column names: `snake_case` — `created_at`, `is_sample`, `total_score`
- Primary keys: `id UUID DEFAULT gen_random_uuid()`
- Timestamps: `created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()` on every table
- Foreign keys: named explicitly — `CONSTRAINT fk_submissions_contest FOREIGN KEY (contest_id) REFERENCES contests(id)`
- Indexes: named `idx_{table}_{columns}` — `idx_submissions_contest_id`, `idx_submissions_user_id`
- Migration files: every file starts with a comment block describing what and why
- No DML mixed with DDL in the same migration file

```sql
-- V1__create_users_and_roles.sql
-- Creates core user authentication tables.
-- Roles are stored separately and linked via user_roles join table.

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    ...
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

---

## Git commit messages

Format: `<type>(<scope>): <subject>`

```
feat(auth): implement JWT refresh token rotation
fix(evaluation): handle Judge0 timeout with retry queue
refactor(submission): extract verdict calculation to separate method
docs(readme): add Judge0 CE setup instructions
test(contest): add integration test for contest lifecycle
chore(deps): bump Spring Boot to 3.3.1
memory: 2026-05-23 claude — scaffold + AI agent infrastructure
```

- **Types:** `feat` `fix` `refactor` `docs` `test` `chore` `perf` `style`
- **Scope:** the module or area affected — `auth`, `submission`, `frontend`, `infra`, `deps`
- **Subject:** present tense, lowercase, no period — `add X`, `fix Y`, `remove Z`
- **Memory commits:** always `memory: YYYY-MM-DD <tool> — <summary>` (no scope)
- **Max subject length:** 72 characters
- **Body:** optional, use when the "why" is not obvious from the subject
