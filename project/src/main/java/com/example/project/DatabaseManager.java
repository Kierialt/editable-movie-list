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

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
