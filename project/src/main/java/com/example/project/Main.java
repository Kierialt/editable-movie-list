package com.example.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Загружаем login.fxml вместо main.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/project/login.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Вход в систему");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
