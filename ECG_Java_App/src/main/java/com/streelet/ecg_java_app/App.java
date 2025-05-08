package com.streelet.ecg_java_app;

import javafx.application.Application;
import javafx.application.Platform; 
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.streelet.ecg_java_app.StartController.OnStartMonitoringListener; // Import the listener interface
import com.streelet.ecg_java_app.StartController.PatientData; // Import the PatientData class

import java.io.IOException;
import java.net.URL; // Import URL

// Implement the listener interface to receive signals from StartController
public class App extends Application implements OnStartMonitoringListener {

    private Stage primaryStage; // Reference to the primary Stage
    // We don't strictly need Scene fields here, as we can get/set scenes on the stage

    // References to the controllers for cleanup
    private StartController startController;
    private EcgMonitorController ecgMonitorController; // Changed field name for consistency


    @Override
    public void start(Stage stage) { // Removed throws IOException here, handle it inside
        System.out.println("App: Iniciando aplicación JavaFX.");
        this.primaryStage = stage; // Store the reference to the primary Stage

        // --- Cargar y mostrar la pantalla de inicio primero ---
        loadAndShowStartScreen();
        // --- Fin cargar pantalla de inicio ---

        primaryStage.setTitle("ECG Monitor App"); // Initial window title
        primaryStage.show(); // Show the window

        System.out.println("App: Aplicación JavaFX iniciada.");
    }

