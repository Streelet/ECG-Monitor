/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.streelet.ecg_java_app.serial;

/**
 *
 * @author erick
 */


public interface SerialDataListener {
    void onDataReceived(int value); // Método que se llama cuando llega un nuevo dato
    void onErrorOccurred(String message); // Método para notificar errores
    // Espacio para agregar más métodos más adelante
}