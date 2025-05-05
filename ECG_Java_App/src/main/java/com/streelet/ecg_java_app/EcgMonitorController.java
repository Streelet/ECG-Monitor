package com.streelet.ecg_java_app; // <<< Asegúrate que este sea el paquete correcto para este archivo

// Importaciones JavaFX
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis; // Importar NumberAxis
import javafx.scene.control.Label; // Si usas etiquetas en tu FXML

// Importar la clase Beep (la versión de AudioCue)
import com.streelet.ecg_java_app.sound.Beep; // <--- Importar la clase Beep

// Importaciones de tus clases seriales
import com.streelet.ecg_java_app.serial.SerialDataListener;
import com.streelet.ecg_java_app.serial.SerialDataManager;

// Eliminamos la importación de SoundEffectManager (ya estaba comentada)
// import com.streelet.ecg_java_app.sound.SoundEffectManager;


public class EcgMonitorController implements SerialDataListener { // El Controller implementa el Listener

    // Variables para el flanco de subida del pitido
    private boolean wasBelowThreshold = true; // Estado para detectar el flanco de subida
    private final int beepThreshold = 400; // <-- Umbral configurable para activar el pitido (nuevo valor 400)

    @FXML
    private LineChart<Number, Number> ecgChart; // Tu gráfica definida en FXML

    // @FXML // Descomenta si tienes una etiqueta para la frecuencia cardíaca en tu FXML
    // private Label labelHeartRate;

    private SerialDataManager serialDataManager; // Manager para la comunicación serial
    private XYChart.Series<Number, Number> ecgSeries; // Serie de datos para la gráfica

    // Un contador simple para el eje X (tiempo o número de muestras)
    // Usa 'long' si esperas que el tiempo crezca mucho para evitar desbordamiento.
    private long time = 0;

    // --- Configuración para el Desplazamiento (Scrolling) ---
    private final int WINDOW_SIZE = 700; // Tamaño de la ventana de datos visible en la gráfica

    // Eliminamos la instancia de SoundEffectManager (ya estaba eliminada)
    // SoundEffectManager soundManager = new SoundEffectManager();


    /**
     * Método de inicialización del controlador JavaFX.
     * Se llama automáticamente después de cargar el archivo FXML.
     */
    @FXML
    public void initialize() {
        System.out.println("EcgMonitorController: Inicializando...");

        // --- Configuración de la Gráfica ---
        ecgSeries = new XYChart.Series<>(); // Crea la serie de datos
        ecgSeries.setName("Onda PQRST"); // Nombre de la serie (opcional)
        ecgChart.setLegendVisible(false); // Oculta la leyenda si solo tienes una serie
        ecgChart.getData().add(ecgSeries); // Añade la serie a la gráfica
        ecgChart.setCreateSymbols(false); // No mostrar los puntos individuales en la línea
        ecgChart.setAnimated(false); // Deshabilita animaciones para mejor rendimiento en tiempo real

        // --- Configuración del Eje X para Desplazamiento ---
        NumberAxis xAxis = (NumberAxis) ecgChart.getXAxis(); // Obtiene el eje X (asumiendo que es NumberAxis)
        xAxis.setAutoRanging(false); // Deshabilita auto-ajuste para controlar los límites manualmente

        // Establece los límites iniciales del eje X
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(WINDOW_SIZE);

        // Configura la unidad entre las marcas del eje X (si las etiquetas estuvieran visibles)
        double desiredTickUnit = 100; // Para que el eje x (Linea de tiempo) Se etiquete cada 50 unidades
        xAxis.setTickUnit(desiredTickUnit);

        xAxis.setTickLabelsVisible(false); // Oculta las etiquetas del eje X (si no las necesitas)
        // xAxis.setTickMarksVisible(false); // Opcional: Oculta las marcas también

        // --- Inicialización y Configuración del SerialDataManager ---
        serialDataManager = new SerialDataManager(); // Crea una instancia del manager serial
        serialDataManager.addListener(this); // Registra este controlador para recibir datos y errores

        // Nota: La conexión serial se inicia típicamente con un botón en la UI,
        // no directamente en initialize, para permitir al usuario elegir el puerto.
        // Aquí se incluyen métodos de ejemplo handleConnectButton/handleDisconnectButton.

        // Si quieres iniciar automáticamente (SOLO PARA PRUEBAS):
        // String defaultPort = "COM4"; // <<< CONFIGURA TU PUERTO SERIAL
        // int defaultBaudRate = 9600; // <<< CONFIGURA LA VELOCIDAD
        // System.out.println("EcgMonitorController: Intentando iniciar SerialDataManager con " + defaultPort + "@" + defaultBaudRate);
        // serialDataManager.startReading(defaultPort, defaultBaudRate);

         // --- Inicializar el sistema de sonido Beep (AudioCue) ---
        try {
            Beep.init(); // <--- Llama al método de inicialización estático de Beep
            System.out.println("EcgMonitorController: Sistema Beep (AudioCue) inicializado.");
        } catch (Exception e) {
            System.err.println("EcgMonitorController: Error al inicializar sistema Beep (AudioCue): " + e.getMessage());
            e.printStackTrace();
            // Considera qué hacer si el audio falla al inicializar (ej. deshabilitar pitidos en la UI)
        }

        System.out.println("EcgMonitorController: Inicialización completa.");
        handleConnectButton();
    }

    /**
     * Método de ejemplo para iniciar la lectura serial.
     * Debería ser llamado por una acción del usuario (ej. botón Conectar).
     */
     @FXML
     private void handleConnectButton() {
         String selectedPort = "COM4"; // <<< OBTENER DEL ComboBox EN TU UI
         int baudRate = 9600; // <<< OBTENER DE ALGUNA CONFIGURACIÓN O UI
         System.out.println("EcgMonitorController: Botón Conectar presionado. Intentando iniciar SerialDataManager con " + selectedPort + "@" + baudRate);
         try {
             serialDataManager.startReading(selectedPort, baudRate);
             System.out.println("EcgMonitorController: Lectura serial iniciada.");
         } catch (Exception e) { // Captura excepciones de startReading (ej. puerto no encontrado/ocupado)
             System.err.println("EcgMonitorController: Error al iniciar lectura serial: " + e.getMessage());
             // Aquí actualizarías una etiqueta de estado en la UI para informar al usuario
         }
     }

    /**
     * Método de ejemplo para detener la lectura serial.
     * Debería ser llamado por una acción del usuario (ej. botón Desconectar).
     */
     @FXML
     private void handleDisconnectButton() {
         System.out.println("EcgMonitorController: Botón Desconectar presionado. Deteniendo lectura serial.");
         serialDataManager.disconnect(); // Detiene la lectura y cierra el puerto
         System.out.println("EcgMonitorController: Lectura serial detenida y puerto cerrado.");
     }


    /**
     * Método de la interfaz SerialDataListener llamado cuando se recibe un nuevo punto de dato.
     * NOTA: Este método se ejecuta en un HILO SECUNDARIO (el hilo de lectura serial).
     * Para actualizar la UI o interactuar con JavaFX, DEBES usar Platform.runLater.
     *
     * @param value El valor entero recibido del puerto serial (el punto de dato del ECG).
     */
    @Override
    public void onDataReceived(int value) {
        // Este código está en el HILO SERIAL. Mueve las operaciones de UI y sonido al hilo de JavaFX.
        // System.out.println(value); // Puedes descomentar esto para depurar los valores recibidos

        // Usamos Platform.runLater para asegurar que las actualizaciones de UI y las llamadas a sonido
        // se ejecuten de forma segura en el hilo principal de JavaFX.
        Platform.runLater(() -> {
            // --- Actualización de la Gráfica ---
            // 1. Agregar el nuevo punto con el tiempo actual y el valor recibido.
            ecgSeries.getData().add(new XYChart.Data<>(time, value));

            // Eliminamos la condición if(value==500) de aquí.
            // if(value==500){
            // System.out.println("beep");
            //     Beep.play();
            // }


            // 2. Llamar al método para la lógica del pitido
            playBeep(value); // <--- La lógica de detección está aquí

            time++; // Incrementa el contador de tiempo para el próximo punto

            // 3. Configurar el desplazamiento del eje X para mantener la ventana visible
            NumberAxis xAxis = (NumberAxis) ecgChart.getXAxis(); // Obtiene el eje X

            // Calcula los nuevos límites inferior y superior para que el 'time' actual esté en el borde derecho
            double newLowerBound = time - WINDOW_SIZE;
            double newUpperBound = time;

            // Asegura que los límites no sean negativos al inicio
            if (newLowerBound < 0) {
                newLowerBound = 0;
                newUpperBound = WINDOW_SIZE; // Mantén el tamaño de la ventana incluso al inicio
            }

            // Actualiza los límites del eje X
            xAxis.setLowerBound(newLowerBound);
            xAxis.setUpperBound(newUpperBound);

            // 4. Eliminar Datos Viejos para liberar memoria y mantener el rendimiento
            // Elimina puntos cuyo valor X (tiempo) esté fuera del límite inferior visible
            double currentLowerBound = xAxis.getLowerBound();
            while (ecgSeries.getData().size() > 0 && ecgSeries.getData().get(0).getXValue().doubleValue() < currentLowerBound) {
                 // Elimina el punto más antiguo (en la posición 0)
                 ecgSeries.getData().remove(0);
            }

            // --- Otras Actualizaciones de UI ---
            // Aquí también actualizarías etiquetas de frecuencia cardíaca, estado de conexión, etc.
            // if (labelHeartRate != null) { /* lógica para calcular y mostrar HR */ }

        }); // Fin de Platform.runLater
    }

