# Contributing to SplicePoint

SplicePoint is an early-stage prototype. Contributions should keep the upload → analyze → select → export workflow stable.

## Development Philosophy

- Keep `main` as the primary working branch unless a risky change needs isolation.
- Prefer small, focused commits.
- Do not add branches, dependencies, or architectural layers without a clear reason.
- Preserve the frontend/backend contract unless the task is specifically to change it.
- Update documentation with behavior changes.

## Branch Style

Preferred simple workflow:

```text
main
stable or rollback branch only when needed
```

Temporary feature branches are acceptable for risky work, but they should be merged or closed quickly.

## Commit Style

Use short, descriptive commit messages:

```text
Add backend feedback schema
Fix selected region export
Document setup troubleshooting
Refactor audio scoring components
```

Avoid vague commits such as:

```text
Update stuff
Fix things
More changes
```

## Pull Request Expectations

A useful pull request should include:

- what changed
- why it changed
- how it was tested
- any known limitations
- documentation updates when behavior changes

## Testing Expectations

Before proposing backend changes, run:

```bash
./backend/mvnw test
```

For frontend changes, manually verify:

- upload still works
- waveform still renders
- candidate regions still appear
- selected-region export still downloads a WAV
- errors appear visibly instead of failing silently

## Documentation Expectations

Update the relevant doc when changing:

| Change Type | Doc |
|---|---|
| setup/run steps | `docs/SETUP.md` |
| user workflow | `docs/USAGE.md` |
| API/component behavior | `docs/ARCHITECTURE.md` |
| runtime options | `docs/CONFIGURATION.md` |
| security boundary | `docs/SECURITY.md` |
| tests or QA | `docs/TESTING.md` |
| future work | `docs/ROADMAP.md` |
| major design decision | `docs/DECISIONS/` |

## LLM-Assisted Contributions

LLM-generated changes are welcome when they are reviewed like any other code.

Rules for LLM-assisted work:

- inspect existing files before editing
- use current file SHAs when updating through GitHub tools
- do not claim tests passed unless they actually ran
- do not invent setup steps
- do not add raw-audio logging by default
- do not replace small modules with giant rewrites
- mark uncertain claims as TODO or unknown

## Security Rules

- Treat uploaded files as untrusted.
- Do not log raw audio by default.
- Do not store local file paths in feedback logs.
- Do not expose permissive CORS in production.
- Document any new import/export or storage behavior.

## Contribution Principle

Improve the loop pipeline without making the failure modes harder to understand.
