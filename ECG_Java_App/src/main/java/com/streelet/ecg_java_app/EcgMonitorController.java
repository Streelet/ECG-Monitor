package com.streelet.ecg_java_app; 

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import com.streelet.ecg_java_app.sound.Beep;
import com.streelet.ecg_java_app.serial.SerialDataListener;
import com.streelet.ecg_java_app.serial.SerialDataManager;
import com.streelet.ecg_java_app.model.EcgDataModel;
import com.streelet.ecg_java_app.model.EcgPeakListener;
import javafx.animation.FadeTransition;
import javafx.scene.shape.Circle;
import javafx.animation.PauseTransition; 
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.util.Duration; 


// El Controlador implementa SerialDataListener y EcgPeakListener
public class EcgMonitorController implements SerialDataListener, EcgPeakListener {

    @FXML
    private LineChart<Number, Number> ecgChart;

     @FXML
     private Label labelHeartRate;
     
     @FXML
     private Circle beatCircle;
     
     @FXML 
     private StackPane electrodesDisconnectedOverlay;
     
     @FXML
     private Label overlayTitle;
     
     @FXML
     private Label overlayMessage;
     
     @FXML 
     private TextArea overlayInstructions;
     
   
     private static final String instructions =
        "La monitorización del ECG se ha detenido.\n\n" +
        "Instrucciones:\n\n" +
        "• Asegúrese de que los electrodos estén bien adheridos a la piel.\n\n\n" +
        "• Verifique que los cables estén conectados firmemente \n" +
        "   a los electrodos y al dispositivo.\n\n\n" +
        "• Pida al paciente que se mantenga quieto si es posible.";

    private SerialDataManager serialDataManager;
    private XYChart.Series<Number, Number> ecgSeries;

    private long time = 0;
    private final int WINDOW_SIZE = 250;

  
    private EcgDataModel ecgDataModel;
    
    
    private int consecutiveValidDataCount = 0;
    private final int minConsecutiveValidData = 10;

    /**
     * Método de inicialización del controlador JavaFX.
     * Se llama automáticamente después de cargar el archivo FXML.
     */
    @FXML
    public void initialize() {
        System.out.println("EcgMonitorController: Inicializando...");
        
        
        if(beatCircle != null){
            beatCircle.setOpacity(0.0);
        }
        
        //Inicializacion de Overlay 
        
         if (electrodesDisconnectedOverlay != null) {
            electrodesDisconnectedOverlay.setOpacity(0.0);
            electrodesDisconnectedOverlay.setVisible(false);
            electrodesDisconnectedOverlay.setManaged(false);
        }
        
        
        
        // --- Configuración de la Gráfica ---
        ecgSeries = new XYChart.Series<>();
        ecgSeries.setName("Onda PQRST");
        ecgChart.setLegendVisible(false);
        ecgChart.getData().add(ecgSeries);
        ecgChart.setCreateSymbols(false);
        ecgChart.setAnimated(false);
        
        //Configuracion de Eje Y para no dejar espacio en blanco
        NumberAxis yAxis = (NumberAxis) ecgChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(1200);
        yAxis.setLowerBound(360);
        
        //Configuración del Eje X para Desplazamiento   
        NumberAxis xAxis = (NumberAxis) ecgChart.getXAxis();
        xAxis.setAutoRanging(false);

        xAxis.setLowerBound(0);
        xAxis.setUpperBound(WINDOW_SIZE);

        double desiredTickUnit = 100;
        xAxis.setTickUnit(desiredTickUnit);

        xAxis.setTickLabelsVisible(false);


        serialDataManager = new SerialDataManager();
        serialDataManager.addListener(this); // Este controlador se registra como SerialDataListener

        ecgDataModel = new EcgDataModel(960);
        ecgDataModel.addPeakListener(this); // Este controlador se registra como EcgPeakListener


        // Inicializar el sistema de sonido 
        try {
            Beep.init();
            System.out.println("EcgMonitorController: Sistema Beep (AudioCue) inicializado.");
        } catch (Exception e) {
            System.err.println("EcgMonitorController: Error al inicializar sistema Beep (AudioCue): " + e.getMessage());
            e.printStackTrace();
        }

        // Llama al método handleConnectButton() directamente para iniciar la lectura serial
        handleConnectButton();


        System.out.println("EcgMonitorController: Inicialización completa.");
    }


    @FXML
    private void handleConnectButton() {
         
        hideStatusOverlay();
        
         String selectedPort = "COM4"; 
         int baudRate = 9600;
         System.out.println("EcgMonitorController: Botón Conectar presionado. Intentando iniciar SerialDataManager con " + selectedPort + "@" + baudRate);
         try {
             serialDataManager.startReading(selectedPort, baudRate);
            // Reiniciar el estado del modelo al iniciar una nueva conexión
            if (ecgDataModel != null) {
                ecgDataModel.resetState();
            }
             System.out.println("EcgMonitorController: Lectura serial iniciada.");
         } catch (Exception e) {
             System.err.println("EcgMonitorController: Error al iniciar lectura serial: " + e.getMessage());
         }
    }


