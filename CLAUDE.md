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

## End-of-session ## Memory block format (required in final reply)

Every session that changes files must end with this block in the final reply:

```
## Memory
- Digest: .obsidian-ai-memory/01-SESSIONS/YYYY-MM-DD/session-HHMM-claude.md
- Code commit: <short-hash> — <subject>
- Memory commit: <short-hash> — memory: YYYY-MM-DD claude — <summary>
- Push: ✓ pushed to origin/main
```

If push was not done, state why explicitly — do not silently omit it.
