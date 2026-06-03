# Security

## Security Status

SplicePoint is currently a local-development prototype. It accepts user-provided audio files, analyzes them, and returns WAV exports.

Because audio upload and decoding are involved, the project must treat every uploaded file as untrusted input.

## Trust Boundaries

```text
browser file input
→ frontend JavaScript
→ backend multipart upload
→ Java Sound decoder
→ analysis/export services
→ WAV response
```

The highest-risk boundary is the backend decoder. Malformed media files can trigger parser errors, high memory use, or unexpected runtime behavior.

## Current Safety Measures

The backend currently:

- rejects missing or empty file uploads
- validates export start/end values
- limits multipart uploads through Spring configuration
- returns controlled JSON error responses for failed analysis/export requests
- exports WAV data rather than writing user files into the repository

## Current Risks

| Risk | Current Status | Notes |
|---|---|---|
| Untrusted audio input | Present | All uploaded audio must be treated as hostile until decoded safely |
| Large file memory use | Present | Current decode path reads uploaded files into memory |
| Permissive CORS | Present | `@CrossOrigin(origins = "*")` is acceptable only for local prototype work |
| Raw audio persistence | Not currently intended | Do not add raw-audio logging without explicit user consent |
| Feedback logging privacy | Planned | Must store features/outcomes by default, not raw audio |
| Dependency supply chain | Present | Maven dependencies should be reviewed before production deployment |

## Rules for Future Development

- Do not store uploaded raw audio by default.
- Do not log full file contents or private paths.
- Do not trust MIME type alone.
- Do not assume Java Sound can safely decode every user file.
- Do not expose permissive CORS in production.
- Do not add feedback logging that stores raw audio without opt-in.
- Do not add cloud upload/storage without documenting retention and deletion behavior.
- Keep export paths controlled by the application, not user-provided filenames.

## File Handling Guidance

For Phase 1 and Phase 2:

- Prefer short WAV/AIFF files for local testing.
- Keep upload limits conservative.
- Validate start/end export ranges.
- Return clear errors for unsupported formats.
- Avoid temporary file writes unless needed.
- If temporary files are introduced, delete them in `finally` blocks.

## Feedback and Learning Safety

Future learning systems should log:

- candidate IDs
- feature scores
- user actions
- accepted/rejected state
- edit distances
- engine/model versions

They should not log by default:

- raw audio
- full local file paths
- personally identifying metadata
- private project names embedded in filenames, unless sanitized

## Production TODOs

Before any hosted deployment:

- restrict CORS to approved origins
- add request size/rate limits
- add structured error logging without sensitive data
- document retention policy for feedback logs
- decide whether uploads are ever persisted
- add dependency vulnerability scanning
- add CI testing for malformed or unsupported inputs

## Security Principle

Every uploaded file is untrusted until the app proves it can fail safely.
