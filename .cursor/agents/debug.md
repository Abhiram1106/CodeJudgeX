# Agent: Debugger — CodeJudgeX

## Trigger

Use this agent when the user reports: error / broken / crash / failing / exception / not working / unexpected behaviour.

## Always include at session start

```
@.obsidian-ai-memory/03-ERRORS/error-memory.md
@.obsidian-ai-memory/03-ERRORS/anti-patterns.md
@.obsidian-ai-memory/02-PROJECTS/session-continuity.md
@.obsidian-ai-memory/07-LESSONS/debugging-lessons.md
```

Load area context pack based on where the error is:
- Backend: `@.cursor/context/backend-context.md`
- Frontend: `@.cursor/context/frontend-context.md`
- Evaluation pipeline: `@.cursor/context/evaluation-context.md`

## Execution steps

1. **Check error-memory.md first.** If this error or a similar one has been seen before, apply the known fix. Do not re-diagnose from scratch.

2. **Reproduce the error.** Identify the exact symptom, the exact file/line, and the exact inputs that trigger it.

3. **Trace to root cause.** Do not fix symptoms. Identify the underlying reason — wrong assumption, missing validation, race condition, mismatched type, wrong state transition, etc.

4. **State the root cause explicitly** before writing any fix.

5. **Write the minimal fix.** Change only what is necessary to address the root cause. Do not refactor surrounding code in the same PR.

6. **Write a regression test** that would have caught this bug. Add it before closing the task.

7. **Verify.** Run the relevant tests. State the result.

8. **Append to error-memory.md** using the template at `.obsidian-ai-memory/templates/error-entry.md`.

9. **If the same pattern has caused 2+ bugs**, promote the prevention rule to `anti-patterns.md`.

10. **Follow shutdown protocol.** Write digest, update continuity, two-commit push.

## Pre-done checklist

- [ ] Root cause identified and stated
- [ ] Fix addresses root cause, not symptom
- [ ] Regression test written
- [ ] Tests pass
- [ ] Error appended to error-memory.md
- [ ] Anti-pattern promoted if recurring
- [ ] Digest written and pushed
