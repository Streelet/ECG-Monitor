package com.streelet.ecg_java_app.serial;


import com.fazecast.jSerialComm.SerialPort;

import java.io.InputStream;

import java.util.ArrayList;

import java.util.List;

import java.util.Scanner;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;


public class SerialDataManager {


    private SerialPort currentPort = null;

    private ExecutorService serialReaderService; // Método que permite realizar la lectura en un hilo

    private List<SerialDataListener> listeners = new ArrayList<>(); // Lista de oyentes que deben ser notificados

    private volatile boolean reading = false;


    public SerialDataManager() {

    }


    /**

     * Agrega un oyente para ser notificado cuando lleguen datos o ocurran errores.

     * Los oyentes son notificados en el hilo de lectura serial, por lo que deben usar Platform.runLater para actualizaciones de UI.

     * @param listener El oyente a agregar.

     */

    public void addListener(SerialDataListener listener) {

        if (listener != null) {

            listeners.add(listener);

        }

    }


    /**

     * Remueve un oyente de la lista de notificación.

     * @param listener El oyente a remover.

     */

    public void removeListener(SerialDataListener listener) {

        listeners.remove(listener);

    }


/**

 * Notifica a los oyentes que se ha recibido un dato numérico válido.

 * @param value contiene el valor numérico recibido.

 */

    private void notifyDataReceived(int value) {

        for (SerialDataListener listener : listeners) {

            listener.onDataReceived(value);

        }

    }


/**

 * Notifica los oyentes si se ha producido un error de lectura.

 * @param message contiene el error de lectura devuelto

 */

     private void notifyErrorOccurred(String message) {

        for (SerialDataListener listener : listeners) {

             listener.onErrorOccurred(message);

        }

    }


    /**

     * Inicia la lectura de datos desde el puerto serial especificado en un hilo separado.

     * Configura el puerto, lo abre, configura el timeout para Scanner y comienza el bucle de lectura.

     * Si ya está leyendo, no hace nada.

     * @param portName El nombre del puerto serial en el sistema (ej. "COM3", "/dev/ttyACM0").

     * @param baudRate La velocidad (baudios) para la comunicación (el ECG trabaja a 9600 en este caso).

     */

    public boolean startReading(String portName, int baudRate) {

        if (reading) {

            System.out.println("SerialDataManager: Ya leyendo de un puerto serial.");

            return true ;
            
            
        }


        System.out.println("SerialDataManager: Intentando abrir puerto " + portName + " a una velocidad de  " + baudRate + " Baudios");

        // Obtener el puerto por su nombre del sistema y asignar velocidad en Baudios

        currentPort = SerialPort.getCommPort(portName);

        currentPort.setBaudRate(baudRate);

        // Los parámetros por defecto (8 data bits, 1 stop bit, sin paridad) están definidos por defecto


        // Abrir el puerto serial

        boolean portOpened = currentPort.openPort();


        if (!portOpened) {

            // Si falla la apertura se llama al método que informa los errores

            System.err.println("SerialDataManager: Error al abrir el puerto " + portName);

            notifyErrorOccurred("No se pudo abrir el puerto: " + portName + ". Asegurese de que no esté en uso.");

            currentPort = null; // Aseguramos que no quede guardado el nombre del puerto

            return false;

        }


        System.out.println("SerialDataManager: Puerto " + portName + " abierto exitosamente.");

        reading = true; // Para indentificar que ya se está leyendo


        /**

         * Se configura el timeout de los puertos seriales para hacerlos compatibles al máximo

         * con la libreria Scanner de Java, asegurando así que no se produzcan bloqueos.

         */

        currentPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);


        // Crear un ExecutorService de un solo hilo para ejecutar la tarea de lectura en segundo plano.

        // Solo crear uno si no existe o si el que existe está apagado.

        

        if (serialReaderService == null || serialReaderService.isShutdown()) {

             serialReaderService = Executors.newSingleThreadExecutor();

        }


        /** Enviar la tarea de lectura al ExecutorService, se usa lambda para evitar hacer uso de la clase interna

             con Runnable **/

        

