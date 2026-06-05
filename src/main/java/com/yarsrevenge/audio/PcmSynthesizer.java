package com.yarsrevenge.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class PcmSynthesizer {

    private static final int SAMPLE_RATE = 44_100;

    public static byte[] generate(SoundEffect fx) {
        return switch (fx) {
            // Deep space ambient hum: layered low sine + slow LFO pulse + subtle noise
            case BG_HUM -> buildBgHum();

            // Bullet shoot: short bright downward sweep
            case BULLET_SHOOT -> buildSweep(900, 300, 120, 2, 10, 0.5, 50);

            // Shield nibble: high-pitched click — short square wave chirp + noise snap
            case SHIELD_NIBBLE -> buildNibble();

            // Swirl explode: medium noise explosion + pitch-down ring
            case SWIRL_EXPLODE -> buildExplosion(350, 0.80, false);

            // Shield cell pop: small quick pop
            case SHIELD_CELL_POP -> buildNoiseBurst(60, 0.45, 40);

            // Cannon launch: deep crack + ascending rip
            case CANNON_LAUNCH -> buildCannonLaunch();

            // Cannon flying: rising hiss — looped, medium length
            case CANNON_FLYING -> buildCannonFlight();

            // Quotile big explosion: long layered noise + decreasing rumble
            case QUOTILE_EXPLODE -> buildBigExplosion();

            // Enemy alarm: repeating two-tone pulse warning
            case ENEMY_ALARM -> buildAlarm();

            // Enemy jet fly: square-wave engine roar with vibrato
            case PLAYER_DEATH -> buildSweep(440, 80, 1200, 50, 100, 0.7, 400);

            // Wave start: ascending arpeggio
            case WAVE_START -> buildWaveStart();

            // Quotile missile: high-pitched screaming rising tone, loopable 1s
            case QUOTILE_MISSILE -> buildQuotileMissile();
        };
    }

    // ---------------------------------------------------------------------------
    // Specific sound builders
    // ---------------------------------------------------------------------------

    private static byte[] buildBgHum() {
        // 4-second loop: deep sub-bass + three detuned mid tones + upper harmonic + slow pulse LFO
        int n = msToSamples(4000);
        short[] s = new short[n];
        Random rng = new Random(0x4A2B);
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            // Slow 0.2 Hz volume pulse — gives life to the hum
            double lfo  = 0.55 + 0.45 * Math.sin(2 * Math.PI * 0.20 * t);
            // Sub-bass layer: 40 Hz square-ish (adds body you feel more than hear)
            double sub  = 0.40 * Math.signum(Math.sin(2 * Math.PI * 40.0 * t));
            // Three slightly detuned mid tones — beating creates the "alive" shimmer
            double mid1 = 0.35 * Math.sin(2 * Math.PI * 80.0  * t);
            double mid2 = 0.25 * Math.sin(2 * Math.PI * 83.7  * t); // +3.7 Hz beating
            double mid3 = 0.20 * Math.sin(2 * Math.PI * 160.0 * t); // octave up
            // Upper shimmer: 320 Hz soft sine, faint
            double hi   = 0.10 * Math.sin(2 * Math.PI * 320.0 * t);
            // Tiny noise floor for texture
            double noise = 0.04 * (rng.nextDouble() * 2 - 1);
            double wave = sub + mid1 + mid2 + mid3 + hi + noise;
            // Fade in/out at loop boundaries to avoid clicks
            double fade = 1.0;
            int fadeLen = msToSamples(180);
            if (i < fadeLen)     fade = (double) i / fadeLen;
            if (i > n - fadeLen) fade = (double)(n - i) / fadeLen;
            s[i] = (short)(wave * lfo * fade * 0.55 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildNibble() {
        // High-pitched chirp: 2400 Hz square wave sweeping up to 3600 Hz, 60ms
        int n = msToSamples(60);
        short[] s = new short[n];
        double phaseAcc = 0;
        Random rng = new Random(0x4E1B);
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = 2400 + progress * 1200;
            phaseAcc += freq / SAMPLE_RATE;
            double sq    = Math.signum(Math.sin(2 * Math.PI * phaseAcc));
            double noise = (rng.nextDouble() * 2 - 1) * 0.15;
            int rS = msToSamples(25), aS = msToSamples(2);
            double amp = envelope(i, n, aS, 0, 1.0, rS);
            s[i] = (short)((sq * 0.85 + noise) * amp * 0.65 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildNoiseBurst(double durationMs, double volume, double releaseMs) {
        int n = msToSamples(durationMs);
        short[] s = new short[n];
        Random rng = new Random(0xBEEF);
        int rS = msToSamples(releaseMs);
        int aS = msToSamples(5);
        for (int i = 0; i < n; i++) {
            double amp = envelope(i, n, aS, 0, 1.0, rS);
            s[i] = (short)((rng.nextDouble() * 2 - 1) * amp * volume * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildExplosion(double durationMs, double volume, boolean big) {
        int n = msToSamples(durationMs);
        short[] s = new short[n];
        Random rng = new Random(0xDEAD);
        int rS = msToSamples(durationMs * 0.6);
        int aS = msToSamples(5);
        double baseFreq = big ? 40 : 80;
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = baseFreq * (1.0 - progress * 0.7);
            phaseAcc += freq / SAMPLE_RATE;
            double noise = rng.nextDouble() * 2 - 1;
            double tone  = Math.sin(2 * Math.PI * phaseAcc);
            double mix   = noise * 0.7 + tone * 0.3;
            double amp   = envelope(i, n, aS, 0, 1.0, rS);
            s[i] = (short)(mix * amp * volume * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildCannonLaunch() {
        // Sharp crack (noise) + rising tone up from 80 Hz
        int n = msToSamples(300);
        short[] s = new short[n];
        Random rng = new Random(0xC0DE);
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = 80 + progress * 600;
            phaseAcc += freq / SAMPLE_RATE;
            double noise = rng.nextDouble() * 2 - 1;
            double tone  = Math.sin(2 * Math.PI * phaseAcc);
            double mix   = (i < msToSamples(40)) ? noise * 0.9 : tone * 0.7 + noise * 0.3;
            int aS = msToSamples(5), rS = msToSamples(120);
            double amp = envelope(i, n, aS, 0, 1.0, rS);
            s[i] = (short)(mix * amp * 0.75 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildCannonFlight() {
        // Rising hiss with square-wave undertone — 1.5 second loopable
        int n = msToSamples(1500);
        short[] s = new short[n];
        Random rng = new Random(0xF1A8);
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = 200 + progress * 400;
            phaseAcc += freq / SAMPLE_RATE;
            double sq    = Math.signum(Math.sin(2 * Math.PI * phaseAcc)) * 0.3;
            double noise = (rng.nextDouble() * 2 - 1) * 0.7;
            int fadeLen  = msToSamples(150);
            double fade  = 1.0;
            if (i < fadeLen)     fade = (double) i / fadeLen;
            if (i > n - fadeLen) fade = (double)(n - i) / fadeLen;
            s[i] = (short)((sq + noise) * fade * 0.55 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildBigExplosion() {
        // 3-second layered boom: initial crack, then descending rumble, then noise tail
        int n = msToSamples(3000);
        short[] s = new short[n];
        Random rng = new Random(0xB00B5L);
        double phaseAcc1 = 0, phaseAcc2 = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            // Layer 1: descending sub-bass boom
            double f1 = 60 * Math.pow(1.0 - progress, 2.0) + 20;
            phaseAcc1 += f1 / SAMPLE_RATE;
            double bass = Math.sin(2 * Math.PI * phaseAcc1);
            // Layer 2: mid crunch sweep
            double f2 = 200 * Math.pow(1.0 - progress, 1.5) + 30;
            phaseAcc2 += f2 / SAMPLE_RATE;
            double mid  = Math.sin(2 * Math.PI * phaseAcc2) * 0.5;
            // Layer 3: noise
            double noise = (rng.nextDouble() * 2 - 1) * (1.0 - progress * 0.6);
            double mix = bass * 0.4 + mid * 0.3 + noise * 0.5;
            // Envelope: instant attack, long tail
            double env = Math.pow(1.0 - progress, 0.6);
            s[i] = (short)(mix * env * 0.85 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildAlarm() {
        // Two-tone beep repeating 4×: 800 Hz / 600 Hz alternating, 120ms each
        int beepMs = 120;
        int reps    = 1;
        int n = msToSamples(beepMs * 2 * reps);
        short[] s = new short[n];
        int beepLen = msToSamples(beepMs);
        for (int i = 0; i < n; i++) {
            int beepIdx = i / beepLen;
            double freq = (beepIdx % 2 == 0) ? 800 : 550;
            double t    = (double) i / SAMPLE_RATE;
            double raw  = Math.signum(Math.sin(2 * Math.PI * freq * t));
            // Small envelope per beep to avoid clicks
            int posInBeep = i % beepLen;
            int fLen = msToSamples(8);
            double fade = 1.0;
            if (posInBeep < fLen)            fade = (double) posInBeep / fLen;
            if (posInBeep > beepLen - fLen)  fade = (double)(beepLen - posInBeep) / fLen;
            s[i] = (short)(raw * fade * 0.60 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildJetFly() {
        // Engine roar: sawtooth with vibrato, rising pitch, 800ms
        int n = msToSamples(800);
        short[] s = new short[n];
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double t = (double) i / SAMPLE_RATE;
            double vibrato = 1.0 + 0.04 * Math.sin(2 * Math.PI * 18 * t);
            double freq = (120 + progress * 350) * vibrato;
            phaseAcc += freq / SAMPLE_RATE;
            // Sawtooth
            double saw = 2.0 * (phaseAcc - Math.floor(phaseAcc)) - 1.0;
            int aS = msToSamples(30), rS = msToSamples(200);
            double amp = envelope(i, n, aS, 0, 1.0, rS);
            s[i] = (short)(saw * amp * 0.55 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildQuotileMissile() {
        // Rising screaming missile: sawtooth sweep 400→2800 Hz, 1s loopable, vibrato
        int n = msToSamples(1000);
        short[] s = new short[n];
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double t = (double) i / SAMPLE_RATE;
            double vibrato = 1.0 + 0.03 * Math.sin(2 * Math.PI * 35 * t);
            double freq = (400 + progress * 2400) * vibrato;
            phaseAcc += freq / SAMPLE_RATE;
            double saw = 2.0 * (phaseAcc - Math.floor(phaseAcc)) - 1.0;
            int fadeLen = msToSamples(40);
            double fade = 1.0;
            if (i < fadeLen)     fade = (double) i / fadeLen;
            if (i > n - fadeLen) fade = (double)(n - i) / fadeLen;
            s[i] = (short)(saw * fade * 0.65 * Short.MAX_VALUE);
        }
        return wrapWav(s);
    }

    private static byte[] buildWaveStart() {
        // Retro power-up: high laser sweep down (2400→300 Hz, 300ms) then a
        // three-voice square chord stab (A2+E3+A3 = 110/165/220 Hz, 500ms)
        int sweepMs = 300, chordMs = 500, gapMs = 40;
        int n = msToSamples(sweepMs + gapMs + chordMs);
        short[] s = new short[n];

        // Sweep part: sawtooth sweep 2400→300 Hz with noise crackle
        int sweepLen = msToSamples(sweepMs);
        Random rng = new Random(0x1234);
        double phaseAcc = 0;
        for (int i = 0; i < sweepLen; i++) {
            double progress = (double) i / sweepLen;
            double freq = 2400 - progress * 2100;
            phaseAcc += freq / SAMPLE_RATE;
            double saw   = 2.0 * (phaseAcc - Math.floor(phaseAcc)) - 1.0;
            double noise = (rng.nextDouble() * 2 - 1) * 0.12;
            int rS = msToSamples(60);
            double amp = envelope(i, sweepLen, msToSamples(4), 0, 1.0, rS);
            s[i] = (short)((saw * 0.75 + noise) * amp * 0.70 * Short.MAX_VALUE);
        }

        // Chord stab: three square-wave voices summed
        double[] chordFreqs = {110.0, 165.0, 220.0};
        int chordStart = msToSamples(sweepMs + gapMs);
        int chordLen   = msToSamples(chordMs);
        double[] phases = new double[chordFreqs.length];
        for (int i = 0; i < chordLen && chordStart + i < n; i++) {
            double mix = 0;
            for (int v = 0; v < chordFreqs.length; v++) {
                phases[v] += chordFreqs[v] / SAMPLE_RATE;
                mix += Math.signum(Math.sin(2 * Math.PI * phases[v])) / chordFreqs.length;
            }
            int aS = msToSamples(8), rS = msToSamples(280);
            double amp = envelope(i, chordLen, aS, 0, 0.85, rS);
            s[chordStart + i] = (short)(mix * amp * 0.60 * Short.MAX_VALUE);
        }

        return wrapWav(s);
    }

    // ---------------------------------------------------------------------------
    // Generic builders
    // ---------------------------------------------------------------------------

    private static byte[] build(WaveType type, double freqHz, double durationMs,
                                 double attackMs, double decayMs,
                                 double sustainLevel, double releaseMs) {
        int n = msToSamples(durationMs);
        short[] samples = new short[n];
        int aS = msToSamples(attackMs);
        int dS = msToSamples(decayMs);
        int rS = msToSamples(releaseMs);
        Random rng = new Random(42);
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double raw = switch (type) {
                case SINE   -> Math.sin(2 * Math.PI * freqHz * t);
                case SQUARE -> Math.signum(Math.sin(2 * Math.PI * freqHz * t));
                case NOISE  -> rng.nextDouble() * 2.0 - 1.0;
                case SWEEP  -> 0;
            };
            double amp = envelope(i, n, aS, dS, sustainLevel, rS);
            samples[i] = (short) (raw * amp * Short.MAX_VALUE);
        }
        return wrapWav(samples);
    }

    private static byte[] buildSweep(double startHz, double endHz, double durationMs,
                                      double attackMs, double decayMs,
                                      double sustainLevel, double releaseMs) {
        int n = msToSamples(durationMs);
        short[] samples = new short[n];
        int aS = msToSamples(attackMs);
        int dS = msToSamples(decayMs);
        int rS = msToSamples(releaseMs);
        double phaseAcc = 0;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = startHz + (endHz - startHz) * progress;
            phaseAcc += freq / SAMPLE_RATE;
            double raw = Math.sin(2 * Math.PI * phaseAcc);
            double amp = envelope(i, n, aS, dS, sustainLevel, rS);
            samples[i] = (short) (raw * amp * Short.MAX_VALUE);
        }
        return wrapWav(samples);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static double envelope(int i, int n, int a, int d, double s, int r) {
        if (i < a)       return (double) i / Math.max(1, a);
        if (i < a + d)   return lerp(1.0, s, (double)(i - a) / Math.max(1, d));
        if (i >= n - r)  return s * (double)(n - i) / Math.max(1, r);
        return s;
    }

    private static double lerp(double x, double y, double t) {
        return x + (y - x) * Math.min(1, Math.max(0, t));
    }

    private static int msToSamples(double ms) {
        return Math.max(1, (int)(SAMPLE_RATE * ms / 1000.0));
    }

    private static byte[] wrapWav(short[] samples) {
        int dataLen  = samples.length * 2;
        int totalLen = 44 + dataLen;
        ByteBuffer b = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R','I','F','F'});
        b.putInt(totalLen - 8);
        b.put(new byte[]{'W','A','V','E'});
        b.put(new byte[]{'f','m','t',' '});
        b.putInt(16);
        b.putShort((short)1);
        b.putShort((short)1);
        b.putInt(SAMPLE_RATE);
        b.putInt(SAMPLE_RATE * 2);
        b.putShort((short)2);
        b.putShort((short)16);
        b.put(new byte[]{'d','a','t','a'});
        b.putInt(dataLen);
        for (short s : samples) b.putShort(s);
        return b.array();
    }

    private enum WaveType { SINE, SQUARE, NOISE, SWEEP }
}
