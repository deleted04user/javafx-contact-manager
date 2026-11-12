# Contact Manager

Contact Manager is a desktop address book for saving and organizing personal contacts. It uses Java 17, JavaFX, SQLite, JDBC, Maven, and JavaFX CSS.

## Features

- Add, view, update, and delete contacts. Deleting asks for confirmation.
- Search by part of a name or phone number as you type.
- Assign and filter by Family, Friends, Work, University, or Other.
- See the total number of saved contacts, even when filters are active.

## Setup and launch

Install JDK 17 and Maven. Run `java -version` and `mvn -version` to check that Maven is using JDK 17. In IntelliJ IDEA, open this folder as a Maven project and select JDK 17 as the project SDK.

Open a terminal in the folder containing `pom.xml` and run:

```sh
mvn javafx:run
```

Maven downloads the dependencies on the first run. After that, `mvn -o javafx:run` works offline. To build the project, run `mvn clean package`.

Enter a full name and phone number to add a contact. The email field is optional, and new contacts default to the Other group. Select a row to load it into the form for editing or deleting. Clear Form resets the fields and selection. Clear the search and select All groups to see the entire list.

## Project structure

```text
pom.xml
README.md
.gitignore
contacts.db (created on first launch)
src/main/
  java/com/contactmanager/
    ContactManagerApp.java
    Contact.java
    Database.java
    ContactDAO.java
  resources/
    styles.css
```

- `ContactManagerApp` builds the JavaFX window, handles form and table actions, and refreshes the contact list.
- `Contact` holds one contact's ID, name, phone, email, and group.
- `Database` opens the SQLite connection and creates the contacts table at startup.
- `ContactDAO` reads and writes contacts using JDBC prepared statements. It also searches and filters the loaded list.

## Database

The application creates `contacts.db` beside `pom.xml` when launched with Maven. SQLite saves contacts in a single table named `contacts` with the columns `id`, `name`, `phone`, `email`, and `group_name`. The ID is generated automatically. Phone numbers are stored as text so that leading zeros and plus signs are preserved. The same file is used the next time you open the app.
