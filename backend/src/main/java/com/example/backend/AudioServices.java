package com.example.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
class AudioAnalysisService {

    public AnalysisResponse analyze(MultipartFile file) throws IOException, UnsupportedAudioFileException {
        DecodedAudio audio = AudioDecoder.decode(file);
        double[] mono = audio.toMono();
        List<EnergyWindow> windows = AudioMath.energyWindows(mono, audio.sampleRate());
        List<LoopCandidate> candidates = LoopFinder.findCandidates(mono, audio, windows);

        List<String> warnings = new ArrayList<>();
        warnings.add("BPM and beat-grid detection are not fully implemented yet; candidates use transient, energy, and boundary heuristics.");
        warnings.add("Java Sound format support depends on the local runtime; WAV and AIFF are the safest Phase 1 formats.");

        return new AnalysisResponse(
                safeName(file),
                file.getContentType(),
                file.getSize(),
                AudioMath.round(audio.durationSeconds(), 3),
                Math.round(audio.sampleRate()),
                audio.channels(),
                null,
                0.0,
                "phase-1-heuristic",
                candidates,
                warnings
        );
    }

    private String safeName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "uploaded-audio";
        }
        return name;
    }
}

@Service
class AudioExportService {

    public byte[] exportWav(MultipartFile file, double start, double end, int fadeMs)
            throws IOException, UnsupportedAudioFileException {
        DecodedAudio audio = AudioDecoder.decode(file);
        validateSelection(start, end, audio.durationSeconds());

        long startFrame = Math.max(0, Math.round(start * audio.sampleRate()));
        long endFrame = Math.min(audio.frames(), Math.round(end * audio.sampleRate()));
        long selectedFrames = endFrame - startFrame;

        if (selectedFrames <= 0) {
            throw new IllegalArgumentException("Selection is empty. Choose an end time after the start time.");
        }

        int fadeFrames = (int) Math.min(selectedFrames / 2, Math.max(0, Math.round((fadeMs / 1000.0) * audio.sampleRate())));
        return WavEncoder.encodePcm16(audio, startFrame, selectedFrames, fadeFrames);
    }

    private void validateSelection(double start, double end, double duration) {
        if (!Double.isFinite(start) || !Double.isFinite(end)) {
            throw new IllegalArgumentException("Start and end must be valid numbers.");
        }
        if (start < 0) {
            throw new IllegalArgumentException("Start time cannot be negative.");
        }
        if (end <= start) {
            throw new IllegalArgumentException("End time must be greater than start time.");
        }
        if (start >= duration) {
            throw new IllegalArgumentException("Start time is outside the uploaded audio duration.");
        }
    }
}

final class AudioDecoder {

    private AudioDecoder() {
    }

    static DecodedAudio decode(MultipartFile file) throws IOException, UnsupportedAudioFileException {
        byte[] bytes = file.getBytes();
        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(new ByteArrayInputStream(bytes)))) {

            AudioFormat sourceFormat = sourceStream.getFormat();
            if (sourceFormat.getChannels() <= 0 || sourceFormat.getSampleRate() <= 0) {
                throw new UnsupportedAudioFileException("Audio format is missing channel or sample-rate information.");
            }

            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, sourceStream)) {
                byte[] pcmBytes = pcmStream.readAllBytes();
                int channels = pcmFormat.getChannels();
                int frameSize = pcmFormat.getFrameSize();
                long frames = pcmBytes.length / frameSize;
                double[][] samples = new double[channels][Math.toIntExact(frames)];

                for (int frame = 0; frame < frames; frame++) {
                    int frameOffset = frame * frameSize;
                    for (int channel = 0; channel < channels; channel++) {
                        int sampleOffset = frameOffset + channel * 2;
                        int low = pcmBytes[sampleOffset] & 0xff;
                        int high = pcmBytes[sampleOffset + 1];
                        short value = (short) ((high << 8) | low);
                        samples[channel][frame] = value / 32768.0;
                    }
                }

                return new DecodedAudio(pcmFormat.getSampleRate(), channels, frames, samples);
            }
        } catch (IllegalArgumentException ex) {
            UnsupportedAudioFileException wrapped = new UnsupportedAudioFileException(
                    "Unsupported audio encoding. Try WAV or AIFF for the Phase 1 backend."
            );
            wrapped.initCause(ex);
            throw wrapped;
        }
    }
}

record DecodedAudio(float sampleRate, int channels, long frames, double[][] samples) {
    double durationSeconds() {
        return frames / sampleRate;
    }

    double[] toMono() {
        int length = Math.toIntExact(frames);
        double[] mono = new double[length];
        for (int frame = 0; frame < length; frame++) {
            double sum = 0.0;
            for (int channel = 0; channel < channels; channel++) {
                sum += samples[channel][frame];
            }
            mono[frame] = sum / channels;
        }
        return mono;
    }
}

