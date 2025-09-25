// package lite.sqlite.server.storage.buffer;

// import static org.junit.jupiter.api.Assertions.*;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.io.TempDir;

// import java.io.File;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;

// import lite.sqlite.server.storage.BasicFileManager;
// import lite.sqlite.server.storage.BlockId;
// import lite.sqlite.server.storage.Page;
// import lite.sqlite.server.storage.filemanager.FileManager;

// public class BufferPoolTest {

//     @TempDir
//     File tempFolder;
//     private FileManager fileManager;
//     private BufferPool bufferPool;
//     private static final int BLOCK_SIZE = Page.PAGE_SIZE;
//     private static final int POOL_SIZE = 5;
//     private File dbDir;
//     private String testFile = "test_data.dat";
    
//     @BeforeEach
//     public void setUp() throws IOException {
//         dbDir = new File(tempFolder, "test_db");
//         dbDir.mkdirs();
        
//         // Initialize FileManager BEFORE BufferPool
//         fileManager = new BasicFileManager(dbDir);
//         bufferPool = new BufferPool(POOL_SIZE, fileManager);
        
//         // Create test file with some blocks
//         for (int i = 0; i < 10; i++) {
//             // ✅ FIX: Create BlockId first, then use it for Page constructor
//             BlockId blockId = new BlockId(testFile, i);
            
//             // Create the file block
//             if (i == 0) {
//                 fileManager.append(testFile);
//             } else {
//                 fileManager.append(testFile);
//             }
            
//             Page page = new Page(blockId);
//             fillPage(page, i);
//             fileManager.write(blockId, page);
//         }
//     }
    
//     @AfterEach
//     public void tearDown() throws IOException {
//         if (bufferPool != null) {
//             bufferPool.flushAll();
//             bufferPool.close();
//         }
//         if (dbDir != null && dbDir.exists()) {
//             deleteDirectory(dbDir);
//         }
//     }
    
//     @Test
//     public void testPinAndUnpinBlock() throws IOException {
//         BlockId blockId1 = new BlockId(testFile, 1);
//         BlockId blockId2 = new BlockId(testFile, 2);
        
//         // Pin first block (miss)
//         Page page1 = bufferPool.pinBlock(blockId1);
//         assertNotNull(page1, "Pinned page should not be null");
//         assertEquals(1, page1.getInt(0), "Block content incorrect");
        
//         // Pin second block (miss)
//         Page page2 = bufferPool.pinBlock(blockId2);
//         assertNotNull(page2, "Second pinned page should not be null");
//         assertEquals(2, page2.getInt(0), "Block content incorrect");
        
//         // Pin first block again WITHOUT unpinning - guaranteed hit
//         Page page1Again = bufferPool.pinBlock(blockId1);
//         assertSame(page1, page1Again, "Should be same page object");
        
//         // Now test statistics
//         assertEquals(1, bufferPool.getHits(), "Should have 1 cache hit");
//         assertEquals(2, bufferPool.getMisses(), "Should have 2 cache misses");
        
//         // Test frame counts
//         assertEquals(2, bufferPool.getUsedFrames(), "Used frames should be 2");
//         assertEquals(POOL_SIZE - 2, bufferPool.getFreeFrames(), "Free frames should be 3");
        
//         // Clean up - unpin all pins
//         bufferPool.unpinBlock(blockId1);
//         bufferPool.unpinBlock(blockId1); // Unpin twice - we pinned twice
//         bufferPool.unpinBlock(blockId2);
//     }
    
//     @Test
//     public void testBufferReplacement() throws IOException {
//         // Pin enough blocks to fill the buffer pool
//         List<BlockId> blockIds = new ArrayList<>();
//         List<Page> pages = new ArrayList<>();
        
//         for (int i = 0; i < POOL_SIZE; i++) {
//             BlockId blockId = new BlockId(testFile, i);
//             Page page = bufferPool.pinBlock(blockId);
//             blockIds.add(blockId);
//             pages.add(page);
//         }
        
//         assertEquals(POOL_SIZE, bufferPool.getUsedFrames(), "All frames should be used");
//         assertEquals(0, bufferPool.getFreeFrames(), "No frames should be free");
        
//         // Unpin all blocks
//         for (BlockId blockId : blockIds) {
//             bufferPool.unpinBlock(blockId);
//         }
        
//         // Pin a new block - should replace one of the existing blocks
//         BlockId newBlockId = new BlockId(testFile, POOL_SIZE);
        
//         // ✅ FIX: Create the new block with proper Page constructor
//         fileManager.append(testFile);
//         Page newPage = new Page(newBlockId);
//         fillPage(newPage, POOL_SIZE);
//         fileManager.write(newBlockId, newPage);
        
//         Page page = bufferPool.pinBlock(newBlockId);
//         assertNotNull(page, "Should be able to pin a new block");
//         assertEquals(POOL_SIZE, page.getInt(0), "Content should match");
        
//         bufferPool.unpinBlock(newBlockId);
//     }
    
