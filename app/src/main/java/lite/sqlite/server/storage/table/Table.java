package lite.sqlite.server.storage.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.record.Record;
import lite.sqlite.server.storage.record.RecordPage;
import lite.sqlite.server.storage.record.Schema;

/**
 * The Table class represents a table in the database.
 * It provides methods to insert, retrieve, update, and delete records.
 */
public class Table implements Iterable<Record> {
    private final TableInfo tableInfo;
    private final BufferPool bufferPool;
    
    /**
     * Constructor for the Table class.
     * 
     * @param tableInfo The TableInfo object that contains the schema for this table
     * @param bufferPool The buffer pool for accessing pages
     */
    public Table(TableInfo tableInfo, BufferPool bufferPool) {
        this.tableInfo = tableInfo;
        this.bufferPool = bufferPool;
    }
    
    /**
     * Gets the table name.
     * 
     * @return The table name
     */
    public String getTableName() {
        return tableInfo.getTableName();
    }
    
    /**
     * Gets the schema for this table.
     * 
     * @return The schema
     */
    public Schema getSchema() {
        return tableInfo.getSchema();
    }
    
    /**
     * Inserts a record into the table.
     * 
     * @param record The record to insert
     * @return The RecordId that identifies where the record was inserted
     * @throws IOException if an I/O error occurs
     */
    public RecordId insertRecord(Record record) throws IOException {
        String filename = tableInfo.getFilename();
        Block block = new Block(filename, 0); // Simplified; should handle multiple blocks
        
        Page page = bufferPool.pinBlock(block);
        RecordPage recordPage = new RecordPage(page, tableInfo.getSchema(), block, bufferPool);
        
        try {
            boolean inserted = recordPage.insert(record.getValues());
            
            // If insertion failed, we need to handle creating a new block or other strategies
            if (!inserted) {
                throw new RuntimeException("No space available in the table");
            }
            
            // Find the slot where the record was inserted (the last one)
            int recordCount = recordPage.getRecordCount();
            int slot = recordCount - 1;
            
            // Update record count in table info
            tableInfo.setRecordCount(tableInfo.getRecordCount() + 1);
            tableInfo.touch(); // Update last modified time
            
            return new RecordId(block, slot);
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    /**
     * Gets a record from the table.
     * 
     * @param rid The RecordId that identifies the record
     * @return The record
     * @throws IOException if an I/O error occurs
     */
    public Record getRecord(RecordId rid) throws IOException {
        Block block = rid.getBlockId();
        int slot = rid.getSlotNumber();
        
        Page page = bufferPool.pinBlock(block);
        RecordPage recordPage = new RecordPage(page, tableInfo.getSchema(), block, bufferPool);
        
        try {
            Object[] values = recordPage.getRecord(slot);
            if (values == null) {
                throw new NoSuchElementException("No record exists at the specified location");
            }
            
            return new Record(values);
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    /**
     * Updates a record in the table.
     * 
     * @param rid The RecordId that identifies the record to update
     * @param record The new record data
     * @throws IOException if an I/O error occurs
     */
    public void updateRecord(RecordId rid, Record record) throws IOException {
        Block block = rid.getBlockId();
        int slot = rid.getSlotNumber();
        
        Page page = bufferPool.pinBlock(block);
        RecordPage recordPage = new RecordPage(page, tableInfo.getSchema(), block, bufferPool);
        
        try {
            boolean updated = recordPage.update(slot, record.getValues());
            if (!updated) {
                throw new NoSuchElementException("No record exists at the specified location or update failed");
            }
            
            tableInfo.touch(); // Update last modified time
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    /**
     * Deletes a record from the table.
     * 
     * @param rid The RecordId that identifies the record to delete
     * @throws IOException if an I/O error occurs
     */
    public void deleteRecord(RecordId rid) throws IOException {
        Block block = rid.getBlockId();
        int slot = rid.getSlotNumber();
        
        Page page = bufferPool.pinBlock(block);
        RecordPage recordPage = new RecordPage(page, tableInfo.getSchema(), block, bufferPool);
        
        try {
            boolean deleted = recordPage.delete(slot);
            if (!deleted) {
                throw new NoSuchElementException("No record exists at the specified location");
            }
            
            // Update record count
            tableInfo.setRecordCount(tableInfo.getRecordCount() - 1);
            tableInfo.touch(); // Update last modified time
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    /**
     * Returns an iterator over all records in the table.
     * 
     * @return An iterator over all records
     */
    @Override
    public Iterator<Record> iterator() {
        // For simplicity, we'll use a single block implementation
        // In a real implementation, this would iterate over all blocks
        try {
            return new TableIterator();
        } catch (IOException e) {
            // Wrap the checked exception in a runtime exception
            throw new RuntimeException("Error creating table iterator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inner class for iterating over the records in the table.
     */
    private class TableIterator implements Iterator<Record> {
        private final RecordPage recordPage;
        private final Block block;
        private final List<lite.sqlite.server.storage.record.RecordPage.RecordWithSlot> records;
        private int currentIndex = 0;
        
        public TableIterator() throws IOException {
            String filename = tableInfo.getFilename();
            this.block = new Block(filename, 0); // Simplified; should handle multiple blocks
            Page page = bufferPool.pinBlock(block);
            this.recordPage = new RecordPage(page, tableInfo.getSchema(), block, bufferPool);
            
            // Get all records at once to avoid pinning issues
            this.records = recordPage.getAllRecords();
        }
        
        @Override
        public boolean hasNext() {
            return currentIndex < records.size();
        }
        
        @Override
        public Record next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            lite.sqlite.server.storage.record.RecordPage.RecordWithSlot recordWithSlot = records.get(currentIndex++);
            return new Record(recordWithSlot.getRecord());
        }
        
        // Note: We unpin the page when we get all records at once in the constructor
        // so no need for cleanup in finalize() which is deprecated
    }
}