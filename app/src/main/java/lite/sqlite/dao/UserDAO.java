package lite.sqlite.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lite.sqlite.model.User;
import lite.sqlite.database.DatabaseManager;

/**
 * Data Access Object (DAO) for User entity.
 * This class provides methods to interact with the User table in the database.
 * Implementing this class will help you learn about the DAO pattern, which separates
 * database access logic from business logic.
 */
public class UserDAO {
    private DatabaseManager dbManager;
    
    /**
     * Constructor initializes the database manager.
     * 
     * [IMPLEMENT] Initialize the database manager in the constructor.
     * This teaches dependency injection, a key concept in modern software development.
     * 
     * @param dbManager the database manager
     */
    
    /**
     * Inserts a new user into the database.
     * 
     * [IMPLEMENT] Create a method that inserts a new user into the database.
     * You'll learn about:
     * - PreparedStatements (to prevent SQL injection)
     * - SQL INSERT statements
     * - Parameter binding in JDBC
     * - Proper exception handling
     * 
     * @param user the user to insert
     * @return true if successful, false otherwise
     */
    
    /**
     * Retrieves a user by ID.
     * 
     * [IMPLEMENT] Create a method that finds a user by their ID.
     * You'll learn about:
     * - SQL SELECT statements with WHERE clauses
     * - ResultSet handling in JDBC
     * - Object mapping (converting database rows to Java objects)
     * 
     * @param id the user ID
     * @return the User object if found, null otherwise
     */
    
    /**
     * Retrieves all users from the database.
     * 
     * [IMPLEMENT] Create a method that retrieves all users from the database.
     * You'll learn about:
     * - Handling multiple results with ResultSets
     * - Collection usage in Java
     * - Basic SQL queries
     * 
     * @return a list of User objects
     */
    
    /**
     * Updates a user in the database.
     * 
     * [IMPLEMENT] Create a method that updates an existing user.
     * You'll learn about:
     * - SQL UPDATE statements
     * - Tracking changes in database records
     * 
     * @param user the user to update
     * @return true if successful, false otherwise
     */
    
    /**
     * Deletes a user from the database.
     * 
     * [IMPLEMENT] Create a method that deletes a user from the database.
     * You'll learn about:
     * - SQL DELETE statements
     * - Data integrity considerations
     * 
     * @param id the ID of the user to delete
     * @return true if successful, false otherwise
     */
}