record AnalysisResponse(
        String fileName,
        String contentType,
        long byteSize,
        double durationSeconds,
        long sampleRate,
        int channels,
        Double bpmEstimate,
        double bpmConfidence,
        String engineStatus,
        List<LoopCandidate> loops,
        List<String> warnings
) {
}

record LoopCandidate(
        double start,
        double end,
        double duration,
        double confidence,
        String label,
        List<String> reasons
) {
}

record EnergyWindow(int index, int startFrame, int endFrame, double rms, double transientScore) {
}

final class LoopFinder {

    private static final int MAX_CANDIDATES = 8;

    private LoopFinder() {
    }

    static List<LoopCandidate> findCandidates(double[] mono, DecodedAudio audio, List<EnergyWindow> windows) {
        double duration = audio.durationSeconds();
        if (duration <= 0.05 || mono.length < 2) {
            return List.of();
        }

        List<EnergyWindow> rankedStarts = new ArrayList<>(windows);
        rankedStarts.sort(Comparator.comparingDouble(EnergyWindow::transientScore).reversed());

        List<Double> loopDurations = loopDurations(duration);
        List<LoopCandidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        addCandidate(candidates, seen, mono, audio, 0.0, Math.min(duration, loopDurations.getFirst()), "opening slice", 0.18);

        int startLimit = Math.min(24, rankedStarts.size());
        for (int i = 0; i < startLimit; i++) {
            double startSeconds = rankedStarts.get(i).startFrame() / audio.sampleRate();
            for (double loopDuration : loopDurations) {
                double endSeconds = Math.min(duration, startSeconds + loopDuration);
                if (endSeconds - startSeconds >= 0.25) {
                    addCandidate(candidates, seen, mono, audio, startSeconds, endSeconds, "transient candidate", rankedStarts.get(i).transientScore());
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(LoopCandidate::confidence).reversed());
        if (candidates.size() > MAX_CANDIDATES) {
            return new ArrayList<>(candidates.subList(0, MAX_CANDIDATES));
        }
        return candidates;
    }

    private static List<Double> loopDurations(double duration) {
        List<Double> durations = new ArrayList<>();
        for (double candidate : List.of(1.0, 2.0, 4.0, 8.0)) {
            if (candidate <= duration) {
                durations.add(candidate);
            }
        }
        if (durations.isEmpty()) {
            durations.add(Math.max(0.25, duration));
        }
        return durations;
    }

    private static void addCandidate(List<LoopCandidate> candidates,
                                     Set<String> seen,
                                     double[] mono,
                                     DecodedAudio audio,
                                     double start,
                                     double end,
                                     String label,
                                     double transientScore) {
        double roundedStart = AudioMath.round(start, 3);
        double roundedEnd = AudioMath.round(end, 3);
        String key = String.format(Locale.ROOT, "%.2f:%.2f", roundedStart, roundedEnd);
        if (!seen.add(key)) {
            return;
        }

        int startFrame = AudioMath.secondsToFrame(start, audio.sampleRate(), mono.length);
        int endFrame = AudioMath.secondsToFrame(end, audio.sampleRate(), mono.length);
        if (endFrame <= startFrame) {
            return;
        }

        double boundaryCleanliness = AudioMath.boundaryCleanliness(mono, startFrame, endFrame);
        double energyStability = AudioMath.energyStability(mono, startFrame, endFrame, audio.sampleRate());
        double durationScore = AudioMath.durationScore(end - start);
        double confidence = AudioMath.clamp(
                0.18 + transientScore * 0.32 + boundaryCleanliness * 0.24 + energyStability * 0.18 + durationScore * 0.08,
                0.05,
                0.98
        );

        List<String> reasons = new ArrayList<>();
        if (transientScore > 0.25) {
            reasons.add("strong transient near start");
        }
        if (boundaryCleanliness > 0.65) {
            reasons.add("clean boundary estimate");
        }
        if (energyStability > 0.55) {
            reasons.add("stable energy window");
        }
        if (durationScore > 0.5) {
            reasons.add("phase-1 friendly loop length");
        }
        if (reasons.isEmpty()) {
            reasons.add("fallback region candidate");
        }

        candidates.add(new LoopCandidate(
                roundedStart,
                roundedEnd,
                AudioMath.round(end - start, 3),
                AudioMath.round(confidence, 3),
                label,
                reasons
        ));
    }
}

final class AudioMath {

    private AudioMath() {
    }

    static List<EnergyWindow> energyWindows(double[] mono, float sampleRate) {
        int windowSize = Math.max(512, Math.round(sampleRate / 20));
        int hop = Math.max(256, windowSize / 2);
        List<EnergyWindow> windows = new ArrayList<>();
        double previousRms = 0.0;
        double maxTransient = 0.000001;

        for (int start = 0, index = 0; start < mono.length; start += hop, index++) {
            int end = Math.min(mono.length, start + windowSize);
            double rms = rms(mono, start, end);
            double transientScore = Math.max(0.0, rms - previousRms);
            maxTransient = Math.max(maxTransient, transientScore);
            windows.add(new EnergyWindow(index, start, end, rms, transientScore));
            previousRms = rms * 0.75 + previousRms * 0.25;
        }

        List<EnergyWindow> normalized = new ArrayList<>();
        for (EnergyWindow window : windows) {
            normalized.add(new EnergyWindow(
                    window.index(),
                    window.startFrame(),
                    window.endFrame(),
                    window.rms(),
                    clamp(window.transientScore() / maxTransient, 0.0, 1.0)
            ));
        }
        return normalized;
    }

    static double boundaryCleanliness(double[] mono, int startFrame, int endFrame) {
        double startLevel = localAbsMean(mono, startFrame, 96);
        double endLevel = localAbsMean(mono, Math.max(startFrame, endFrame - 1), 96);
        double risk = Math.min(1.0, (startLevel + endLevel) * 8.0);
        return clamp(1.0 - risk, 0.0, 1.0);
    }

    static double energyStability(double[] mono, int startFrame, int endFrame, float sampleRate) {
        int length = endFrame - startFrame;
        if (length <= 0) {
            return 0.0;
        }
        int segment = Math.max(256, Math.round(sampleRate / 10));
        List<Double> values = new ArrayList<>();
        for (int start = startFrame; start < endFrame; start += segment) {
            values.add(rms(mono, start, Math.min(endFrame, start + segment)));
        }
        if (values.size() < 2) {
            return 0.5;
        }
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean <= 0.000001) {
            return 0.0;
        }
        double variance = 0.0;
        for (double value : values) {
            double delta = value - mean;
            variance += delta * delta;
        }
        variance /= values.size();
        double coefficient = Math.sqrt(variance) / mean;
        return clamp(1.0 - coefficient, 0.0, 1.0);
    }

    static double durationScore(double duration) {
        double best = 0.0;
        for (double target : List.of(1.0, 2.0, 4.0, 8.0)) {
            double distance = Math.abs(duration - target);
            best = Math.max(best, 1.0 - Math.min(1.0, distance / target));
        }
        return best;
    }

    static int secondsToFrame(double seconds, float sampleRate, int maxFrames) {
        return (int) clamp(Math.round(seconds * sampleRate), 0, Math.max(0, maxFrames - 1));
    }

    static double rms(double[] samples, int start, int end) {
        if (end <= start) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = start; i < end; i++) {
            sum += samples[i] * samples[i];
        }
        return Math.sqrt(sum / (end - start));
    }

