package com.streelet.ecg_java_app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Importar Initializable
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

import com.streelet.ecg_java_app.StartController.PatientData; 


import java.net.URL; 
import java.time.LocalTime; 
import java.time.format.DateTimeFormatter; 
import java.util.ResourceBundle; 


// El Controlador implementa SerialDataListener y EcgPeakListener
// Implementa Initializable porque tiene un metodo initialize() que es llamado por FXMLLoader
public class EcgMonitorController implements Initializable, SerialDataListener, EcgPeakListener { 

    @FXML
    private LineChart<Number, Number> ecgChart;

    @FXML
    private Label labelHeartRate;

    @FXML
    private Circle beatCircle;

    @FXML
    private TextArea patientSummaryTextArea; // Campo para el resumen del paciente en la UI del monitor

    // --- Campos @FXML para el overlay de desconexion (ajustados segun FXML de overlay diseñado) ---
    @FXML
    private StackPane electrodesDisconnectedOverlay; // Nodo raiz del overlay FXML
    @FXML
    private Label overlayTitle; 
    

    // Campo @FXML para el UNICO TextArea de instrucciones en el overlay

    @FXML
    private TextArea overlayInstructionsPart1; // 
    // --- Fin Campos @FXML para el overlay ---


   
    private PatientData patientData; 
    private String serialPortName; 
    // --- Fin Campos recibidos ---



    private static final String FULL_INSTRUCTIONS_TEXT = // 
        "La monitorización del ECG se ha detenido.\n\n" +
        "Instrucciones:\n\n" +
        "• Asegúrese de que los electrodos estén bien adheridos a la piel.\n\n\n" +
        "• Verifique que los cables estén conectados firmemente \n" +
        "   a los electrodos y al dispositivo.\n\n\n" +
        "• Pida al paciente que se mantenga quieto si es posible.";
     
       


    private SerialDataManager serialDataManager; 


    private XYChart.Series<Number, Number> ecgSeries;


    private long time = 0; 
    private final int WINDOW_SIZE = 900; 


    private EcgDataModel ecgDataModel; 


    private int consecutiveValidDataCount = 0; 
    private final int minConsecutiveValidData = 10; 


    /**
     * Método de inicialización del controlador JavaFX.
     * Se llama automáticamente después de cargar el archivo FXML.
     * IMPORTANTE: Este metodo NO iniciará la comunicación serial. Solo prepara la UI.
     */
    @FXML
    // Implementa Initializable si no estaba implementado en la firma de la clase
    public void initialize(URL url, ResourceBundle resourceBundle) { 
        System.out.println("EcgMonitorController: Inicialización de UI completada."); 

  

        if(beatCircle != null){
            beatCircle.setOpacity(0.0);
        }

        //Inicializacion de Overlay
         if (electrodesDisconnectedOverlay != null) {
            electrodesDisconnectedOverlay.setOpacity(0.0);
            electrodesDisconnectedOverlay.setVisible(false);
            electrodesDisconnectedOverlay.setManaged(false);
        }


        // --- Configuración de la Gráfica (Mantener todo esto) ---
        ecgSeries = new XYChart.Series<>();
        ecgSeries.setName("Onda PQRST");
        ecgChart.setLegendVisible(false);
        ecgChart.getData().add(ecgSeries);
        ecgChart.setCreateSymbols(false);
        ecgChart.setAnimated(false);

        //Configuracion de Eje Y
        NumberAxis yAxis = (NumberAxis) ecgChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setUpperBound(1200);
        yAxis.setLowerBound(300);

        //Configuración del Eje X para Desplazamiento
        NumberAxis xAxis = (NumberAxis) ecgChart.getXAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(WINDOW_SIZE);
        double desiredTickUnit = 100;
        xAxis.setTickUnit(desiredTickUnit);
        xAxis.setTickLabelsVisible(false);
        // --- Fin Configuración de la Gráfica ---



        ecgDataModel = new EcgDataModel(945); // Ya existe, mantener
        ecgDataModel.addPeakListener(this); // Ya existe, mantener


        // Inicializar el sistema de sonido (Mantener esto)
        try {
            Beep.init();
            System.out.println("EcgMonitorController: Sistema Beep (AudioCue) inicializado.");
        } catch (Exception e) {
            System.err.println("EcgMonitorController: Error al inicializar sistema Beep (AudioCue): " + e.getMessage());
            e.printStackTrace();
        }


        System.out.println("EcgMonitorController: Inicialización completa.");
    }


