package lite.sqlite.server.storage.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.index.Index;
import lite.sqlite.server.storage.record.DataType;
import lite.sqlite.server.storage.record.Record;
import lite.sqlite.server.storage.record.RecordPage;
import lite.sqlite.server.storage.record.RecordPage.RecordWithSlot;
import lite.sqlite.server.storage.record.Schema;

public class Table implements Iterable<Record> {

    private final String tableName;
    private final Schema schema;
    private final BufferPool bufferPool;
    private List<Index<?>> indexes;  // Add this field
    
    public Table(Schema schema, BufferPool bufferPool, String tableName) {
        this.tableName = tableName;
        this.bufferPool = bufferPool;
        this.schema = schema;
        this.indexes = new ArrayList<>();  // Initialize here
    }
    
    // Index management methods
    public Index<?> createTypedIndex(String columnName, String tableName, String indexName, boolean isUnique, DataType columnType) throws IOException {
        // Check if index already exists
        for (Index<?> existingIndex : indexes) {
            if (existingIndex.getIndexName().equals(indexName)) {
                throw new IllegalArgumentException("Index '" + indexName + "' already exists");
            }
        }
        
        // Validate column exists
        int columnIndex = schema.getColumnIndex(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
        }
        
        // Create index based on column type
        Index<?> newIndex;
        switch (columnType) {
            case INTEGER:
                newIndex = new Index<Integer>(indexName, tableName, columnName, isUnique, 100);
                break;
            case VARCHAR:
                newIndex = new Index<String>(indexName, tableName, columnName, isUnique, 100);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported column type for indexing: " + columnType);
        }
        
        // Populate index with existing data
        populateIndex(newIndex, columnIndex);
        
        // Add to indexes list
        indexes.add(newIndex);
        
        return newIndex;
    }
    
    public Index<?> findIndexForColumn(String columnName) {
        for (Index<?> index : indexes) {
            if (index.getColumnName().equals(columnName)) {
                return index;
            }
        }
        return null;
    }
    
    public List<Index<?>> getIndexes() {
        return new ArrayList<>(indexes);
    }
    
    private void populateIndex(Index<?> index, int columnIndex) throws IOException {
        String filename = getFileName();
        int blockNum = 0;
        
        while (true) {
            try {
                Block block = new Block(filename, blockNum);
                Page page = bufferPool.pinBlock(block);
                
                try {
                    RecordPage recordPage = new RecordPage(page, schema, block, bufferPool);
                    
                    for (RecordWithSlot rws : recordPage.getAllRecords()) {
                        Object[] values = rws.getRecord();
                        Object valueObj = values[columnIndex];
                        
                        if (valueObj != null && valueObj instanceof Comparable) {
                            try {
                                RecordId rid = rws.toRecordId();
                                updateIndexTyped(index, (Comparable) valueObj, rid);
                            } catch (Exception e) {
                                System.err.println("Warning: Failed to index record: " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    bufferPool.unpinBlock(block);
                }
                
                blockNum++;
            } catch (Exception e) {
                break; // No more blocks
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <K extends Comparable<K>> void updateIndexTyped(Index<?> index, Comparable value, RecordId rid) {
        ((Index<K>) index).insert((K) value, rid);
    }
    
    public RecordId insertRecord(Record record) throws IOException {
        String filename = this.getFileName();
        Block block = new Block(filename, 0); 
        Page page = bufferPool.pinBlock(block);

        RecordPage recordPage = new RecordPage(page, getSchema(), block, bufferPool);
        
        try {
            if (!indexes.isEmpty()) {
                for (Index<?> index : indexes) {
                    if (index.isUnique()) {
                        String colName = index.getColumnName();
                        int colIndex = schema.getColumnIndex(colName);
                        Object value = record.getValues()[colIndex];
                        
                        if (value instanceof Comparable) {
                            RecordId existingRid = searchInIndexTyped(index, (Comparable) value);
                            if (existingRid != null) {
                                throw new IllegalArgumentException(
                                    "Duplicate key '" + value + "' in unique index '" + 
                                    index.getIndexName() + "'"
                                );
                            }
                        }
                    }
                }
            }
            
            boolean inserted = recordPage.insert(record.getValues());
            
            if (!inserted) {
                throw new RuntimeException("No space available in the table");
            }
            
            int recordCount = recordPage.getRecordCount();
            int slot = recordCount - 1;
            
            RecordId rid = new RecordId(block, slot);
            
            // Update all indexes AFTER successful insert
            if (!indexes.isEmpty()) {
                for (Index<?> index : indexes) {
                    String colName = index.getColumnName();
                    int colIndex = schema.getColumnIndex(colName);
                    Object value = record.getValues()[colIndex];
                    
                    if (value instanceof Comparable) {
                        try {
                            updateIndexTyped(index, (Comparable) value, rid);
                        } catch (Exception e) {
                            System.err.println("Warning: Failed to update index: " + e.getMessage());
                        }
                    }
                }
            }
            
            touch();
            return rid;
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <K extends Comparable<K>> RecordId searchInIndexTyped(Index<?> index, Comparable value) {
        return ((Index<K>) index).search((K) value);
    }
    
    public Record getRecord(RecordId rid) throws IOException {
        Block block = rid.getBlockId();
        Page page = bufferPool.pinBlock(block);
        
        try {
            RecordPage recordPage = new RecordPage(page, schema, block, bufferPool);
            Object[] values = recordPage.getRecord(rid.getSlotNumber());
            
            if (values == null) {
                return null;
            }
            
            return new Record(values);
        } finally {
            bufferPool.unpinBlock(block);
        }
    }
    
    // Existing methods
    public Schema getSchema() {
        return schema;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    private String getFileName() {
        return tableName + ".tbl";
    }
    
    private void touch() throws IOException {
        // Implementation for table metadata updates
    }
    
    @Override
    public Iterator<Record> iterator() {
        return new TableIterator();
    }
    
    private class TableIterator implements Iterator<Record> {
        private int currentBlockNum = 0;
        private List<RecordWithSlot> currentRecords = new ArrayList<>();
        private int currentRecordIndex = 0;
        private boolean hasMoreBlocks = true;
        
        public TableIterator() {
            loadNextBlock();
        }
        
        private void loadNextBlock() {
            currentRecords.clear();
            currentRecordIndex = 0;
            
            if (!hasMoreBlocks) return;
            
            try {
                String filename = getFileName();
                Block block = new Block(filename, currentBlockNum);
                Page page = bufferPool.pinBlock(block);
                
                try {
                    RecordPage recordPage = new RecordPage(page, schema, block, bufferPool);
                    currentRecords = recordPage.getAllRecords();
                    currentBlockNum++;
                } finally {
                    bufferPool.unpinBlock(block);
                }
            } catch (Exception e) {
                hasMoreBlocks = false;
            }
            
            if (currentRecords.isEmpty()) {
                hasMoreBlocks = false;
            }
        }
        
        @Override
        public boolean hasNext() {
            if (currentRecordIndex < currentRecords.size()) {
                return true;
            }
            
            if (hasMoreBlocks) {
                loadNextBlock();
                return currentRecordIndex < currentRecords.size();
            }
            
            return false;
        }
        
        @Override
        public Record next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            RecordWithSlot rws = currentRecords.get(currentRecordIndex++);
            return new Record(rws.getRecord());
        }
    }
}