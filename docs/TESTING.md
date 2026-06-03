# Testing

## Current Test Coverage

SplicePoint currently has backend tests under:

```text
backend/src/test/java/com/example/backend/BackendApplicationTests.java
```

The tests currently verify:

- Spring Boot application context loads
- a synthetic WAV file can be analyzed
- selected audio can be exported as a WAV response

## Run Tests

From the repository root:

```bash
./backend/mvnw test
```

On Windows PowerShell:

```powershell
.\backend\mvnw.cmd test
```

The first run requires network access so Maven can download dependencies.

## Expected Result

A passing test run should complete without failures and confirm that backend analysis/export still work for a generated WAV fixture.

## Manual QA Checklist

Use this checklist after changing frontend, backend API, audio analysis, or export behavior.

### Backend

- `GET /api/health` returns `status: ok`
- backend starts on port `8080`
- backend rejects empty file uploads clearly
- backend rejects invalid export start/end values clearly
- backend returns WAV data from `/api/export`
- backend does not write uploaded audio into the repository

### Frontend

- `frontend/index.html` opens in a browser
- upload panel accepts an audio file
- waveform renders
- candidate regions appear
- selecting a candidate updates the selection readout
- region drag/resize works
- export button downloads a WAV file
- failed analysis shows a visible error instead of crashing

### Audio Fixtures

Start with small files:

- 1 second synthetic WAV
- short drum loop WAV
- short melodic phrase WAV
- unsupported or malformed file for error handling

Do not use large full-length tracks as the first regression test.

## Regression Areas

When changing analysis code, check:

- loop candidates are returned
- candidates have start/end/duration/confidence/label/reasons
- confidence values stay between `0` and `1`
- warnings remain useful and honest
- export still works after analysis changes

When changing frontend code, check:

- API base still points to `localhost:8080` when opened from disk
- selected region is preserved before export
- errors are shown in the status area
- candidate cards and waveform regions stay aligned

## Missing Tests

The repo should eventually add:

- controller-level API tests
- invalid upload tests
- invalid export range tests
- unsupported audio format tests
- longer synthetic fixture tests
- known transient/onset tests
- future BPM/beat-grid tests
- frontend syntax/lint checks
- CI workflow for backend tests

## Testing Principle

Every analysis upgrade must prove it still exports a valid loop.
