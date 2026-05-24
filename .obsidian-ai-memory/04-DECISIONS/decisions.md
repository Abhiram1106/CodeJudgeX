---
type: decisions
updated: 2026-05-23
tags: [decisions, adr]
---

# Decision Log — CodeJudgeX

> Append-only. Never edit past decisions.
> To reverse a decision, append a new entry and mark the old one [SUPERSEDED by D-NNN].

---

## D-001 — Modular monolith over microservices

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** CodeJudgeX backend is a modular monolith, not microservices.
- **Context:** Academic project requiring fast iteration, local development, and demo-ability. No DevOps team.
- **Options considered:**
  - Microservices: high DevOps overhead, complex debugging, overkill for this scale
  - Modular monolith: clean module boundaries, easy to evolve, single deployment unit
  - Simple monolith: no module boundaries, becomes spaghetti fast
- **Final choice:** Modular monolith
- **Why:** Easier to build, debug, test, and deploy. Still enforces clean separation. Can be split into services later if needed.
- **Tradeoffs:** Cannot scale individual modules independently without restructuring.
- **Review date:** After Week 5 — if scale requirements change, reconsider.

---

## D-002 — Async evaluation via RabbitMQ (never synchronous)

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** Code evaluation is always asynchronous. POST /submissions returns 202 Accepted immediately.
- **Context:** Code execution can take up to the time limit (e.g. 2 seconds × N test cases). Blocking an HTTP thread for this is unacceptable.
- **Options considered:**
  - Synchronous evaluation: simple, but API timeouts, poor UX during load
  - WebSocket-pushed evaluation: complex, requires stateful connections
  - Queue-based async (chosen): API returns instantly, worker processes independently, frontend polls
- **Final choice:** RabbitMQ queue + evaluation worker + frontend polling
- **Why:** API stays fast. Workers can scale independently. Retry/DLQ handles failures cleanly.
- **Tradeoffs:** Frontend must poll for results. Slightly higher complexity in evaluation flow.

---

## D-003 — PostgreSQL as source of truth, Redis for speed layer only

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** PostgreSQL is the authoritative source for all data. Redis is used only for fast-read ephemeral data.
- **Redis uses:** Live leaderboard (sorted sets), submission status cache (during evaluation), rate limiting counters.
- **PostgreSQL holds:** Everything — including leaderboard snapshots as fallback.
- **Why:** Redis data is reconstructible from PostgreSQL. This keeps recovery simple.
- **Tradeoffs:** Slightly more complex leaderboard logic (write to both, fallback to PG on Redis miss).

---

## D-004 — MapStruct for all entity ↔ DTO mapping (never expose JPA entities)

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** JPA entities are never serialized directly to JSON. All API responses use response DTOs mapped by MapStruct.
- **Why:** Prevents accidental exposure of sensitive fields (passwordHash, hidden test case data), decouples API contract from DB schema, prevents N+1 serialization issues.
- **Rule:** If you see an `@Entity` class used as a `@ResponseBody` type, it is a bug.

---

## D-005 — Judge0 CE for sandboxed code execution (never exec user code in Spring Boot)

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** All user-submitted code runs inside Judge0 CE. Spring Boot NEVER calls Runtime.exec() or ProcessBuilder on user input.
- **Why:** Security — user code could attempt to read files, open network connections, fork-bomb the JVM, etc. Judge0 provides isolation, resource limits, and sandboxing.
- **Tradeoffs:** Dependency on Judge0 availability. Mitigated by retry queue + DLQ.

---

## D-006 — Flyway for database migrations (no Hibernate ddl-auto create/update)

- **Date:** 2026-05-23
- **Status:** Active
- **Decision:** `spring.jpa.hibernate.ddl-auto=validate` in all environments. All schema changes via Flyway versioned SQL files.
- **Why:** Hibernate auto-create loses history. Flyway gives explicit, reviewable, versioned migrations. Validate catches drift between entity and DB.
- **Rule:** Never set ddl-auto to create, create-drop, or update in any environment except throw-away test containers.

---

## D-007 — docs/ROADMAP.md is the authoritative build plan

- **Date:** 2026-05-24
- **Status:** Active
- **Decision:** `docs/ROADMAP.md` is the canonical implementation guide. All AI agents read it for any feature-build task.
- **Context:** A detailed 5-week build plan was written covering exact file creation order, SQL blueprints, module acceptance criteria, Judge0 API reference, and risk register. Without a single authoritative source, agents would invent their own build order.
- **Rule:** Before implementing any module, read the ROADMAP section for that module. The file order and acceptance criteria there override any agent's intuition.

---

## D-008 — Mandatory two-commit + git push at end of every session

- **Date:** 2026-05-24
- **Status:** Active
- **Decision:** Every session that touches code or memory must end with: (1) code commit scoped to application files, (2) separate memory commit scoped to vault files, (3) `git push origin HEAD`.
- **Context:** Previously the shutdown protocol existed but git push was "optional" and the `## Memory` block was not enforced. Sessions were completing without vault state being committed, making recovery across tool switches unreliable.
- **Why:** The vault is only useful as a handoff mechanism if it is committed and pushed. An uncommitted session-continuity.md is invisible to the next session on a different machine or tool.
- **Rule:** No session is "done" until `git push origin HEAD` has executed and the final reply contains the `## Memory` block with commit hashes and push status.
