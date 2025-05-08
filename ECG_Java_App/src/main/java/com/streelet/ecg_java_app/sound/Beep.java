
package com.streelet.ecg_java_app.sound;

import com.adonax.audiocue.AudioCue;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;


/**
 * Clase para gestionar la reproducción del sonido 'beep.wav' utilizando la librería AudioCue.
 * Precarga el sonido en un pool de "voces" para permitir reproducción rápida y superpuesta.
 * Debe ser inicializada y apagada.
 */
public class Beep {

    private static final String BEEP_RESOURCE_PATH = "/com/streelet/ecg_java_app/sounds/beep.wav";
    // Número de instancias concurrentes (voces) para el pitido.
    private static final int BEEP_POOL_SIZE = 8;

    // Instancia estática de AudioCue para el pitido
    private static AudioCue beepCue;

    // Bandera para saber si la librería ha sido inicializada
    private static volatile boolean isInitialized = false;

    // Constructor privado para prevenir instanciación
    private Beep() { }

    /**
     * Inicializa el sistema de AudioCue y precarga el sonido del pitido.
     * Debe ser llamado una sola vez al inicio de la aplicación.
     * Es seguro llamar a este método múltiples veces; solo se inicializará la primera vez.
     *
     * @throws IOException Si ocurre un error de I/O al leer el recurso.
     * @throws UnsupportedAudioFileException Si el formato de audio no es soportado por AudioCue (espera WAV PCM 16-bit, 44.1kHz, estéreo/mono).
     * @throws LineUnavailableException Si una línea de audio no está disponible.
     * @throws IllegalStateException Si AudioCue ya fue inicializado con otra configuración o error de estado interno.
     * @throws IllegalArgumentException Si el recurso no se encuentra o es inválido.
     * @throws Exception Captura otras posibles excepciones durante la inicialización de AudioCue.
     */
    public static synchronized void init() throws IOException, UnsupportedAudioFileException, LineUnavailableException, Exception {
        if (isInitialized) {
            System.out.println("DEBUG Beep: AudioCue ya inicializado.");
            return;
        }

        System.out.println("DEBUG Beep: Inicializando AudioCue y cargando pitido...");

        URL audioResourceUrl = Beep.class.getResource(BEEP_RESOURCE_PATH);

        if (audioResourceUrl == null) {
            throw new IllegalArgumentException("Recurso de pitido no encontrado en el classpath: " + BEEP_RESOURCE_PATH);
        }

       
            // AudioCue.makeStereoCue(URL url, int nVoices) crea el cue
            beepCue = AudioCue.makeStereoCue(audioResourceUrl, BEEP_POOL_SIZE);

            // --- IMPORTANTE: Llamar a open() explícitamente como se muestra en los ejemplos de la documentación ---
            // Esto es crucial para asegurar que la línea de audio se abra correctamente.
            beepCue.open(); // <--- ¡Añadimos esta llamada!

            isInitialized = true;
            System.out.println("DEBUG Beep: AudioCue y pitido precargado y abierto con " + BEEP_POOL_SIZE + " voces.");

        
    }

    /**
     * Reproduce el sonido del pitido utilizando una voz disponible del pool de AudioCue.
     * Este método no bloquea el hilo llamador.
     * Si no hay voces disponibles en el pool, la reproducción podría ser omitida por AudioCue
     * o podría cortar una voz existente dependiendo de la configuración interna de AudioCue.
     *
     * @return El ID de la instancia de reproducción si se inició correctamente (>= 0), o un valor negativo si falló (-1 si no inicializado, < -1 por otras razones de AudioCue).
     */
    public static int play() {
        if (!isInitialized || beepCue == null) {
            System.err.println("ERROR Beep: Beep.init() no ha sido llamado o falló. No se puede reproducir el pitido.");
            return -1;
        }

        int instanceId = beepCue.play();

        if (instanceId >= 0) {
             System.out.println("DEBUG Beep: Pitido disparado (AudioCue instanceID: " + instanceId + ").");
        } else {
             System.out.println("WARNING Beep: No se pudo disparar pitido (AudioCue play() retornó " + instanceId + "). Posiblemente pool lleno o error interno.");
        }

        return instanceId;
    }

    /**
     * Cierra el sistema de AudioCue y libera todos los recursos asociados al pitido.
     * Debe ser llamado una sola vez al finalizar la aplicación.
     */
    public static synchronized void shutdown() {
        if (!isInitialized || beepCue == null) {
            System.out.println("DEBUG Beep: AudioCue no está inicializado o ya apagado.");
            return;
        }

        System.out.println("DEBUG Beep: Apagando AudioCue...");
        try {
            beepCue.close(); // Cierra el AudioCue, liberando los Clips del pool.
             System.out.println("DEBUG Beep: AudioCue cerrado exitosamente.");
        } catch (Exception e) {
             System.err.println("ERROR Beep: Excepción al cerrar AudioCue: " + e.getMessage());
             e.printStackTrace();
        } finally {
             beepCue = null;
             isInitialized = false;
             System.out.println("DEBUG Beep: AudioCue apagado (estado interno actualizado).");
        }
    }

    // --- Método main de Ejemplo para la clase Beep usando AudioCue ---
    public static void main(String[] args) {
        System.out.println("--- Test de la clase Beep (AudioCue) ---");

        try {
            Beep.init();

            System.out.println("\n--- Disparando pitidos rapidamente ---");
            for (int i = 0; i < 10; i++) {
                 System.out.println("Disparo " + (i+1) + "...");
                 Beep.play();
                 try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

             try {
                System.out.println("Esperando 2 segundos para que terminen los pitidos...");
                Thread.sleep(2000);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }


        } catch (Exception e) {
            System.err.println("Error durante el test de Beep (AudioCue): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Beep.shutdown();
            System.out.println("\n--- Test de la clase Beep (AudioCue) finalizado ---");
        }
    }
}