package com.example.project;

import java.sql.*;


public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:films.db";

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();

            // SQL command to create the 'users' table

            String userTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            );
        """;

            // Table 'films' with a foreign key referencing the user_id

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


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }


}
