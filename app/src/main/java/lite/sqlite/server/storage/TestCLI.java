package lite.sqlite.server.storage;

import lite.sqlite.cli.TableDto;
import lite.sqlite.cli.TablePrinter;
import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

import java.util.List;
import java.io.IOException;

/**
 * A simple command-line tool for testing the query engine.
 * Runs a series of predefined SQL statements and displays the results.
 */
public class TestCLI {

    private static TablePrinter printer = new TablePrinter();
    private static QueryEngine engine;

    public static void main(String[] args) throws IOException {
        System.out.println("Lite-SQLite Test Harness");
        System.out.println("=======================");
        
        try {
            // Initialize the query engine
            engine = new QueryEngineImpl();
            
            // Run the tests
            testCreateTable();
            testInsertData();
            testSelectData();
            testWhereFilter();
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (engine instanceof QueryEngineImpl) {
                ((QueryEngineImpl) engine).close();
            }
        }
    }
    
    private static void testCreateTable() {
        System.out.println("\n== Testing CREATE TABLE ==");
        
        // Create a customers table
        String sql = "CREATE TABLE customers (id INTEGER, name VARCHAR(100), balance INTEGER)";
        executeAndPrint(sql);
    }
    
    private static void testInsertData() {
        System.out.println("\n== Testing INSERT ==");
        
        // Insert test data
        List<String> insertStatements = List.of(
            "INSERT INTO customers (id, name, balance) VALUES (1, 'Alice Johnson', 500)",
            "INSERT INTO customers (id, name, balance) VALUES (2, 'Bob Smith', 1200)",
            "INSERT INTO customers (id, name, balance) VALUES (3, 'Carol Williams', 750)"
        );
        
        for (String sql : insertStatements) {
            executeAndPrint(sql);
        }
    }
    
    private static void testSelectData() {
        System.out.println("\n== Testing SELECT * ==");
        
        // Basic select all
        String sql = "SELECT * FROM customers";
        executeAndPrint(sql);
        
        // Select specific columns
        System.out.println("\n== Testing Column Projection ==");
        sql = "SELECT name, balance FROM customers";
        executeAndPrint(sql);
    }
    
    private static void testWhereFilter() {
        System.out.println("\n== Testing WHERE Filtering ==");
        
        // Equality filter
        System.out.println("\n-- Equality filter --");
        String sql = "SELECT name FROM customers WHERE id = 2";
        executeAndPrint(sql);
        
        // Greater than filter
        System.out.println("\n-- Greater than filter --");
        sql = "SELECT id, name, balance FROM customers WHERE balance > 700";
        executeAndPrint(sql);
    }
    
    private static void executeAndPrint(String sql) {
        System.out.println("\nExecuting: " + sql);
        
        TableDto result;
        if (sql.trim().toUpperCase().startsWith("SELECT")) {
            result = engine.doQuery(sql);
        } else {
            result = engine.doUpdate(sql);
        }
        
        if (result.getErrorMessage() != null) {
            System.out.println("ERROR: " + result.getErrorMessage());
        } else {
            printer.print(result);
        }
        System.out.println();
    }
}