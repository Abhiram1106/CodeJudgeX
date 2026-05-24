# Agent: Frontend Feature — CodeJudgeX

## Trigger

Use this agent when building a new frontend page, component, feature, route, form,
or integrating a new API endpoint into the UI.

## Always include at session start

```
@.obsidian-ai-memory/02-PROJECTS/project-context.md
@.obsidian-ai-memory/02-PROJECTS/active-goals.md
@.obsidian-ai-memory/03-ERRORS/anti-patterns.md
@.cursor/context/frontend-context.md
```

## Execution steps

1. **Read active-goals.md.** Confirm the feature is in scope. If not, ask.

2. **Identify the role.** Which user role is this for — STUDENT, FACULTY, ADMIN?
   Add route guards accordingly.

3. **Design the component hierarchy before writing:**
   - Which page does this live in (`src/pages/`)?
   - What reusable components are needed (`src/components/`)?
   - What feature logic is needed (`src/features/{name}/`)?
   - What API calls are needed (`src/services/`)?
   - What TanStack Query keys are involved?

4. **Write in this order:**
   a. TypeScript types in `src/types/` for the API request/response shapes
   b. Service function in `src/services/` (typed, using `src/lib/axios.ts`)
   c. TanStack Query hooks in `src/features/{name}/` (`useQuery` for reads, `useMutation` for writes)
   d. Zod schema for any form validation
   e. Page component in `src/pages/` (thin — compose from features + components)
   f. Any new shared components in `src/components/shared/`
   g. Add route in `src/app/App.tsx` with role guard

5. **Submission polling pattern** (if this involves submissions):
   - Initiate POST, receive `submissionId`
   - Poll `GET /api/v1/submissions/{submissionId}` with `refetchInterval: 2000`
   - Stop when status is terminal (ACCEPTED, WRONG_ANSWER, TLE, MLE, RE, CE, EVALUATION_ERROR)
   - Show animated status badge while polling

6. **Monaco editor** (if this involves a code editor):
   - Use `src/components/editor/` wrapper — do not instantiate Monaco directly in pages
   - Always pass `language`, `value`, `onChange`, and `readOnly` props explicitly

7. **Keyboard accessibility:** every interactive element reachable by tab, every action triggerable by keyboard.

8. **No layout shift:** loading skeletons for all async content, fixed dimensions for editor.

9. **Verify:** `npm run typecheck` — must pass with zero errors. State result.

10. **Follow shutdown protocol.** Two-commit push.

## Pre-done checklist

- [ ] Feature in active-goals scope
- [ ] Types defined in `src/types/`
- [ ] API calls via `src/services/` only
- [ ] TanStack Query hooks in `src/features/`
- [ ] Page component thin — composed from features + components
- [ ] Route added with role guard
- [ ] Submission polling implemented if applicable
- [ ] Keyboard accessible
- [ ] No layout shift
- [ ] `npm run typecheck` passes
- [ ] Digest written and pushed
