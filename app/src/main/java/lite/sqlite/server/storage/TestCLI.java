package lite.sqlite.server.storage;

import lite.sqlite.cli.TableDto;
import lite.sqlite.cli.TablePrinter;
import lite.sqlite.server.datastructure.BplusTree.BplusTree;
import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced command-line tool for testing the query engine.
 * Includes comprehensive tests and result validation.
 */
public class TestCLI {

    // private static TablePrinter printer = new TablePrinter();
    // private static QueryEngine engine;
    // private static AtomicInteger testsPassed = new AtomicInteger(0);
    // private static AtomicInteger testsFailed = new AtomicInteger(0);

    public static void main(String[] args) {
        BplusTree<Integer, String> tree = new BplusTree<>(3);
        
        tree.insert(10, "ten");
        tree.insert(20, "twenty");
        tree.insert(5, "five");
        tree.insert(15, "fifteen");
        tree.insert(25, "twenty-five");
        
        tree.printTree();
        
        System.out.println("\nSearch 15: " + tree.search(15));
        System.out.println("Search 100: " + tree.search(100));
    }
    //     System.out.println("Lite-SQLite Enhanced Test Harness");
    //     System.out.println("================================");
        
    //     try {
    //         // Clean up any old test data
    //         cleanupTestData();
            
    //         // Initialize the query engine
    //         engine = new QueryEngineImpl();
            
    //         // Core functionality tests
    //         runTest("Schema Creation", TestCLI::testCreateTable);
    //         runTest("Basic Insert", TestCLI::testInsertData);
    //         runTest("Basic Select", TestCLI::testSelectData);
    //         runTest("Column Projection", TestCLI::testColumnProjection);
            
    //         // WHERE clause tests
    //         runTest("Equality Filter", TestCLI::testEqualityFilter);
    //         runTest("Range Filter", TestCLI::testRangeFilter);
    //         runTest("Multiple Conditions", TestCLI::testMultipleConditions);
            
    //         // Error cases
    //         runTest("Error Handling", TestCLI::testErrorHandling);
            
    //         // Print summary
    //         System.out.println("\n=== Test Summary ===");
    //         System.out.println("Passed: " + testsPassed.get());
    //         System.out.println("Failed: " + testsFailed.get());
            
    //     } catch (Exception e) {
    //         System.err.println("Test framework failed with exception: " + e.getMessage());
    //         e.printStackTrace();
    //     } finally {
    //         // Clean up resources
    //         if (engine instanceof QueryEngineImpl) {
    //             ((QueryEngineImpl) engine).close();
    //         }
    //     }
    // }
    
    // private static void cleanupTestData() {
    //     File dbDir = new File("database");
    //     if (dbDir.exists()) {
    //         for (File file : dbDir.listFiles()) {
    //             if (file.getName().endsWith(".tbl")) {
    //                 file.delete();
    //             }
    //         }
    //     }
    // }
    
    // private static void runTest(String testName, Runnable testCase) {
    //     System.out.println("\n=== Running Test: " + testName + " ===");
    //     try {
    //         testCase.run();
    //         System.out.println("✅ " + testName + " - PASSED");
    //         testsPassed.incrementAndGet();
    //     } catch (AssertionError | Exception e) {
    //         System.err.println("❌ " + testName + " - FAILED: " + e.getMessage());
    //         e.printStackTrace();
    //         testsFailed.incrementAndGet();
    //     }
    // }
    
    // private static void testCreateTable() {
    //     // Create test tables
    //     assertSuccess("CREATE TABLE customers (id INTEGER, name VARCHAR(100), balance INTEGER)");
    //     assertSuccess("CREATE TABLE products (id INTEGER, name VARCHAR(100), price INTEGER, category VARCHAR(50))");
    // }
    
    // private static void testInsertData() {
    //     // Insert customers data
    //     assertSuccess("INSERT INTO customers (id, name, balance) VALUES (1, 'Alice Johnson', 500)");
    //     assertSuccess("INSERT INTO customers (id, name, balance) VALUES (2, 'Bob Smith', 1200)");
    //     assertSuccess("INSERT INTO customers (id, name, balance) VALUES (3, 'Carol Williams', 750)");
        
    //     // Insert products data
    //     assertSuccess("INSERT INTO products (id, name, price, category) VALUES (101, 'Laptop', 1200, 'Electronics')");
    //     assertSuccess("INSERT INTO products (id, name, price, category) VALUES (102, 'Book', 25, 'Books')");
    //     assertSuccess("INSERT INTO products (id, name, price, category) VALUES (103, 'Phone', 800, 'Electronics')");
    //     assertSuccess("INSERT INTO products (id, name, price, category) VALUES (104, 'Desk', 350, 'Furniture')");
    // }
    
    // private static void testSelectData() {
    //     // Basic select tests
    //     TableDto result = assertQuery("SELECT * FROM customers");
    //     assertRowCount(result, 3);
        
    //     result = assertQuery("SELECT * FROM products");
    //     assertRowCount(result, 4);
    // }
    
    // private static void testColumnProjection() {
    //     TableDto result = assertQuery("SELECT name, balance FROM customers");
    //     assertColumnCount(result, 2);
    //     assertColumnsExist(result, "name", "balance");
        
    //     result = assertQuery("SELECT name, price, category FROM products");
    //     assertColumnCount(result, 3);
    //     assertColumnsExist(result, "name", "price", "category");
    // }
    
    // private static void testEqualityFilter() {
    //     TableDto result = assertQuery("SELECT name FROM customers WHERE id = 2");
    //     assertRowCount(result, 1);
    //     assertCellValue(result, 0, 0, "Bob Smith");
        
    //     result = assertQuery("SELECT name, price FROM products WHERE category = 'Electronics'");
    //     assertRowCount(result, 2);
    // }
    
    // private static void testRangeFilter() {
    //     TableDto result = assertQuery("SELECT id, name FROM customers WHERE balance > 700");
    //     assertRowCount(result, 2);
        
    //     result = assertQuery("SELECT name FROM products WHERE price < 500");
    //     assertRowCount(result, 2);
    //     assertCellValue(result, 0, 0, "Book");
    // }
    
    // private static void testMultipleConditions() {
    //     // This test would require support for AND/OR in WHERE clauses
    //     // For now, we'll just mark it as a placeholder
    //     System.out.println("Test for multiple conditions not implemented yet");
    // }
    
    // private static void testErrorHandling() {
    //     // Test for non-existent table
    //     TableDto result = engine.doQuery("SELECT * FROM nonexistent_table");
    //     assertHasError(result);
        
    //     // Test for invalid column
    //     result = engine.doQuery("SELECT invalid_column FROM customers");
    //     assertHasError(result);
    // }
    
    // // Helper methods for assertions
    
    // private static TableDto assertQuery(String sql) {
    //     System.out.println("\nExecuting: " + sql);
    //     TableDto result = engine.doQuery(sql);
    //     if (result.getErrorMessage() != null) {
    //         throw new AssertionError("Query failed: " + result.getErrorMessage());
    //     }
    //     printer.print(result);
    //     return result;
    // }
    
    // private static void assertSuccess(String sql) {
    //     System.out.println("\nExecuting: " + sql);
    //     TableDto result;
    //     if (sql.trim().toUpperCase().startsWith("SELECT")) {
    //         result = engine.doQuery(sql);
    //     } else {
    //         result = engine.doUpdate(sql);
    //     }
        
    //     if (result.getErrorMessage() != null) {
    //         throw new AssertionError("Statement failed: " + result.getErrorMessage());
    //     }
    //     printer.print(result);
    // }
    
    // private static void assertRowCount(TableDto result, int expected) {
    //     int actual = result.getRows().size();
    //     if (actual != expected) {
    //         throw new AssertionError("Expected " + expected + " rows but got " + actual);
    //     }
    // }
    
    // private static void assertColumnCount(TableDto result, int expected) {
    //     int actual = result.getColumnNames().size();
    //     if (actual != expected) {
    //         throw new AssertionError("Expected " + expected + " columns but got " + actual);
    //     }
    // }
    
    // private static void assertColumnsExist(TableDto result, String... columnNames) {
    //     List<String> actualColumns = result.getColumnNames();
    //     for (String column : columnNames) {
    //         if (!actualColumns.contains(column)) {
    //             throw new AssertionError("Expected column '" + column + "' not found");
    //         }
    //     }
    // }
    
    // private static void assertCellValue(TableDto result, int row, int col, String expected) {
    //     String actual = result.getRows().get(row).get(col);
    //     if (!expected.equals(actual)) {
    //         throw new AssertionError("Expected value '" + expected + "' at [" + row + "," + col + "] but got '" + actual + "'");
    //     }
    // }
    
    // private static void assertHasError(TableDto result) {
    //     if (result.getErrorMessage() == null) {
    //         throw new AssertionError("Expected an error but none was returned");
    //     }
    // }
    
    // private static void executeAndPrint(String sql) {
    //     System.out.println("\nExecuting: " + sql);
        
    //     TableDto result;
    //     if (sql.trim().toUpperCase().startsWith("SELECT")) {
    //         result = engine.doQuery(sql);
    //     } else {
    //         result = engine.doUpdate(sql);
    //     }
        
    //     if (result.getErrorMessage() != null) {
    //         System.out.println("ERROR: " + result.getErrorMessage());
    //     } else {
    //         printer.print(result);
    //     }
    // }
}