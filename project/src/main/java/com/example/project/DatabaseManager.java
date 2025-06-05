package com.example.project;

import java.sql.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:/Users/eugene/Desktop/DBForInteliJIDEA/films.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();

            // Перепишем CREATE TABLE users для наших целей:
            String userTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            );
        """;

            // Таблица фильмов с внешним ключом user_id
            String filmTable = """
            CREATE TABLE IF NOT EXISTS films (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                genre TEXT NOT NULL,
                year INTEGER NOT NULL,
                watched INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            );
        """;

            stmt.execute(userTable);
            stmt.execute(filmTable);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public static Connection connect() {
        String url = "jdbc:sqlite:/Users/eugene/Desktop/DBForInteliJIDEA/films.db"; // имя файла базы, если у тебя другое — измени
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }


    public static void updateFilm(Film film) {
        String sql = "UPDATE films SET title = ?, genre = ?, year = ?, watched = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, film.getTitle());
            pstmt.setString(2, film.getGenre());
            pstmt.setInt(3, film.getYear());
            pstmt.setBoolean(4, film.isWatched());
            pstmt.setInt(5, film.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("❗ Обновление не удалось: фильм с id " + film.getId() + " не найден.");
            } else {
                System.out.println("✅ Фильм успешно обновлён: " + film.getTitle());
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении фильма: " + e.getMessage());
        }
    }



    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }



    public static ObservableList<Film> loadFilmsFromDatabase() {
        ObservableList<Film> films = FXCollections.observableArrayList();
        String sql = "SELECT id, title, genre, year, watched FROM films";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String genre = rs.getString("genre");
                int year = rs.getInt("year");
                boolean watched = rs.getBoolean("watched");

                Film film = new Film(id, title, genre, year, watched);
                film.setId(id); // 💥 Устанавливаем ID из базы

                films.add(film);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при загрузке фильмов из базы: " + e.getMessage());
        }

        return films;
    }


}
