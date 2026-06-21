package com.streelet.ecg_java_app;

/**
 * Modos de operacion del monitor ECG.
 *
 * <ul>
 *   <li>{@link #IOT}: lee datos reales desde un puerto serial (sensor AD8232 +
 *       microcontrolador).</li>
 *   <li>{@link #MOCK}: genera una onda PQRST sintetica para pruebas sin
 *       hardware.</li>
 * </ul>
 */
public enum MonitorMode {
    IOT,
    MOCK
}
