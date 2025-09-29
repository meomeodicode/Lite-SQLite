// package lite.sqlite.server.demo;

// import java.io.File;
// import java.io.IOException;

// import lite.sqlite.server.storage.BasicFileManager;
// import lite.sqlite.server.storage.Block;
// import lite.sqlite.server.storage.buffer.BufferPool;
// import lite.sqlite.server.storage.filemanager.FileManager;
// import lite.sqlite.server.storage.record.DataType;
// import lite.sqlite.server.storage.record.Record;
// import lite.sqlite.server.storage.record.Schema;
// import lite.sqlite.server.storage.table.RecordId;
// import lite.sqlite.server.storage.table.Table;
// import lite.sqlite.server.storage.table.TableInfo;

// /**
//  * A demonstration of the Table layer.
//  * This class shows how to create a table, insert records, and retrieve them.
//  */
// public class TableDemo {

//     private static final String DB_DIRECTORY = "table_demo_db";
//     private static final String TABLE_NAME = "users";
    
//     public static void main(String[] args) {
//         try {
//             // Set up the database components
//             File dbDir = new File(DB_DIRECTORY);
//             FileManager fileManager = new BasicFileManager(dbDir);
//             BufferPool bufferPool = new BufferPool(10, fileManager);
            
//             // Create a schema for the users table
//             Schema schema = new Schema();
//             schema.addColumn("id", DataType.INTEGER);
//             schema.addColumn("name", DataType.VARCHAR, 50);
//             schema.addColumn("age", DataType.INTEGER);
            
//             // Create a table info object
//             TableInfo tableInfo = new TableInfo(TABLE_NAME, schema, TABLE_NAME + ".tbl");
            
//             // Create the table
//             Table table = new Table(tableInfo, bufferPool);
            
//             // Insert some records
//             System.out.println("Inserting records...");
            
//             Record record1 = new Record(new Object[] { 1, "Alice", 25 });
//             RecordId rid1 = table.insertRecord(record1);
//             System.out.println("Inserted record with ID: " + rid1);
            
//             Record record2 = new Record(new Object[] { 2, "Bob", 30 });
//             RecordId rid2 = table.insertRecord(record2);
//             System.out.println("Inserted record with ID: " + rid2);
            
//             Record record3 = new Record(new Object[] { 3, "Charlie", 35 });
//             RecordId rid3 = table.insertRecord(record3);
//             System.out.println("Inserted record with ID: " + rid3);
            
//             // Retrieve the records
//             System.out.println("\nRetrieving records...");
            
//             Record retrieved1 = table.getRecord(rid1);
//             System.out.println("Retrieved record: " + retrieved1);
            
//             Record retrieved2 = table.getRecord(rid2);
//             System.out.println("Retrieved record: " + retrieved2);
            
//             Record retrieved3 = table.getRecord(rid3);
//             System.out.println("Retrieved record: " + retrieved3);
            
//             // Update a record
//             System.out.println("\nUpdating record...");
            
//             Record updatedRecord = new Record(new Object[] { 2, "Robert", 31 });
//             table.updateRecord(rid2, updatedRecord);
//             System.out.println("Updated record with ID: " + rid2);
            
//             // Retrieve the updated record
//             Record retrievedUpdated = table.getRecord(rid2);
//             System.out.println("Retrieved updated record: " + retrievedUpdated);
            
//             // Delete a record
//             System.out.println("\nDeleting record...");
            
//             table.deleteRecord(rid3);
//             System.out.println("Deleted record with ID: " + rid3);
            
//             // Try to retrieve the deleted record
//             try {
//                 table.getRecord(rid3);
//                 System.out.println("Error: Deleted record was retrieved!");
//             } catch (Exception e) {
//                 System.out.println("Expected exception: " + e.getMessage());
//             }
            
//             // Iterate through all records
//             System.out.println("\nIterating through all records...");
            
//             for (Record record : table) {
//                 System.out.println("Record: " + record);
//             }
            
//             // Clean up
//             bufferPool.flushAll();
//             System.out.println("\nDemo completed successfully!");
            
//         } catch (IOException e) {
//             System.err.println("Error during demonstration: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }