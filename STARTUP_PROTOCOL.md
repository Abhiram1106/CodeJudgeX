# Startup Protocol — CodeJudgeX

> Every AI tool executes this before any response or edit on every session.

## Detect project markers

Scan for: `.obsidian-ai-memory/` · `AGENTS.md` · `AI_RULES.md` · `.claude/` · `.cursor/`

If found → full protocol active.

## Identify project type

- `pom.xml` + `vite.config.ts` → **fullstack-java** (this project)
- Backend: Java 21 + Spring Boot 3 modular monolith
- Frontend: React 18 + Vite + TypeScript
- NOT a Next.js project, NOT a Python project, NOT microservices

## Retrieve memory (balanced mode by default)

```
Always:  02-PROJECTS/session-continuity.md   ← first — rolling handoff
Always:  02-PROJECTS/project-context.md      ← stack, constraints
Always:  02-PROJECTS/active-goals.md         ← current week priorities
Always:  03-ERRORS/error-memory.md           ← never repeat known bugs
Always:  03-ERRORS/anti-patterns.md          ← never repeat patterns
Default: 01-SESSIONS/ last 1–3 digests       ← recent context

On architecture/design:  04-DECISIONS/decisions.md + 05-ARCHITECTURE/system-overview.md
On debugging:            07-LESSONS/debugging-lessons.md
```

Full retrieval protocol: `.obsidian-ai-memory/MEMORY-READ-PROTOCOL.md`

## Auto-route workflow

| Signal | Workflow |
|---|---|
| build / add / create / implement | feature-build |
| error / broken / crash / failing | debug → bug-fix |
| test failing | bug-fix + testing |
| review / audit | code-review |
| refactor / clean | refactor |
| deploy / ship | deployment |
| slow / performance | debug + performance |
| docs / readme | docs-update |
| security / auth / CVE | code-review + security |
| schema / migration / database | feature-build + database |
| empty vault / first run | project-onboarding |

## Activate agent roles

| Area | Roles |
|---|---|
| Backend Java/Spring | backend + reviewer |
| Frontend React/TS | frontend + reviewer |
| Auth / JWT / security | backend + security |
| Database / Flyway | database + backend |
| RabbitMQ / evaluation | backend + devops |
| Architecture | architect + reviewer |
| Full-stack feature | fullstack + architect + reviewer |

## Emit startup block (required)

```
[CodeJudgeX] Stack: Java 21 + Spring Boot 3 | React 18 + Vite | Mode: {balanced|deep|minimal}
[Memory] Read: {N files} | Last session: {YYYY-MM-DD} | Active goal: {one-line goal}
[Workflow] → {workflow}
[Agents] → {roles}
[Starting] {one sentence}
```

## Work

Execute the task following `AGENTS.md` engineering rules.

## Completion checklist (before claiming done)

- [ ] Changed files correct and match stated intent
- [ ] Tests/typecheck/lint ran — state result or state why skipped
- [ ] Docs updated if behaviour or setup changed
- [ ] Session digest written to `01-SESSIONS/YYYY-MM-DD/session-HHMM-<tool>.md`
- [ ] `session-continuity.md` overwritten
- [ ] `error-memory.md` appended if a bug was fixed
- [ ] `decisions.md` appended if a non-trivial decision was made
- [ ] No secrets in any written or modified file
- [ ] Open risks explicitly listed

## Shutdown

Follow `MEMORY-WRITE-PROTOCOL.md` + two-commit rule:
1. Code commit: `feat:|fix:|refactor:|docs: <description>`
2. Memory commit: `memory: YYYY-MM-DD <tool> — <summary>`
3. Push + include `## Memory` block in final reply
