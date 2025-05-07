
package com.streelet.ecg_java_app.model;

/**
 *
 * @author erick
 */

import java.util.List;
import java.util.ArrayList;

/**
 * Representa el modelo de datos del ECG y contiene la lógica
 * para procesar los datos entrantes y detectar picos.
 * Este modelo no tiene dependencia directa de la UI o del sonido.
 */
public class EcgDataModel {

    // Variables de estado para la detección de flanco
    private boolean wasBelowThreshold = true;
    private final int beepThreshold;
    private final int numberOfBeatsToAverage = 5;
    private List<Integer> bpmHistory = new ArrayList<>();
    
    private long lastPeakTime = -1;
    private int currentBpm = 0;
    private final int samplingRate = 250;

    // Lista de oyentes que serán notificados cuando se detecte un pico
    private List<EcgPeakListener> listeners = new ArrayList<>();

    /**
     * Constructor para el modelo de datos ECG.
     *
     * @param beepThreshold El valor del umbral para la detección de picos.
     */
    public EcgDataModel(int beepThreshold) {
        this.beepThreshold = beepThreshold;
        System.out.println("EcgDataModel creado con umbral = " + this.beepThreshold);
    }

    /**
     * Añade un oyente a la lista de oyentes de picos.
     * Cuando se detecte un pico, el método onPeakDetected() de este oyente será llamado.
     *
     * @param listener El oyente a añadir.
     */
    public void addPeakListener(EcgPeakListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println(" Oyente añadido: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Elimina un oyente de la lista de oyentes de picos.
     *
     * @param listener El oyente a eliminar.
     */
    public void removePeakListener(EcgPeakListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            System.out.println("DEBUG Model: Oyente eliminado: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Procesa un nuevo valor de dato del ECG. Contiene la lógica de detección de pico.
     * Si se detecta un pico, notifica a todos los oyentes registrados.
     *
     * NOTA: Este método DEBERÍA ser llamado desde un hilo seguro para la lógica
     * de detección de pico (como el hilo de JavaFX si los datos llegan vía Platform.runLater).
     *
     * @param currentValue El valor entero del dato del ECG.
     * @param currentTime El contador de tiempo (o índice de muestra) asociado a este valor.
     */
    public void processNewValue(int currentValue, long currentTime) {
        // Lógica de detección de flanco de subida (movida desde EcgMonitorController)
        boolean isRisingEdge = (currentValue >= beepThreshold) && wasBelowThreshold;

        // Si se detecta un flanco de subida, notifica a todos los oyentes
        if (isRisingEdge) {
            System.out.println("DEBUG Model: Pico detectado en time=" + currentTime + ", value=" + currentValue + ". Notificando a oyentes.");
            
         
         if(lastPeakTime !=-1){
             long intervalSamples = currentTime- lastPeakTime;
             
             //Solo asegurar que es positivo
             if(intervalSamples >0){
                 double instantaneousBpm = (double)samplingRate * 60 / intervalSamples;
                 bpmHistory.add((int) Math.round(instantaneousBpm));
                 System.out.println("BPM ACTUAL " + instantaneousBpm);
                 System.out.println("BPM " + currentBpm);
                 
                 /*Mantener la lista en el numero de datos especificado por
                 @numberOfBeatsToAverage
                 */
                 while (bpmHistory.size() > numberOfBeatsToAverage){
                     bpmHistory.remove(0);
                 }
                 
                 double sumBpm = 0;
                 for(int bpm : bpmHistory){
                 sumBpm += bpm;
             }
                currentBpm = (int) Math.round(sumBpm / bpmHistory.size()); 
                 
             } //Fin del If de Lectura de Pico R
             else {
                 System.out.println("El intervalo entre picos de onda R es 0 ");
             }
         }
         else {
                System.out.println("Se detecta el primer pico");
                currentBpm = 0;
         }

         lastPeakTime = currentTime;

// Notifica a todos los oyentes registrados llamando a su método onPeakDetected
            for (EcgPeakListener listener : listeners) {
                // Llama al método definido en la interfaz EcgPeakListener
                listener.onPeakDetected(currentValue, currentTime);
            }
        }

        // Actualiza el estado para la próxima lectura 
        wasBelowThreshold = (currentValue < beepThreshold);

      
    }

    public void resetState() {
        wasBelowThreshold = true;
        lastPeakTime = -1;
        currentBpm= 0;
        bpmHistory.clear();
        System.out.println("DEBUG Model: Estado restablecido.");
    }


    public int getBeepThreshold() {
        return beepThreshold;
    }
    
    public int getCurrentBpm(){
        return currentBpm;
    }
}