    // Este metodo es llamado por App.java despues de initialize()
    // Asegurate de importar com.streelet.ecg_java_app.StartController.PatientData;
    public void setPatientDataAndPort(PatientData patientData, String portName) {
        System.out.println("EcgMonitorController: Datos de paciente y puerto recibidos.");
        this.patientData = patientData;
        this.serialPortName = portName;

        // Mostrar el resumen del paciente en el TextArea (si el campo @FXML no es null)
        if (patientSummaryTextArea != null && this.patientData != null) {
            patientSummaryTextArea.setText(this.patientData.getSummary());
             // Opcional: Puedes hacer que este TextArea no sea editable y no tenga scrollbars via CSS o FXML
             // Ya agregamos reglas CSS para esto anteriormente
             patientSummaryTextArea.setEditable(false); // Hacerlo no editable
             patientSummaryTextArea.setFocusTraversable(false); // No se puede seleccionar con tab
        } else {
             System.err.println("EcgMonitorController: ERROR: patientSummaryTextArea es null o patientData es null al intentar mostrar resumen.");
        }


    }
    // --- Fin Metodo publico para recibir datos ---



    // Este metodo es llamado por App.java DESPUES de setPatientDataAndPort()
    public void startMonitoring() {
         System.out.println("EcgMonitorController: Iniciando monitorización serial...");

         // Verificar que tenemos el nombre del puerto antes de intentar iniciar
         if (this.serialPortName == null || this.serialPortName.isEmpty()) {
             System.err.println("EcgMonitorController: ERROR: No se puede iniciar la monitorización serial. Nombre de puerto no configurado.");
             // Mostrar un mensaje de error al usuario en la UI (usando el overlay)
             showStatusOverlay("ERROR DE INICIO"); // Usa el metodo showStatusOverlay
             return; // Salir del metodo si no hay puerto
         }

         serialDataManager = new SerialDataManager();
         serialDataManager.addListener(this); 

         int baudRate = 9600; 

         try {
             // Llama al metodo startReading con el puerto y baudRate
             boolean serialStarted = serialDataManager.startReading(this.serialPortName, baudRate);

             if (serialStarted) {
                 System.out.println("EcgMonitorController: Monitorización serial iniciada en puerto: " + this.serialPortName + "@" + baudRate);
                 // Reiniciar el estado del modelo al iniciar una nueva conexión (mantener esta logica de tu initialize/handleConnect)
                 if (ecgDataModel != null) {
                     ecgDataModel.resetState();
                 }
                 // Puedes añadir alguna indicacion visual de que la monitorizacion esta activa
             } else {
                 System.err.println("EcgMonitorController: ERROR: No se pudo iniciar la monitorización serial en puerto: " + this.serialPortName + "@" + baudRate);
                 // Mostrar un mensaje de error al usuario en la UI (usando el overlay)
                 showStatusOverlay("ERROR DE CONEXIÓN"); // Usa el metodo showStatusOverlay
             }
         } catch (Exception e) { // Capturar excepciones generales al iniciar serial
             System.err.println("EcgMonitorController: Excepción al iniciar lectura serial en puerto " + this.serialPortName + ": " + e.getMessage());
             e.printStackTrace();
             showStatusOverlay("ERROR DE CONEXIÓN"); // Usa el metodo showStatusOverlay
         }
    }
    // --- Fin Metodo para iniciar monitorizacion serial ---


   


