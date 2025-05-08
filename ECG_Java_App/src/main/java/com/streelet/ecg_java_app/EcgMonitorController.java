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

import com.streelet.ecg_java_app.StartController.PatientData; // Importar PatientData (Necesario si la clase esta en StartController)


import java.net.URL; // Mantener
import java.time.LocalTime; // Mantener si se usa
import java.time.format.DateTimeFormatter; // Mantener si se usa
import java.util.ResourceBundle; // Mantener


// El Controlador implementa SerialDataListener y EcgPeakListener
// Implementa Initializable porque tiene un metodo initialize() que es llamado por FXMLLoader
public class EcgMonitorController implements Initializable, SerialDataListener, EcgPeakListener { // Added Initializable

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
    private Label overlayTitle; // Etiqueta del titulo en el overlay
    // Eliminamos overlayMessage (Label) ya que el overlay diseñado usa un TextArea para las instrucciones
    // @FXML private Label overlayMessage; // <-- ELIMINAR ESTE CAMPO

    // Campo @FXML para el UNICO TextArea de instrucciones en el overlay
    // Asegurate que el fx:id en ElectrodesDisconnectedOverlay.fxml sea "overlayInstructionsPart1"
    @FXML
    private TextArea overlayInstructionsPart1; // <-- CAMBIADO/AÑADIDO para coincidir con FXML diseñado
    // --- Fin Campos @FXML para el overlay ---


    // --- Campos para almacenar los datos del paciente y el nombre del puerto recibidos de la pantalla de inicio (AÑADIR) ---
    private PatientData patientData; // <-- AÑADIDO: Campo para datos del paciente
    private String serialPortName; // <-- AÑADIDO: Campo para nombre del puerto
    // --- Fin Campos recibidos ---


    // Constante con el texto completo de las instrucciones (Ya la tenias, solo la renombramos por claridad si quieres)
    private static final String FULL_INSTRUCTIONS_TEXT = // <-- RENOMBRADA (o manten 'instructions' si prefieres)
        "La monitorización del ECG se ha detenido.\n\n" +
        "Instrucciones:\n\n" +
        "• Asegúrese de que los electrodos estén bien adheridos a la piel.\n\n\n" +
        "• Verifique que los cables estén conectados firmemente \n" +
        "   a los electrodos y al dispositivo.\n\n\n" +
        "• Pida al paciente que se mantenga quieto si es posible.";
        // NOTA: Asegurate que este texto coincide con la version final que quieres en el overlay.
        // La version anterior que discutimos era un poco mas detallada. Usa la que prefieras.


    private SerialDataManager serialDataManager; // Ya existe


    private XYChart.Series<Number, Number> ecgSeries; // Ya existe


    private long time = 0; // Ya existe
    private final int WINDOW_SIZE = 250; // Ya existe


    private EcgDataModel ecgDataModel; // Ya existe


    private int consecutiveValidDataCount = 0; // Ya existe
    private final int minConsecutiveValidData = 10; // Ya existe


    /**
     * Método de inicialización del controlador JavaFX.
     * Se llama automáticamente después de cargar el archivo FXML.
     * IMPORTANTE: Este metodo NO iniciará la comunicación serial. Solo prepara la UI.
     */
    @FXML
    // Implementa Initializable si no estaba implementado en la firma de la clase
    public void initialize(URL url, ResourceBundle resourceBundle) { // Añadidos parametros aunque no se usen si implementa Initializable
        System.out.println("EcgMonitorController: Inicialización de UI completada."); // Mensaje ajustado

        // ... Tu codigo de inicializacion existente (configurar grafica, modelo, AudioCue) ...
        // Mantener todo el codigo de setup de la UI, grafica, modelo, Beep, etc.

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


        // --- ELIMINAR: La inicializacion del SerialDataManager y la llamada a handleConnectButton YA NO van AQUI ---
        // serialDataManager = new SerialDataManager(); // <-- ELIMINAR ESTA LINEA
        // serialDataManager.addListener(this); // <-- ELIMINAR ESTA LINEA

        // Llama al método handleConnectButton() directamente para iniciar la lectura serial <-- ESTO TAMBIEN SE ELIMINA
        // handleConnectButton(); // <-- ELIMINAR ESTA LINEA


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

    // --- Metodo publico para recibir los datos y el puerto (AÑADIR) ---
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

        // Opcional: Si tienes un Label en tu UI para mostrar el puerto en uso, actualizalo aqui.
        // Necesitas añadir un Label con un fx:id (ej. portInUseLabel) en tu monitor.fxml
        // @FXML private Label portInUseLabel;
        // if (portInUseLabel != null) { portInUseLabel.setText("Puerto: " + portName); }
    }
    // --- Fin Metodo publico para recibir datos ---


    // --- Metodo publico para iniciar la monitorizacion serial (AÑADIR) ---
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

         // *** Ahora SI, crea e inicia el SerialDataManager AQUI ***
         // Mover la logica de tu handleConnectButton a este metodo
         serialDataManager = new SerialDataManager(); // Crea la instancia (Si tu constructor no necesita puerto)
         serialDataManager.addListener(this); // Registra este controlador como listener

         int baudRate = 9600; // Define la tasa de baudios (Puedes pasarla desde la pantalla de inicio si la recopilas)

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


    // --- ELIMINAR los metodos handleConnectButton y handleDisconnectButton (ya no se llaman desde FXML) ---
    // Estos metodos fueron llamados desde botones en FXML en tu diseño original, pero ahora
    // el inicio y fin de la monitorizacion se controlan desde App -> StartController -> App -> EcgMonitorController

