# AI_RULES.md — CodeJudgeX

> Universal engineering rules for every AI tool on this project.
> Full rules and protocol: `AGENTS.md` (the canonical single source of truth).

## Quick reference

| Rule | Detail |
|---|---|
| Memory first | Read `.obsidian-ai-memory/02-PROJECTS/session-continuity.md` before every response |
| No repeat errors | Check `03-ERRORS/error-memory.md` before diagnosing anything |
| Small changes | One concern per edit, one concern per commit |
| Verify before done | Run tests/typecheck/lint — state result or explicitly state why skipped |
| No secrets | Never log, print, commit, or hardcode credentials or tokens |
| Ask before destructive | `rm -rf`, DROP TABLE, force push, hard reset → confirm with user first |
| Update docs | Behaviour change without doc update is incomplete |
| Write digest | Every meaningful session ends with a session digest |
| Record assumptions | Unstated assumptions go in the session digest |
| Two commits | Code commit (feat:/fix:) + memory commit (memory: ...) — always separate |

## CodeJudgeX-specific rules

| Rule | Detail |
|---|---|
| No entity in responses | JPA entities are NEVER returned in API responses — always use response DTOs |
| Hidden test cases | Never returned in any student-facing API response — enforced at service layer |
| Async evaluation | POST /submissions returns 202 Accepted — evaluation is never synchronous |
| No exec user code | Spring Boot never calls Runtime.exec() on user input — Judge0 CE handles all execution |
| Env-only config | All secrets from environment variables — never hardcoded |
| Thin controllers | Controllers: validate → service → return DTO. No business logic in controllers. |

## Retrieval modes

- `minimal` (~400 tokens): Quick lookups, one-liners
- `balanced` (~1500 tokens): Default for most work
- `deep` (~3000 tokens): Architecture, complex debugging

Full protocol: `.obsidian-ai-memory/MEMORY-READ-PROTOCOL.md`