    /**
     * Método de la interfaz SerialDataListener llamado cuando se recibe un nuevo punto de dato.
     * (Mantener este método como está, pero asegurar que la lógica de conteo y ocultar overlay funcione)
     * @param value El valor entero recibido del puerto serial (el punto de dato del ECG).
     */
    @Override
    public void onDataReceived(int value) {


        // Este metodo se ejecuta en un HILO SECUNDARIO (el hilo de lectura serial).
        // Para actualizar la UI o interactuar con el Modelo (si su lógica no es thread-safe)

        Platform.runLater(() -> {


           consecutiveValidDataCount++; 


            if (consecutiveValidDataCount >= minConsecutiveValidData && 
                electrodesDisconnectedOverlay != null && //  Asegurarse que el campo FXML no sea null
                electrodesDisconnectedOverlay.isVisible()) { 

                 System.out.println("EcgMonitorController: Recibidos " + minConsecutiveValidData + " datos validos consecutivos. Ocultando overlay.");
                 hideStatusOverlay(); // <-- Asegurate de que la llamada a hideStatusOverlay() este AQUI
            }
            // --- Fin Verificar y Ocultar overlay ---



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


            // Para el Heart Rate 
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
     * (Mantener este método como está)
     * @param peakValue El valor del dato en el momento en que se detectó el pico.
     * @param time El contador de tiempo (o índice de muestra) en que ocurrió el pico.
     */
    @Override
    public void onPeakDetected(int peakValue, long time) {
        // Este método se ejecuta en el HILO DE JavaFX

        if (beatCircle != null) {
        // Hacer el círculo completamente visible instantáneamente
        beatCircle.setOpacity(1.0);

        // Crear una transición de pausa que dure 300ms
        PauseTransition fadeOut = new PauseTransition(Duration.millis(300));

        fadeOut.setOnFinished(event -> {
            beatCircle.setOpacity(0.0);
        });

        // Iniciar la pausa y, por lo tanto, la animación 
        fadeOut.play();
    }

        // Acción a realizar cuando el Modelo detecta un pico 
        System.out.println("DEBUG ECG: Pico detectado por el Modelo en time=" + time + ", value=" + peakValue + ". Llamando a Beep.play().");
        Beep.play();

    }


    /**
     * Método de la interfaz SerialDataListener llamado cuando ocurre un error serial o un mensaje de estado.
     * (Mantener este método, pero ajustar la llamada a showStatusOverlay)
     * @param message El mensaje de error.
     */
    @Override
    public void onErrorOccurred(String message) {
        Platform.runLater(() -> {
            System.err.println("Error serial en UI (via Platform.runLater): " + message);

            // --- Reiniciar el contador de datos validos consecutivos en cualquier error o mensaje de estado ---
           consecutiveValidDataCount = 0; // <-- Asegurate de tener esta linea aqui
           System.out.println("EcgMonitorController: Contador de datos validos reiniciado.");
            // --- Fin Reiniciar contador ---


   
            if(message.startsWith("STATUS:")){
                String statusType = message.substring("STATUS:".length());

                if(statusType.contains("ELECTRODES_DISCONNECTED")){
                    showStatusOverlay("SE HAN DESCONECTADO LOS ELECTRODOS"); 
                }
                else if(statusType.equals("ELECTRODES_CONNECTED")){
                    System.out.println("Se han vuelto a conectar los electrodos");

                }
                else {
                    System.out.println("Error desconocido");

                }
            } else {

                System.err.println("Error serial general: " + message);

            }

        });
    }


    /**
     * Muestra el overlay de estado (ej. desconexion de electrodos) con animacion de fade-in.
     * (Ajustar para usar el UNICO TextArea de instrucciones y la constante)
     * @param title El titulo a mostrar en el overlay.
     */
    private void showStatusOverlay(String title) { // <-- AJUSTAR FIRMA: Ahora solo recibe el titulo
        if (electrodesDisconnectedOverlay != null) {

            if (overlayTitle != null) overlayTitle.setText(title); // Actualizar el titulo


            if (overlayInstructionsPart1 != null) {
               overlayInstructionsPart1.setText(FULL_INSTRUCTIONS_TEXT); 
            } else {
               System.err.println("EcgMonitorController: ADVERTENCIA: overlayInstructionsPart1 es null. No se puede setear el texto de instrucciones.");
            }



            electrodesDisconnectedOverlay.setVisible(true); // Hacer visible el nodo
            electrodesDisconnectedOverlay.setManaged(true); // Asegurar que ocupa espacio en el layout

     
            FadeTransition fadeIn = new FadeTransition(Duration.millis(100), electrodesDisconnectedOverlay); // Duracion ajustada a 500ms como en discusion anterior
            fadeIn.setFromValue(electrodesDisconnectedOverlay.getOpacity());
            fadeIn.setToValue(1.0);
            fadeIn.play();

            System.out.println("EcgMonitorController: Mostrando overlay de estado con fade-in.");
        } else {
             System.err.println("EcgMonitorController: ERROR: No se pudo mostrar el overlay porque el nodo 'electrodesDisconnectedOverlay' es null.");
        }
    }

    /**
     * Oculta el overlay de estado con animacion de fade-out.
     * (Mantener este método como está)
     */
    private void hideStatusOverlay() {

         if (electrodesDisconnectedOverlay != null && electrodesDisconnectedOverlay.isVisible()) {


             FadeTransition fadeOut = new FadeTransition(Duration.millis(100), electrodesDisconnectedOverlay); 
             fadeOut.setFromValue(electrodesDisconnectedOverlay.getOpacity()); 
             fadeOut.setToValue(0.0); 


             fadeOut.setOnFinished(event -> {

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
        } else { // Añadido mensaje si serialDataManager es null
             System.out.println("EcgMonitorController: SerialDataManager es null, no se necesita detener.");
        }
        Beep.shutdown();
        System.out.println("EcgMonitorController: Sistema Beep (AudioCue) apagado.");

        System.out.println("EcgMonitorController: Shutdown completo.");
    }
}