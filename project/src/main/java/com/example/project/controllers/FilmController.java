package com.example.project.controllers;

import com.example.project.data.DatabaseManager;
import com.example.project.models.Film;
import com.example.project.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;


public class FilmController {


    @FXML private TextField titleField;
    @FXML private TextField genreField;
    @FXML private TextField yearField;
    @FXML private TableView<Film> filmTable;
    @FXML private TableColumn<Film, String> titleColumn;
    @FXML private TableColumn<Film, String> genreColumn;
    @FXML private TableColumn<Film, Integer> yearColumn;
    @FXML private TableColumn<Film, Boolean> watchedColumn;
    @FXML private TableColumn<Film, Void> actionColumn;

    private final ObservableList<Film> filmList = FXCollections.observableArrayList();


    private User loggedInUser; // The user will be redirected here after logging in


    public void postInitialize() {
        // This method should be called after setting loggedInUser

        DatabaseManager.initialize(); // Create tables (if they don't exist yet)
        loadFilmsFromDatabase();      // Load movies of the current user
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }


    private void loadFilmsFromDatabase() {
        Task<ObservableList<Film>> task = new Task<>() {
            @Override
            protected ObservableList<Film> call() throws Exception {
                ObservableList<Film> loadedFilms = FXCollections.observableArrayList();
                String sql = "SELECT id, title, genre, year, watched FROM films WHERE user_id = ?";

                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setInt(1, loggedInUser.getId());
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        Film film = new Film(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getString("genre"),
                                rs.getInt("year"),
                                rs.getInt("watched") == 1
                        );
                        loadedFilms.add(film);
                    }
                }
                return loadedFilms;
            }
        };

        task.setOnSucceeded(event -> filmList.setAll(task.getValue()));

        task.setOnFailed(event -> task.getException().printStackTrace());

        new Thread(task).start();
    }


    private void saveFilmToDatabase(Film film) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String sql = "INSERT INTO films (title, genre, year, watched, user_id) VALUES (?, ?, ?, ?, ?)";

                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, film.getTitle());
                    pstmt.setString(2, film.getGenre());
                    pstmt.setInt(3, film.getYear());
                    pstmt.setInt(4, film.isWatched() ? 1 : 0);
                    pstmt.setInt(5, loggedInUser.getId());

                    pstmt.executeUpdate();
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                loadFilmsFromDatabase(); // Refresh the list after adding
            }

            @Override
            protected void failed() {
                super.failed();
                getException().printStackTrace();
            }
        };

        new Thread(task).start();
    }


    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        genreColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getGenre()));
        yearColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getYear()).asObject());

        watchedColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isWatched()));
        watchedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(watchedColumn));
        watchedColumn.setEditable(true);

        filmTable.setEditable(true);
        filmTable.setItems(filmList);


        // Handler for the "Watched" checkbox value change

        watchedColumn.setOnEditCommit(event -> {
            Film film = event.getRowValue();
            boolean newValue = event.getNewValue();
            film.setWatched(newValue);
            filmTable.refresh();
            updateFilmWatchedInDatabase(film);
        });

        addActionButtonsToTable();

        // DO NOT load films here! Wait until setLoggedInUser(...) is called, and only then call loadFilmsFromDatabase().
    }


    private void updateFilmWatchedInDatabase(Film film) {
        String sql = "UPDATE films SET watched = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, film.isWatched() ? 1 : 0);
            pstmt.setInt(2, film.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void addActionButtonsToTable() {
        Callback<TableColumn<Film, Void>, TableCell<Film, Void>> cellFactory = param -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final Button editButton = new Button("Edit");
            private final HBox buttonBox = new HBox(5, deleteButton, editButton);

            {
                deleteButton.setOnAction(event -> {
                    Film film = getTableView().getItems().get(getIndex());
                    deleteFilmFromDatabase(film); // First delete from the database
                    filmList.remove(film);        // Then remove from the UI
                });


                editButton.setOnAction(event -> {
                    Film film = getTableView().getItems().get(getIndex());
                    editFilm(film);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }


    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/project/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) filmTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login / Registration");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void handleAddFilm() {
        String title = titleField.getText().trim();
        String genre = genreField.getText().trim();
        String yearText = yearField.getText().trim();

        if (title.isEmpty() || genre.isEmpty() || yearText.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearText);
            if (year < 1800 || year > 2100) {
                showAlert("Please enter a valid year (1800-2100).");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("The year must be a number.");
            return;
        }

        Film newFilm = new Film(-1, title, genre, year, false);
        filmList.add(newFilm);

        titleField.clear();
        genreField.clear();
        yearField.clear();

        saveFilmToDatabase(newFilm);

    }

    private void editFilm(Film film) {
        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Edit Movie");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/dialog-style.css")).toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label titleLabel = new Label("🎬 Title:");
        titleLabel.setStyle("-fx-font-weight: bold;");
        TextField titleField = new TextField(film.getTitle());
        titleField.setPrefWidth(300);

        Label genreLabel = new Label("🎭 Genre:");
        genreLabel.setStyle("-fx-font-weight: bold;");
        TextField genreField = new TextField(film.getGenre());
        genreField.setPrefWidth(300);

        Label yearLabel = new Label("📅 Year:");
        yearLabel.setStyle("-fx-font-weight: bold;");
        TextField yearField = new TextField(String.valueOf(film.getYear()));
        yearField.setPrefWidth(150);

        CheckBox watchedBox = new CheckBox(" Watched");
        watchedBox.setSelected(film.isWatched());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));
        grid.setAlignment(Pos.CENTER_LEFT);

        grid.add(titleLabel, 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(genreLabel, 0, 1);
        grid.add(genreField, 1, 1);
        grid.add(yearLabel, 0, 2);
        grid.add(yearField, 1, 2);
        grid.add(new Label(""), 0, 3); // Empty cell for alignment
        grid.add(watchedBox, 1, 3);

        dialogPane.setContent(grid);

        // Validation before closing

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String newTitle = titleField.getText().trim();
            String newGenre = genreField.getText().trim();
            String newYearText = yearField.getText().trim();

            if (newTitle.isEmpty() || newGenre.isEmpty() || newYearText.isEmpty()) {
                showAlert("Please fill in all fields.");
                event.consume();
                return;
            }

            int newYear;
            try {
                newYear = Integer.parseInt(newYearText);
                if (newYear < 1800 || newYear > 2100) {
                    showAlert("The year must be between 1800 and 2100.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("The year must be a number.");
                event.consume();
                return;
            }

            // Update the data
            film.setTitle(newTitle);
            film.setGenre(newGenre);
            film.setYear(newYear);
            film.setWatched(watchedBox.isSelected());

            updateFilmInDatabase(film);
            filmTable.refresh();
        });

        dialog.showAndWait();
    }


    private void deleteFilmFromDatabase(Film film) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String sql = "DELETE FROM films WHERE id = ? AND user_id = ?";

                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setInt(1, film.getId());
                    pstmt.setInt(2, loggedInUser.getId());

                    pstmt.executeUpdate();
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                loadFilmsFromDatabase(); // Update data

            }

            @Override
            protected void failed() {
                super.failed();
                getException().printStackTrace();
            }
        };

        new Thread(task).start();
    }



    private void updateFilmInDatabase(Film film) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String sql = "UPDATE films SET title = ?, genre = ?, year = ?, watched = ? WHERE id = ? AND user_id = ?";

                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, film.getTitle());
                    pstmt.setString(2, film.getGenre());
                    pstmt.setInt(3, film.getYear());
                    pstmt.setInt(4, film.isWatched() ? 1 : 0);
                    pstmt.setInt(5, film.getId());
                    pstmt.setInt(6, loggedInUser.getId());

                    pstmt.executeUpdate();
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                loadFilmsFromDatabase(); // Refresh the list
            }

            @Override
            protected void failed() {
                super.failed();
                getException().printStackTrace();
            }
        };

        new Thread(task).start();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
