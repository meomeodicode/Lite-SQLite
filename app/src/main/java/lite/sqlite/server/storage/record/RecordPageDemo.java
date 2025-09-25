// package lite.sqlite.server.storage.record;

// import lite.sqlite.server.storage.BasicFileManager;
// import lite.sqlite.server.storage.Block;
// import lite.sqlite.server.storage.Page;
// import lite.sqlite.server.storage.buffer.BufferPool;
// import lite.sqlite.server.storage.filemanager.FileManager;

// import java.nio.ByteBuffer;
// import java.util.List;
// import java.io.File;
// import java.io.IOException;

// public class RecordPageDemo {
//     public static void main(String[] args) {
//         System.out.println("====== RecordPage Demo ======");
        
//         // Create schema for testing
//         Schema schema = new Schema();
//         schema.addColumn("id", DataType.INTEGER);
//         schema.addColumn("name", DataType.VARCHAR, 20);
//         schema.addColumn("email", DataType.VARCHAR, 30);
        
//         // Create test Page
//         Page page = new Page();
        
//         // Create test Block
//         Block blockId = new Block("test.tbl", 1);
        
//         int poolSize = 10; // Small pool size for testing
//         File demoDir = new File("demo");
//         FileManager fileManager = new BasicFileManager(demoDir);

//         // Create a real BufferPool
//         BufferPool bufferPool = new BufferPool(poolSize, fileManager);
        
//         // Create RecordPage
//         RecordPage recordPage = new RecordPage(page, schema, blockId, bufferPool);
        
//         try {
//             // Test visualization
//             System.out.println("\n=== INITIAL STATE ===");
//             visualizePage(recordPage, page);
            
//             // Test 1: Insert records until failure
//             System.out.println("\n=== TEST 1: Insert Until Full ===");
//             int recordsInserted = 0;
//             while (true) {
//                 StringBuilder name = new StringBuilder();
//                 StringBuilder email = new StringBuilder();
                
//                 // Create strings with predictable sizes
//                 for (int j = 0; j < 10; j++) {
//                     name.append((char)('A' + (recordsInserted % 26)));
//                     email.append((char)('a' + (recordsInserted % 26)));
//                 }
//                 email.append("@example.com");
                
//                 Object[] record = {recordsInserted, name.toString(), email.toString()};
//                 boolean success = recordPage.insert(record);
                
//                 if (!success) {
//                     System.out.println("\n=== PAGE FULL ===");
//                     System.out.println("Inserted " + recordsInserted + " records before filling");
//                     break;
//                 }
                
//                 recordsInserted++;
                
//                 // Show progress every 10 records
//                 if (recordsInserted % 10 == 0) {
//                     System.out.println("\n=== AFTER " + recordsInserted + " RECORDS ===");
//                     visualizePage(recordPage, page);
//                 }
//             }
            
//             // Test 2: Retrieve records
//             System.out.println("\n=== TEST 2: Retrieve Records ===");
//             Object[] record5 = recordPage.getRecord(5);
//             if (record5 != null) {
//                 System.out.println("Record #5: id=" + record5[0] + 
//                                   ", name=" + record5[1] + 
//                                   ", email=" + record5[2]);
//             }
            
//             // Test 3: Update a record
//             System.out.println("\n=== TEST 3: Update Record ===");
//             Object[] updatedRecord = {99, "UPDATED", "updated@example.com"};
//             boolean updateResult = recordPage.update(10, updatedRecord);
//             System.out.println("Update result: " + updateResult);
            
//             if (updateResult) {
//                 Object[] retrieved = recordPage.getRecord(10);
//                 System.out.println("Updated record #10: id=" + retrieved[0] + 
//                                   ", name=" + retrieved[1] + 
//                                   ", email=" + retrieved[2]);
//             }
            
//             // Test 4: Delete records
//             System.out.println("\n=== TEST 4: Delete Records ===");
//             for (int i = 0; i < recordsInserted; i += 2) {
//                 recordPage.delete(i);
//             }
//             System.out.println("Deleted every other record");
//             visualizePage(recordPage, page);
            
//             // Test 5: Insert after deletion (should work now that space is available)
//             System.out.println("\n=== TEST 5: Insert After Delete ===");
//             Object[] newRecord = {999, "NEW_RECORD", "new@example.com"};
//             boolean insertResult = recordPage.insert(newRecord);
//             System.out.println("Insert after delete result: " + insertResult);
            
//             // Test 6: Get all records
//             System.out.println("\n=== TEST 6: Get All Records ===");
//             List<RecordPage.RecordWithSlot> allRecords = recordPage.getAllRecords();
//             System.out.println("Found " + allRecords.size() + " active records");
            
