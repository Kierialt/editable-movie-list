package com.example.project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.sql.*;

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


    private void loadFilmsFromDatabase() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM films")) {

            while (rs.next()) {
                Film film = new Film(
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getInt("year"),
                        rs.getInt("watched") == 1
                );
                filmList.add(film);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveFilmToDatabase(Film film) {
        String sql = "INSERT INTO films(title, genre, year, watched) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, film.getTitle());
            pstmt.setString(2, film.getGenre());
            pstmt.setInt(3, film.getYear());
            pstmt.setInt(4, film.isWatched() ? 1 : 0);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        // Обработчик изменения значения чекбокса "Просмотрено"
        watchedColumn.setOnEditCommit(event -> {
            Film film = event.getRowValue();
            boolean newValue = event.getNewValue();
            film.setWatched(newValue);
            filmTable.refresh();

            updateFilmWatchedInDatabase(film);  // Метод для обновления в базе
        });

        addActionButtonsToTable();

        DatabaseManager.initialize();
        loadFilmsFromDatabase();
    }


    private void updateFilmWatchedInDatabase(Film film) {
        String sql = "UPDATE films SET watched = ? WHERE title = ? AND genre = ? AND year = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:/Users/eugene/Desktop/DBForInteliJIDEA/films.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, film.isWatched());
            pstmt.setString(2, film.getTitle());
            pstmt.setString(3, film.getGenre());
            pstmt.setInt(4, film.getYear());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void addActionButtonsToTable() {
        Callback<TableColumn<Film, Void>, TableCell<Film, Void>> cellFactory = param -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");
            private final Button editButton = new Button("Редактировать");
            private final HBox buttonBox = new HBox(5, deleteButton, editButton);

            {
                deleteButton.setOnAction(event -> {
                    Film film = getTableView().getItems().get(getIndex());
                    deleteFilmFromDatabase(film); // сначала удаляем из БД
                    filmList.remove(film);        // потом удаляем из UI
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

    @FXML
    private void handleAddFilm() {
        String title = titleField.getText().trim();
        String genre = genreField.getText().trim();
        String yearText = yearField.getText().trim();

        if (title.isEmpty() || genre.isEmpty() || yearText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Пожалуйста, заполните все поля.");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearText);
            if (year < 1800 || year > 2100) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите корректный год (1800-2100).");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Год должен быть числом.");
            return;
        }

        Film newFilm = new Film(title, genre, year, false);
        filmList.add(newFilm);

        titleField.clear();
        genreField.clear();
        yearField.clear();

        saveFilmToDatabase(newFilm);

    }

    private void editFilm(Film film) {
        TextInputDialog dialog = new TextInputDialog(film.getTitle());
        dialog.setTitle("Редактировать название");
        dialog.setHeaderText("Введите новое название:");
        dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(newTitle -> {
            if (!newTitle.trim().isEmpty()) {
                film.setTitle(newTitle.trim());
                filmTable.refresh();
            } else {
                showAlert(Alert.AlertType.WARNING, "Внимание", "Название не может быть пустым.");
            }
        });

        dialog = new TextInputDialog(film.getGenre());
        dialog.setTitle("Редактировать жанр");
        dialog.setHeaderText("Введите новый жанр:");
        dialog.setContentText("Жанр:");
        dialog.showAndWait().ifPresent(newGenre -> {
            if (!newGenre.trim().isEmpty()) {
                film.setGenre(newGenre.trim());
                filmTable.refresh();
            } else {
                showAlert(Alert.AlertType.WARNING, "Внимание", "Жанр не может быть пустым.");
            }
        });

        dialog = new TextInputDialog(String.valueOf(film.getYear()));
        dialog.setTitle("Редактировать год");
        dialog.setHeaderText("Введите новый год:");
        dialog.setContentText("Год:");
        dialog.showAndWait().ifPresent(newYearText -> {
            try {
                int newYear = Integer.parseInt(newYearText.trim());
                if (newYear < 1800 || newYear > 2100) {
                    showAlert(Alert.AlertType.WARNING, "Ошибка", "Введите корректный год (1800-2100).");
                } else {
                    film.setYear(newYear);
                    filmTable.refresh();
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Ошибка", "Год должен быть числом.");
            }
        });
    }

    private void deleteFilmFromDatabase(Film film) {
        String sql = "DELETE FROM films WHERE title = ? AND genre = ? AND year = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, film.getTitle());
            pstmt.setString(2, film.getGenre());
            pstmt.setInt(3, film.getYear());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
