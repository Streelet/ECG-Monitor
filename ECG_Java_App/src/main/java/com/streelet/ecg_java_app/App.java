package com.streelet.ecg_java_app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL; // Importar URL

public class App extends Application {

    private static Scene scene;
    private EcgMonitorController ecgController;

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("App: Iniciando aplicación JavaFX. Cargando monitor.fxml...");

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/streelet/ecg_java_app/monitor.fxml"));

        Parent root = null;
        try {
            root = fxmlLoader.load();
            ecgController = fxmlLoader.getController();
            if (ecgController != null) {
                System.out.println("App: Controlador obtenido. Su método initialize() se está ejecutando o ya terminó.");
            } else {
                 System.err.println("App: Advertencia: No se pudo obtener el controlador. ¿Está fx:controller definido en monitor.fxml?");
            }

        } catch (IOException e) {
             System.err.println("App: ¡ERROR FATAL! No se pudo cargar el archivo FXML: monitor.fxml");
             e.printStackTrace();
             throw e;
        }

        scene = new Scene(root, 1020, 614); // Usar las dimensiones del BorderPane de tu FXML si quieres que inicie con ese tamaño
        stage.setTitle("Monitor ECG"); // Título más descriptivo
        stage.setScene(scene);

        System.out.println("App: Ventana mostrada. Si el controlador está bien configurado, la gráfica empezará a mostrar datos.");


        // --- Sección para Cargar el CSS (Descomentada y Corregida) ---
        try {
            // La ruta debe ser relativa a la carpeta de recursos del classpath,
            // coincidiendo con la estructura de paquetes.
            // Si ecg-style.css está en src/main/resources/com/streelet/ecg_java_app/,
            // la ruta del recurso es "/com/streelet/ecg_java_app/ecg-style.css".
            URL cssUrl = getClass().getResource("/com/streelet/ecg_java_app/monitor.css");

            if (cssUrl != null) {
                 String cssPath = cssUrl.toExternalForm();
                 scene.getStylesheets().add(cssPath);
                 System.out.println("App: CSS '" + cssPath + "' cargado exitosamente.");
            } else {
                 System.err.println("App: Advertencia: No se encontró el archivo ecg-style.css en la ruta de recursos especificada.");
            }

        } catch (Exception e) {
             System.err.println("App: Error al cargar ecg-style.css.");
             e.printStackTrace();
        }
        // --- Fin de la Sección de Carga CSS ---


        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("App: Método stop() llamado al cerrar la aplicación. Ejecutando limpieza...");
        if (ecgController != null) {
            System.out.println("App: Llamando al método shutdown() del controlador para cerrar el serial...");
            ecgController.shutdown();
            System.out.println("App: Limpieza del serial completada.");
        } else {
             System.out.println("App: Controlador nulo, no se pudo realizar limpieza del serial.");
        }
        System.out.println("App: Método stop() finalizado.");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


