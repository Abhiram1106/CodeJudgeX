# Omnix Workflows — CodeJudgeX

> Workflow overrides for this project. Each file customizes a default Omnix
> workflow with CodeJudgeX-specific steps, gates, and conventions.

---

## feature-build

Triggered by: build / add / implement / create

### Steps

1. **Memory** — load `project-context.md` + `active-goals.md` + `error-memory.md`
2. **Scope** — confirm which module(s) are affected; check `active-goals.md` for week priority
3. **Design gate** — for anything non-trivial, run brainstorming skill before writing code
4. **Layer order (backend)** — entity → Flyway migration → repository → request DTO → response DTO → mapper → service → controller → exception classes
5. **Layer order (frontend)** — types → service → TanStack Query hooks → Zod schema → page → components → route registration
6. **Security check** — hidden test cases never in student responses; no hardcoded secrets; no Runtime.exec()
7. **Async check** — evaluation always via RabbitMQ; never synchronous Judge0 calls from controller
8. **Verification** — `./mvnw compile` passes + `npm run typecheck` passes
9. **Memory write** — session digest + update `session-continuity.md`
10. **Vault commit (automatic)** — `git add .obsidian-ai-memory/ AGENTS.md CLAUDE.md .claude/ .cursor/ .omnix/` → commit → push. No consent needed.
11. **Code commit (requires consent)** — ask user before staging `backend/ frontend/ infra/ docs/`

---

## debug

Triggered by: error / broken / crash / failing / exception

### Steps

1. **Memory** — load `error-memory.md` first; stop if error is already documented
2. **Anti-patterns** — load `anti-patterns.md`; confirm this path wasn't already ruled out
3. **Reproduce** — get a minimal reproduction before touching code
4. **Trace** — follow the call chain: HTTP → service → RabbitMQ → consumer → Judge0 → response
5. **Root cause** — fix the origin, not the symptom
6. **Regression test** — add a test that would have caught this
7. **Memory write** — append to `error-memory.md`; promote pattern to `anti-patterns.md` if it could recur
8. **Vault commit (automatic)** — commit + push vault files immediately, no consent needed
9. **Code commit (requires consent)** — ask user before committing fix to `backend/` or `frontend/`

---

## code-review

Triggered by: review / audit / check quality

### Gates (all must pass before marking clean)

- [ ] No JPA entities in API responses — always mapped through DTOs
- [ ] No `any` in TypeScript
- [ ] No magic numbers — named constants with units
- [ ] `@Transactional` on service methods only, never controller or repo
- [ ] `@PreAuthorize` on controller methods or class
- [ ] `@Valid` on every `@RequestBody`
- [ ] Hidden test cases (`is_sample = false`) excluded from student-facing endpoints
- [ ] No hardcoded secrets, credentials, or connection strings
- [ ] Flyway migrations: no modification of existing versioned files
- [ ] Submission polling: `refetchInterval` stops on terminal status

---

## deployment

Triggered by: deploy / ship / release

### Steps (not automated — manual checklist)

1. All tests pass: `./mvnw test` + `npm run typecheck`
2. No `TODO` comments without linked issue
3. Flyway migrations validated against a clean DB restore
4. `.env.example` updated if new env vars were added
5. `infra/docker-compose.yml` reflects any new service dependencies
6. `README.md` updated if setup steps changed
7. Session digest written with release summary
8. **Vault commit (automatic)** — commit + push `.obsidian-ai-memory/` and all agent configs immediately, no consent needed
9. **Code commit (requires consent)** — ask user before committing `backend/ frontend/ infra/ docs/`

---

## database

Triggered by: migration / schema / flyway / table

### Steps

1. **Memory** — load `database-context.md` from `.cursor/context/` for current schema state
2. **Design** — confirm table name (plural snake_case), column types, FK constraints, index plan
3. **Write migration** — `V{n}__{description}.sql` with comment header block; DDL only
4. **Co-update** — entity class + repository method + MapStruct mapper updated in same commit
5. **Validate** — `ddl-auto=validate` will catch mismatches at startup; run backend to confirm
6. **Decision log** — if schema design was non-trivial, append to `decisions.md`
7. **Vault commit (automatic)** — commit + push vault files, no consent needed
8. **Code commit (requires consent)** — ask user before committing migration + entity changes

---

---

## END-OF-SESSION DIGEST — applies to ALL workflows above

Every workflow's final step MUST output a session digest directly to the user in the chat reply.
This is mandatory across all workflows (feature-build, debug, code-review, deployment, database).

**Required digest format:**

```markdown
## Session Digest — YYYY-MM-DD

### ✅ What was done this session
- <every file written or changed, test run, bug fixed, decision made>

### 🔧 What still needs to be done
- <unchecked tasks from active-goals.md relevant to current week>

### 🧪 What you should test / verify manually
- <real curl commands, browser URLs, docker log greps — specific and actionable>

### ⚠️ Open risks / known issues
- <anything incomplete, broken, or unsafe — be honest>

### 📋 Decisions made
- <non-trivial choices this session — what and why>

### 🚀 Recommended next step
<one specific sentence — exact file or command to start with next session>
```

Rules:
- Output this in the reply itself — the user reads this instead of opening vault files
- "What to test" must have real commands, not vague statements
- Do NOT skip for short sessions — every session with any change gets a digest

---

## Adding workflow overrides

Create a `.md` file in this directory named after the workflow to override.
The file replaces the default workflow steps for this project only.
