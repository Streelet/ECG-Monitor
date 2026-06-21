/*
Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/

package com.streelet.ecg_java_app;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections; // Importar para ObservableList
import javafx.collections.ObservableList; // Importar para ObservableList
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button; // Importar Button
import javafx.scene.control.ComboBox; // Importar ComboBox
import javafx.scene.control.DatePicker; // Importar DatePicker
import javafx.scene.control.Label;
import javafx.scene.control.TextArea; // Importar TextArea
import javafx.scene.control.TextField; // Importar TextField
import javafx.event.ActionEvent; // Importar ActionEvent
import javafx.util.Duration;

import com.fazecast.jSerialComm.SerialPort; // Importar jSerialComm

import java.net.URL;
import java.time.LocalDate; // Importar LocalDate
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controlador para la pantalla de inicio.
 */
public class StartController implements Initializable {

    // Campos @FXML para el reloj (ya existian)
    @FXML
    private Label clockLabel;
    @FXML
    private Label amPmLabel;

    // --- Campos @FXML para los controles de entrada de datos y seleccion de puerto ---
    @FXML
    private TextField patientNameTextBox;

    @FXML
    private javafx.scene.control.ChoiceBox<String> genderComboBox; // O ComboBox<String> si cambiaste el FXML

    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private TextArea medicalHistoryArea;
    // Campo @FXML para el ComboBox del puerto serial (asegurate de añadir fx:id="serialPortComboBox" en FXML)
    @FXML
    private ComboBox<String> serialPortComboBox; // Debe coincidir con fx:id="serialPortComboBox" en FXML

    // ChoiceBox para elegir el modo de operacion (Mock vs IoT)
    @FXML
    private javafx.scene.control.ChoiceBox<String> modeChoiceBox; // fx:id="modeChoiceBox" en FXML

    // Etiquetas visibles para cada modo en el ChoiceBox
    private static final String MODE_MOCK_LABEL = "Simulación (Mock)";
    private static final String MODE_IOT_LABEL = "IoT (Serial)";

    @FXML
    private Button startButton; // Boton "Iniciar Monitor ECG"
    @FXML
    private Button startWithoutDataButton; // Boton "Iniciar Sin Datos"
    @FXML
    private Button exitButton; // Boton "Salir"
    // --- Fin Campos @FXML de entrada ---


    private DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm");
    private DateTimeFormatter amPmFormat = DateTimeFormatter.ofPattern("a");

    // Interfaz para comunicar a App que inicie el monitor con los datos
    private OnStartMonitoringListener startMonitoringListener;


    /**
     * Metodo de inicializacion.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock(); // Inicia el reloj

        // --- Inicializar el ComboBox de puertos seriales ---
        populateSerialPortComboBox();
        // --- Fin inicializacion ComboBox ---

        // --- Configurar manejadores de eventos para los botones ---
        if (startButton != null) {
            startButton.setOnAction(this::handleStartButton);
        }
        if (startWithoutDataButton != null) {
             startWithoutDataButton.setOnAction(this::handleStartWithoutDataButton);
        }
        if (exitButton != null) {
            exitButton.setOnAction(this::handleExitButton);
        }
        
         if (genderComboBox != null) {
            ObservableList<String> genders = FXCollections.observableArrayList("Masculino", "Femenino");
            genderComboBox.setItems(genders);
        }

         // --- Inicializar el ChoiceBox de modo de operacion ---
         if (modeChoiceBox != null) {
             modeChoiceBox.setItems(FXCollections.observableArrayList(MODE_MOCK_LABEL, MODE_IOT_LABEL));
             // Por defecto se selecciona Mock para poder probar sin hardware conectado
             modeChoiceBox.getSelectionModel().select(MODE_MOCK_LABEL);
             modeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                     (obs, oldVal, newVal) -> applyModeUi());
         }
         // Ajustar el estado inicial de la UI segun el modo seleccionado
         applyModeUi();

         System.out.println("StartController: Inicialización completa.");
    }

    /**
     * Devuelve el modo de operacion seleccionado actualmente en el ChoiceBox.
     * Por defecto (sin seleccion) asume modo Mock.
     */
    private MonitorMode getSelectedMode() {
        if (modeChoiceBox != null && MODE_IOT_LABEL.equals(modeChoiceBox.getSelectionModel().getSelectedItem())) {
            return MonitorMode.IOT;
        }
        return MonitorMode.MOCK;
    }

