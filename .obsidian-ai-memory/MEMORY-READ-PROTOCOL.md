---
type: protocol
updated: 2026-05-23
tags: [protocol, memory, retrieval]
---

# Memory Read Protocol — CodeJudgeX

> Defines HOW agents retrieve memory at session start.
> Follow this exactly — do not dump the entire vault into context.

## Retrieval modes

| Mode | Token budget | When to use |
|---|---|---|
| `minimal` | ~400 tokens | One-liner answers, trivial edits, quick lookups |
| `balanced` | ~1500 tokens | Default — most feature, fix, review work |
| `deep` | ~3000 tokens | Architecture changes, complex debugging, major refactors |

**Default:** `balanced`. Switch explicitly when the task demands it.

## Standard read order (stop when budget hit)

Read in this exact order. Stop when the token budget for the selected mode is reached.

```
1. 02-PROJECTS/session-continuity.md     ← ALWAYS first — rolling handoff from last session
2. 02-PROJECTS/project-context.md        ← ALWAYS — stack, constraints, do-not-repeat
3. 02-PROJECTS/active-goals.md           ← ALWAYS — current priorities and week scope
4. 03-ERRORS/error-memory.md             ← ALWAYS — never repeat known bugs
5. 03-ERRORS/anti-patterns.md            ← ALWAYS — never repeat known patterns
6. 01-SESSIONS/ (last 1–3 digests)       ← Context continuity
7. 04-DECISIONS/decisions.md             ← Architecture/design tasks only
8. 05-ARCHITECTURE/ relevant files       ← Architecture/design tasks only
9. 07-LESSONS/debugging-lessons.md       ← Debug tasks only
```

## Task-type read order overrides

Adjust priority before reading when the task type is clear:

| Task type | Read first (before step 3) |
|---|---|
| `debug / error / crash / failing` | `03-ERRORS/error-memory.md` then `07-LESSONS/debugging-lessons.md` |
| `architecture / design / schema` | `04-DECISIONS/decisions.md` then `05-ARCHITECTURE/` |
| `refactor / clean` | `03-ERRORS/anti-patterns.md` |
| `feature / build / implement` | `02-PROJECTS/active-goals.md` |

## Red flags — STOP and ask before proceeding if:

- A known error in `error-memory.md` matches exactly what the user is describing
- The last session digest says tests were failing and have not been fixed
- The user's request contradicts an active decision in `04-DECISIONS/decisions.md`
- The active-goals week scope doesn't match what the user is asking to build
- The task requires destructive operations (rm, DROP TABLE, force push, hard reset)

## Startup block (emit at top of first response)

```
[CodeJudgeX] Stack: Java 21 + Spring Boot 3 | React 18 + Vite | Mode: {balanced|deep|minimal}
[Memory] Read: {N files} | Last session: {YYYY-MM-DD} | Active goal: {one-line goal}
[Workflow] → {workflow name}
[Agents] → {roles}
[Starting] {one sentence}
```

This proves memory was loaded. Skip it only for truly trivial one-liners.