//             // Show first 3 records (if available)
//             for (int i = 0; i < Math.min(3, allRecords.size()); i++) {
//                 RecordPage.RecordWithSlot rws = allRecords.get(i);
//                 Object[] rec = rws.getRecord();
//                 System.out.println("Record slot " + rws.getSlot() + 
//                                  ": id=" + rec[0] + 
//                                  ", name=" + rec[1] + 
//                                  ", email=" + rec[2]);
//             }
            
//             // Final state
//             System.out.println("\n=== FINAL STATE ===");
//             visualizePage(recordPage, page);
            
//         } catch (Exception e) {
//             System.err.println("Error during demo: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
    
//     /**
//      * Visualizes the current page layout
//      */
//     private static void visualizePage(RecordPage recordPage, Page page) {
//         try {
//             // Use reflection to access private fields
//             java.lang.reflect.Method getRecordCountMethod = 
//                 RecordPage.class.getDeclaredMethod("getRecordCount");
//             getRecordCountMethod.setAccessible(true);
//             int recordCount = (int) getRecordCountMethod.invoke(recordPage);
            
//             java.lang.reflect.Method getFreeSpacePointerMethod = 
//                 RecordPage.class.getDeclaredMethod("getFreeSpacePointer");
//             getFreeSpacePointerMethod.setAccessible(true);
//             int freePointer = (int) getFreeSpacePointerMethod.invoke(recordPage);
            
//             // Constants that should match RecordPage
//             final int HEADER_SIZE = 32;
//             final int DIRECTORY_OFFSET = 0;
//             final int SLOT_SIZE = 8;
//             final int PAGE_SIZE = Page.PAGE_SIZE;
            
//             int directorySpace = HEADER_SIZE + (recordCount * SLOT_SIZE);
//             int usedDataSpace = PAGE_SIZE - freePointer;
//             int freeSpace = freePointer - directorySpace;
            
//             System.out.println("=== PAGE LAYOUT ===");
//             System.out.println("Total size: " + PAGE_SIZE + " bytes");
//             System.out.println("Records: " + recordCount);
            
//             // Visual representation
//             StringBuilder visual = new StringBuilder("[");
            
//             // Header (scaled down for display)
//             int headerScale = Math.max(1, HEADER_SIZE / 20);
//             for (int i = 0; i < HEADER_SIZE; i += headerScale) {
//                 visual.append("H");
//             }
            
//             // Directory (scaled down for display)
//             int directoryWithoutHeader = directorySpace - HEADER_SIZE;
//             int dirScale = Math.max(1, directoryWithoutHeader / 20);
//             for (int i = 0; i < directoryWithoutHeader; i += dirScale) {
//                 visual.append("D");
//             }
            
//             // Free space (scaled down for display)
//             int freeScale = Math.max(1, freeSpace / 30);
//             for (int i = 0; i < freeSpace; i += freeScale) {
//                 visual.append(".");
//             }
            
//             // Used data (scaled down for display)
//             int dataScale = Math.max(1, usedDataSpace / 30);
//             for (int i = 0; i < usedDataSpace; i += dataScale) {
//                 visual.append("X");
//             }
            
//             visual.append("]");
//             System.out.println(visual.toString());
//             System.out.println("H: Header | D: Directory | .: Free | X: Data");
            
//             // Print statistics
//             System.out.println("Directory space: " + directorySpace + " bytes (" + 
//                               (directorySpace * 100 / PAGE_SIZE) + "%)");
//             System.out.println("Free space: " + freeSpace + " bytes (" + 
//                               (freeSpace * 100 / PAGE_SIZE) + "%)");
//             System.out.println("Used data space: " + usedDataSpace + " bytes (" + 
//                               (usedDataSpace * 100 / PAGE_SIZE) + "%)");
            
//             // Calculate and show fragmentation ratio
//             java.lang.reflect.Method getFragmentationRatioMethod = 
//                 RecordPage.class.getDeclaredMethod("getFragmentationRatio");
//             getFragmentationRatioMethod.setAccessible(true);
//             double fragRatio = (double) getFragmentationRatioMethod.invoke(recordPage);
//             System.out.println("Fragmentation: " + (fragRatio * 100) + "%");
            
//         } catch (Exception e) {
//             System.err.println("Error visualizing page: " + e.getMessage());
//         }
//     }
    
//     /**
//      * Dumps the raw bytes of a page
//      */
//     private static void dumpPage(Page page) {
//         System.out.println("\n=== PAGE HEX DUMP ===");
//         // Get first 200 bytes to see header and some data
//         ByteBuffer buffer = page.contents();
//         buffer.position(0);
//         byte[] data = new byte[Math.min(200, buffer.capacity())];
//         buffer.get(data);
        
//         // Print hex representation
//         for (int i = 0; i < data.length; i++) {
//             System.out.printf("%02X ", data[i]);
//             if ((i + 1) % 16 == 0) System.out.println();
//         }
//         System.out.println("\n====================");
//     }
// }