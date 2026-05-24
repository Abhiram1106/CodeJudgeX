# Agent: Backend Feature — CodeJudgeX

## Trigger

Use this agent when building a new backend feature, module, endpoint, service method,
RabbitMQ consumer, or database migration.

## Always include at session start

```
@.obsidian-ai-memory/02-PROJECTS/project-context.md
@.obsidian-ai-memory/02-PROJECTS/active-goals.md
@.obsidian-ai-memory/03-ERRORS/anti-patterns.md
@.cursor/context/backend-context.md
@.cursor/context/database-context.md
```

For evaluation/queue work also include:
```
@.cursor/context/evaluation-context.md
```

## Execution steps

1. **Read active-goals.md.** Confirm the feature is in scope for the current week. If not, ask.

2. **Identify the module.** Which of the 16 modules does this belong to? Never add code to a module it doesn't belong to.

3. **Design the layer stack before writing code:**
   - What is the request DTO (fields, validation annotations)?
   - What is the response DTO (fields to expose, fields to exclude)?
   - What does the service method do (business logic, validation, DB calls, queue publish)?
   - What entity changes are needed (new fields, new table)?
   - Does this require a Flyway migration?
   - Does this need a RabbitMQ message published?

4. **Write in this order:**
   a. Entity (if new fields/table)
   b. Flyway migration script `backend/src/main/resources/db/migration/V{N}__{description}.sql`
   c. Repository (if new query methods)
   d. Request DTO + Zod validation
   e. Response DTO (never expose entity directly)
   f. MapStruct mapper
   g. Service (business logic, `@Transactional` where needed)
   h. Controller (thin — validate → service → return)
   i. Exception types if needed

5. **Security check on every endpoint:**
   - Protected endpoints have JWT validation
   - Role checks enforced (`@PreAuthorize` or service-layer assertion)
   - Hidden test cases stripped from student responses
   - No sensitive fields in response DTOs

6. **Async work:** if the operation is slow (evaluation, plagiarism, notifications), publish to RabbitMQ — never block the API thread.

7. **Write or update tests:**
   - Unit test for service logic
   - Integration test for controller + DB (Testcontainers if touching the database)

8. **Verify:** `./mvnw test` on affected module. State result.

9. **Follow shutdown protocol.** Two-commit push.

## Pre-done checklist

- [ ] Feature in active-goals scope
- [ ] Correct module — no cross-module contamination
- [ ] Entity + migration written (if schema changed)
- [ ] Request DTO validated, response DTO safe (no entity exposure)
- [ ] Service handles business logic, controller is thin
- [ ] Security: JWT + role check + hidden test case protection
- [ ] Async work via RabbitMQ where applicable
- [ ] Tests written and passing
- [ ] Digest written and pushed
