package com.streelet.ecg_java_app.data;

import com.streelet.ecg_java_app.serial.SerialDataListener;

/**
 * Abstraccion de una fuente de datos de ECG. Permite que el controlador trate
 * de forma uniforme tanto la lectura serial real (modo IoT) como el generador
 * de senal sintetica (modo Mock).
 *
 * Al igual que {@code SerialDataManager}, los oyentes son notificados desde un
 * hilo secundario, por lo que deben usar {@code Platform.runLater} para
 * actualizar la interfaz de usuario.
 */
public interface EcgDataSource {

    /** Registra un oyente que recibira los datos y los mensajes de estado/error. */
    void addListener(SerialDataListener listener);

    /**
     * Inicia la produccion de datos.
     *
     * @return {@code true} si la fuente arranco correctamente.
     */
    boolean start();

    /** Detiene la produccion de datos y libera los recursos asociados. */
    void stop();
}
