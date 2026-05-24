# Memory Sanitization Checklist

> Run before committing the vault or sharing it externally.
> Or run `omnix memory curate` for automated check.

## Auto-detected patterns to redact

- [ ] API keys: `sk-...`, `pk_...`, `ghp_...`, `AKIA...`, `xoxb-...`
- [ ] JWTs: tokens starting with `ey` and 3 base64 segments
- [ ] Private keys: `-----BEGIN .* PRIVATE KEY-----`
- [ ] Database connection strings with passwords: `postgres://user:pass@`
- [ ] `.env`-pattern secrets: lines like `API_KEY=...`
- [ ] AWS/GCP/Azure resource ARNs/URIs containing account IDs

## Manual review patterns

- [ ] Internal hostnames: `*.prod.internal`, IPs starting with private ranges
- [ ] Customer names / emails (unless explicitly allowed for project)
- [ ] Pasted terminal output > 100 lines (likely contains accidentals)
- [ ] Pasted log lines containing user data
- [ ] References to non-public roadmap items

## Replacement formats

```
sk-abc123...            → [REDACTED:openai-key]
ghp_xyz...              → [REDACTED:github-token]
postgres://u:p@host/db  → postgres://u:[REDACTED]@host/db
api.prod.internal       → <prod-host>
user@company.com        → <user-email>
```

## Per-folder review

- `00-INBOX/` — quick captures, highest risk
- `01-SESSIONS/` — terminal pastes risk
- `02-PROJECTS/` — should be safe, low risk
- `03-ERRORS/` — stack traces may include paths to secret files
- `04-DECISIONS/` — should be safe
- `05-ARCHITECTURE/` — production hostnames may appear
- `07-LESSONS/` — should be safe
- `08-PROMPTS/` — review for leaked system prompts

## Audit commands (FUTURE)

```bash
omnix memory audit                  # report only
omnix memory curate                 # report + auto-fix safe patterns
omnix memory curate --interactive   # ask before each replacement
```