        serialReaderService.submit(() -> {

            System.out.println("SerialDataManager: Hilo de lectura serial iniciado.");

            Scanner portScanner = null;


            try {

                // Obtener el stream de entrada del puerto

                InputStream inputStream = currentPort.getInputStream();

                // Configurar el Scanner para leer del InputStream del puerto

                portScanner = new Scanner(inputStream);


                // Bucle principal de lectura.

                

                

                String line = null; // Se declara aquí para que sea visible por el catch

                

                while (reading && currentPort != null && currentPort.isOpen() && portScanner.hasNextLine()) {

                    

                    

                    try {

                        /**

                         * Next Line intenta leer la siguiente linea, de no encontrarse una nueva se bloquea.

                         */

                        line = portScanner.nextLine();
                        
                        
                        String trimmedLine = line.trim();
                        
                        
                        if(trimmedLine.contains("STATUS:ELECTRODES_DISCONNECTED")){
                            //Notificar el estado
                            notifyErrorOccurred("STATUS:ELECTRODES_DISCONNECTED");
                            continue; //Saltar iteracion del bucle
                        }

                        // Se intenta parsear el dato leído como entero.

                        int value = Integer.parseInt(line.trim());


                        // Si el parseo es exitoso, notifica a todos los oyentes.

                        notifyDataReceived(value);


                    } catch (NumberFormatException e) {

                        System.err.println(

                                "SerialDataManager: Error al parsear dato: '" + (line != null ? line.trim() : "null") + "' - "

                                        + e.getMessage());

                        

                        notifyErrorOccurred("Error al parsear dato: " + (line != null ? "'" + line.trim() + "'" : "null"));

                        

                        

                        

                    } catch (java.util.NoSuchElementException e) {

                         // Capturar si el stream del Scanner se cierra inesperadamente mientras espera hasNextLine().

                         System.out.println("SerialDataManager: "

                                 + "Stream del Scanner cerrado inesperadamente durante hasNextLine().");

                         reading = false;  //Sale del bucle

                         

                    } catch (Exception e) {

                        // Excepciones genéricas

                        System.err.println("SerialDataManager: Error durante la lectura serial en bucle: "

                                + e.getMessage());

                        

                         notifyErrorOccurred("Error de lectura serial: " + e.getMessage());

                         reading = false; // Sale del bucle

                    }

                }


                System.out.println("SerialDataManager: Bucle de lectura serial finalizado"

                        + " (reading=" + reading + ", ¿El puerto está abierto? : " + (currentPort != null && currentPort.isOpen()) + ").");


            } catch (Exception e) {

                // Capturar excepciones que ocurren *antes* o *durante* la creación del scanner o stream.

                System.err.println("SerialDataManager: Error fatal al iniciar hilo de lectura o crear Scanner: "

                        + e.getMessage());

                notifyErrorOccurred("Error fatal al iniciar lectura: " + e.getMessage());

                e.printStackTrace(); // Opcional: imprimir la pila


            } finally {

                System.out.println("SerialDataManager: Bloque finally del hilo de lectura.");

                 // Asegura que el Scanner se cierre al igual de que la variable de lectura se coloque en false.

                 

                 if (portScanner != null) {

                    portScanner.close();

                 }

                 

                 reading = false; // Asegurar que la bandera esté en false

            }

             System.out.println("SerialDataManager: Hilo de lectura serial terminó su ejecución.");

        }); // Fin de submit().

