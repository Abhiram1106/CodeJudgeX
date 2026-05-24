---
type: lessons
updated: 2026-05-23
tags: [lessons, debugging]
---

# Debugging Lessons — CodeJudgeX

> Append-only. Hard-won lessons from debugging sessions.
> Read this before starting any debug task.

---

## General lessons

### L-001 — Check error-memory.md before diagnosing anything

Before spending time on root cause analysis, always check `03-ERRORS/error-memory.md`.
If the bug has been seen before, the fix and prevention rule are already there.
Rediagnosing known bugs wastes time and risks introducing the same wrong fix.

### L-002 — Fix root causes, not symptoms

A submission showing EVALUATION_ERROR in the UI might be caused by:
- Judge0 unavailable (infrastructure)
- Worker crash mid-evaluation (application)
- Malformed evaluation job DTO (serialization)
- Idempotency bypass (logic bug)

Do not fix the display of EVALUATION_ERROR. Fix the underlying cause.

### L-003 — Async debugging requires tracing across boundaries

The submission → evaluation pipeline crosses three boundaries:
1. HTTP (frontend → API)
2. Message queue (API → RabbitMQ → worker)
3. HTTP again (worker → Judge0 CE)

A bug in step 2 may appear as a symptom in step 3.
Always trace the full path, not just the visible failure point.
Use correlation IDs in logs to follow a specific submission through all steps.

---

_New lessons are appended here after each debugging session._
