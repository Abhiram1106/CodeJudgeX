# Claude Code Skills — CodeJudgeX

> Skills are slash commands that load specialized behavior.
> Invoke with the `Skill` tool before responding to any task that matches.

---

## When to invoke a skill

Check for skills before ANY response — even clarifying questions.
If there is a 1% chance a skill applies, invoke it.

---

## Available skills (CodeJudgeX context)

### Design and planning

| Skill | Invoke when |
|---|---|
| `brainstorming` | Designing a new feature, API, or architecture before implementation |
| `superpowers:writing-skills` | Creating or updating documentation, runbooks, or process guides |

### Workflow

| Skill | Invoke when |
|---|---|
| `superpowers:test-driven-development` | Writing tests or implementing a feature that needs test coverage |

---

## CodeJudgeX-specific invocation guide

### Before implementing a new backend module
```
Skill({ skill: "brainstorming" })
```
Use to validate: module boundaries, DTO shapes, async vs sync, which layer owns the logic.

### Before writing Flyway migrations
```
Skill({ skill: "brainstorming" })
```
Use to confirm: table name, column types, FK constraints, index plan, rollback strategy.

### Before adding a new frontend feature
```
Skill({ skill: "brainstorming" })
```
Use to confirm: which feature folder, TanStack Query key shape, Zustand vs local state, role guard needed.

### When writing a runbook, agent recipe, or session digest
```
Skill({ skill: "superpowers:writing-skills" })
```

---

## Installing new skills

User-level skills live in `C:\Users\ADMIN\.claude\skills\`.
Project-level skills (if added) go in `.claude/skills/` in this repo.

Skills are discovered automatically by Claude Code on session start.
