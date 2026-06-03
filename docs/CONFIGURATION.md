# Configuration

## Current Configuration Surface

SplicePoint currently has a small configuration surface.

Most values are hardcoded for Phase 1 local development and should be made more explicit before hosted deployment.

## Backend Configuration

Backend configuration lives in:

```text
backend/src/main/resources/application.properties
```

Current values:

```properties
spring.application.name=splicepoint-backend
server.port=8080
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

## Backend Defaults

| Setting | Default | Purpose |
|---|---:|---|
| `server.port` | `8080` | Local backend API port |
| `spring.servlet.multipart.max-file-size` | `100MB` | Maximum uploaded file size |
| `spring.servlet.multipart.max-request-size` | `100MB` | Maximum multipart request size |
| export fade | `5 ms` | Small edge fade for WAV export |

## Frontend Configuration

The frontend currently decides the API base in `frontend/app.js`:

```js
const API_BASE = window.location.protocol === 'file:' ? 'http://localhost:8080' : '';
```

This means:

- when opened from disk, API calls go to `http://localhost:8080`
- when served from a web server, API calls use the same origin

## Safe Local Values

For local development:

```text
backend port: 8080
upload limit: 100MB or lower
allowed formats: WAV / AIFF preferred
export fade: 5 ms
```

## Deployment TODOs

Before hosting SplicePoint publicly, decide and document:

- allowed origins for CORS
- maximum upload size
- supported audio formats
- whether files are processed in memory or temporary storage
- whether user feedback logs are stored
- whether raw audio is ever persisted
- production logging policy
- rate limiting or request throttling

## Configuration Rule

Any option that changes file handling, export behavior, model behavior, or feedback logging should be documented here before it is exposed to users.
