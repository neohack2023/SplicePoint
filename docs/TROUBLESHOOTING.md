# Troubleshooting

## Backend Will Not Start

| Symptom | Likely Cause | Fix |
|---|---|---|
| `java` not found | Java is not installed or not on PATH | Install Java 21 and reopen the terminal |
| Maven dependency errors | First run cannot reach Maven repositories | Check network, firewall, or proxy settings |
| Port already in use | Another service is using `8080` | Stop the other service or change `server.port` |
| `JAVA_HOME` error | Java path is misconfigured | Set `JAVA_HOME` to a valid JDK 21 install |

## Frontend Opens but Analysis Fails

| Symptom | Likely Cause | Fix |
|---|---|---|
| Upload works but status shows analysis error | Backend is not running | Start backend with `./backend/mvnw spring-boot:run` |
| Browser console shows failed request | Wrong API base or backend unavailable | Confirm `http://localhost:8080/api/health` works |
| File fails to decode | Unsupported audio format | Try WAV or AIFF first |
| Large file hangs or fails | File too large for current in-memory decode path | Try a shorter test file |

## Export Fails

| Symptom | Likely Cause | Fix |
|---|---|---|
| Export button does nothing | No region selected | Click a candidate or waveform region first |
| Export returns error | Invalid start/end values | Resize the region so end is after start |
| Downloaded file missing | Browser blocked download | Check browser download permissions |
| Exported loop clicks | Boundary is not clean enough | Adjust region start/end or increase future fade support |

## No Loop Candidates Appear

Possible causes:

- backend returned an error
- audio file decoded but produced weak analysis features
- file duration is too short
- unsupported format or malformed audio

Current fallback behavior should still create a simple editable region when analysis fails after waveform load.

## Test Failures

If tests fail on first run:

1. Confirm network access for dependency resolution.
2. Confirm Java 21 is active.
3. Run from repository root.
4. Try cleaning backend build output:

```bash
./backend/mvnw clean test
```

## Format Issues

The Phase 1 backend depends on Java Sound support.

Safest test formats:

- WAV
- AIFF

Potentially unreliable until future codec support:

- MP3
- AAC
- FLAC
- unusual compressed containers

## Debugging Checklist

Before changing code, capture:

- browser console error
- backend terminal error
- file type and duration
- whether `/api/health` works
- whether tests pass with synthetic WAV

## Troubleshooting Principle

Fix the smallest broken link in the upload → analyze → select → export chain first.
