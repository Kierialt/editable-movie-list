package com.example.project;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
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
                        rs.getInt("id"),
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
        // 👇 Вот тут загружаем данные из базы
        filmList.setAll(DatabaseManager.loadFilmsFromDatabase());
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

        Film newFilm = new Film(-1, title, genre, year, false);
        filmList.add(newFilm);

        titleField.clear();
        genreField.clear();
        yearField.clear();

        saveFilmToDatabase(newFilm);

    }

    private void editFilm(Film film) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактирование фильма");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/dialog-style.css").toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label titleLabel = new Label("🎬 Название:");
        titleLabel.setStyle("-fx-font-weight: bold;");
        TextField titleField = new TextField(film.getTitle());
        titleField.setPrefWidth(300);

        Label genreLabel = new Label("🎭 Жанр:");
        genreLabel.setStyle("-fx-font-weight: bold;");
        TextField genreField = new TextField(film.getGenre());
        genreField.setPrefWidth(300);

        Label yearLabel = new Label("📅 Год:");
        yearLabel.setStyle("-fx-font-weight: bold;");
        TextField yearField = new TextField(String.valueOf(film.getYear()));
        yearField.setPrefWidth(150);

        CheckBox watchedBox = new CheckBox(" Просмотрено");
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
        grid.add(new Label(""), 0, 3); // пустая ячейка для выравнивания
        grid.add(watchedBox, 1, 3);

        dialogPane.setContent(grid);

        // Валидация перед закрытием
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String newTitle = titleField.getText().trim();
            String newGenre = genreField.getText().trim();
            String newYearText = yearField.getText().trim();

            if (newTitle.isEmpty() || newGenre.isEmpty() || newYearText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Пожалуйста, заполните все поля.");
                event.consume();
                return;
            }

            int newYear;
            try {
                newYear = Integer.parseInt(newYearText);
                if (newYear < 1800 || newYear > 2100) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Год должен быть в пределах 1800–2100.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Год должен быть числом.");
                event.consume();
                return;
            }

            // Обновляем данные
            film.setTitle(newTitle);
            film.setGenre(newGenre);
            film.setYear(newYear);
            film.setWatched(watchedBox.isSelected());

            updateFilmInDatabase(film);
            filmTable.refresh();
        });

        dialog.showAndWait();
    }



    private void updateFilmInDatabase(Film film) {
        DatabaseManager.updateFilm(film);
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
