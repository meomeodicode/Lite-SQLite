package lite.sqlite.server.storage.buffer;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lite.sqlite.server.storage.BasicFileManager;
import lite.sqlite.server.storage.BlockId;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.filemanager.FileManager;

public class BufferPoolTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private FileManager fileManager;
    private BufferPool bufferPool;
    private static final int BLOCK_SIZE = Page.PAGE_SIZE;
    private static final int POOL_SIZE = 5;
    private File dbDir;
    private String testFile = "test_data.dat";
    
    @Before
    public void setUp() throws IOException {
        dbDir = tempFolder.newFolder("test_db");
        fileManager = new BasicFileManager(dbDir);
        bufferPool = new BufferPool(POOL_SIZE, fileManager);
        
        // Create test file with some blocks
        for (int i = 0; i < 10; i++) {
            BlockId blockId = new BlockId(testFile, i);
            Page page = new Page(i);
            fillPage(page, i);
            fileManager.write(blockId, page);
        }
    }
    
    @After
    public void tearDown() throws IOException {
        if (bufferPool != null) {
            bufferPool.close();
        }
    }
    
@Test
public void testPinAndUnpinBlock() throws IOException {
    BlockId blockId1 = new BlockId(testFile, 1);
    BlockId blockId2 = new BlockId(testFile, 2);
    
    // Pin first block (miss)
    Page page1 = bufferPool.pinBlock(blockId1);
    assertNotNull("Pinned page should not be null", page1);
    assertEquals("Block content incorrect", 1, page1.getInt(0));
    
    // Pin second block (miss)
    Page page2 = bufferPool.pinBlock(blockId2);
    assertNotNull("Second pinned page should not be null", page2);
    assertEquals("Block content incorrect", 2, page2.getInt(0));
    
    // Pin first block again WITHOUT unpinning - guaranteed hit
    Page page1Again = bufferPool.pinBlock(blockId1);
    assertSame("Should be same page object", page1, page1Again);
    
    // Now test statistics
    assertEquals("Should have 1 cache hit", 1, bufferPool.getHits());
    assertEquals("Should have 2 cache misses", 2, bufferPool.getMisses());
    
    // Test frame counts
    assertEquals("Used frames should be 2", 2, bufferPool.getUsedFrames());
    assertEquals("Free frames should be 3", POOL_SIZE - 2, bufferPool.getFreeFrames());
    
    // Clean up - unpin all pins
    bufferPool.unpinBlock(blockId1);
    bufferPool.unpinBlock(blockId1); // Unpin twice - we pinned twice
    bufferPool.unpinBlock(blockId2);
}
    
    @Test
    public void testBufferReplacement() throws IOException {
        // Pin enough blocks to fill the buffer pool
        List<BlockId> blockIds = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            BlockId blockId = new BlockId(testFile, i);
            bufferPool.pinBlock(blockId);
            blockIds.add(blockId);
        }
        
        assertEquals("All frames should be used", POOL_SIZE, bufferPool.getUsedFrames());
        assertEquals("No frames should be free", 0, bufferPool.getFreeFrames());
        
        // Unpin all blocks
        for (BlockId blockId : blockIds) {
            bufferPool.unpinBlock(blockId);
        }
        
        // Pin a new block - should replace one of the existing blocks
        BlockId newBlockId = new BlockId(testFile, POOL_SIZE);
        Page page = bufferPool.pinBlock(newBlockId);
        assertNotNull("Should be able to pin a new block", page);
        assertEquals("Content should match", POOL_SIZE, page.getInt(0));
    }
    
    @Test
    public void testDirtyPageFlush() throws IOException {
        // Pin a block and modify it
        BlockId blockId = new BlockId(testFile, 3);
        Page page = bufferPool.pinBlock(blockId);
        page.setInt(0, 999);
        page.markDirty();
        
        // Flush the block
        bufferPool.flushBlock(blockId);
        
        // Check if the page is clean now
        assertFalse("Page should be clean after flush", page.isDirty());
        
        // Unpin and re-pin to verify data persisted
        bufferPool.unpinBlock(blockId);
        Page reloadedPage = bufferPool.pinBlock(blockId);
        assertEquals("Persisted data should be retrieved", 999, reloadedPage.getInt(0));
    }
    
    @Test
    public void testFlushAll() throws IOException {
        // Pin multiple blocks and modify them
        List<BlockId> blockIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BlockId blockId = new BlockId(testFile, i);
            Page page = bufferPool.pinBlock(blockId);
            page.setInt(4, 1000 + i);
            page.markDirty();
            blockIds.add(blockId);
        }
        
        // Flush all dirty pages
        bufferPool.flushAll();
        
        // Unpin all
        for (BlockId blockId : blockIds) {
            bufferPool.unpinBlock(blockId);
        }
        
        // Verify data persistence
        for (int i = 0; i < 3; i++) {
            BlockId blockId = new BlockId(testFile, i);
            Page page = bufferPool.pinBlock(blockId);
            assertEquals("Data should persist after flushAll", 1000 + i, page.getInt(4));
            bufferPool.unpinBlock(blockId);
        }
    }
    
    @Test
    public void testBufferFullAndReuse() throws IOException {
        // Pin more blocks than capacity
        for (int i = 0; i < POOL_SIZE + 5; i++) {
            BlockId blockId = new BlockId(testFile, i);
            Page page = bufferPool.pinBlock(blockId);
            assertNotNull("Should be able to pin block even beyond capacity", page);
            
            // Unpin immediately to allow replacement
            bufferPool.unpinBlock(blockId);
        }
        
        // Buffer should have handled it
        assertEquals("Used frames should not exceed capacity", Math.min(POOL_SIZE + 5, POOL_SIZE), bufferPool.getUsedFrames());
        
        // Test hit ratio - we expect some cache hits due to LRU-2
        assertTrue("Should have some cache hits", bufferPool.getHitRatio() > 0);
    }
    
    @Test
    public void testBufferPoolStatistics() throws IOException {
        // Pin some blocks multiple times to generate statistics
        BlockId blockId1 = new BlockId(testFile, 0);
        BlockId blockId2 = new BlockId(testFile, 1);
        
        // First access (miss)
        bufferPool.pinBlock(blockId1);
        
        // Second access (miss) 
        bufferPool.pinBlock(blockId2);
        
        // Third access (hit)
        bufferPool.pinBlock(blockId1);
        
        // Check stats
        assertEquals("Should have 1 hit", 1, bufferPool.getHits());
        assertEquals("Should have 2 misses", 2, bufferPool.getMisses());
        
        double expectedHitRatio = 100 / 3.0; // 1 hit out of 3 accesses
        assertEquals("Hit ratio should be correct", expectedHitRatio, bufferPool.getHitRatio(), 0.01);
    }
    
    @Test
    public void testCreateNewBlock() throws IOException {
        // Test pinning a new block (this would be part of BufferManager normally)
        BlockId newBlockId = fileManager.append(testFile);
        Page page = bufferPool.pinBlock(newBlockId);
        
        page.setInt(0, 5555);
        page.markDirty();
        
        // Flush and verify
        bufferPool.flushBlock(newBlockId);
        bufferPool.unpinBlock(newBlockId);
        
        // Repin and verify data
        Page reloadedPage = bufferPool.pinBlock(newBlockId);
        assertEquals("Data in new block should persist", 5555, reloadedPage.getInt(0));
    }
    
    private void fillPage(Page page, int id) {
        page.setInt(0, id);  // Store the block ID at the beginning of data area
        
        // Fill rest with pattern data in the data area
        for (int i = 4; i < Page.getDataSize(); i += 4) {
            if (i + 4 <= Page.getDataSize()) {
                page.setInt(i, id * 10 + i);
            }
        }
    }
    @Test
