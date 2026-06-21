package com.streelet.ecg_java_app.data;

import com.streelet.ecg_java_app.serial.SerialDataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fuente de datos del modo Mock: genera una onda PQRST sintetica realista, sin
 * necesidad de hardware. Entrega las muestras a la misma frecuencia que asume el
 * modelo (200 Hz), de modo que la deteccion del pico R y el calculo de BPM
 * funcionan igual que con el sensor real.
 *
 * La forma de onda se construye sumando funciones gaussianas (P, Q, R, S, T)
 * sobre una linea base, con leve variabilidad latido a latido (HRV), ruido y
 * deriva de linea base para que se vea natural. El pico R supera el umbral de
 * deteccion (945) mientras el resto de las ondas se mantienen por debajo, lo que
 * garantiza exactamente un pico por ciclo.
 */
public class MockEcgDataManager implements EcgDataSource {

    // --- Parametros de muestreo (deben coincidir con EcgDataModel) ---
    private static final int SAMPLE_RATE_HZ = 200;
    private static final long TICK_MS = 10; // el planificador despierta cada 10 ms

    // --- Morfologia de la onda (compatible con el eje Y 300..1200, umbral 945) ---
    private static final double BASELINE = 600.0;
    // Centro (fraccion del ciclo), ancho (fraccion del ciclo) y amplitud de cada onda
    private static final double P_CENTER = 0.18, P_WIDTH = 0.030, P_AMP = 90;
    private static final double Q_CENTER = 0.30, Q_WIDTH = 0.012, Q_AMP = -60;
    private static final double R_CENTER = 0.33, R_WIDTH = 0.011, R_AMP = 520; // pico ~1120 (> 945)
    private static final double S_CENTER = 0.37, S_WIDTH = 0.014, S_AMP = -180;
    private static final double T_CENTER = 0.58, T_WIDTH = 0.045, T_AMP = 170;

    private final List<SerialDataListener> listeners = new ArrayList<>();
    private final Random random = new Random();
    private final int targetBpm;

    private ScheduledExecutorService scheduler;
    private long startNanos;
    private long emittedSamples;

    // Estado del ciclo actual
    private int sampleInBeat = 0;
    private int samplesPerBeat;

    /** Crea un generador con una frecuencia cardiaca objetivo por defecto (72 BPM). */
    public MockEcgDataManager() {
        this(72);
    }

    /** @param targetBpm frecuencia cardiaca objetivo aproximada (latidos por minuto). */
    public MockEcgDataManager(int targetBpm) {
        this.targetBpm = targetBpm;
        this.samplesPerBeat = nextBeatLength();
    }

    @Override
    public void addListener(SerialDataListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public boolean start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return true; // ya esta corriendo
        }
        System.out.println("MockEcgDataManager: Iniciando generador de onda PQRST (~"
                + targetBpm + " BPM, " + SAMPLE_RATE_HZ + " Hz).");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mock-ecg-generator");
            t.setDaemon(true);
            return t;
        });
        startNanos = System.nanoTime();
        emittedSamples = 0;
        scheduler.scheduleAtFixedRate(this::emitDueSamples, 0, TICK_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void stop() {
        System.out.println("MockEcgDataManager: Deteniendo generador de onda PQRST.");
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        listeners.clear();
    }

    /**
     * Emite todas las muestras que "deberian" haberse generado segun el tiempo
     * transcurrido. Asi la frecuencia media se mantiene exacta aunque el
     * planificador tenga jitter (algo comun en Windows).
     */
    private void emitDueSamples() {
        try {
            long elapsedNanos = System.nanoTime() - startNanos;
            long shouldHaveEmitted = (long) (elapsedNanos / 1_000_000_000.0 * SAMPLE_RATE_HZ);
            while (emittedSamples < shouldHaveEmitted) {
                notifyDataReceived(nextSample());
                emittedSamples++;
            }
        } catch (Exception e) {
            System.err.println("MockEcgDataManager: Error generando muestra: " + e.getMessage());
        }
    }

    /** Calcula el siguiente valor de la onda y avanza el estado del ciclo. */
    private int nextSample() {
        double phase = (double) sampleInBeat / samplesPerBeat; // 0..1 dentro del latido

        double value = BASELINE
                + P_AMP * gaussian(phase, P_CENTER, P_WIDTH)
                + Q_AMP * gaussian(phase, Q_CENTER, Q_WIDTH)
                + R_AMP * gaussian(phase, R_CENTER, R_WIDTH)
                + S_AMP * gaussian(phase, S_CENTER, S_WIDTH)
                + T_AMP * gaussian(phase, T_CENTER, T_WIDTH);

        // Ligero ruido y deriva de linea base para una apariencia mas natural
        value += random.nextGaussian() * 2.5;
        value += 8.0 * Math.sin(emittedSamples / 600.0);

        sampleInBeat++;
        if (sampleInBeat >= samplesPerBeat) {
            sampleInBeat = 0;
            samplesPerBeat = nextBeatLength(); // variabilidad latido a latido
        }

        // Mantener dentro del rango visible del eje Y (300..1200)
        return (int) Math.round(Math.max(310, Math.min(1190, value)));
    }

    /** Longitud (en muestras) del proximo latido, con +-4 % de variabilidad. */
    private int nextBeatLength() {
        double base = SAMPLE_RATE_HZ * 60.0 / targetBpm;
        double jitter = 1.0 + (random.nextDouble() - 0.5) * 0.08; // +-4 %
        return Math.max(1, (int) Math.round(base * jitter));
    }

    private static double gaussian(double x, double center, double width) {
        double d = x - center;
        return Math.exp(-(d * d) / (2.0 * width * width));
    }

    private void notifyDataReceived(int value) {
        for (SerialDataListener listener : listeners) {
            listener.onDataReceived(value);
        }
    }
}
