/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.streelet.ecg_java_app.model;

/**
 *
 * @author erick
 */

/**
 * Interfaz para que el Modelo de datos ECG notifique a los oyentes
 * (típicamente el Controlador) cuando se detecta un pico.
 */
public interface EcgPeakListener {

    /**
     * Este método es llamado por el modelo de datos ECG
     * cuando se detecta un flanco de subida que cruza el umbral de pico.
     *
     * @param peakValue El valor del dato en el momento en que se detectó el pico.
     * @param time El contador de tiempo (o índice de muestra) en que ocurrió el pico.
     */
    void onPeakDetected(int peakValue, long time);
}