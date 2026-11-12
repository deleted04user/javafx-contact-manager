package com.contactmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactDAO {
    public void addContact(Contact contact) throws SQLException {
        String sql = "INSERT INTO contacts (name, phone, email, group_name) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setContactFields(statement, contact);
            statement.executeUpdate();
        }
    }

    public void updateContact(Contact contact) throws SQLException {
        String sql = "UPDATE contacts SET name = ?, phone = ?, email = ?, group_name = ? WHERE id = ?";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setContactFields(statement, contact);
            statement.setInt(5, contact.getId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("This contact no longer exists.");
            }
        }
    }

    public void deleteContact(int id) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM contacts WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<Contact> getAllContacts() throws SQLException {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, group_name FROM contacts ORDER BY name COLLATE NOCASE, id";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                contacts.add(new Contact(rows.getInt("id"), rows.getString("name"),
                        rows.getString("phone"), rows.getString("email"), rows.getString("group_name")));
            }
        }
        return contacts;
    }

    public List<Contact> searchContacts(List<Contact> contacts, String searchText) {
        String query = searchText.strip().toLowerCase(Locale.ROOT);
        List<Contact> matches = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact.getName().toLowerCase(Locale.ROOT).contains(query)
                    || contact.getPhone().contains(query)) {
                matches.add(contact);
            }
        }
        return matches;
    }

    public List<Contact> getContactsByGroup(List<Contact> contacts, String group) {
        List<Contact> matches = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact.getGroup().equals(group)) {
                matches.add(contact);
            }
        }
        return matches;
    }

    // Bind values separately from the SQL so names such as O'Neil work normally.
    private void setContactFields(PreparedStatement statement, Contact contact) throws SQLException {
        statement.setString(1, contact.getName());
        statement.setString(2, contact.getPhone());
        statement.setString(3, contact.getEmail());
        statement.setString(4, contact.getGroup());
    }
}
