package com.streelet.ecg_java_app.data;

import com.streelet.ecg_java_app.serial.SerialDataListener;
import com.streelet.ecg_java_app.serial.SerialDataManager;

/**
 * Fuente de datos del modo IoT: lee la senal real desde un puerto serial.
 * Envuelve a {@link SerialDataManager} para exponer la interfaz comun
 * {@link EcgDataSource}.
 */
public class SerialEcgDataSource implements EcgDataSource {

    private final SerialDataManager manager = new SerialDataManager();
    private final String portName;
    private final int baudRate;

    public SerialEcgDataSource(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    @Override
    public void addListener(SerialDataListener listener) {
        manager.addListener(listener);
    }

    @Override
    public boolean start() {
        return manager.startReading(portName, baudRate);
    }

    @Override
    public void stop() {
        manager.disconnect();
    }
}
