# Claude Code Subagents — CodeJudgeX

> Subagents run as isolated Claude instances via the `Agent` tool.
> Use them for tasks that are large, independent, or would pollute the main context window.

---

## When to spawn a subagent

Spawn when the task is:
- **Independent** — does not need the current conversation's state
- **Broad** — requires searching multiple directories or files
- **Risky for context** — reading hundreds of lines would crowd out working memory

Do NOT spawn for:
- A single file read or edit — use `Read`/`Edit` directly
- A quick grep — use `Grep` directly
- Anything that depends on results already in this conversation

---

## Available subagent types

| Type | When to use |
|---|---|
| `Explore` | Search the codebase for unknown files, symbols, or patterns |
| `Plan` | Design an implementation strategy before writing code |
| `feature-dev:code-architect` | Architect a new feature: which files to create/modify, data flow, build sequence |
| `feature-dev:code-explorer` | Deep-trace an existing feature: execution path, dependencies, abstractions |
| `feature-dev:code-reviewer` | Review code for bugs, logic errors, security issues, convention violations |
| `coderabbit:code-reviewer` | Thorough code review with CodeRabbit analysis |
| `code-simplifier:code-simplifier` | Simplify and refactor code after a feature is complete |
| `general-purpose` | Multi-step research or investigation that doesn't fit a specialized type |

---

## CodeJudgeX-specific subagent patterns

### Backend feature research
```
Agent({
  subagent_type: "feature-dev:code-explorer",
  description: "Trace submission evaluation pipeline",
  prompt: "Trace the full execution path for code evaluation in CodeJudgeX backend.
           Start at SubmissionController, follow through SubmissionService,
           RabbitMQ publish, EvaluationWorker, Judge0Client, and result persistence.
           Map each class, method, and queue constant involved.
           Working directory: c:\\Users\\ADMIN\\Desktop\\CodeJudgeX\\backend"
})
```

### Security audit
```
Agent({
  subagent_type: "feature-dev:code-reviewer",
  description: "Audit auth and hidden test case protection",
  prompt: "Review CodeJudgeX backend for:
           1. JWT token validation — is it applied to all protected endpoints?
           2. Hidden test case protection — do any student-facing endpoints expose is_sample=false test cases?
           3. Role hierarchy enforcement — STUDENT < FACULTY < ADMIN < SUPER_ADMIN
           4. No Runtime.exec() or ProcessBuilder on user input
           Report only confirmed issues with file path and line number."
})
```

### Frontend type audit
```
Agent({
  subagent_type: "Explore",
  description: "Find all any usages in frontend",
  prompt: "Search the CodeJudgeX frontend for TypeScript 'any' usages.
           Directory: c:\\Users\\ADMIN\\Desktop\\CodeJudgeX\\frontend\\src
           Pattern: ': any' and 'as any'
           Report each file path and line. Ignore node_modules and dist."
})
```

### Database schema review
```
Agent({
  subagent_type: "feature-dev:code-reviewer",
  description: "Review Flyway migrations",
  prompt: "Review all Flyway migration files in
           c:\\Users\\ADMIN\\Desktop\\CodeJudgeX\\backend\\src\\main\\resources\\db\\migration.
           Check: correct V{n}__ naming, no DML mixed with DDL, explicit FK constraint names,
           idx_ index naming, UUID PKs, TIMESTAMP WITH TIME ZONE timestamps.
           Report violations with file name and line."
})
```

---

## Rules for subagent prompts

- State the working directory explicitly — subagents don't inherit cwd context.
- State what NOT to report (reduces noise).
- Ask for file path + line number in findings.
- If the agent needs to write code, say so explicitly. Otherwise it may just research.
- Foreground (default) when you need the result before proceeding.
- Background (`run_in_background: true`) only for tasks genuinely independent of your next step.
