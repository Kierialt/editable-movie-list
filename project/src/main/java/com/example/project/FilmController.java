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


    private User loggedInUser; // сюда придёт пользователь после логина

    public void postInitialize() {
        // Этот метод должен вызываться после того, как установили loggedInUser
        DatabaseManager.initialize(); // Создаём таблицы (если их ещё нет)
        loadFilmsFromDatabase();      // Загружаем фильмы текущего пользователя
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }


    private void loadFilmsFromDatabase() {
        //filmList.clear(); // если вдруг вызывали раньше

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
                filmList.add(film);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private void saveFilmToDatabase(Film film) {
        String sql = "INSERT INTO films(title, genre, year, watched, user_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, film.getTitle());
            pstmt.setString(2, film.getGenre());
            pstmt.setInt(3, film.getYear());
            pstmt.setInt(4, film.isWatched() ? 1 : 0);
            pstmt.setInt(5, loggedInUser.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("❗ Не удалось добавить фильм в БД.");
                return;
            }
            // Получим сгенерированный ID и запишем в объект film
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newId = generatedKeys.getInt(1);
                film.setId(newId);
            }
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
            updateFilmWatchedInDatabase(film);
        });

        addActionButtonsToTable();

        // НЕ загружаем фильмы здесь! Ждём, пока кто-то вызовет setLoggedInUser(...) и после этого — loadFilmsFromDatabase().
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

    private void deleteFilmFromDatabase(Film film) {
        String sql = "DELETE FROM films WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, film.getId());
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
                if (newYear < 1800 || newYear > 2025) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Год должен быть в пределах 1800–2025.");
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



    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