        return false;
    } //Final del método

    


    /**

     * Solicita detener el bucle de lectura serial del hilo.

     * Intenta interrumpir el hilo si está bloqueado esperando datos.

     * Nota: El puerto serial permanece abierto después de llamar a stopReading().

     * Se debe llamar a disconnect() o closePort() para cerrar el recurso del puerto.

     */

    public void stopReading() {

        System.out.println("SerialDataManager: Solicitud de detención.");

        reading = false;


        // Intentar apagar el ExecutorService para que intente interrumpir el hilo si está bloqueado en hasNextLine().

        // Esto ayuda a salir rápidamente si hasNextLine() está esperando indefinidamente (timeout = 0).

         if (serialReaderService != null && !serialReaderService.isShutdown()) {

             serialReaderService.shutdownNow(); // Intenta cancelar las tareas en ejecución

             System.out.println("SerialDataManager: Solicitud de apagado inmediato de ExecutorService.");

             try {

                 // Esperar un corto tiempo para dar oportunidad al hilo de terminar después de la interrupción.

                 if (!serialReaderService.awaitTermination(500, TimeUnit.MILLISECONDS)) {

                     System.err.println("SerialDataManager: ExecutorService no terminó limpiamente tras apagado en stopReading().");

                     // Si no termina, podrías loguear esto o intentar un awaitTermination más largo si es crítico

                 }

             } catch (InterruptedException e) {

                 System.err.println("SerialDataManager: Hilo principal interrumpido esperando apagado de ExecutorService en stopReading().");

                 Thread.currentThread().interrupt(); // Restaurar la bandera de interrupción del hilo actual

             }

         }

         // NO cerrar el puerto serial aquí. El puerto se cierra en closePort() o disconnect().

    }


    /**

     * Cierra el puerto serial, detiene el hilo de lectura y apaga el servicio ExecutorService.

     * Este método debe ser llamado cuando se termina completamente de usar el puerto serial (ej. al cerrar la aplicación).

     */

    public void disconnect() {

        System.out.println("SerialDataManager: Solicitud de desconexión completa.");

        // Primero, detener el hilo de lectura de forma controlada. Esto también intenta apagar el ExecutorService.

        stopReading();


        // Ahora, asegurar que el puerto se cierre y que el servicio ExecutorService esté realmente apagado.

        try {

            // Si shutdownNow en stopReading() no fue suficiente, esperar un poco más para el ExecutorService.

            if (serialReaderService != null && !serialReaderService.isTerminated()) {

                 System.err.println("SerialDataManager: ExecutorService no terminado antes de cerrar puerto en disconnect().");

                 serialReaderService.awaitTermination(500, TimeUnit.MILLISECONDS); // Esperar un poco más

            }

        } catch (InterruptedException e) {

             System.err.println("SerialDataManager: Hilo principal interrumpido esperando apagado de ExecutorService en disconnect().");

             Thread.currentThread().interrupt(); // Restaurar la bandera de interrupción del hilo actual

        } finally {

             // Finalmente, asegurarse de que el puerto serial esté cerrado, independientemente de los hilos.

             closePort();

             // Limpiar la referencia al ExecutorService si terminó correctamente.

             if (serialReaderService != null && serialReaderService.isTerminated()) {

                 serialReaderService = null; // Limpiar referencia

             } else if (serialReaderService != null) {

                  System.err.println("SerialDataManager: ExecutorService no terminó. Referencia no limpiada en disconnect().");

             }

        }

         System.out.println("SerialDataManager: disconnect finalizado.");

    }


    /**

     * Método interno para cerrar el recurso del puerto serial si está abierto y no nulo.

     * Establece la referencia a null después de intentar cerrar.

     */

    private void closePort() {

        System.out.println("SerialDataManager: Intentando cerrar puerto.");

        // Aunque stopReading pone 'reading' a false, aseguramos aquí también por si acaso.

        reading = false; // Asegurar que la bandera esté en false para detener cualquier lógica pendiente.


        if (currentPort != null && currentPort.isOpen()) {

             // Intentar cerrar el puerto.

             currentPort.closePort();

             System.out.println("SerialDataManager: Puerto serial cerrado.");

        }

        // Limpiar la referencia al objeto SerialPort.

        currentPort = null;

         System.out.println("SerialDataManager: closePort finalizado.");

    }



    /**

     * Método estático conveniente para obtener una lista de los puertos seriales disponibles en el sistema.

     * Útil para llenar un ComboBox en la interfaz de usuario.

     * @return Un array de objetos SerialPort disponibles. Puede estar vacío si no hay puertos.

     */

    public static SerialPort[] getAvailablePorts() {

        // Este método usa SerialPort.getCommPorts() de la librería jSerialComm.

        // No requiere un puerto abierto para funcionar.

        System.out.println("SerialDataManager: Obteniendo puertos disponibles...");

        try {

             return SerialPort.getCommPorts();

        } catch (Exception e) {

            System.err.println("SerialDataManager: Error al obtener puertos disponibles: " + e.getMessage());

            e.printStackTrace();

            return new SerialPort[0]; // Devolver un array vacío en caso de error

        }

    }

} 