     @FXML
    private void handleDisconnectButton() {
         System.out.println("EcgMonitorController: Botón Desconectar presionado. Deteniendo lectura serial.");
         serialDataManager.disconnect();
        // Opcional: Limpiar la gráfica al desconectar
        ecgSeries.getData().clear();
        time = 0; // Reiniciar el contador de tiempo de la gráfica
         System.out.println("EcgMonitorController: Lectura serial detenida y puerto cerrado.");
    }


    /**
     * Método de la interfaz SerialDataListener llamado cuando se recibe un nuevo punto de dato.
     * NOTA: Este método se ejecuta en un HILO SECUNDARIO (el hilo de lectura serial).
     * Para actualizar la UI o interactuar con el Modelo (si su lógica no es thread-safe)
     * en el hilo de JavaFX, DEBES usar Platform.runLater.
     *
     * @param value El valor entero recibido del puerto serial (el punto de dato del ECG).
     */
    @Override
    public void onDataReceived(int value) {

  

        //  Platform.runLater para realizar actualizaciones de UI y pasar datos al Modelo

        Platform.runLater(() -> {
            
            
            
             // --- Incrementar el contador de datos validos consecutivos ---
        // Este metodo solo se llama cuando llega un dato numerico VALIDO, asi que incrementamos.
        consecutiveValidDataCount++; // <-- Asegurate de tener esta linea aqui
        System.out.println("EcgMonitorController: Dato valido recibido. Contador: " + consecutiveValidDataCount);
        // --- Fin Incrementar contador ---


        // --- Verificar si se cumplen las condiciones para ocultar el overlay ---
        // Si el numero de datos validos consecutivos es suficiente (>= minConsecutiveValidData)
        // Y el overlay de desconexion esta actualmente visible (isVisible())...
        if (consecutiveValidDataCount >= minConsecutiveValidData && // <-- Usando tu variable 'minConsecutiveValidData'
            electrodesDisconnectedOverlay != null && // <-- Asegurarse que el campo FXML no sea null
            electrodesDisconnectedOverlay.isVisible()) { // <-- Asegurarse que el overlay esta visible

             System.out.println("EcgMonitorController: Recibidos " + minConsecutiveValidData + " datos validos consecutivos. Ocultando overlay.");
             hideStatusOverlay(); // <-- Asegurate de que la llamada a hideStatusOverlay() este AQUI
        }

            ecgSeries.getData().add(new XYChart.Data<>(time, value));

            if (ecgDataModel != null) {
                ecgDataModel.processNewValue(value, time);
            }

            time++; 
            
            NumberAxis xAxis = (NumberAxis) ecgChart.getXAxis();
            double newLowerBound = time - WINDOW_SIZE;
            double newUpperBound = time;
            if (newLowerBound < 0) {
                newLowerBound = 0;
                newUpperBound = WINDOW_SIZE;
            }
            xAxis.setLowerBound(newLowerBound);
            xAxis.setUpperBound(newUpperBound);

            double currentLowerBound = xAxis.getLowerBound();
            while (ecgSeries.getData().size() > 0 && ecgSeries.getData().get(0).getXValue().doubleValue() < currentLowerBound) {
                 ecgSeries.getData().remove(0);
            }

          
            
         //Para el Heart Rate
         
         int currentBpm = ecgDataModel.getCurrentBpm();
         if(labelHeartRate != null){
             if(currentBpm > 0){
                 labelHeartRate.setText(currentBpm+"");
             }
             else  {
                 labelHeartRate.setText("00");
             }
         }
            
        });
    }

    /**
     * Implementación del método de la interfaz EcgPeakListener.
     * Este método es llamado por el Modelo (EcgDataModel) cuando detecta un pico.
     * Se ejecuta en el HILO DE JavaFX (porque el Modelo es llamado desde Platform.runLater).
     * @param peakValue El valor del dato en el momento en que se detectó el pico.
     * @param time El contador de tiempo (o índice de muestra) en que ocurrió el pico.
     */
    @Override
    public void onPeakDetected(int peakValue, long time) {
        
        if (beatCircle != null) {
        // Hacer el círculo completamente visible instantáneamente
        beatCircle.setOpacity(1.0);

        // Crear una transición de pausa que dure 300ms
        PauseTransition fadeOut = new PauseTransition(Duration.millis(300));

        // Al terminar la pausa, establecer la opacidad a 0
        // La transición CSS hará que este cambio de 1.0 a 0.0 sea animado (el desvanecimiento)
        fadeOut.setOnFinished(event -> {
            beatCircle.setOpacity(0.0);
        });

        // Iniciar la pausa y, por lo tanto, la animación de desvanecimiento después
        fadeOut.play();
    }
        
        // Acción a realizar cuando el Modelo detecta un pico
        // Aquí es donde disparamos el sonido del pitido
        System.out.println("DEBUG ECG: Pico detectado por el Modelo en time=" + time + ", value=" + peakValue + ". Llamando a Beep.play().");
        Beep.play();

    }


