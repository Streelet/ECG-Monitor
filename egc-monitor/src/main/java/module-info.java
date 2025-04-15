module com.mycompany.javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens com.mycompany.javafx to javafx.fxml;
    exports com.mycompany.javafx;
}
