# Lite SQLite CLI

This is a simple command-line interface for interacting with an SQLite database. The CLI allows you to execute SQL queries and statements against your database directly from a terminal.

## Features

- Interactive SQL shell
- Support for both queries (SELECT) and updates (INSERT, UPDATE, DELETE, CREATE, etc.)
- Tabular display of query results
- Simple command syntax

## Usage

1. Start the application:
   ```
   ./gradlew run
   ```

2. Once the CLI is running, you can enter SQL commands followed by a semicolon (;):
   ```
   litesql> CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT);
   ```

3. Run queries to retrieve data:
   ```
   litesql> SELECT * FROM users;
   ```

4. Insert data into tables:
   ```
   litesql> INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com');
   ```

5. Exit the CLI:
   ```
   litesql> exit;
   ```

## Command Reference

- **SELECT** - Query data from the database
  ```
  SELECT * FROM table_name WHERE condition;
  ```

- **INSERT** - Add data to the database
  ```
  INSERT INTO table_name (column1, column2) VALUES (value1, value2);
  ```

- **UPDATE** - Modify existing data
  ```
  UPDATE table_name SET column1 = value1 WHERE condition;
  ```

- **DELETE** - Remove data
  ```
  DELETE FROM table_name WHERE condition;
  ```

- **CREATE TABLE** - Create a new table
  ```
  CREATE TABLE table_name (column1 type1, column2 type2);
  ```

- **DROP TABLE** - Delete a table
  ```
  DROP TABLE table_name;
  ```

## Implementation

The CLI is implemented with the following components:

1. `CLI` - Interface for command-line interfaces
2. `ConsoleCLI` - Implementation of the CLI interface for console interaction
3. `IQueryEngine` - Interface for executing SQL queries and updates
4. `SQLiteQueryEngine` - SQLite implementation of the query engine
5. `TableDto` - Data transfer object for table data
6. `TablePrinter` - Utility for printing tables to the console

These components work together to provide a simple, interactive database interface.

## Extending the CLI

To extend the functionality of the CLI:

1. Enhance the `SQLiteQueryEngine` implementation to handle more complex queries
2. Add support for additional SQL commands
3. Implement transaction support
4. Add command history and auto-completion features

## Learning Opportunities

By working with this CLI, you can learn:

- SQL syntax and database operations
- Command parsing and interpretation
- Data presentation in console applications
- Error handling in interactive applications