    /**
     * Método de la interfaz SerialDataListener llamado cuando ocurre un error serial.
     * Se mantiene igual.
     * @param message El mensaje de error.
     */
    @Override
    public void onErrorOccurred(String message) {
        Platform.runLater(() -> {
            System.err.println("Error serial en UI (via Platform.runLater): " + message);
            
            consecutiveValidDataCount=0;
            
            if(message.startsWith("STATUS:")){
                String statusType = message.substring("STATUS:".length());
                
                if(statusType.contains("ELECTRODES_DISCONNECTED")){
                    showStatusOverlay("Electrodos Desconectados", "SE DETUVO LA MONITORIZACION \n ESTE ES EL SEGUNDO ");
                }
                else if(statusType.equals("ELECTRODES_CONNECTED")){
                    System.out.println("Se han vuelto a conectar los electrodos");
                }
                
                else {
                    System.out.println("Error desconocido");
                }
            }
            
        });
    }

    
    /**
     * Muestra el overlay de estado (ej. desconexion de electrodos) con animacion de fade-in.
     * Actualiza el texto de las etiquetas del overlay.
     * @param title El titulo a mostrar en el overlay.
     * @param message El mensaje/instrucciones a mostrar en la etiqueta del mensaje.
     */
    private void showStatusOverlay(String title, String message) {
        
        
        if (electrodesDisconnectedOverlay != null) {
           
            if (overlayTitle != null) overlayTitle.setText(title);
            if (overlayMessage != null) overlayMessage.setText(message); 
             
             
                //  overlayInstructions.setText(instructions); 
                // System.out.println("FALLA");
             //    System.out.println(overlayInstructions.getText());
         //   overlayInstructions.setScrollTop(0);
        

            electrodesDisconnectedOverlay.setVisible(true);
            electrodesDisconnectedOverlay.setManaged(true);

            // Animar la opacidad del overlay desde su valor actual (normalmente 0.0 si esta oculto) hasta 1.0 (completamente opaco)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), electrodesDisconnectedOverlay); // 500ms de duracion
            fadeIn.setFromValue(electrodesDisconnectedOverlay.getOpacity()); 
            fadeIn.setToValue(1.0); 
            fadeIn.play(); 

            System.out.println("EcgMonitorController: Mostrando overlay de estado con fade-in .");
        } else {
             System.err.println("EcgMonitorController: ERROR: No se pudo mostrar el overlay porque el nodo 'electrodesDisconnectedOverlay' es null.");
        }
    }

    /**
     * Oculta el overlay de estado con animacion de fade-out.
     */
    private void hideStatusOverlay() {
         // Solo intentar ocultar si el overlay existe y esta visible (para evitar animar algo ya invisible)
         if (electrodesDisconnectedOverlay != null && electrodesDisconnectedOverlay.isVisible()) {

             // Crear y reproducir la animacion de desaparicion (fade-out)
             // Animar la opacidad del overlay desde su valor actual (normalmente 1.0 si esta visible) hasta 0.0 (completamente transparente)
             FadeTransition fadeOut = new FadeTransition(Duration.millis(500), electrodesDisconnectedOverlay); // 500ms de duracion
             fadeOut.setFromValue(electrodesDisconnectedOverlay.getOpacity()); // Empezar desde la opacidad actual
             fadeOut.setToValue(0.0); // Terminar en opacidad 0.0 (transparente)

             // Al finalizar la animacion de desvanecimiento...
             fadeOut.setOnFinished(event -> {
                 // ...ocultar completamente el nodo para que no bloquee eventos o espacio en el layout.
                 electrodesDisconnectedOverlay.setVisible(false);
                 electrodesDisconnectedOverlay.setManaged(false);
                 System.out.println("EcgMonitorController: Overlay de estado ocultado y gestionado.");
             });

             fadeOut.play(); // Iniciar la animacion
             System.out.println("EcgMonitorController: Ocultando overlay de estado con fade-out.");
         } else {
             System.out.println("EcgMonitorController: hideStatusOverlay() llamado pero el overlay ya estaba oculto o es null.");
         }
    }
    
    
    
    

    /**
     * Método de limpieza al cerrar la aplicación.
     */
    public void shutdown() {
        System.out.println("EcgMonitorController: Llamando a shutdown()...");
        if (serialDataManager != null) {
            serialDataManager.disconnect();
            System.out.println("EcgMonitorController: SerialDataManager desconectado.");
        }
        Beep.shutdown();
        System.out.println("EcgMonitorController: Sistema Beep (AudioCue) apagado.");

        System.out.println("EcgMonitorController: Shutdown completo.");
    }
}