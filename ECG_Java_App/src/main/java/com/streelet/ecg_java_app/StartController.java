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

         System.out.println("StartController: Inicialización completa.");
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
        if (serialPortComboBox != null) {
            SerialPort[] availablePorts = SerialPort.getCommPorts();
            ObservableList<String> portNames = FXCollections.observableArrayList();
            if (availablePorts.length > 0) {
                for (SerialPort port : availablePorts) {
                    portNames.add(port.getSystemPortName());
                }
                serialPortComboBox.setItems(portNames);
                if (!portNames.isEmpty()) {
                    serialPortComboBox.getSelectionModel().selectFirst(); // Seleccionar el primer puerto por defecto
                }
            } else {
                 // Mostrar un mensaje si no hay puertos disponibles
                 serialPortComboBox.setPromptText("No hay puertos disponibles");
                 serialPortComboBox.setDisable(true); // Deshabilitar el ComboBox
                 if (startButton != null) startButton.setDisable(true); // Deshabilitar boton si no se puede iniciar serial
                 System.err.println("StartController: No se encontraron puertos seriales disponibles.");
            }
        } else {
            System.err.println("StartController: ERROR: serialPortComboBox es null. Asegúrate de que fx:id=\"serialPortComboBox\" está en el FXML.");
        }
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

        // 2. Recopilar el puerto serial seleccionado
        String selectedPort = (serialPortComboBox != null && serialPortComboBox.getSelectionModel().getSelectedItem() != null) ?
                              serialPortComboBox.getSelectionModel().getSelectedItem() : null;

        // Validar que se selecciono un puerto (es crucial)
        if (selectedPort == null || selectedPort.isEmpty()) {
            System.err.println("StartController: No se selecciono un puerto serial.");
            // Opcional: Mostrar un mensaje de error al usuario en la UI
            return; // No continuar si no hay puerto
        }

        // 3. Empaquetar los datos (puedes crear una pequeña clase para esto si son muchos)
        // Crearemos una clase simple PatientData temporalmente
        PatientData patientData = new PatientData(patientName, gender, birthDate, medicalHistory);

        // 4. Notificar a la App para que cambie de escena y pase los datos
        if (startMonitoringListener != null) {
            startMonitoringListener.onStartMonitoring(patientData, selectedPort);
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

         // Recopilar el puerto serial seleccionado (aun es necesario)
         String selectedPort = (serialPortComboBox != null && serialPortComboBox.getSelectionModel().getSelectedItem() != null) ?
                               serialPortComboBox.getSelectionModel().getSelectedItem() : null;

         if (selectedPort == null || selectedPort.isEmpty()) {
             System.err.println("StartController: No se selecciono un puerto serial para inicio sin datos.");
             return;
         }

         // Notificar a la App (si el listener esta configurado)
         if (startMonitoringListener != null) {
             startMonitoringListener.onStartMonitoring(patientData, selectedPort);
         } else {
             System.err.println("StartController: startMonitoringListener no esta configurado para inicio sin datos.");
         }
    }

    @FXML
    private void handleExitButton(ActionEvent event) {
        System.out.println("StartController: Boton 'Salir' clickeado. Cerrando aplicacion.");
        // Logica para cerrar la aplicacion
        // Platform.exit(); // Si estas en App.java y tienes acceso al Stage, puedes cerrarlo.
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
        void onStartMonitoring(PatientData patientData, String portName);
        void onExit(); // Para notificar a App que cierre
    }

    // Clase simple para empaquetar los datos del paciente
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

    // Opcional: Metodo cleanup si es necesario (ej. detener el timeline del reloj)
     public void shutdown() {
         // if (timeline != null) timeline.stop(); // Si timeline fuera un campo de clase
         System.out.println("StartController: Metodo shutdown() llamado.");
     }
}