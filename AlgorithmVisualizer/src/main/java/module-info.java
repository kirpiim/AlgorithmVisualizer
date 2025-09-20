module com.halime.visualizer {
    requires javafx.controls;
    requires javafx.fxml;

    exports com.halime.visualizer;
    exports com.halime.visualizer.algorithm;

    // so FXMLLoader can access controller methods
    opens com.halime.visualizer.controller to javafx.fxml;
}

