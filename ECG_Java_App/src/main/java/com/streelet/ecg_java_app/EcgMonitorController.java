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
import javafx.scene.shape.Circle;
import javafx.animation.PauseTransition; 
import javafx.util.Duration; 


// El Controlador implementa SerialDataListener y EcgPeakListener
public class EcgMonitorController implements SerialDataListener, EcgPeakListener {

    @FXML
    private LineChart<Number, Number> ecgChart;

     @FXML
     private Label labelHeartRate;
     
     @FXML
     private Circle beatCircle;

    private SerialDataManager serialDataManager;
    private XYChart.Series<Number, Number> ecgSeries;

    private long time = 0;
    private final int WINDOW_SIZE = 400;

  
    private EcgDataModel ecgDataModel;

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
        yAxis.setLowerBound(0);
        
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

        ecgDataModel = new EcgDataModel(830);
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
        });
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