    /**
     * Habilita o deshabilita los controles segun el modo elegido.
     * En modo Mock no se requiere puerto serial, por lo que el selector de
     * puerto se deshabilita y los botones de inicio quedan siempre disponibles.
     * En modo IoT los botones solo se habilitan si hay puertos disponibles.
     */
    private void applyModeUi() {
        boolean iot = (getSelectedMode() == MonitorMode.IOT);
        boolean portsAvailable = serialPortComboBox != null && !serialPortComboBox.getItems().isEmpty();

        if (serialPortComboBox != null) {
            serialPortComboBox.setDisable(!iot || !portsAvailable);
        }

        boolean canStart = !iot || portsAvailable; // Mock siempre puede iniciar
        if (startButton != null) startButton.setDisable(!canStart);
        if (startWithoutDataButton != null) startWithoutDataButton.setDisable(!canStart);
    }

    /**
     * Inicia el reloj digital.
     */
    private void startClock() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    LocalTime now = LocalTime.now();
                    clockLabel.setText(now.format(timeFormat));
                    amPmLabel.setText(
                            (
                                    now.format(amPmFormat).replace(".","")
                                    ).toUpperCase()
                    );
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Popula el ComboBox con la lista de puertos seriales disponibles.
     */
    private void populateSerialPortComboBox() {
        if (serialPortComboBox == null) {
            System.err.println("StartController: ERROR: serialPortComboBox es null. Asegúrate de que fx:id=\"serialPortComboBox\" está en el FXML.");
            return;
        }

        SerialPort[] availablePorts = SerialPort.getCommPorts();
        ObservableList<String> portNames = FXCollections.observableArrayList();
        for (SerialPort port : availablePorts) {
            portNames.add(port.getSystemPortName());
        }
        serialPortComboBox.setItems(portNames);

        if (!portNames.isEmpty()) {
            serialPortComboBox.getSelectionModel().selectFirst(); // Seleccionar el primer puerto por defecto
        } else {
            serialPortComboBox.setPromptText("No hay puertos disponibles");
            System.err.println("StartController: No se encontraron puertos seriales disponibles.");
        }
        // El habilitado/deshabilitado se decide en applyModeUi() segun el modo activo.
    }


    // --- Manejadores de eventos para los botones ---

    @FXML
    private void handleStartButton(ActionEvent event) {
        System.out.println("StartController: Boton 'Iniciar Monitor ECG' clickeado.");

        // 1. Recopilar datos de los campos de entrada
        String patientName = patientNameTextBox != null ? patientNameTextBox.getText() : "";
        // Obtener valor de ChoiceBox (si esta inicializado)
        String gender = (genderComboBox != null && genderComboBox.getSelectionModel().getSelectedItem() != null) ?
                        genderComboBox.getSelectionModel().getSelectedItem() : "";
        // Obtener valor de DatePicker (si esta inicializado)
        LocalDate birthDate = birthDatePicker != null ? birthDatePicker.getValue() : null;
        String medicalHistory = medicalHistoryArea != null ? medicalHistoryArea.getText() : "";

        String selectedPort = (serialPortComboBox != null && serialPortComboBox.getSelectionModel().getSelectedItem() != null) ?
                              serialPortComboBox.getSelectionModel().getSelectedItem() : null;

        MonitorMode mode = getSelectedMode();

        // En modo IoT es obligatorio seleccionar un puerto; en Mock no se requiere
        if (mode == MonitorMode.IOT && (selectedPort == null || selectedPort.isEmpty())) {
            System.err.println("StartController: No se selecciono un puerto serial (requerido en modo IoT).");
            return; // No continuar si no hay puerto en modo IoT
        }

        PatientData patientData = new PatientData(patientName, gender, birthDate, medicalHistory);

        // 4. Notificar a la App para que cambie de escena y pase los datos
        if (startMonitoringListener != null) {
            startMonitoringListener.onStartMonitoring(patientData, selectedPort, mode);
        } else {
            System.err.println("StartController: startMonitoringListener no esta configurado.");
            // Opcional: Iniciar sin datos si el listener no esta listo?
        }
    }

