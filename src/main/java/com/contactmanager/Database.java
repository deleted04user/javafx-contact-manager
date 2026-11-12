package com.contactmanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:contacts.db");
    }

    public static void initialize() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS contacts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT,
                    group_name TEXT NOT NULL
                )
                """;

        // Opening the connection creates the file when it is missing.
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
