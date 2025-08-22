# SplicePoint
This app is a browser-based sample extractor built for producers, DJs, and audio tinkerers who want fast, reliable loops without digging through waveforms manually.
        •       Upload any track – MP3, WAV, AIFF, you name it.
        •       Automatic analysis – Detects BPM, beat grid, and suggests the cleanest loop points with confidence scores.
        •       Interactive waveform preview – Drag start/end markers or snap to the beat grid for bar-perfect precision.
        •       Loop auditioning – Instantly play and loop your selection right in the browser.
        •       One-click export – Download your loop as a trimmed WAV file, ready to drop into your DAW, sampler, or set.

Whether you’re chopping samples for hip-hop, prepping loops for a live set, or slicing stems for remixing, this tool makes the process quick, visual, and musical.

## Frontend

A static frontend lives in [`frontend/`](frontend/) using [WaveSurfer.js](https://wavesurfer-js.org/) with Regions and Timeline plugins. It supports drag-and-drop audio upload, loop suggestions, region auditioning, and exporting loops by posting to `/api/export`.

Open `frontend/index.html` in a browser to try it out.

## Backend

The backend lives in [`backend/`](backend/). It's a Spring Boot application exposing two endpoints:

- `POST /api/extract` – returns placeholder BPM, duration, and loop suggestions for an uploaded file.
- `POST /api/export` – trims the uploaded audio between `start` and `end` seconds and returns a WAV file.

Run it with:

```
./backend/mvnw spring-boot:run
```
