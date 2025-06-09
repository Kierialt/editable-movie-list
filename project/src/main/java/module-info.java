module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.project to javafx.fxml;
    exports com.example.project;
    exports com.example.project.models;
    opens com.example.project.models to javafx.fxml;
    exports com.example.project.controllers;
    opens com.example.project.controllers to javafx.fxml;
    exports com.example.project.data;
    opens com.example.project.data to javafx.fxml;
}