package lite.sqlite.service;

import java.util.List;

import lite.sqlite.dao.UserDAO;
import lite.sqlite.model.User;

/**
 * Service class for User entity.
 * This class provides business logic for operations related to users.
 * The service layer acts as a bridge between the controller (UI) and the DAO (data access).
 */
public class UserService {
    private UserDAO userDAO;
    
    /**
     * Constructor initializes the user DAO.
     * 
     * [IMPLEMENT] Initialize the UserDAO in the constructor.
     * This demonstrates dependency injection at the service level.
     * 
     * @param userDAO the user DAO
     */
    
    /**
     * Creates a new user.
     * 
     * [IMPLEMENT] Create a method that validates input and creates a new user.
     * This method should demonstrate:
     * - Input validation
     * - Business logic separation
     * - Proper error handling
     * 
     * @param username the username
     * @param email the email
     * @return true if successful, false otherwise
     */
    
    /**
     * Gets a user by ID.
     * 
     * [IMPLEMENT] Create a method that retrieves a user by ID.
     * This teaches you about delegation to the DAO layer.
     * 
     * @param id the user ID
     * @return the User object if found, null otherwise
     */
    
    /**
     * Gets all users.
     * 
     * [IMPLEMENT] Create a method that retrieves all users.
     * This demonstrates how to handle collections in service methods.
     * 
     * @return a list of User objects
     */
    
    /**
     * Updates a user.
     * 
     * [IMPLEMENT] Create a method that validates input and updates a user.
     * This method should demonstrate:
     * - Checking if a record exists before updating
     * - Input validation
     * - Error handling
     * 
     * @param id the user ID
     * @param username the new username
     * @param email the new email
     * @return true if successful, false otherwise
     */
    
    /**
     * Deletes a user.
     * 
     * [IMPLEMENT] Create a method that deletes a user.
     * This method shows how to handle deletion operations safely.
     * 
     * @param id the user ID
     * @return true if successful, false otherwise
     */
}
