package lite.sqlite.server.queryengine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lite.sqlite.cli.TableDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the QueryEngine implementation, covering SQL operations
 * like CREATE TABLE, INSERT and SELECT with various conditions.
 */
@DisplayName("QueryEngine Tests")
public class QueryEngineTest {

    private QueryEngineImpl queryEngine;
    
    @BeforeEach
    void setUp() {
        // Create database directory if it doesn't exist
        File dbDirectory = new File("database");
        if (!dbDirectory.exists()) {
            dbDirectory.mkdirs();
        }
        
        // Initialize query engine
        queryEngine = new QueryEngineImpl();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Close resources
        if (queryEngine != null) {
            queryEngine.close();
        }
        
        // Clean up test files
        File dbDirectory = new File("database");
        if (dbDirectory.exists()) {
            deleteDirectory(dbDirectory);
        }
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Nested
    @DisplayName("CREATE TABLE Tests")
    class CreateTableTests {
        
        @Test
        @DisplayName("Should create a table successfully")
        void testCreateTable() {
            // When: Creating a table with valid SQL
            String sql = "CREATE TABLE users (id INTEGER, name VARCHAR(50))";
            TableDto result = queryEngine.doUpdate(sql);
    
            // Then: No errors should occur
            assertNull(result.getErrorMessage(), "Error message should be null on success");
            
            // And: Table file should be created on disk
            File tableFile = new File("database/users.tbl");
            assertTrue(tableFile.exists(), "Table file 'users.tbl' should be created.");
        }
        
        @Test
        @DisplayName("Should not allow duplicate table names")
        void testCreateTableDuplicate() {
            // Given: An existing table
            queryEngine.doUpdate("CREATE TABLE duplicates (id INTEGER)");
            
            // When: Trying to create a table with the same name
            TableDto result = queryEngine.doUpdate("CREATE TABLE duplicates (value VARCHAR(20))");
            
            // Then: It should return an error
            assertNotNull(result.getErrorMessage(), "Should return error for duplicate table");
            assertTrue(result.getErrorMessage().contains("already exists"), 
                "Error should mention table already exists");
        }
    }

    @Nested
    @DisplayName("INSERT Tests")
    class InsertTests {
        
        @Test
        @DisplayName("Should insert data into a table")
        void testInsertData() {
            // Given: A table
            queryEngine.doUpdate("CREATE TABLE test_insert (id INTEGER, value VARCHAR(50))");
            
            // When: Inserting data
            TableDto result = queryEngine.doUpdate("INSERT INTO test_insert (id, value) VALUES (42, 'test')");
            
            // Then: Insert should succeed
            assertNull(result.getErrorMessage(), "Insert should succeed without errors");
            
            // And: We should be able to retrieve the data
            TableDto selectResult = queryEngine.doQuery("SELECT * FROM test_insert");
            List<List<String>> rows = selectResult.getRows();
            
            assertEquals(1, rows.size(), "Should have inserted exactly one row");
            assertEquals("42", rows.get(0).get(0), "First column should match inserted value");
            assertEquals("test", rows.get(0).get(1), "Second column should match inserted value");
        }
        
        @Test
        @DisplayName("Should return error when inserting into non-existent table")
        void testInsertIntoNonExistentTable() {
            // When: Trying to insert into a table that doesn't exist
            TableDto result = queryEngine.doUpdate(
                "INSERT INTO non_existent (id) VALUES (1)");
            
            // Then: It should return an error
            assertNotNull(result.getErrorMessage(), "Should return error for non-existent table");
            assertTrue(result.getErrorMessage().contains("not exist") || 
                       result.getErrorMessage().contains("doesn't exist"), 
                "Error should mention table doesn't exist");
        }
        
        @Test
        @DisplayName("Should return error when inserting invalid column")
        void testInsertInvalidColumn() {
            // Given: A table with defined columns
            queryEngine.doUpdate("CREATE TABLE test_cols (id INTEGER, name VARCHAR(50))");
            
            // When: Trying to insert into a non-existent column
            TableDto result = queryEngine.doUpdate(
                "INSERT INTO test_cols (id, invalid_col) VALUES (1, 'test')");
            
            // Then: It should return an error
            assertNotNull(result.getErrorMessage(), "Should return error for invalid column");
            assertTrue(result.getErrorMessage().contains("not exist") || 
                      result.getErrorMessage().contains("doesn't exist"), 
                "Error should mention column doesn't exist");
        }
    }

    @Nested
    @DisplayName("SELECT Tests")
    class SelectTests {
        
        @Test
        @DisplayName("Should select all records")
        void testSelectAll() {
            // Given: A table with data
            queryEngine.doUpdate("CREATE TABLE employees (id INTEGER, name VARCHAR(100))");
            queryEngine.doUpdate("INSERT INTO employees (id, name) VALUES (1, 'Alice')");
            queryEngine.doUpdate("INSERT INTO employees (id, name) VALUES (2, 'Bob')");
    
            // When: Selecting all data
            TableDto selectResult = queryEngine.doQuery("SELECT * FROM employees");
            
            // Then: Query should succeed
            assertNull(selectResult.getErrorMessage(), "Select should succeed without errors");
    
            // And: Column names should match
            List<String> expectedColumns = List.of("id", "name");
            assertEquals(expectedColumns, selectResult.getColumnNames(), "Columns should match");
    
            // And: We should get all rows
            List<List<String>> rows = selectResult.getRows();
            assertNotNull(rows, "Rows should not be null");
            assertEquals(2, rows.size(), "Should retrieve 2 rows");
    
            // And: Data should match what was inserted
            assertEquals("1", rows.get(0).get(0), "First row, first column should be 1");
            assertEquals("Alice", rows.get(0).get(1), "First row, second column should be Alice");
            assertEquals("2", rows.get(1).get(0), "Second row, first column should be 2");
            assertEquals("Bob", rows.get(1).get(1), "Second row, second column should be Bob");
        }
        
        @Test
        @DisplayName("Should select specific columns")
        void testSelectColumns() {
            // Given: A table with data
            queryEngine.doUpdate("CREATE TABLE products (id INTEGER, name VARCHAR(100), price INTEGER)");
            queryEngine.doUpdate("INSERT INTO products (id, name, price) VALUES (1, 'Apple', 5)");
            
            // When: Selecting only specific columns
            TableDto result = queryEngine.doQuery("SELECT id, price FROM products");
            
            // Then: Only requested columns should be returned
            assertEquals(List.of("id", "price"), result.getColumnNames(), 
                "Should return only requested columns");
                
            // And: Data should match
            assertEquals("1", result.getRows().get(0).get(0), "ID column should be returned");
            assertEquals("5", result.getRows().get(0).get(1), "Price column should be returned");
        }
    
        @Test
        @DisplayName("Should filter with WHERE clause")
        void testSelectWithWhereClause() {
            // Given: A table with filtered data
            queryEngine.doUpdate("CREATE TABLE products (id INTEGER, category VARCHAR(50), price INTEGER)");
            queryEngine.doUpdate("INSERT INTO products (id, category, price) VALUES (101, 'books', 20)");
            queryEngine.doUpdate("INSERT INTO products (id, category, price) VALUES (102, 'electronics', 150)");
            queryEngine.doUpdate("INSERT INTO products (id, category, price) VALUES (103, 'books', 25)");
    
            // When: Selecting with WHERE clause
            TableDto selectResult = queryEngine.doQuery("SELECT id, price FROM products WHERE category = 'books'");
    
            // Then: Query should succeed
            assertNull(selectResult.getErrorMessage(), "Select with WHERE should succeed without errors");
            assertEquals(List.of("id", "price"), selectResult.getColumnNames());
    
            // And: Only matching rows should be returned
            List<List<String>> rows = selectResult.getRows();
            assertEquals(2, rows.size(), "WHERE clause should filter to 2 rows");
    
            // Check for both books (order may not be guaranteed)
            boolean found101 = false;
            boolean found103 = false;
            
            for (List<String> row : rows) {
                if ("101".equals(row.get(0)) && "20".equals(row.get(1))) {
                    found101 = true;
                }
                if ("103".equals(row.get(0)) && "25".equals(row.get(1))) {
                    found103 = true;
                }
            }
            
            assertTrue(found101, "Should find book with id=101, price=20");
            assertTrue(found103, "Should find book with id=103, price=25");
        }
        
        @Test
        @DisplayName("Should handle numeric comparisons in WHERE clause")
        void testSelectWithNumericComparison() {
            // Given: A table with numeric data
            queryEngine.doUpdate("CREATE TABLE items (id INTEGER, price INTEGER)");
            queryEngine.doUpdate("INSERT INTO items (id, price) VALUES (1, 10)");
            queryEngine.doUpdate("INSERT INTO items (id, price) VALUES (2, 20)");
            queryEngine.doUpdate("INSERT INTO items (id, price) VALUES (3, 30)");
            
            // When: Filtering by numeric comparison
            TableDto result = queryEngine.doQuery("SELECT id FROM items WHERE price > 15");
            
            // Then: Only matching rows should be returned
            assertEquals(2, result.getRows().size(), "Should return 2 rows with price > 15");
            
            // Check specific IDs are returned (order may vary)
            boolean found2 = false;
            boolean found3 = false;
            
            for (List<String> row : result.getRows()) {
                if ("2".equals(row.get(0))) found2 = true;
                if ("3".equals(row.get(0))) found3 = true;
            }
            
            assertTrue(found2, "Should find item with id=2");
            assertTrue(found3, "Should find item with id=3");
        }
    
        @Test
        @DisplayName("Should return error for non-existent table")
        void testTableNotFound() {
            // When: Querying a non-existent table
            TableDto result = queryEngine.doQuery("SELECT * FROM non_existent_table");
    
            // Then: It should return an error
            assertNotNull(result.getErrorMessage(), "Error message should be provided");
            assertTrue(result.getErrorMessage().contains("doesn't exist") || 
                       result.getErrorMessage().contains("not exist"), 
                       "Error message should indicate table not found");
        }
    }
    
    @Test
    @DisplayName("Should handle multiple operations in sequence")
    void testCompleteWorkflow() {
        // Create table
        queryEngine.doUpdate("CREATE TABLE customers (id INTEGER, name VARCHAR(100), active INTEGER)");
        
        // Insert multiple rows
        queryEngine.doUpdate("INSERT INTO customers (id, name, active) VALUES (1, 'John', 1)");
        queryEngine.doUpdate("INSERT INTO customers (id, name, active) VALUES (2, 'Mary', 0)");
        queryEngine.doUpdate("INSERT INTO customers (id, name, active) VALUES (3, 'Steve', 1)");
        
        // Query with filter
        TableDto result = queryEngine.doQuery("SELECT name FROM customers WHERE active = 1");
        
        // Verify results
        assertNull(result.getErrorMessage());
        assertEquals(2, result.getRows().size(), "Should find 2 active customers");
        
        // Verify the right customers were found
        List<String> names = List.of(
            result.getRows().get(0).get(0),
            result.getRows().get(1).get(0)
        );
        
        assertTrue(names.contains("John"), "Active customers should include John");
        assertTrue(names.contains("Steve"), "Active customers should include Steve");
        assertFalse(names.contains("Mary"), "Inactive customers should be filtered out");
    }
}