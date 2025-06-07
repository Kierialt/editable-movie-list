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

    // After successful login, we need to pass the user ID to the FilmController
    private User loggedInUser;

    @FXML
    private void handleLogin() {
        User newUser = new User();
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(passwordField.getText().trim());

        if (newUser.getUsername().isEmpty() || newUser.getPassword().isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        // Checking the database: SELECT id, username, password FROM users WHERE username = ? AND password = ?
        String sql = "SELECT id, username, password FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUser.getUsername());
            pstmt.setString(2, newUser.getPassword());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // User found — login successful
                int id = rs.getInt("id");
                String uname = rs.getString("username");
                String pass = rs.getString("password");
                loggedInUser = new User(id, uname, pass);

                // Opening the film list screen and passing loggedInUser to its controller
                openFilmScreen();
            } else {
                messageLabel.setText("Incorrect username or password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database connection error", e.getMessage());
        }
    }


    @FXML
    private void handleRegister() {
        User newUser = new User();
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(passwordField.getText().trim());


        if (newUser.getUsername().isEmpty() || newUser.getPassword().isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        // Attempting to insert a new user into the users table
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newUser.getUsername());
            pstmt.setString(2, newUser.getPassword());
            pstmt.executeUpdate();

            messageLabel.setText("Registration successful. You can now log in.");
        } catch (SQLException e) {
            // If the username uniqueness constraint is violated, an exception will be thrown
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique")) {
                messageLabel.setText("A user with this username already exists.");
            } else {
                e.printStackTrace();
                showAlert("Registration error", e.getMessage());
            }
        }
    }

    private void openFilmScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/project/main.fxml"));
            Scene scene = new Scene(loader.load());

            FilmController filmController = loader.getController();
            filmController.setLoggedInUser(loggedInUser);
            filmController.postInitialize(); // Loading films and creating database

            Stage primaryStage = (Stage) usernameField.getScene().getWindow();
            primaryStage.setScene(scene);
            primaryStage.setTitle("Film list — user: " + loggedInUser.getUsername());
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error loading the film screen", e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
