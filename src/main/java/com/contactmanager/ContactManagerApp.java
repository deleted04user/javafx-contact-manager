package com.contactmanager;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ContactManagerApp extends Application {
    private static final List<String> GROUPS = List.of("Family", "Friends", "Work", "University", "Other");
    private static final String ALL_GROUPS = "All groups";

    private final ContactDAO dao = new ContactDAO();
    private List<Contact> allContacts = List.of();

    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final ComboBox<String> groupField = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> groupFilter = new ComboBox<>();
    private final TableView<Contact> table = new TableView<>();
    private final Label total = new Label();
    private final Label status = new Label();

    @Override
    public void start(Stage stage) {
        Label title = styledLabel("Contact Manager", "title");
        Label subtitle = styledLabel("Manage your contacts easily", "subtitle");
        total.getStyleClass().add("counter");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, new VBox(3, title, subtitle), spacer, total);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox contactList = createContactList();
        HBox.setHgrow(contactList, Priority.ALWAYS);
        HBox content = new HBox(18, createForm(), contactList);
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox root = new VBox(22, header, content);
        root.getStyleClass().add("page");

        Scene scene = new Scene(root, 1040, 640);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Contact Manager");
        stage.setMinWidth(820);
        stage.setMinHeight(650);
        stage.setScene(scene);

        try {
            Database.initialize();
            refreshContacts();
        } catch (SQLException exception) {
            showMessage("Could not open the contact database. " + exception.getMessage(), true);
            content.setDisable(true);
        }
        stage.show();
    }

    private VBox createForm() {
        groupField.getItems().setAll(GROUPS);
        groupField.setValue("Other");
        groupField.setMaxWidth(Double.MAX_VALUE);

        Button add = new Button("Add Contact");
        add.getStyleClass().add("primary");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(event -> saveContact(null));

        Button update = new Button("Update Contact");
        update.setOnAction(event -> saveContact(table.getSelectionModel().getSelectedItem()));
        Button delete = new Button("Delete Contact");
        delete.getStyleClass().add("danger");
        delete.setOnAction(event -> deleteContact());
        update.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button clear = new Button("Clear Form");
        clear.setOnAction(event -> {
            clearForm();
            nameField.requestFocus();
        });

        status.setWrapText(true);
        status.setMinHeight(38);
        VBox form = new VBox(8, styledLabel("Contact details", "section-title"),
                labeledField("Full Name", nameField), labeledField("Phone Number", phoneField),
                labeledField("Email Address (optional)", emailField), labeledField("Group", groupField),
                add, new HBox(8, update, delete), clear, status);
        form.getStyleClass().add("panel");
        form.setPrefWidth(300);
        form.setMinWidth(300);
        return form;
    }

    private VBox createContactList() {
        searchField.setPromptText("Search by name or phone");
        searchField.setAccessibleText("Search by name or phone");
        groupFilter.setAccessibleText("Filter by group");
        groupFilter.getItems().add(ALL_GROUPS);
        groupFilter.getItems().addAll(GROUPS);
        groupFilter.setValue(ALL_GROUPS);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox filters = new HBox(10, searchField, groupFilter);

        addColumn("ID", 45, Contact::getId);
        addColumn("Full Name", 150, Contact::getName);
        addColumn("Phone Number", 120, Contact::getPhone);
        addColumn("Email", 160, Contact::getEmail);
        addColumn("Group", 85, Contact::getGroup);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                nameField.setText(selected.getName());
                phoneField.setText(selected.getPhone());
                emailField.setText(selected.getEmail() == null ? "" : selected.getEmail());
                groupField.setValue(selected.getGroup());
                showMessage("", false);
            }
        });
        searchField.textProperty().addListener((observable, previous, current) -> applyFilters());
        groupFilter.valueProperty().addListener((observable, previous, current) -> applyFilters());

        VBox list = new VBox(12, styledLabel("Contacts", "section-title"), filters, table);
        list.getStyleClass().add("panel");
        VBox.setVgrow(table, Priority.ALWAYS);
        return list;
    }

    private <T> void addColumn(String title, double width, Function<Contact, T> value) {
        TableColumn<Contact, T> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private void saveContact(Contact selected) {
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();
        if (name.isEmpty() || phone.isEmpty()) {
            showMessage("Full name and phone number are required.", true);
            if (name.isEmpty()) {
                nameField.requestFocus();
            } else {
                phoneField.requestFocus();
            }
            return;
        }

        Contact contact = new Contact(name, phone, emailField.getText().strip(), groupField.getValue());
        String message;
        try {
            if (selected == null) {
                dao.addContact(contact);
                message = "Contact added.";
            } else {
                contact.setId(selected.getId());
                dao.updateContact(contact);
                message = "Contact updated.";
            }
        } catch (SQLException exception) {
            showMessage("Could not save the contact. " + exception.getMessage(), true);
            return;
        }
        afterChange(message);
    }

    private void deleteContact() {
        Contact selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmation.initOwner(table.getScene().getWindow());
        confirmation.setTitle("Delete Contact");
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            dao.deleteContact(selected.getId());
        } catch (SQLException exception) {
            showMessage("Could not delete the contact. " + exception.getMessage(), true);
            return;
        }
        afterChange("Contact deleted.");
    }

    private void afterChange(String message) {
        clearForm();
        try {
            refreshContacts();
            showMessage(message, false);
        } catch (SQLException exception) {
            showMessage(message + " Could not reload contacts. " + exception.getMessage(), true);
        }
    }

    private void refreshContacts() throws SQLException {
        allContacts = dao.getAllContacts();
        total.setText("Total contacts: " + allContacts.size());
        applyFilters();
    }

    private void applyFilters() {
        if (table.getSelectionModel().getSelectedItem() != null) {
            clearForm();
        }
        // The search and group filter both apply to the saved contact list.
        List<Contact> visibleContacts = dao.searchContacts(allContacts, searchField.getText());
        String group = groupFilter.getValue();
        if (!ALL_GROUPS.equals(group)) {
            visibleContacts = dao.getContactsByGroup(visibleContacts, group);
        }
        table.getItems().setAll(visibleContacts);
        table.sort();
        table.setPlaceholder(new Label(allContacts.isEmpty() ? "No contacts yet." : "No matching contacts."));
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        groupField.setValue("Other");
        showMessage("", false);
    }

    private void showMessage(String message, boolean error) {
        status.setText(message);
        status.getStyleClass().removeAll("error", "success");
        status.getStyleClass().add(error ? "error" : "success");
    }

    private static VBox labeledField(String text, Control field) {
        Label label = new Label(text);
        label.setLabelFor(field);
        return new VBox(4, label, field);
    }

    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
