package lite.sqlite.server.storage.record;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.buffer.BufferPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecordPageTest {

    private Schema schema;
    private Page page;
    private Block blockId;
    private BufferPool bufferPool;
    private RecordPage recordPage;
    
    @BeforeEach
    public void setUp() {
        // Create schema
        schema = new Schema();
        schema.addColumn("id", DataType.INTEGER);
        schema.addColumn("name", DataType.VARCHAR, 20);
        schema.addColumn("email", DataType.VARCHAR, 30);
        
        // Create mocks
        page = new Page(); // Using no-args constructor based on your updated Page design
        blockId = mock(Block.class);
        when(blockId.getFileName()).thenReturn("test.tbl");
        when(blockId.getBlockNum()).thenReturn(1);
        
        bufferPool = mock(BufferPool.class);
        
        // Create RecordPage
        recordPage = new RecordPage(page, schema, blockId, bufferPool);
    }
    
    @Test
    public void testInsertAndRetrieve() {
        // Insert a record
        Object[] record1 = {1, "John", "john@example.com"};
        boolean result = recordPage.insert(record1);
        
        // Verify insertion succeeded
        assertTrue(result, "Insert should succeed");
        verify(bufferPool).markDirtyBlock(blockId);  // Use correct method name
        
        // Retrieve the record
        Object[] retrieved = recordPage.getRecord(0);
        assertNotNull(retrieved, "Retrieved record should not be null");
        
        // Verify record contents
        assertEquals(1, retrieved[0], "ID should match");
        assertEquals("John", retrieved[1], "Name should match");
        assertEquals("john@example.com", retrieved[2], "Email should match");
    }
    
    @Test
    public void testMultipleInserts() {
        // Insert multiple records
        Object[] record1 = {1, "John", "john@example.com"};
        Object[] record2 = {2, "Jane", "jane@example.com"};
        Object[] record3 = {3, "Bob", "bob@example.com"};
        
        assertTrue(recordPage.insert(record1));
        assertTrue(recordPage.insert(record2));
        assertTrue(recordPage.insert(record3));
        
        // Verify retrieval
        assertEquals(1, recordPage.getRecord(0)[0]);
        assertEquals(2, recordPage.getRecord(1)[0]);
        assertEquals(3, recordPage.getRecord(2)[0]);
        
        // Verify getAllRecords
        List<RecordPage.RecordWithSlot> allRecords = recordPage.getAllRecords();
        assertEquals(3, allRecords.size(), "Should have 3 records");
    }
    
    @Test
    public void testUpdate() {
        // Insert a record
        Object[] record1 = {1, "John", "john@example.com"};
        recordPage.insert(record1);
        
        // Update the record
        Object[] updatedRecord = {1, "Johnny", "johnny@example.com"};
        boolean result = recordPage.update(0, updatedRecord);
        
        // Verify update succeeded
        assertTrue(result, "Update should succeed");
        verify(bufferPool, atLeast(2)).markDirtyBlock(blockId);  // Use correct method name
        
        // Retrieve and verify
        Object[] retrieved = recordPage.getRecord(0);
        assertEquals("Johnny", retrieved[1], "Name should be updated");
        assertEquals("johnny@example.com", retrieved[2], "Email should be updated");
    }
    
    @Test
    public void testDelete() {
        // Insert records
        Object[] record1 = {1, "John", "john@example.com"};
        Object[] record2 = {2, "Jane", "jane@example.com"};
        recordPage.insert(record1);
        recordPage.insert(record2);
        
        // Delete first record
        boolean result = recordPage.delete(0);
        
        // Verify delete succeeded
        assertTrue(result, "Delete should succeed");
        verify(bufferPool, atLeast(3)).markDirtyBlock(blockId);  // Use correct method name
        
        // Verify record is gone
        assertNull(recordPage.getRecord(0), "Record should be deleted");
        assertNotNull(recordPage.getRecord(1), "Second record should remain");
    }
    
    @Test
    public void testFragmentationAndCompaction() {
        // Insert multiple records
        for (int i = 0; i < 10; i++) {
            Object[] record = {i, "User" + i, "user" + i + "@example.com"};
            recordPage.insert(record);
        }
        
        // Delete some records to create fragmentation
        recordPage.delete(1);
        recordPage.delete(3);
        recordPage.delete(5);
        recordPage.delete(7);
        recordPage.delete(9);
        
        // Force compaction by inserting a record that needs space
        StringBuilder largeName = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            largeName.append("X");
        }
        Object[] largeRecord = {99, largeName.toString(), "large@example.com"};
        
        // This should trigger compaction
        boolean result = recordPage.insert(largeRecord);
        assertTrue(result, "Insert after compaction should succeed");
        
        // Verify remaining records are still accessible
        assertNotNull(recordPage.getRecord(0), "Record 0 should exist");
        assertNotNull(recordPage.getRecord(2), "Record 2 should exist");
        assertNotNull(recordPage.getRecord(4), "Record 4 should exist");
    }
    
    @Test
    public void testInsufficientSpace() {
        // Fill page with MUCH LARGER data
        for (int i = 0; i < 50; i++) {
            StringBuilder name = new StringBuilder();
            StringBuilder email = new StringBuilder();
            
            for (int j = 0; j < 50; j++) {
                name.append("X");
                email.append("Y");
            }
            
            Object[] record = {i, name.toString(), email.toString()};
            boolean inserted = recordPage.insert(record);
            
            if (!inserted) {
                System.out.println("Page filled after " + i + " records");
                break;
            }
        }
        
        System.out.println("Current record count: " +recordPage.getRecordCount());
        
        StringBuilder largeName = new StringBuilder();
        StringBuilder largeEmail = new StringBuilder();
        for (int i = 0; i < 100; i++) {  
            largeName.append("X");
            largeEmail.append("Y");
        }
        Object[] largeRecord = {999, largeName.toString(), largeEmail.toString()};
        
        boolean result = recordPage.insert(largeRecord);
        assertFalse(result, "Insert should fail due to insufficient space");
    }
}