    /**
     * Loads and shows the initial start screen.
     */
    private void loadAndShowStartScreen() {
        try {
            // Use the correct FXML file name for your start screen (e.g., start_screen.fxml)
            // Ensure this FXML file is in the correct resource path
            System.out.println("App: Cargando start_screen.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/streelet/ecg_java_app/design.fxml"));
            Parent startScreenRoot = loader.load();

            startController = loader.getController(); // Get the controller for the start screen
            if (startController != null) {
                // Set this App instance as the listener for the StartController
                startController.setOnStartMonitoringListener(this); // 'this' because App implements the interface
                System.out.println("App: StartController obtenido y listener configurado.");
            } else {
                 System.err.println("App: ERROR: No se pudo obtener el StartController. ¿Está fx:controller definido en el FXML de inicio?");
                 // Consider handling this error more robustly, e.g., showing an alert and exiting
                 Platform.exit(); // Exit if the start screen controller cannot be obtained
                 return; // Exit the method
            }

            Scene startScene = new Scene(startScreenRoot, primaryStage.getWidth(), primaryStage.getHeight()); // Use current stage size
           
             try {
           
              URL startCssUrl = getClass().getResource("/com/streelet/ecg_java_app/design.css");
              if (startCssUrl != null) {
                     startScene.getStylesheets().add(startCssUrl.toExternalForm());
                     System.out.println("App: CSS para pantalla de inicio cargado.");
                 }
             } catch (Exception e) {
                 System.err.println("App: Error al cargar CSS para pantalla de inicio."); e.printStackTrace();
            }


            primaryStage.setScene(startScene); // Set the start screen as the current scene

        } catch (IOException e) {
            System.err.println("App: ¡ERROR FATAL! No se pudo cargar el archivo FXML de inicio: start_screen.fxml");
            e.printStackTrace(); // Print the full stack trace
            // Handle the error, e.g., show an alert and close the application gracefully
            Platform.exit(); // Exit the application if the start screen cannot be loaded
        }
    }

    /**
     * Implementation of the OnStartMonitoringListener interface method.
     * Called by StartController when the "Start Monitoring" button is clicked.
     * Loads the monitor scene, passes the data, and switches the scene.
     * @param patientData The collected patient data.
     * @param portName The selected serial port name.
     */
    @Override
    public void onStartMonitoring(PatientData patientData, String portName) {
        System.out.println("App: Recibida señal para iniciar monitor. Puerto: " + portName);
        System.out.println("App: Cargando monitor.fxml y pasando datos...");

        // Clean up the start controller if necessary (e.g., stop clock timeline)
        if (startController != null) {
             startController.shutdown(); // Call a cleanup method in StartController if you implemented it
             startController = null; // Release reference if not needed anymore
             System.out.println("App: StartController cleanup ejecutado.");
        }

        // --- Cargar y mostrar la pantalla del monitor ---
        try {
            // Ensure the monitor.fxml file is in the correct resource path
            FXMLLoader loader = new FXMLLoader(getClass().getResource("monitor.fxml")); // Load the monitor FXML
            Parent monitorRoot = loader.load(); // This loads FXML, creates EcgMonitorController, and injects @FXMLs
            
            // IMPORTANT: Get the controller AFTER loader.load()
            ecgMonitorController = loader.getController(); // Get the controller for the monitor scene
            if (ecgMonitorController != null) {
                System.out.println("App: EcgMonitorController obtenido.");

                // *** IMPORTANT: Pass the data and port to the monitor controller BEFORE starting serial ***
                // Make sure EcgMonitorController has a public method like setPatientDataAndPort(patientData, portName)
                ecgMonitorController.setPatientDataAndPort(patientData, portName);
                System.out.println("App: Datos de paciente y puerto pasados al EcgMonitorController.");

                // Now that data and port are set, tell the monitor controller to start monitoring
                // Make sure EcgMonitorController has a public startMonitoring() method
                // that initiates the serial communication and graph updates.
                // EcgMonitorController.initialize() should NOT start serial automatically anymore.
                ecgMonitorController.startMonitoring(); // <-- Start serial monitoring here

                System.out.println("App: Monitorización iniciada en EcgMonitorController.");

            } else {
                System.err.println("App: ERROR: No se pudo obtener el EcgMonitorController. ¿Está fx:controller definido en monitor.fxml?");
                // Handle error, perhaps show an alert and go back to start screen or exit
                loadAndShowStartScreen(); // Example: go back to start screen on monitor load error if its controller is null
                return; // Exit the method
            }

            // Create the monitor scene using the loaded root and current stage dimensions
            Scene monitorScene = new Scene(monitorRoot, primaryStage.getWidth(), primaryStage.getHeight()); // Use current window size

            // --- Load CSS for the monitor scene ---
            try {
                // Make sure the CSS path is correct relative to your resources root
                URL monitorCssUrl = getClass().getResource("/com/streelet/ecg_java_app/monitor.css"); // Correct CSS file name and path
                 if (monitorCssUrl != null) {
                     String cssPath = monitorCssUrl.toExternalForm();
                     monitorScene.getStylesheets().add(cssPath);
                     System.out.println("App: CSS para pantalla de monitor cargado exitosamente: " + cssPath);
                 } else {
                      System.err.println("App: Advertencia: No se encontró el archivo ecg-style.css en la ruta de recursos especificada.");
                 }
            } catch (Exception e) {
                System.err.println("App: Error al cargar ecg-style.css."); e.printStackTrace();
            }
            // --- End Load CSS ---


            primaryStage.setScene(monitorScene); // Switch to the monitor scene
            primaryStage.setTitle("ECG Monitor"); // Change the window title

            System.out.println("App: Pantalla del monitor cargada y mostrada con datos.");

        } catch (IOException e) {
            System.err.println("App: ¡ERROR FATAL! No se pudo cargar el archivo FXML del monitor: monitor.fxml");
            e.printStackTrace();
            // Handle the error, e.g., show an alert and go back to start screen or exit
            loadAndShowStartScreen(); // Example: go back to start screen on monitor load error
        }
    }

    /**
     * Implementation of the OnStartMonitoringListener interface method for exiting.
     * Called by StartController when the "Exit" button is clicked.
     */
    @Override
    public void onExit() {
        System.out.println("App: Recibida señal para cerrar aplicacion.");
        Platform.exit(); // Use Platform.exit() for a clean shutdown
    }


    @Override
    public void stop() throws Exception {
        System.out.println("App: Método stop() llamado al cerrar la aplicación. Ejecutando limpieza...");
        // Call the shutdown() method of the currently active controller if it exists
        // Check the monitor controller first, as it manages the serial connection
        if (ecgMonitorController != null) {
            ecgMonitorController.shutdown();
            System.out.println("App: Limpieza del EcgMonitorController finalizada.");
        } else if (startController != null) {
             // If monitor controller was never created, maybe the start controller needs cleanup
             startController.shutdown(); // Call cleanup if implemented in StartController
             System.out.println("App: Limpieza del StartController finalizada.");
        } else {
             System.out.println("App: No hay controladores activos para limpiar.");
        }
        super.stop(); // Call the superclass stop method
    }

    public static void main(String[] args) {
        launch(args);
    }
}