// /*
//  * Lite SQLite Database Project
//  */
// package lite.sqlite;

// import lite.sqlite.database.DatabaseManager;
// import lite.sqlite.database.IQueryEngine;
// import lite.sqlite.dao.UserDAO;
// import lite.sqlite.service.UserService;
// import lite.sqlite.model.User;
// import lite.sqlite.cli.CLI;
// import lite.sqlite.cli.SQLiteQueryEngine;

// import java.util.List;
// import java.util.Scanner;

// /**
//  * Main application class for Lite SQLite Database Project.
//  * This class serves as the entry point for the application and demonstrates
//  * how to build a simple console menu system for a database application.
//  */
// public class App {
//     private static DatabaseManager dbManager;
//     private static UserService userService;
//     private static Scanner scanner;
    
//     /**
//      * Main method to run the application.
//      * 
//      * This method initializes the database components and starts the CLI.
//      * 
//      * @param args command line arguments
//      */
//     public static void main(String[] args) {
//         // Initialize the database manager
//         dbManager = new DatabaseManager();
        
//         // Initialize the CLI
//         IQueryEngine queryEngine = new SQLiteQueryEngine(dbManager);
//         CLI cli = new ConsoleCLI(queryEngine);
        
//         // Start the CLI
//         cli.start();
//     }
    
//     /**
//      * Displays the main menu.
//      * 
//      * [IMPLEMENT] Create a method to display a menu of options to the user.
//      * This teaches UI design in console applications.
//      */
    
//     /**
//      * Gets a valid user choice within a range.
//      * 
//      * [IMPLEMENT] Create a method that validates user input for menu choices.
//      * This teaches input validation and error handling.
//      * 
//      * @param min the minimum valid choice
//      * @param max the maximum valid choice
//      * @return the user's choice
//      */
    
//     /**
//      * Creates a new user based on user input.
//      * 
//      * [IMPLEMENT] Create a method that gets user input for a new user
//      * and calls the service layer to create it.
//      * This demonstrates the controller-service pattern.
//      */
    
//     /**
//      * Lists all users in the database.
//      * 
//      * [IMPLEMENT] Create a method that displays all users from the database.
//      * This teaches data presentation in console applications.
//      */
    
//     /**
//      * Finds a user by ID.
//      * 
//      * [IMPLEMENT] Create a method that gets a user ID from input
//      * and displays the corresponding user details.
//      * This demonstrates searching and displaying specific records.
//      */
    
//     /**
//      * Updates a user based on user input.
//      * 
//      * [IMPLEMENT] Create a method that gets user input for updating a user
//      * and calls the service layer to update it.
//      * This demonstrates the update operation in CRUD applications.
//      */
    
//     /**
//      * Deletes a user based on user input.
//      * 
//      * [IMPLEMENT] Create a method that gets a user ID from input
//      * and calls the service layer to delete it after confirmation.
//      * This demonstrates the delete operation in CRUD applications with confirmation.
//      */
    
//     /**
//      * Cleans up resources and exits the application.
//      * 
//      * [IMPLEMENT] Create a method that properly closes resources before exiting.
//      * This demonstrates proper resource management in Java applications.
//      */
// }


