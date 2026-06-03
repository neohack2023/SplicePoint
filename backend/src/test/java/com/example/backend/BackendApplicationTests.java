package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void analyzesSimpleWavFile() throws Exception {
        MockMultipartFile file = testWavFile();
        AudioAnalysisService service = new AudioAnalysisService();

        AnalysisResponse response = service.analyze(file);

        assertThat(response.durationSeconds()).isGreaterThan(0.9).isLessThan(1.1);
        assertThat(response.sampleRate()).isEqualTo(44_100);
        assertThat(response.channels()).isEqualTo(1);
        assertThat(response.loops()).isNotEmpty();
    }

    @Test
    void exportsSelectedWavLoop() throws Exception {
        MockMultipartFile file = testWavFile();
        AudioExportService service = new AudioExportService();

        byte[] exported = service.exportWav(file, 0.0, 0.5, 5);

        assertThat(exported).hasSizeGreaterThan(44);
        assertThat(new String(exported, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(exported, 8, 4)).isEqualTo("WAVE");
    }

    private MockMultipartFile testWavFile() throws IOException {
        int sampleRate = 44_100;
        int seconds = 1;
        int channels = 1;
        int bitsPerSample = 16;
        int totalSamples = sampleRate * seconds;
        int dataSize = totalSamples * channels * bitsPerSample / 8;

        ByteArrayOutputStream output = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(output, "RIFF");
        writeLittleEndianInt(output, 36 + dataSize);
        writeAscii(output, "WAVE");
        writeAscii(output, "fmt ");
        writeLittleEndianInt(output, 16);
        writeLittleEndianShort(output, 1);
        writeLittleEndianShort(output, channels);
        writeLittleEndianInt(output, sampleRate);
        writeLittleEndianInt(output, sampleRate * channels * bitsPerSample / 8);
        writeLittleEndianShort(output, channels * bitsPerSample / 8);
        writeLittleEndianShort(output, bitsPerSample);
        writeAscii(output, "data");
        writeLittleEndianInt(output, dataSize);

        for (int i = 0; i < totalSamples; i++) {
            double envelope = i < 256 ? i / 256.0 : 1.0;
            double sample = Math.sin(2.0 * Math.PI * 220.0 * i / sampleRate) * envelope * 0.35;
            writeLittleEndianShort(output, (int) Math.round(sample * 32767.0));
        }

        return new MockMultipartFile("file", "test.wav", "audio/wav", output.toByteArray());
    }

    private void writeAscii(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private void writeLittleEndianShort(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }
}