    @FXML
    private void handleStartWithoutDataButton(ActionEvent event) {
         System.out.println("StartController: Boton 'Iniciar Sin Datos' clickeado.");

         // Opcion: Pasar datos de paciente vacios/nulos
         PatientData patientData = new PatientData("", "", null, ""); // Datos vacios

         // Recopilar el puerto serial seleccionado
         String selectedPort = (serialPortComboBox != null && serialPortComboBox.getSelectionModel().getSelectedItem() != null) ?
                               serialPortComboBox.getSelectionModel().getSelectedItem() : null;

         MonitorMode mode = getSelectedMode();

         // En modo IoT es obligatorio seleccionar un puerto; en Mock no se requiere
         if (mode == MonitorMode.IOT && (selectedPort == null || selectedPort.isEmpty())) {
             System.err.println("StartController: No se selecciono un puerto serial para inicio sin datos (requerido en modo IoT).");
             return;
         }

         // Notificar a la App (si el listener esta configurado)
         if (startMonitoringListener != null) {
             startMonitoringListener.onStartMonitoring(patientData, selectedPort, mode);
         } else {
             System.err.println("StartController: startMonitoringListener no esta configurado para inicio sin datos.");
         }
    }

    @FXML
    private void handleExitButton(ActionEvent event) {
        System.out.println("StartController: Boton 'Salir' clickeado. Cerrando aplicacion.");
        // Logica para cerrar la aplicacion
        // Alternativamente, puedes notificar a App para que cierre.
         if (startMonitoringListener != null) {
             startMonitoringListener.onExit(); // Notificar a App para cerrar
         }
    }
    // --- Fin manejadores de eventos ---

    /**
     * Metodo para establecer el listener que notifica a la App.
     * @param listener El listener (normalmente la instancia de App).
     */
    public void setOnStartMonitoringListener(OnStartMonitoringListener listener) {
        this.startMonitoringListener = listener;
    }

    // Interfaz para definir el contrato de comunicacion con la App principal
    public interface OnStartMonitoringListener {
        void onStartMonitoring(PatientData patientData, String portName, MonitorMode mode);
        void onExit(); // Para notificar a App que cierre
    }


    public static class PatientData {
        private final String name;
        private final String gender;
        private final LocalDate birthDate;
        private final String medicalHistory;

        public PatientData(String name, String gender, LocalDate birthDate, String medicalHistory) {
            this.name = name;
            this.gender = gender;
            this.birthDate = birthDate;
            this.medicalHistory = medicalHistory;
        }

        // Getters
        public String getName() { return name; }
        public String getGender() { return gender; }
        public LocalDate getBirthDate() { return birthDate; }
        public String getMedicalHistory() { return medicalHistory; }

        // Opcional: Metodo para generar un resumen de los datos
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Paciente: ").append(name != null && !name.isEmpty() ? name : "No Especificado").append("\n");
            summary.append("Género: ").append(gender != null && !gender.isEmpty() ? gender : "No Especificado").append("\n");
            summary.append("Fecha Nacimiento: ").append(birthDate != null ? birthDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "No Especificada").append("\n");
            summary.append("Historial Médico:\n").append(medicalHistory != null && !medicalHistory.isEmpty() ? medicalHistory : "No Proporcionado");
            return summary.toString();
        }
    }

     public void shutdown() {
         // if (timeline != null) timeline.stop(); // Si timeline fuera un campo de clase
         System.out.println("StartController: Metodo shutdown() llamado.");
     }
}