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
import lite.sqlite.server.storage.record.RecordPage.RecordWithSlot;
import lite.sqlite.server.storage.record.Schema;


public class Table implements Iterable<Record> {

    private final String tableName;
    private final Schema schema;
    private final BufferPool bufferPool;
    private int recordCount = 0;                    // Track directly in Table
    private long lastModified = System.currentTimeMillis();

    public Table(Schema schema, BufferPool bufferPool, String tableName) {
        this.tableName = tableName;
        this.bufferPool = bufferPool;
        this.schema = schema;
    }
    
    public String getTableName() {
        return this.tableName;
    }
    
    public Schema getSchema() {
        return this.schema;
    }

    public String getFileName() {
        return this.tableName + ".tbl";
    }
    
    /**
     * Inserts a record into the table.
     * 
     * @param record The record to insert
     * @return The RecordId that identifies where the record was inserted
     * @throws IOException if an I/O error occurs
     */
    public RecordId insertRecord(Record record) throws IOException {

        String filename = this.getFileName();
        Block block = new Block(filename, 0); 
        Page page = bufferPool.pinBlock(block);

        RecordPage recordPage = new RecordPage(page, getSchema(), block, bufferPool);
        
        try {
            boolean inserted = recordPage.insert(record.getValues());
            
            if (!inserted) {
                throw new RuntimeException("No space available in the table");
            }
            
            int recordCount = recordPage.getRecordCount();
            int slot = recordCount - 1;
            touch();
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
        RecordPage recordPage = new RecordPage(page, getSchema(), block, bufferPool);
        
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
        RecordPage recordPage = new RecordPage(page, getSchema(), block, bufferPool);
        
        try {
            boolean updated = recordPage.update(slot, record.getValues());
            if (!updated) {
                throw new NoSuchElementException("No record exists at the specified location or update failed");
            }
            touch(); // Update last modified time
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
        RecordPage recordPage = new RecordPage(page, getSchema(), block, bufferPool);
        
        try {
            boolean deleted = recordPage.delete(slot);
            if (!deleted) {
                throw new NoSuchElementException("No record exists at the specified location");
            }
            touch();
        } finally {
            bufferPool.unpinBlock(block);
        }
    }

    private void touch() {
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Returns an iterator over all records in the table.
     * 
     * @return An iterator over all records
     */
    @Override
    public Iterator<Record> iterator() {
        try {
            return new TableIterator();
        } catch (IOException e) {
            throw new RuntimeException("Error creating table iterator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inner class for iterating over the records in the table.
     */
    private class TableIterator implements Iterator<Record> {
        private final RecordPage recordPage;
        private final Block block;
        private final List<RecordWithSlot> records;
        private int currentIndex = 0;
        
        public TableIterator() throws IOException {
            String filename = getFileName();
            this.block = new Block(filename, 0); 
            Page page = bufferPool.pinBlock(block);
            this.recordPage = new RecordPage(page, getSchema(), block, bufferPool);
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
            
            RecordWithSlot recordWithSlot = records.get(currentIndex++);
            return new Record(recordWithSlot.getRecord());
        }
        
    }
}