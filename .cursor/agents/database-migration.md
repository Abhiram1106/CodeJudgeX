# Agent: Database Migration — CodeJudgeX

## Trigger

Use this agent when creating or modifying a database schema, writing a Flyway migration,
adding indexes, or changing entity relationships.

## Always include at session start

```
@.obsidian-ai-memory/02-PROJECTS/project-context.md
@.obsidian-ai-memory/04-DECISIONS/decisions.md
@.cursor/context/database-context.md
@.cursor/context/backend-context.md
```

## Execution steps

1. **Check decisions.md** for any prior schema decisions that affect this change.

2. **Never modify an existing Flyway migration file.** Always create a new versioned file.
   Naming: `V{N}__{snake_case_description}.sql` where N follows the current highest version.

3. **Before writing the migration, state:**
   - What tables are affected
   - What columns are added/removed/modified
   - Whether any existing data needs to be backfilled
   - Whether any foreign keys or indexes are added/removed
   - Whether the migration is reversible

4. **Migration file must include:**
   - A comment header describing what it does and why
   - `NOT NULL` constraints with defaults for any new non-nullable columns on existing tables
   - Explicit index creation for any foreign key columns and frequently queried fields
   - No data modifications mixed with schema changes (separate migrations)

5. **Update the JPA entity** to match the migration.

6. **Update any affected repositories** (new query methods if needed).

7. **Update MapStruct mappers** if DTO shape changes.

8. **Confirm with user before running against any live data.**

9. **Test:** run `./mvnw test` — Flyway will validate the migration in the test context. State result.

10. **Record the decision** in `.obsidian-ai-memory/04-DECISIONS/decisions.md` if this is a non-trivial schema choice.

11. **Follow shutdown protocol.** Two-commit push.

## Pre-done checklist

- [ ] Existing migration files untouched
- [ ] New migration file correctly versioned
- [ ] Migration is safe for existing data (default values, nullability handled)
- [ ] JPA entity updated
- [ ] Repositories updated
- [ ] Mappers updated
- [ ] Confirmed with user before any live data migration
- [ ] Tests pass
- [ ] Decision recorded if non-trivial
- [ ] Digest written and pushed
