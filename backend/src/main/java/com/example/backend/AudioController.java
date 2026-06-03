package com.example.backend;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AudioController {

    private final AudioAnalysisService analysisService;
    private final AudioExportService exportService;

    public AudioController(AudioAnalysisService analysisService, AudioExportService exportService) {
        this.analysisService = analysisService;
        this.exportService = exportService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "splicepoint-backend",
                "version", "0.1.0",
                "engine", "java-sound"
        );
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            return ResponseEntity.ok(analysisService.analyze(file));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(error("extract_failed", ex));
        }
    }

    @PostMapping(value = "/export", produces = "audio/wav")
    public ResponseEntity<?> export(@RequestParam("file") MultipartFile file,
                                    @RequestParam("start") double start,
                                    @RequestParam("end") double end,
                                    @RequestParam(value = "fadeMs", defaultValue = "5") int fadeMs) {
        try {
            validateFile(file);
            byte[] data = exportService.exportWav(file, start, end, fadeMs);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(data.length);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("splicepoint-loop.wav")
                    .build());

            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(error("export_failed", ex));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload an audio file first.");
        }
    }

    private Map<String, Object> error(String code, Exception ex) {
        return Map.of(
                "code", code,
                "message", safeMessage(ex)
        );
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "SplicePoint could not process this audio file.";
        }
        return message;
    }
}