    /**
     * Método de la interfaz SerialDataListener llamado cuando ocurre un error en la lectura serial.
     * NOTA: Este método se llama desde el HILO SECUNDARIO de lectura serial por defecto en SerialDataManager.
     * Si quieres mostrar el error en la UI, DEBES usar Platform.runLater.
     *
     * @param message El mensaje de error.
     */
    @Override
    public void onErrorOccurred(String message) {
         // Mueve las actualizaciones de UI al hilo de JavaFX.
         Platform.runLater(() -> {
             System.err.println("Error serial en UI (via Platform.runLater): " + message);
             // Aquí actualizarías una etiqueta de estado en la UI para informar al usuario
             // if (labelEstado != null) { /* actualizarLabelEstado("Error: " + message); */ }
         });
    }

    /**
     * Verifica si el valor actual cruza el umbral de pitido en un flanco de subida
     * y reproduce el sonido 'beep' usando la clase Beep (AudioCue) si la condición se cumple.
     * Este método se espera que sea llamado desde el hilo de JavaFX (via Platform.runLater).
     *
     * @param currentValue El valor entero del punto de dato actual recibido del serial.
     */
    private void playBeep (int currentValue) {
        // Las variables beepThreshold y wasBelowThreshold deben ser miembros de la clase (ya lo son).

        // --- Restauramos la lógica de detección de flanco de subida ---

        // Ocurre un flanco de subida si el valor actual está en o por encima del umbral
        // Y el valor anterior (según el estado 'wasBelowThreshold') estaba estrictamente por debajo.
        boolean isRisingEdge = (currentValue >= beepThreshold) && wasBelowThreshold;

        // Imprimir el valor actual y el estado ANTES de la detección (para depuración)
        System.out.println("DEBUG ECG Data: value=" + currentValue + ", wasBelowThreshold (antes)=" + wasBelowThreshold + ", isRisingEdge=" + isRisingEdge);


        // Si se detecta un flanco de subida, reproducir el pitido.
        if (isRisingEdge) {
            System.out.println("DEBUG ECG: Flanco de subida detectado en time=" + (time-1) + ", value=" + currentValue + ". Llamando a Beep.play().");
            // Llama al método estático play() de la clase Beep (AudioCue)
            Beep.play();
        }

        // ACTUALIZAR EL ESTADO 'wasBelowThreshold' para la próxima lectura.
        // wasBelowThreshold será verdadero si el valor *actual* está estrictamente por debajo del umbral.
        // Esto prepara para detectar el próximo flanco de subida cuando el valor cruce el umbral nuevamente.
        wasBelowThreshold = (currentValue < beepThreshold);
    }


    /**
     * Método de limpieza para asegurar que los recursos se liberen al cerrar la aplicación.
     * Debería ser llamado desde el método stop() de tu clase Application principal.
     */
    public void shutdown() {
        System.out.println("EcgMonitorController: Llamando a shutdown()...");
        // Asegura que el SerialDataManager se desconecte y cierre el puerto serial.
        if (serialDataManager != null) {
            serialDataManager.disconnect();
            System.out.println("EcgMonitorController: SerialDataManager desconectado.");
        }
        // Apagar el sistema de sonido Beep (AudioCue)
        Beep.shutdown();
        System.out.println("EcgMonitorController: Sistema Beep (AudioCue) apagado.");

        System.out.println("EcgMonitorController: Shutdown completo.");
    }
}