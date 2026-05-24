# CLAUDE.md — CodeJudgeX

> Claude Code reads this file on every session. Full rules and protocol live in AGENTS.md.

@AGENTS.md

---

## Claude Code-specific settings

**Default retrieval mode:** `balanced` (~1500 tokens).
Switch to `deep` for architecture tasks or complex debugging.
Switch to `minimal` for one-liner answers or quick lookups.

**Build-phase verification commands (run these, don't skip):**

| Check | Command | When |
|---|---|---|
| Backend compiles | `cd backend && ./mvnw compile -q` | After every Java file change |
| Backend tests pass | `cd backend && ./mvnw test` | After completing any module |
| Frontend typechecks | `cd frontend && npm run typecheck` | After every TypeScript change |
| Frontend lints | `cd frontend && npm run lint` | Before any commit |
| Frontend builds | `cd frontend && npm run build` | End of Week 4 + every deploy |

---

## Completion gate — do not say "done" until ALL boxes are checked

- [ ] Changed files are correct and match stated intent
- [ ] `./mvnw compile` passes (backend) OR `npm run typecheck` passes (frontend) — state the result
- [ ] Unit tests written and passing for every new `@Service` or hook
- [ ] Docs/ROADMAP updated if behaviour or API contract changed
- [ ] Session digest written to `.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-claude.md`
- [ ] `session-continuity.md` overwritten with current state
- [ ] `error-memory.md` appended if a bug was fixed
- [ ] `decisions.md` appended if a non-trivial choice was made
- [ ] Code commit made (`feat:/fix:/refactor:/docs:` — application files only)
- [ ] Memory commit made (`memory: YYYY-MM-DD claude — ...` — vault files only)
- [ ] `git push origin HEAD` executed
- [ ] No secrets in any written or modified file
- [ ] Open risks explicitly listed if any remain

---

## Vault shutdown — AUTOMATIC, no consent needed

At the end of every chat, the Stop hook in `.claude/settings.json` automatically:
1. Stages all vault + config files (`.obsidian-ai-memory/`, `AGENTS.md`, `CLAUDE.md`, `.claude/`, `.cursor/`, `.omnix/`)
2. Commits with `memory: YYYY-MM-DD HH:MM claude — auto session commit`
3. Pushes to `origin HEAD`

**You do not ask. You do not wait. It happens automatically.**

Before the hook fires, you MUST have already written:
- Session digest → `.obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-claude.md`
- Updated `session-continuity.md`
- Checked off any completed tasks in `active-goals.md`
- Appended to `error-memory.md` if a bug was fixed
- Appended to `decisions.md` if a non-trivial decision was made

## Code shutdown — REQUIRES USER CONSENT

Application files (`backend/`, `frontend/`, `infra/`, `docs/`) are NEVER committed automatically.
Always ask: *"Ready to commit the code changes?"* before staging or committing them.

## Required ## Memory block in every final reply

```
## Memory
- Digest: .obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-claude.md
- Vault: auto-committed + pushed ✓ (Stop hook)
- Code commit: <hash> — <subject>   |   pending user consent
- Next task: <one line from active-goals.md>
```
