package lite.sqlite.server.storage.table;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
     * Builds and populates a typed index for a column without registering it on this table.
     * The caller publishes it with {@link #addIndex} only after the catalog save succeeds
     * (slice 3, C14), so a failed save leaves no index in memory.
     *
     * @param indexName index name
     * @param columnName indexed column name
     * @param isUnique true when duplicate keys are not allowed
     * @param maxDegree B+tree max degree, from the catalog or the engine default
     * @return populated, unregistered index
     * @throws IOException when index population reads fail
     */
    public TableIndex<?> buildIndex(String indexName, String columnName, boolean isUnique, int maxDegree) throws IOException {
        for (TableIndex<?> existingIndex : indexes) {
            if (existingIndex.getIndexName().equals(indexName)) {
                throw new IllegalArgumentException("Index '" + indexName + "' already exists");
            }
        }

        int columnIndex = schema.getColumnIndex(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
        }

        TableIndex<?> newIndex = newTypedIndex(indexName, columnName, isUnique, maxDegree,
                schema.getColumn(columnIndex).getType());
        populateIndex(newIndex, columnIndex);
        return newIndex;
    }

    /**
     * Registers an index built by {@link #buildIndex}; from here on INSERT maintains and checks it.
     *
     * @param index populated index
     */
    public void addIndex(TableIndex<?> index) {
        indexes.add(index);
    }

    private TableIndex<?> newTypedIndex(String indexName, String columnName, boolean isUnique, int maxDegree, DataType columnType) {
        switch (columnType) {
            case INTEGER:
                return new TableIndex<Integer>(indexName, tableName, columnName, isUnique, maxDegree);
            case VARCHAR:
                return new TableIndex<String>(indexName, tableName, columnName, isUnique, maxDegree);
            default:
                throw new UnsupportedOperationException("Unsupported column type for indexing: " + columnType);
        }
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

            TableIndex<?> rebuilt = newTypedIndex(
                existingIndex.getIndexName(),
                existingIndex.getColumnName(),
                existingIndex.isUnique(),
                existingIndex.getMaxDegree(),
                schema.getColumn(columnIndex).getType()
            );

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
                        // D-010: the index owns the uniqueness rule. TableIndex.insert throws on a
                        // duplicate (only when isUnique), and nothing here may swallow it: the build
                        // fails, and the caller decides (CREATE INDEX errors, open fails closed).
                        // rebuildIndexes() after UPDATE/DELETE fails the same way; the row is already
                        // changed on its page by then (gap deferred to P1).
                        RecordId rid = rws.toRecordId();
                        updateIndexTyped(index, (Comparable) valueObj, rid);
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

        /** Number of blocks the table file holds now; the scan's end-of-table rule (same as DELETE). */
        private int blockCount() {
            try {
                return fileManager.getBlockCount(getFileName());
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot get block count of " + getFileName(), e);
            }
        }

        /**
         * Loads the live records of block {@code currentBlockNum} and moves to the next block.
         * An empty block yields an empty list; hasNext() then moves on.
         */
        private void loadNextBlock() {
            currentRecords = new ArrayList<>();
            currentRecordIndex = 0;

            Block block = new Block(getFileName(), currentBlockNum);
            currentBlockNum++;
            try {
                Page page = bufferPool.pinBlock(block);
                try {
                    currentRecords = new SlottedRecordPage(page, schema, block, bufferPool).getAllRecords();
                } finally {
                    bufferPool.unpinBlock(block);
                }
            } catch (Exception e) {
                // A damaged block is not an empty block: fail the scan loudly (slice 2.5, Q2).
                throw new IllegalStateException("Cannot read block " + block.getBlockNum()
                        + " of " + getFileName() + ": " + e.getMessage(), e);
            }
        }

        /**
         * True while a record remains. Skips empty blocks; the table ends after its last block
         * on disk (same rule as DELETE), never at the first empty block.
         */
        @Override
        public boolean hasNext() {
            while (currentRecordIndex >= currentRecords.size()) {
                if (currentBlockNum >= blockCount()) {
                    return false;
                }
                loadNextBlock();
            }
            return true;
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
