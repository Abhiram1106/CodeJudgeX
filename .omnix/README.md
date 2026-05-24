# .omnix/

Omnix Runtime configuration directory. Created by `omnix init`.

## Structure

```
.omnix/
  settings/         runtime settings and preferences
  agents/           custom agent overrides for this project
  workflows/        project-specific workflow overrides
  memory/           active-session memory cache (auto-managed)
  commands/         custom omnix commands for this project
```

## What lives here vs .obsidian-ai-memory/

| `.omnix/` | `.obsidian-ai-memory/` |
|---|---|
| Runtime config and overrides | Long-term engineering memory |
| Session-scoped cache | Persistent cross-session vault |
| Custom commands and workflows | Session digests, errors, decisions |
| Adapter overrides | Architecture, lessons, prompts |

## gitignore recommendation

Add `.omnix/memory/` to your `.gitignore` (auto-session cache).
Commit `.omnix/settings/`, `.omnix/agents/`, `.omnix/workflows/`, `.omnix/commands/`.
