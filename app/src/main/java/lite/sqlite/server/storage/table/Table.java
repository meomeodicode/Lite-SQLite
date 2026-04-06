package lite.sqlite.server.storage.table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.filemanager.FileManager;
import lite.sqlite.server.storage.index.TableIndex;
import lite.sqlite.server.storage.record.DataType;
import lite.sqlite.server.storage.record.Record;
import lite.sqlite.server.storage.record.SlottedRecordPage;
import lite.sqlite.server.storage.record.SlottedRecordPage.RecordWithSlot;
import lite.sqlite.server.storage.record.Schema;

public class Table implements Iterable<Record> {

    private final String tableName;
    private final Schema schema;
    private final BufferPool bufferPool;
    private final FileManager fileManager;
    private List<TableIndex<?>> indexes;  // Add this field
    
    /**
     * Creates a table wrapper bound to a schema, backing buffer pool, and table name.
     *
     * @param schema logical schema definition for records in this table
     * @param bufferPool shared buffer pool used for page access
     * @param tableName table name used to derive the underlying file name
     */
    public Table(Schema schema, BufferPool bufferPool, String tableName, FileManager fileManager) {
        this.fileManager = fileManager;
        this.tableName = tableName;
        this.bufferPool = bufferPool;
        this.schema = schema;
        this.indexes = new ArrayList<>(); 
    }
    