//     @Test
//     public void testDirtyPageFlush() throws IOException {
//         // Pin a block and modify it
//         BlockId blockId = new BlockId(testFile, 3);
//         Page page = bufferPool.pinBlock(blockId);
//         page.setInt(0, 999);
//         page.markDirty();
        
//         // Flush the block
//         bufferPool.flushBlock(blockId);
        
//         // Check if the page is clean now
//         assertFalse(page.isDirty(), "Page should be clean after flush");
        
//         // Unpin and re-pin to verify data persisted
//         bufferPool.unpinBlock(blockId);
//         Page reloadedPage = bufferPool.pinBlock(blockId);
//         assertEquals(999, reloadedPage.getInt(0), "Persisted data should be retrieved");
        
//         bufferPool.unpinBlock(blockId);
//     }
    
//     @Test
//     public void testFlushAll() throws IOException {
//         // Pin multiple blocks and modify them
//         List<BlockId> blockIds = new ArrayList<>();
//         for (int i = 0; i < 3; i++) {
//             BlockId blockId = new BlockId(testFile, i);
//             Page page = bufferPool.pinBlock(blockId);
//             page.setInt(4, 1000 + i);
//             page.markDirty();
//             blockIds.add(blockId);
//         }
        
//         // Flush all dirty pages
//         bufferPool.flushAll();
        
//         // Check that pages are clean after flush
//         for (int i = 0; i < 3; i++) {
//             BlockId blockId = new BlockId(testFile, i);
//             Page page = bufferPool.pinBlock(blockId);
//             assertFalse(page.isDirty(), "Page should be clean after flushAll");
//             bufferPool.unpinBlock(blockId);
//         }
        
//         // Unpin all
//         for (BlockId blockId : blockIds) {
//             bufferPool.unpinBlock(blockId);
//         }
        
//         // Verify data persistence by creating fresh buffer pool
//         bufferPool.close();
//         bufferPool = new BufferPool(POOL_SIZE, fileManager);
        
//         for (int i = 0; i < 3; i++) {
//             BlockId blockId = new BlockId(testFile, i);
//             Page page = bufferPool.pinBlock(blockId);
//             assertEquals(1000 + i, page.getInt(4), "Data should persist after flushAll");
//             bufferPool.unpinBlock(blockId);
//         }
//     }
    
//     @Test
//     public void testBufferFullAndReuse() throws IOException {
//         List<BlockId> allBlockIds = new ArrayList<>();
        
//         // Pin more blocks than capacity
//         for (int i = 0; i < POOL_SIZE + 5; i++) {
//             BlockId blockId = new BlockId(testFile, i);
            
//             // Only create new blocks if they don't exist
//             if (i >= 10) { // We created 10 blocks in setUp
//                 fileManager.append(testFile);
//                 // ✅ FIX: Pass blockId to Page constructor
//                 Page newPage = new Page(blockId);
//                 fillPage(newPage, i);
//                 fileManager.write(blockId, newPage);
//             }
            
//             Page page = bufferPool.pinBlock(blockId);
//             assertNotNull(page, "Should be able to pin block even beyond capacity");
//             allBlockIds.add(blockId);
            
//             // Unpin immediately to allow replacement
//             bufferPool.unpinBlock(blockId);
//         }
        
//         // Buffer should have handled it without exceeding capacity
//         assertTrue(bufferPool.getUsedFrames() <= POOL_SIZE, "Used frames should not exceed capacity");
        
//         // Test that we can still access recently used blocks
//         BlockId recentBlockId = allBlockIds.get(allBlockIds.size() - 1);
//         Page recentPage = bufferPool.pinBlock(recentBlockId);
//         assertNotNull(recentPage, "Recently used block should still be accessible");
//         bufferPool.unpinBlock(recentBlockId);
//     }
    
//     @Test
//     public void testBufferPoolStatistics() throws IOException {
//         // Start with fresh buffer pool for clean statistics
//         bufferPool.close();
//         bufferPool = new BufferPool(POOL_SIZE, fileManager);
        
//         BlockId blockId1 = new BlockId(testFile, 0);
//         BlockId blockId2 = new BlockId(testFile, 1);
        
//         // First access (miss)
//         Page page1 = bufferPool.pinBlock(blockId1);
//         assertEquals(0, bufferPool.getHits(), "Should have 0 hits initially");
//         assertEquals(1, bufferPool.getMisses(), "Should have 1 miss");
        
//         // Second access (miss) 
//         Page page2 = bufferPool.pinBlock(blockId2);
//         assertEquals(0, bufferPool.getHits(), "Should still have 0 hits");
//         assertEquals(2, bufferPool.getMisses(), "Should have 2 misses");
        
//         // Third access (hit)
//         Page page1Again = bufferPool.pinBlock(blockId1);
//         assertEquals(1, bufferPool.getHits(), "Should have 1 hit");
//         assertEquals(2, bufferPool.getMisses(), "Should still have 2 misses");
        
//         // Check hit ratio
//         double expectedHitRatio = (1.0 / 3.0) * 100; // 1 hit out of 3 accesses
//         assertEquals(expectedHitRatio, bufferPool.getHitRatio(), 0.01, "Hit ratio should be correct");
        
