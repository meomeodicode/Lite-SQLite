// package lite.sqlite.server.storage.table;

// import java.io.File;
// import java.io.IOException;
// import java.util.Iterator;
// import java.util.NoSuchElementException;

// import lite.sqlite.server.storage.BasicFileManager;
// import lite.sqlite.server.storage.Block;
// import lite.sqlite.server.storage.buffer.BufferPool;
// import lite.sqlite.server.storage.filemanager.FileManager;
// import lite.sqlite.server.storage.record.DataType;
// import lite.sqlite.server.storage.record.Record;
// import lite.sqlite.server.storage.record.Schema;

// /**
//  * Test class for the Table implementation.
//  * NOTE: This is a simplified test that doesn't use JUnit for easier implementation.
//  */
// public class TableTest {
//     private static final String TEST_DIR = "test_db";
//     private static final String TEST_TABLE = "test_table";
    
//     private FileManager fileManager;
//     private BufferPool bufferPool;
//     private TableInfo tableInfo;
//     private Table table;
    
//     /**
//      * Main method to run the tests manually.
//      */
//     public static void main(String[] args) {
//         try {
//             TableTest test = new TableTest();
//             test.setUp();
            
//             System.out.println("Running tests:");
//             System.out.println("1. Insert and Get Record Test");
//             test.testInsertAndGetRecord();
//             System.out.println("   PASSED!");
            
//             System.out.println("2. Update Record Test");
//             test.testUpdateRecord();
//             System.out.println("   PASSED!");
            
//             System.out.println("3. Delete Record Test");
//             test.testDeleteRecord();
//             System.out.println("   PASSED!");
            
//             System.out.println("4. Iterator Test");
//             test.testIterator();
//             System.out.println("   PASSED!");
            
//             test.tearDown();
//             System.out.println("All tests passed successfully!");
            
//         } catch (Exception e) {
//             System.err.println("Test failed: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
    
//     /**
//      * Set up the test environment.
//      */
//     public void setUp() throws IOException {
//         // Initialize the file manager
//         File dbDir = new File(TEST_DIR);
//         fileManager = new BasicFileManager(dbDir);
        
//         // Initialize the buffer pool
//         bufferPool = new BufferPool(10, fileManager);
        
//         // Create a schema for the test table
//         Schema schema = new Schema();
//         schema.addColumn("id", DataType.INTEGER);
//         schema.addColumn("name", DataType.VARCHAR, 50);
//         schema.addColumn("age", DataType.INTEGER);
        
//         // Create a table info object
//         tableInfo = new TableInfo(TEST_TABLE, schema, TEST_TABLE + ".dat");
        
//         // Create the table
//         table = new Table(tableInfo, bufferPool);
//     }
    
//     /**
//      * Clean up after the tests.
//      */
//     public void tearDown() throws IOException {
//         // Clean up resources
//         bufferPool.flushAll();
        
//         // Delete test files
//         try {
//             // BasicFileManager doesn't have deleteFile method, so we'll use Java File API
//             File testFile = new File(TEST_DIR, TEST_TABLE + ".dat");
//             if (testFile.exists()) {
//                 testFile.delete();
//             }
//         } catch (Exception e) {
//             System.err.println("Warning: Could not delete test file: " + e.getMessage());
//         }
//     }
    
//     /**
//      * Test inserting and retrieving a record.
//      */
//     public void testInsertAndGetRecord() throws IOException {
//         // Create a test record
//         Record record = new Record(new Object[] { 1, "John", 30 });
        
//         // Insert the record
//         RecordId rid = table.insertRecord(record);
//         assert rid != null : "RecordId should not be null";
        
//         // Get the record back
//         Record retrievedRecord = table.getRecord(rid);
//         assert retrievedRecord != null : "Retrieved record should not be null";
        
//         // Check that the record values match
//         Object[] values = retrievedRecord.getValues();
//         assert values.length == 3 : "Record should have 3 values";
//         assert values[0].equals(1) : "First value should be 1";
//         assert values[1].equals("John") : "Second value should be 'John'";
//         assert values[2].equals(30) : "Third value should be 30";
//     }
    
//     /**
//      * Test updating a record.
//      */
//     public void testUpdateRecord() throws IOException {
//         // Create and insert a test record
//         Record record = new Record(new Object[] { 1, "John", 30 });
//         RecordId rid = table.insertRecord(record);
        
//         // Update the record
//         Record updatedRecord = new Record(new Object[] { 1, "John Doe", 31 });
//         table.updateRecord(rid, updatedRecord);
        
//         // Get the updated record
//         Record retrievedRecord = table.getRecord(rid);
        
//         // Check that the record was updated
//         Object[] values = retrievedRecord.getValues();
//         assert values.length == 3 : "Record should have 3 values";
//         assert values[0].equals(1) : "First value should be 1";
//         assert values[1].equals("John Doe") : "Second value should be 'John Doe'";
//         assert values[2].equals(31) : "Third value should be 31";
//     }
    
//     /**
//      * Test deleting a record.
//      */
//     public void testDeleteRecord() throws IOException {
//         // Create and insert a test record
//         Record record = new Record(new Object[] { 1, "John", 30 });
//         RecordId rid = table.insertRecord(record);
        
//         // Delete the record
//         table.deleteRecord(rid);
        
//         // Try to get the deleted record (should throw an exception)
//         boolean exceptionThrown = false;
//         try {
//             table.getRecord(rid);
//         } catch (NoSuchElementException e) {
//             exceptionThrown = true;
//         }
        
//         assert exceptionThrown : "Expected NoSuchElementException was not thrown";
//     }
    
//     /**
//      * Test iterating through records.
//      */
//     public void testIterator() throws IOException {
//         // Create and insert some test records
//         Record record1 = new Record(new Object[] { 1, "John", 30 });
//         Record record2 = new Record(new Object[] { 2, "Jane", 25 });
//         Record record3 = new Record(new Object[] { 3, "Bob", 40 });
        
//         table.insertRecord(record1);
//         table.insertRecord(record2);
//         table.insertRecord(record3);
        
//         // Get an iterator for the records
//         int count = 0;
//         Iterator<Record> iterator = table.iterator();
//         while (iterator.hasNext()) {
//             Record record = iterator.next();
//             assert record != null : "Record should not be null";
//             count++;
//         }
        
//         // Check that we got all the records
//         assert count == 3 : "Iterator should return 3 records";
//     }
// }