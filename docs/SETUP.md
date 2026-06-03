# Setup

## Requirements

SplicePoint currently requires:

- Java 21
- A modern browser
- Network access for the first Maven dependency download
- Git, if cloning locally

The backend uses the Maven Wrapper, so a separate Maven installation is usually not required.

## Clone the Repository

```bash
git clone https://github.com/neohack2023/SplicePoint.git
cd SplicePoint
```

## Run the Backend

From the repository root:

```bash
./backend/mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\backend\mvnw.cmd spring-boot:run
```

The backend defaults to:

```text
http://localhost:8080
```

## Verify the Backend

Open this URL in a browser or use curl:

```text
http://localhost:8080/api/health
```

Expected result is a small JSON response with:

```json
{
  "status": "ok",
  "service": "splicepoint-backend",
  "version": "0.1.0",
  "engine": "java-sound"
}
```

## Open the Frontend

Open this file directly in a browser:

```text
frontend/index.html
```

When opened from disk, the frontend sends API requests to:

```text
http://localhost:8080
```

## Run Tests

```bash
./backend/mvnw test
```

The current automated tests generate a synthetic WAV file and verify that the backend can analyze and export a selected WAV loop.

## First-Run Notes

The first backend run may take longer because Maven downloads Spring Boot dependencies.

If dependency resolution fails, confirm:

- the machine has network access
- firewall or antivirus tools are not blocking Maven
- Java 21 is installed and available on the path

## Format Notes

The Phase 1 backend uses Java Sound for decoding. WAV and AIFF are the safest test formats.

MP3 and other codec-heavy formats may fail depending on the local Java runtime. Broader format support should be treated as a future backend-engine upgrade, likely with FFmpeg or another dedicated audio stack.
