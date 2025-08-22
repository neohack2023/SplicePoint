package com.example.backend;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AudioController {

    @PostMapping("/extract")
    public Map<String, Object> extract(@RequestParam("file") MultipartFile file) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file.getInputStream())) {
            AudioFormat format = ais.getFormat();
            long frames = ais.getFrameLength();
            double duration = frames / format.getFrameRate();
            result.put("bpm", 120); // placeholder until analysis added
            result.put("duration", duration);
            double loopLen = Math.min(2.0, duration);
            List<Map<String, Double>> loops = new ArrayList<>();
            loops.add(Map.of("start", 0.0, "end", loopLen));
            double start2 = Math.max(0.0, duration / 2 - loopLen / 2);
            loops.add(Map.of("start", start2, "end", Math.min(duration, start2 + loopLen)));
            result.put("loops", loops);
        }
        return result;
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam("file") MultipartFile file,
                                         @RequestParam("start") double start,
                                         @RequestParam("end") double end) throws Exception {
        byte[] data = trimWav(file, start, end);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.wav\"");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private byte[] trimWav(MultipartFile file, double start, double end) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file.getInputStream())) {
            AudioFormat format = ais.getFormat();
            long startFrame = (long) (start * format.getFrameRate());
            long endFrame = (long) (end * format.getFrameRate());
            long framesToCopy = endFrame - startFrame;
            ais.skip(startFrame * format.getFrameSize());
            AudioInputStream clipped = new AudioInputStream(ais, format, framesToCopy);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AudioSystem.write(clipped, AudioFileFormat.Type.WAVE, baos);
            return baos.toByteArray();
        }
    }
}