    // Index management methods
    /**
     * Creates and populates a typed index for a specific column.
     *
     * @param columnName indexed column name
     * @param tableName table name used by index metadata
     * @param indexName index name
     * @param isUnique true when duplicate keys are not allowed
     * @param columnType schema type of the indexed column
     * @return created index instance
     * @throws IOException when index population reads fail
     */
    public TableIndex<?> createTypedIndex(String columnName, String tableName, String indexName, boolean isUnique, DataType columnType) throws IOException {
        // Check if index already exists
        for (TableIndex<?> existingIndex : indexes) {
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
        TableIndex<?> newIndex;
        switch (columnType) {
            case INTEGER:
                newIndex = new TableIndex<Integer>(indexName, tableName, columnName, isUnique, 100);
                break;
            case VARCHAR:
                newIndex = new TableIndex<String>(indexName, tableName, columnName, isUnique, 100);
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
    
    /**
     * Finds the first index registered for the given column.
     *
     * @param columnName column name to search
     * @return matching index or null when no index exists
     */
    public TableIndex<?> findIndexForColumn(String columnName) {
        for (TableIndex<?> index : indexes) {
            if (index.getColumnName().equals(columnName)) {
                return index;
            }
        }
        return null;
    }
    
    /**
     * Returns a defensive copy of all indexes currently registered on this table.
     *
     * @return list of indexes
     */
    public List<TableIndex<?>> getIndexes() {
        return new ArrayList<>(indexes);
    }

    /**
     * Rebuilds every index registered on this table from current table contents.
     * Useful after in-place UPDATE/DELETE mutations where keys may have changed.
     *
     * @throws IOException when page access fails during index population
     */
    public void rebuildIndexes() throws IOException {
        if (indexes.isEmpty()) {
            return;
        }

        List<TableIndex<?>> rebuiltIndexes = new ArrayList<>();
        for (TableIndex<?> existingIndex : indexes) {
            int columnIndex = schema.getColumnIndex(existingIndex.getColumnName());
            if (columnIndex == -1) {
                throw new IllegalArgumentException(
                    "Column '" + existingIndex.getColumnName() + "' does not exist"
                );
            }

            DataType columnType = schema.getColumn(columnIndex).getType();
            TableIndex<?> rebuilt;
            switch (columnType) {
                case INTEGER:
                    rebuilt = new TableIndex<Integer>(
                        existingIndex.getIndexName(),
                        existingIndex.getTableName(),
                        existingIndex.getColumnName(),
                        existingIndex.isUnique(),
                        existingIndex.getMaxDegree()
                    );
                    break;
                case VARCHAR:
                    rebuilt = new TableIndex<String>(
                        existingIndex.getIndexName(),
                        existingIndex.getTableName(),
                        existingIndex.getColumnName(),
                        existingIndex.isUnique(),
                        existingIndex.getMaxDegree()
                    );
                    break;
                default:
                    throw new UnsupportedOperationException(
                        "Unsupported column type for indexing: " + columnType
                    );
            }

            populateIndex(rebuilt, columnIndex);
            rebuiltIndexes.add(rebuilt);
        }

        this.indexes = rebuiltIndexes;
    }
    
    /**
     * Scans existing records and inserts eligible values into a newly created index.
     *
     * @param index target index to populate
     * @param columnIndex schema column index used as key source
     * @throws IOException when page access fails
     */
    private void populateIndex(TableIndex<?> index, int columnIndex) throws IOException {
        String filename = getFileName();
        int blockCount = fileManager.getBlockCount(filename);

        for (int blockNum = 0; blockNum < blockCount; blockNum++) {
            Block block = new Block(filename, blockNum);
            Page page = bufferPool.pinBlock(block);

            try {
                SlottedRecordPage recordPage = new SlottedRecordPage(page, schema, block, bufferPool);

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
        }
    }
    
    /**
     * Inserts a comparable key-value mapping into a typed index.
     *
     * @param index target index
     * @param value comparable key value
     * @param rid record identifier value
     * @param <K> key type
     */
    @SuppressWarnings("unchecked")
    private <K extends Comparable<K>> void updateIndexTyped(TableIndex<?> index, Comparable value, RecordId rid) {
        ((TableIndex<K>) index).insert((K) value, rid);
    }
    
    /**
     * Inserts a record into the table and updates all applicable indexes.
     *
     * @param record record to insert
     * @return generated record id
     * @throws IOException when page operations fail
     */
    public RecordId insertRecord(Record record) throws IOException {
        Block block = fileManager.searchForInsertableBlock(this, record.getValues());
        if (block == null) {
            block = fileManager.append(getFileName());
        }

        Page page = bufferPool.pinBlock(block);
        SlottedRecordPage recordPage = new SlottedRecordPage(page, getSchema(), block, bufferPool);
        
        try {
            if (!indexes.isEmpty()) {
                for (TableIndex<?> index : indexes) {
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
                for (TableIndex<?> index : indexes) {
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
    
    /**
     * Performs a typed index lookup using a comparable key.
     *
     * @param index target index
     * @param value search key
     * @param <K> key type
     * @return matching record id or null
     */
    @SuppressWarnings("unchecked")
    private <K extends Comparable<K>> RecordId searchInIndexTyped(TableIndex<?> index, Comparable value) {
        return ((TableIndex<K>) index).search((K) value);
    }
    
    /**
     * Reads a record by record id.
     *
     * @param rid record identifier
     * @return record instance or null when slot is empty
     * @throws IOException when page operations fail
     */
    public Record getRecord(RecordId rid) throws IOException {
        Block block = rid.getBlockId();
        Page page = bufferPool.pinBlock(block);
        
        try {
            SlottedRecordPage recordPage = new SlottedRecordPage(page, schema, block, bufferPool);
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
    /**
     * Returns table schema.
     *
     * @return schema
     */
    public Schema getSchema() {
        return schema;
    }
    
    /**
     * Returns logical table name.
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Derives table file name from table name.
     *
     * @return physical file name
     */
    private String getFileName() {
        return tableName + ".tbl";
    }
    
    /**
     * Hook for metadata updates after mutating operations.
     *
     * @throws IOException when metadata persistence fails
     */
    private void touch() throws IOException {
        // Implementation for table metadata updates
    }
    
    /**
     * Creates an iterator over records in this table.
     *
     * @return table iterator
     */
    @Override
    public Iterator<Record> iterator() {
        return new TableIterator();
    }
    
    private class TableIterator implements Iterator<Record> {
        private int currentBlockNum = 0;
        private List<RecordWithSlot> currentRecords = new ArrayList<>();
        private int currentRecordIndex = 0;
        private boolean hasMoreBlocks = true;
        
        /**
         * Initializes iterator state and loads the first data block.
         */
        public TableIterator() {
            loadNextBlock();
        }
        
        /**
         * Loads records from the next block into memory for iteration.
         */
        private void loadNextBlock() {
            currentRecords.clear();
            currentRecordIndex = 0;
            
            if (!hasMoreBlocks) return;
            
            try {
                String filename = getFileName();
                Block block = new Block(filename, currentBlockNum);
                Page page = bufferPool.pinBlock(block);
                
                try {
                    SlottedRecordPage recordPage = new SlottedRecordPage(page, schema, block, bufferPool);
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
        
        /**
         * Indicates whether another record is available.
         *
         * @return true when a subsequent record exists
         */
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
        
        /**
         * Returns the next record in scan order.
         *
         * @return next record
         * @throws NoSuchElementException when no further records exist
         */
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
