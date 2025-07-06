package lite.sqlite.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager is responsible for managing the database connection
 * and providing methods to interact with the database.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:database.db";
    private Connection connection;
    
    /**
     * Constructor initializes the database connection.
     * 
     * [IMPLEMENT] Create a constructor that initializes the database connection.
     * This is crucial for establishing a connection to SQLite that can be used
     * throughout the application. Learn about connection management in JDBC.
     */
    
    /**
     * Initializes the database connection and creates tables if they don't exist.
     * 
     * [IMPLEMENT] This private method should establish the database connection
     * and create necessary tables. This teaches proper database initialization
     * and error handling in JDBC applications.
     */
    
    /**
     * Creates the necessary tables in the database if they don't exist.
     * 
     * [IMPLEMENT] Define the schema of your database by creating tables.
     * You'll learn about SQL DDL (Data Definition Language) statements and
     * how to execute them from Java.
     */
    
    /**
     * Closes the database connection.
     * 
     * [IMPLEMENT] Properly close the database connection to release resources.
     * This teaches proper resource management, which is essential for avoiding
     * memory leaks and connection pool exhaustion.
     */
    
    /**
     * Gets the database connection.
     * 
     * [IMPLEMENT] Method to return the current database connection.
     * This is needed by other classes to execute SQL statements.
     * 
     * @return the Connection object
     */
}