    private static double localAbsMean(double[] samples, int center, int radius) {
        int start = Math.max(0, center - radius);
        int end = Math.min(samples.length, center + radius + 1);
        if (end <= start) {
            return 1.0;
        }
        double sum = 0.0;
        for (int i = start; i < end; i++) {
            sum += Math.abs(samples[i]);
        }
        return sum / (end - start);
    }

    static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

final class WavEncoder {

    private WavEncoder() {
    }

    static byte[] encodePcm16(DecodedAudio audio, long startFrame, long selectedFrames, int fadeFrames) throws IOException {
        int channels = audio.channels();
        long dataSize = selectedFrames * channels * 2;
        if (dataSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Selected loop is too large to export in this Phase 1 backend.");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(44 + (int) dataSize);
        writeAscii(output, "RIFF");
        writeLittleEndianInt(output, 36 + (int) dataSize);
        writeAscii(output, "WAVE");
        writeAscii(output, "fmt ");
        writeLittleEndianInt(output, 16);
        writeLittleEndianShort(output, 1);
        writeLittleEndianShort(output, channels);
        writeLittleEndianInt(output, Math.round(audio.sampleRate()));
        writeLittleEndianInt(output, Math.round(audio.sampleRate()) * channels * 2);
        writeLittleEndianShort(output, channels * 2);
        writeLittleEndianShort(output, 16);
        writeAscii(output, "data");
        writeLittleEndianInt(output, (int) dataSize);

        for (int frame = 0; frame < selectedFrames; frame++) {
            double gain = fadeGain(frame, selectedFrames, fadeFrames);
            int sourceFrame = Math.toIntExact(startFrame + frame);
            for (int channel = 0; channel < channels; channel++) {
                double value = AudioMath.clamp(audio.samples()[channel][sourceFrame] * gain, -1.0, 1.0);
                int pcm = (int) Math.round(value * 32767.0);
                writeLittleEndianShort(output, pcm);
            }
        }

        return output.toByteArray();
    }

    private static double fadeGain(long frame, long selectedFrames, int fadeFrames) {
        if (fadeFrames <= 0) {
            return 1.0;
        }
        double gain = 1.0;
        if (frame < fadeFrames) {
            gain = Math.min(gain, frame / (double) fadeFrames);
        }
        long framesFromEnd = selectedFrames - frame - 1;
        if (framesFromEnd < fadeFrames) {
            gain = Math.min(gain, framesFromEnd / (double) fadeFrames);
        }
        return AudioMath.clamp(gain, 0.0, 1.0);
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }
}
