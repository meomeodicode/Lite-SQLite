package lite.sqlite.server.storage;

import lite.sqlite.cli.TableDto;
import lite.sqlite.cli.TablePrinter;
import lite.sqlite.server.datastructure.BplusTree.BplusTree;
import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

import java.io.File;

/**
 * Simple command-line tool for testing basic query engine functionality.
 */
public class TestCLI {

    private static TablePrinter printer = new TablePrinter();
    private static QueryEngine engine;

    public static void main(String[] args) {
        
        // Test B+ Tree first
        System.out.println("=== B+ Tree Test ===");
        testBPlusTree();
        
        System.out.println("\n\nLite-SQLite Simple Test");
        System.out.println("=======================");
        
        try {
            // Clean up old data
            cleanupTestData();
            
            // Initialize engine
            engine = new QueryEngineImpl();
            
            // Run basic tests
            testBasicFunctionality();
            testIndexFunctionality();
            
            System.out.println("\n✅ All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (engine instanceof QueryEngineImpl) {
                ((QueryEngineImpl) engine).close();
            }
        }
    }
    
    private static void testBPlusTree() {
        BplusTree<Integer, String> tree = new BplusTree<>(3);
        tree.insert(1, "one");
        tree.insert(2, "two");
        tree.insert(3, "three");
        tree.insert(4, "four");
        
        tree.printTree();
        
        System.out.println("\nSearch 1: " + tree.search(1));
        System.out.println("Search 3: " + tree.search(3));
    }
    
    private static void testBasicFunctionality() {
        System.out.println("\n=== Basic Database Operations ===");
        
        // 1. Create a simple table
        System.out.println("\n1. Creating table...");
        execute("CREATE TABLE students (id INTEGER, name VARCHAR(50), grade INTEGER)");
        
        // 2. Insert some data
        System.out.println("\n2. Inserting data...");
        execute("INSERT INTO students (id, name, grade) VALUES (1, 'Alice', 85)");
        execute("INSERT INTO students (id, name, grade) VALUES (2, 'Bob', 92)");
        execute("INSERT INTO students (id, name, grade) VALUES (3, 'Charlie', 78)");
        execute("INSERT INTO students (id, name, grade) VALUES (4, 'Diana', 95)");
        
        // 3. Basic queries
        System.out.println("\n3. Basic SELECT queries...");
        execute("SELECT * FROM students");
        execute("SELECT name, grade FROM students");
        execute("SELECT name FROM students WHERE grade > 80");
        execute("SELECT * FROM students WHERE id = 2");
    }
    
    private static void testIndexFunctionality() {
        System.out.println("\n=== Index Operations ===");
        
        // 1. Create indexes
        System.out.println("\n1. Creating indexes...");
        execute("CREATE INDEX idx_student_id ON students(id)");
        execute("CREATE INDEX idx_student_grade ON students(grade)");
        
        // 2. Queries that should use indexes
        System.out.println("\n2. Index-optimized queries...");
        execute("SELECT * FROM students WHERE id = 1");
        execute("SELECT * FROM students WHERE id = 3");
        execute("SELECT name FROM students WHERE id = 4");
        
        // 3. Performance test with simple timing
        System.out.println("\n3. Simple performance test...");
        
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= 4; i++) {
            TableDto result = engine.doQuery("SELECT * FROM students WHERE id = " + i);
            System.out.println("Found student: " + result.getRows().get(0).get(1)); // Print name
        }
        long endTime = System.currentTimeMillis();
        
        System.out.println("✓ Completed 4 lookups in " + (endTime - startTime) + "ms");
        
        // 4. Range query
        System.out.println("\n4. Range query...");
        execute("SELECT name FROM students WHERE grade > 85");
    }
    
    private static void execute(String sql) {
        System.out.println("\nSQL: " + sql);
        
        TableDto result;
        
        // Route to appropriate method based on SQL type
        if (sql.trim().toUpperCase().startsWith("SELECT")) {
            result = engine.doQuery(sql);
        } else if (sql.trim().toUpperCase().contains("CREATE INDEX")) {
            result = engine.doCreateIndex(sql);
        } else {
            result = engine.doUpdate(sql);
        }
        
        // Print result or error
        if (result.getErrorMessage() != null) {
            System.out.println("❌ ERROR: " + result.getErrorMessage());
        } else {
            System.out.println("✅ SUCCESS");
            if (!result.getRows().isEmpty()) {
                printer.print(result);
            }
        }
    }
    
    private static void cleanupTestData() {
        File dbDir = new File("database");
        if (dbDir.exists()) {
            for (File file : dbDir.listFiles()) {
                if (file.getName().endsWith(".tbl")) {
                    file.delete();
                }
            }
        }
    }
}