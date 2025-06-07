package com.example.project;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // После удачного входа нам нужно передать ID пользователя в FilmController
    private User loggedInUser;

    @FXML
    private void handleLogin() {
        User newUser = new User();
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(passwordField.getText().trim());

        if (newUser.getUsername().isEmpty() || newUser.getPassword().isEmpty()) {
            messageLabel.setText("Пожалуйста, заполните все поля.");
            return;
        }

        // Проверяем в БД: SELECT id, username, password FROM users WHERE username = ? AND password = ?
        String sql = "SELECT id, username, password FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUser.getUsername());
            pstmt.setString(2, newUser.getPassword());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Пользователь найден — вход успешен
                int id = rs.getInt("id");
                String uname = rs.getString("username");
                String pass = rs.getString("password");
                loggedInUser = new User(id, uname, pass);

                // Открываем экран со списком фильмов и передаём в его контроллер loggedInUser
                openFilmScreen();
            } else {
                messageLabel.setText("Неправильный логин или пароль.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка при подключении к БД", e.getMessage());
        }
    }




    @FXML
    private void handleRegister() {
        User newUser = new User();
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(passwordField.getText().trim());


        if (newUser.getUsername().isEmpty() || newUser.getPassword().isEmpty()) {
            messageLabel.setText("Пожалуйста, заполните все поля.");
            return;
        }

        // Пытаемся вставить нового пользователя в таблицу users
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUser.getUsername());
            pstmt.setString(2, newUser.getPassword());
            pstmt.executeUpdate();

            messageLabel.setText("Регистрация успешна. Можно войти.");
        } catch (SQLException e) {
            // Если уникальность по username нарушена, придёт исключение
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique")) {
                messageLabel.setText("Пользователь с таким именем уже есть.");
            } else {
                e.printStackTrace();
                showAlert("Ошибка при регистрации", e.getMessage());
            }
        }
    }

    private void openFilmScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/project/main.fxml"));
            Scene scene = new Scene(loader.load());

            FilmController filmController = loader.getController();
            filmController.setLoggedInUser(loggedInUser);
            filmController.postInitialize(); // загружаем фильмы и создаём БД

            Stage primaryStage = (Stage) usernameField.getScene().getWindow();
            primaryStage.setScene(scene);
            primaryStage.setTitle("Список фильмов — пользователь: " + loggedInUser.getUsername());
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка при загрузке экрана фильмов", e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