//         // Clean up
//         bufferPool.unpinBlock(blockId1);
//         bufferPool.unpinBlock(blockId1); // Unpin twice (pinned twice)
//         bufferPool.unpinBlock(blockId2);
//     }
    
//     @Test
//     public void testCreateNewBlock() throws IOException {
//         // Test creating and pinning a new block
//         BlockId newBlockId = fileManager.append(testFile);
//         Page page = bufferPool.pinBlock(newBlockId);
        
//         page.setInt(0, 5555);
//         page.markDirty();
        
//         // Flush and verify
//         bufferPool.flushBlock(newBlockId);
//         bufferPool.unpinBlock(newBlockId);
        
//         // Repin and verify data persisted
//         Page reloadedPage = bufferPool.pinBlock(newBlockId);
//         assertEquals(5555, reloadedPage.getInt(0), "Data in new block should persist");
//         bufferPool.unpinBlock(newBlockId);
//     }
    
//     @Test
//     public void testDebugSpecific() throws IOException {
//         System.out.println("=== DEBUGGING BUFFER POOL ===");
        
//         // Start with fresh buffer pool
//         bufferPool.close();
//         bufferPool = new BufferPool(POOL_SIZE, fileManager);
        
//         // Test basic pin operation
//         BlockId blockId = new BlockId(testFile, 0);
//         System.out.println("1. Pinning block: " + blockId);
        
//         Page page = bufferPool.pinBlock(blockId);
//         System.out.println("   Page retrieved, data at pos 0: " + page.getInt(0));
//         System.out.println("   Used frames: " + bufferPool.getUsedFrames());
//         System.out.println("   Cache hits: " + bufferPool.getHits());
//         System.out.println("   Cache misses: " + bufferPool.getMisses());
//         System.out.println("   Hit ratio: " + bufferPool.getHitRatio());
        
//         // Test cache hit
//         System.out.println("2. Pinning same block again (should be cache hit):");
//         Page page2 = bufferPool.pinBlock(blockId);
//         System.out.println("   Cache hits: " + bufferPool.getHits());
//         System.out.println("   Cache misses: " + bufferPool.getMisses());
//         System.out.println("   Hit ratio: " + bufferPool.getHitRatio());
        
//         // Verify expectations
//         assertEquals(1, bufferPool.getHits(), "Should have 1 cache hit");
//         assertEquals(1, bufferPool.getMisses(), "Should have 1 cache miss");
        
//         // Clean up
//         bufferPool.unpinBlock(blockId);
//         bufferPool.unpinBlock(blockId);
        
//         System.out.println("=== DEBUG COMPLETE ===");
//     }
    
//     @Test
//     public void testBufferReplacementWithPinning() throws IOException {
//         // Fill buffer pool completely
//         int poolSize = POOL_SIZE;
//         BlockId[] blockIds = new BlockId[poolSize + 1];
        
//         // Pin enough blocks to fill the pool
//         for (int i = 0; i < poolSize; i++) {
//             blockIds[i] = new BlockId(testFile, i);
//             bufferPool.pinBlock(blockIds[i]);
//         }
        
//         assertEquals(poolSize, bufferPool.getUsedFrames(), "All frames should be used");
//         assertEquals(0, bufferPool.getFreeFrames(), "No frames should be free");
        
//         // Keep first block pinned, unpin others
//         for (int i = 1; i < poolSize; i++) {
//             bufferPool.unpinBlock(blockIds[i]);
//         }
        
//         // Create and pin a new block
//         BlockId newBlockId = fileManager.append(testFile);
//         // ✅ FIX: Pass blockId to Page constructor
//         Page newPage = new Page(newBlockId);
//         fillPage(newPage, poolSize);
//         fileManager.write(newBlockId, newPage);
        
//         Page pinnedNewPage = bufferPool.pinBlock(newBlockId);
//         assertNotNull(pinnedNewPage, "Should be able to pin new block");
        
//         // First block should still be in cache since it's pinned
//         Page firstBlockPage = bufferPool.pinBlock(blockIds[0]);
//         assertEquals(0, firstBlockPage.getInt(0), "First block should still be accessible");
        
//         // Cleanup
//         bufferPool.unpinBlock(blockIds[0]);
//         bufferPool.unpinBlock(blockIds[0]); // Unpin twice (pinned twice)
//         bufferPool.unpinBlock(newBlockId);
//     }
    
//     // Helper methods
    
//     private void fillPage(Page page, int id) {
//         page.setInt(0, id);  // Store the block ID at the beginning
        
//         // Fill rest with pattern data (avoid going beyond page bounds)
//         int dataSize = Page.PAGE_SIZE;
//         for (int i = 4; i < dataSize - 4; i += 4) {
//             page.setInt(i, id * 10 + i);
//         }
//     }
    
//     private void deleteDirectory(File dir) {
//         if (dir.isDirectory()) {
//             File[] children = dir.listFiles();
//             if (children != null) {
//                 for (File child : children) {
//                     deleteDirectory(child);
//                 }
//             }
//         }
//         dir.delete();
//     }
// }