    // @FXML
    // private void handleConnectButton() { ... } // <-- ELIMINAR ESTE METODO

    // @FXML
    // private void handleDisconnectButton() { ... } // <-- ELIMINAR ESTE METODO (La desconexion se hace en shutdown)
    // --- Fin Eliminar metodos ---


    /**
     * Método de la interfaz SerialDataListener llamado cuando se recibe un nuevo punto de dato.
     * (Mantener este método como está, pero asegurar que la lógica de conteo y ocultar overlay funcione)
     * @param value El valor entero recibido del puerto serial (el punto de dato del ECG).
     */
    @Override
    public void onDataReceived(int value) {


        // Este metodo se ejecuta en un HILO SECUNDARIO (el hilo de lectura serial).
        // Para actualizar la UI o interactuar con el Modelo (si su lógica no es thread-safe)
        // en el hilo de JavaFX, DEBES usar Platform.runLater.

        Platform.runLater(() -> {


            // --- Incrementar el contador de datos validos consecutivos ---
        // Este metodo solo se llama cuando llega un dato numerico VALIDO, asi que incrementamos.
           consecutiveValidDataCount++; // <-- Asegurate de tener esta linea aqui
           // System.out.println("EcgMonitorController: Dato valido recibido. Contador: " + consecutiveValidDataCount); // Debug opcional
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
            // --- Fin Verificar y Ocultar overlay ---


            // --- Logica existente para añadir datos a la grafica, procesar con el modelo, y desplazar el eje X ---
            // Mantener todo este bloque
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
            // --- Fin Logica existente ---

            // Para el Heart Rate (Mantener este bloque)
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

        // Al terminar la pausa, establecer la opacidad a 0
        // La transición CSS hará que este cambio de 1.0 a 0.0 sea animado (el desvanecimiento)
        fadeOut.setOnFinished(event -> {
            beatCircle.setOpacity(0.0);
        });

        // Iniciar la pausa y, por lo tanto, la animación de desvanecimiento después
        fadeOut.play();
    }

        // Acción a realizar cuando el Modelo detecta un pico (Mantener esto)
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


            // --- Verificar si el mensaje es un estado específico (usando startsWith) ---
            if(message.startsWith("STATUS:")){
                String statusType = message.substring("STATUS:".length());

                if(statusType.contains("ELECTRODES_DISCONNECTED")){
                    // Llama al metodo showStatusOverlay para mostrar el overlay de desconexion
                    // Pasa solo el titulo; el texto completo de instrucciones se toma de la constante dentro de showStatusOverlay
                    showStatusOverlay("SE HAN DESCONECTADO LOS ELECTRODOS"); // <-- AJUSTAR LLAMADA: Pasa solo el titulo
                }
                else if(statusType.equals("ELECTRODES_CONNECTED")){
                    System.out.println("Se han vuelto a conectar los electrodos");
                    // Opcional: Si quieres hacer algo visual al reconectar, hazlo aqui.
                    // El overlay se oculta automaticamente en onDataReceived si empiezan a llegar datos validos.
                }
                else {
                    System.out.println("Error desconocido");
                    // Opcional: Mostrar otros mensajes de estado si los hay
                    // showStatusOverlay("Estado Desconocido", message); // Ejemplo
                }
            } else {
                // Manejo de otros errores generales que no son mensajes de estado STATUS:
                System.err.println("Error serial general: " + message);
                // Opcional: Mostrar un overlay de error general si lo necesitas
                // showStatusOverlay("ERROR SERIAL", message); // Ejemplo
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

            // --- Actualizar el texto del UNICO TextArea de instrucciones ---
            // Asegurarse que overlayInstructionsPart1 no sea null y usar la constante
            if (overlayInstructionsPart1 != null) {
               overlayInstructionsPart1.setText(FULL_INSTRUCTIONS_TEXT); // <-- Usar la constante
               overlayInstructionsPart1.setScrollTop(0); // Opcional: asegurar que el texto inicia arriba
               // System.out.println("DEBUG: Texto de overlayInstructionsPart1 seteado."); // Debug opcional
               // Las lineas comentadas originales usaban 'instructions', ahora usamos 'FULL_INSTRUCTIONS_TEXT'
               //  overlayInstructions.setText(instructions); // <-- ELIMINAR/IGNORAR LINEA ORIGINAL
               //     System.out.println(overlayInstructions.getText()); // <-- ELIMINAR/IGNORAR LINEA ORIGINAL
            } else {
               System.err.println("EcgMonitorController: ADVERTENCIA: overlayInstructionsPart1 es null. No se puede setear el texto de instrucciones.");
            }
            // --- Fin actualizar texto de instrucciones ---


            electrodesDisconnectedOverlay.setVisible(true); // Hacer visible el nodo
            electrodesDisconnectedOverlay.setManaged(true); // Asegurar que ocupa espacio en el layout

            // Animar la opacidad del overlay desde su valor actual hasta 1.0 (completamente opaco)
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
         // Solo intentar ocultar si el overlay existe y esta visible (para evitar animar algo ya invisible)
         if (electrodesDisconnectedOverlay != null && electrodesDisconnectedOverlay.isVisible()) {

             // Crear y reproducir la animacion de desaparicion (fade-out)
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
        } else { // Añadido mensaje si serialDataManager es null
             System.out.println("EcgMonitorController: SerialDataManager es null, no se necesita detener.");
        }
        Beep.shutdown();
        System.out.println("EcgMonitorController: Sistema Beep (AudioCue) apagado.");

        System.out.println("EcgMonitorController: Shutdown completo.");
    }
}