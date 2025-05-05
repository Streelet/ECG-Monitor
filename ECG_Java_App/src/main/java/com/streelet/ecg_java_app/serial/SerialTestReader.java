package com.streelet.ecg_java_app.serial;

import java.util.Scanner; // Para leer la entrada del usuario en la consola

public class SerialTestReader { // Esta clase de prueba USARÁ SerialDataManager y SerialDataListener

    // --- Configuración HARDCODEADA para la prueba ---
    // Asegúrate de que estos valores coincidan con tu configuración actual de Arduino y puerto.
    private static final String PORT_NAME = "COM8"; // <<< Cambia a tu puerto si no es COM8
    private static final int BAUD_RATE = 9600;     // <<< Cambia a tu velocidad si no es 9600
    // -------------------------------------------------

    public static void main(String[] args) {
        System.out.println("--- Prueba de Componentes SerialManager/Listener ---");
        System.out.println("Puerto: " + PORT_NAME + ", Velocidad: " + BAUD_RATE);

        SerialDataManager serialDataManager = null; // Declaramos la instancia del Manager
        Scanner consoleScanner = null; // Para leer la entrada del usuario y detener la prueba

        try {
            // --- 1. Crear una instancia de SerialDataManager ---
            // Asegúrate de que la clase SerialDataManager en tu paquete serial esté actualizada
            // con la solución del TIMEOUT_SCANNER.
            serialDataManager = new SerialDataManager();

            // --- 2. Crear una implementación del Listener para esta prueba ---
            // Usamos una clase anónima interna para implementar SerialDataListener aquí mismo.
            SerialDataListener testListener = new SerialDataListener() {
                @Override
                public void onDataReceived(int value) {
                    // Este método es llamado por SerialDataManager cada vez que recibe y parsea un número.
                    // NOTA IMPORTANTE: ¡Este método se ejecuta en el HILO DE LECTURA SERIAL del SerialDataManager!
                    // En el Controller de tu UI (que es otro Listener), aquí usarías Platform.runLater.
                    // Para esta prueba simple de consola, solo imprimimos directamente:
                    System.out.println("Recibido por Listener: " + value);
                }

                @Override
                public void onErrorOccurred(String message) {
                    // Este método es llamado por SerialDataManager si ocurre un error.
                    System.err.println("Error por Listener: " + message);
                }
            };

            // --- 3. Agregar este Listener de prueba al SerialDataManager ---
            serialDataManager.addListener(testListener);

            // --- 4. Iniciar la lectura en el SerialDataManager ---
            // Esto le dice al Manager que abra el puerto y empiece su hilo de lectura.
            System.out.println("Iniciando SerialDataManager...");
            serialDataManager.startReading(PORT_NAME, BAUD_RATE);

            System.out.println("SerialDataManager iniciado. Esperando datos...");
            System.out.println("Presiona ENTER en la consola para detener la prueba.");
            System.out.println("--------------------------------------------------");

            // --- 5. Mantener el programa principal vivo ---
            // El hilo de lectura del SerialDataManager corre en segundo plano.
            // El método main principal terminaría si no lo mantenemos esperando algo.
            consoleScanner = new Scanner(System.in);
            consoleScanner.nextLine(); // Bloquea aquí, esperando a que el usuario presione ENTER

            System.out.println("--------------------------------------------------");
            System.out.println("Detectado ENTER. Solicitando desconexión...");

        } catch (Exception e) { // Captura errores durante la inicialización o arranque del Manager
            System.err.println("Ocurrió un error durante la prueba de inicialización del SerialDataManager: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // --- 6. Desconectar y cerrar recursos ---
            // Este bloque se ejecuta cuando salimos del try/catch (ej. después de presionar ENTER o si hubo una excepción).
            System.out.println("Finalizando prueba...");
            if (serialDataManager != null) {
                // Llama al método disconnect() del Manager para detener su hilo de lectura y cerrar el puerto.
                serialDataManager.disconnect();
            }
            if (consoleScanner != null) {
                consoleScanner.close(); // Cierra el scanner de la consola para liberar System.in
            }
        }

        System.out.println("Prueba de componentes Manager/Listener finalizada.");
    }
}