public void testDebugSpecific() throws IOException {
    System.out.println("=== DEBUGGING BUFFER POOL ===");
    
    // Test basic pin operation
    BlockId blockId = new BlockId(testFile, 0);
    System.out.println("1. Pinning block: " + blockId);
    
    Page page = bufferPool.pinBlock(blockId);
    System.out.println("   Page retrieved, data at pos 0: " + page.getInt(0));
    System.out.println("   Used frames: " + bufferPool.getUsedFrames());
    System.out.println("   Cache hits: " + bufferPool.getHits());
    System.out.println("   Cache misses: " + bufferPool.getMisses());
    System.out.println("   Hit ratio: " + bufferPool.getHitRatio());
    
    // Test cache hit
    System.out.println("2. Pinning same block again (should be cache hit):");
    Page page2 = bufferPool.pinBlock(blockId);
    System.out.println("   Cache hits: " + bufferPool.getHits());
    System.out.println("   Cache misses: " + bufferPool.getMisses());
    System.out.println("   Hit ratio: " + bufferPool.getHitRatio());
    
    // Verify expectations
    assertEquals("Should have 1 cache hit", 1, bufferPool.getHits());
    assertEquals("Should have 1 cache miss", 1, bufferPool.getMisses());
    
    System.out.println("=== DEBUG COMPLETE ===");
}
    @Test
public void testBufferReplacementWithPinning() throws IOException {
    // Fill buffer pool completely
    int poolSize = bufferPool.getPoolSize();
    BlockId[] blockIds = new BlockId[poolSize + 1];
    
    // Pin enough blocks to fill the pool
    for (int i = 0; i < poolSize; i++) {
        blockIds[i] = new BlockId(testFile, i);
        bufferPool.pinBlock(blockIds[i]);
    }
    
    assertEquals("All frames should be used", poolSize, bufferPool.getUsedFrames());
    assertEquals("No frames should be free", 0, bufferPool.getFreeFrames());
    
    // Keep first block pinned, unpin others
    for (int i = 1; i < poolSize; i++) {
        bufferPool.unpinBlock(blockIds[i]);
    }
    
    // Try to pin a new block - should work by replacing one of the unpinned blocks
    BlockId newBlockId = new BlockId(testFile, poolSize);
    Page newPage = bufferPool.pinBlock(newBlockId);
    assertNotNull("Should be able to pin new block", newPage);
    
    // First block should still be in cache since it's pinned
    Page firstBlockPage = bufferPool.pinBlock(blockIds[0]);
    assertEquals("First block should still be accessible", 0, firstBlockPage.getInt(0));
    
    // Cleanup
    bufferPool.unpinBlock(blockIds[0]);
    bufferPool.unpinBlock(blockIds[0]);
    bufferPool.unpinBlock(newBlockId);
}
}