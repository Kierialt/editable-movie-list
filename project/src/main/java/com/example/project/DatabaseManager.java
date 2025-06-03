package com.example.project;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:/Users/eugene/Desktop/DBForInteliJIDEA/films.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS films (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    genre TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    watched INTEGER NOT NULL
                );
                """;
            conn.createStatement().execute(sql);
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
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
