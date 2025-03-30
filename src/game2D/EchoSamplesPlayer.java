package game2D;

import javax.sound.sampled.*;
import java.io.*;

/**
 * This class plays a sound clip and applies an echo effect by appending
 * multiple decayed versions of the original sound. The effect is only
 * supported for 8-bit PCM signed or unsigned audio.
 */
public class EchoSamplesPlayer {
    private static final int ECHO_COUNT = 5;       // Total echoes
    private static final double DECAY_FACTOR = 0.5; // Volume decay rate

    private static AudioInputStream audioStream;
    private static AudioFormat audioFormat;
    private static SourceDataLine audioLine;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java EchoSamplesPlayer <clip file>");
            System.exit(1);
        }

        String filePath = "sounds/" + args[0];
        loadAudioFile(filePath);

        if (!isSupportedFormat()) {
            System.out.println("Only 8-bit PCM (signed or unsigned) formats are supported.");
            System.exit(1);
        }

        setupAudioLine();

        int totalBytes = (int) (audioStream.getFrameLength() * audioFormat.getFrameSize());
        byte[] rawAudio = fetchAudioBytes(totalBytes);
        playAudio(rawAudio);
    }

    private static void loadAudioFile(String filePath) {
        try {
            audioStream = AudioSystem.getAudioInputStream(new File(filePath));
            audioFormat = audioStream.getFormat();

            if (audioFormat.getEncoding() == AudioFormat.Encoding.ULAW ||
                    audioFormat.getEncoding() == AudioFormat.Encoding.ALAW) {

                AudioFormat convertedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        audioFormat.getSampleRate(),
                        audioFormat.getSampleSizeInBits() * 2,
                        audioFormat.getChannels(),
                        audioFormat.getFrameSize() * 2,
                        audioFormat.getFrameRate(),
                        true);

                audioStream = AudioSystem.getAudioInputStream(convertedFormat, audioStream);
                audioFormat = convertedFormat;

                System.out.println("Audio format converted: " + convertedFormat);
            }

        } catch (UnsupportedAudioFileException | IOException e) {
            System.out.println("Error loading audio: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isSupportedFormat() {
        return audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED ||
                audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED;
    }

    private static void setupAudioLine() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Audio line not supported: " + audioFormat);
                System.exit(1);
            }

            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(audioFormat);

        } catch (LineUnavailableException e) {
            System.out.println("Error initializing audio line: " + e.getMessage());
            System.exit(1);
        }
    }

    private static byte[] fetchAudioBytes(int numBytes) {
        byte[] originalSamples = new byte[numBytes];

        try (DataInputStream dis = new DataInputStream(audioStream)) {
            dis.readFully(originalSamples);
        } catch (IOException e) {
            System.out.println("Error reading audio: " + e.getMessage());
            System.exit(1);
        }

        return applyEchoEffect(originalSamples, numBytes);
    }

    private static byte[] applyEchoEffect(byte[] original, int length) {
        int totalCopies = ECHO_COUNT + 1;
        double currentDecay = 1.0;

        byte[] modified = new byte[length * totalCopies];

        for (int echoIndex = 0; echoIndex < totalCopies; echoIndex++) {
            for (int i = 0; i < length; i++) {
                modified[i + (echoIndex * length)] = applyDecay(original[i], currentDecay);
            }
            currentDecay *= DECAY_FACTOR;
        }

        return modified;
    }

    private static byte applyDecay(byte input, double decay) {
        short originalSample;
        short decayedSample;

        if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            originalSample = (short) (input & 0xff);
        } else {
            originalSample = input;
        }

        decayedSample = (short) (originalSample * decay);
        return (byte) decayedSample;
    }

    private static void playAudio(byte[] audioBytes) {
        InputStream inputStream = new ByteArrayInputStream(audioBytes);
        byte[] buffer = new byte[audioLine.getBufferSize()];
        int bytesRead;

        audioLine.start();

        try {
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                int offset = 0;
                while (offset < bytesRead) {
                    offset += audioLine.write(buffer, offset, bytesRead - offset);
                }
            }
        } catch (IOException e) {
            System.out.println("Error during playback: " + e.getMessage());
        }

        audioLine.drain();
        audioLine.stop();
        audioLine.close();
    }
}
