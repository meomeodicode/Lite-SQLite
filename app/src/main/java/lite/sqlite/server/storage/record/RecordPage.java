package lite.sqlite.server.storage.record;

import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.table.RecordId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class RecordPage {
    private static final int HEADER_SIZE = 32;   
    private static final int SLOT_SIZE = 8;       
    
    private static final int SLOT_OFFSET = 0;     
    private static final int SLOT_LENGTH = 4;     
    
    private static final int DIRECTORY_OFFSET = 0;  
    
    private static final int RECORD_COUNT_OFFSET = 0;  
    private static final int FREE_POINTER_OFFSET = 4;
    
    private final Page page;
    private final Schema schema;
    private final Block blockId;       
    private final BufferPool bufferPool; 
    
    /**
     * Creates a record page view over a physical page and initializes page metadata when empty.
     *
     * @param page backing page bytes
     * @param schema schema used for record serialization
     * @param blockId physical block identifier
     * @param bufferPool buffer pool used for dirty tracking
     */
    public RecordPage(Page page, Schema schema, Block blockId, BufferPool bufferPool) {
        this.page = page;
        this.schema = schema;
        this.blockId = blockId;
        this.bufferPool = bufferPool;
        
        if (getRecordCount() == 0) {
            setFreeSpacePointer(Page.PAGE_SIZE);
            setRecordCount(0);
        }
    }
    
    /**
     * Inserts one logical record into the page.
     *
     * @param record logical record values
     * @return true when insertion succeeds, false when insufficient space
     */
    public boolean insert(Object[] record) {
        byte[] recordData = serializeRecord(record);
        int recordLength = recordData.length;
        int requiredSpace = recordLength + SLOT_SIZE; 
        
        System.out.println("DEBUG: Inserting record of length: " + recordLength);
        System.out.println("DEBUG: Free space pointer: " + getFreeSpacePointer());
        System.out.println("DEBUG: Directory end: " + (DIRECTORY_OFFSET + HEADER_SIZE + (getRecordCount() * SLOT_SIZE)));
        
        if (hasSpace(requiredSpace)) {        
            int freePointer = getFreeSpacePointer();
            int newFreePointer = freePointer - recordLength;
            int recordOffset = newFreePointer;
            
            if (recordOffset < DIRECTORY_OFFSET + HEADER_SIZE + ((getRecordCount() + 1) * SLOT_SIZE)) {
                System.out.println("ERROR: Record would overlap with directory!");
                return false;
            }
            
            page.write(recordOffset, recordData);
            setFreeSpacePointer(newFreePointer);
            
            int recordCount = getRecordCount();
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (recordCount * SLOT_SIZE);
            
            page.setInt(slotOffset + SLOT_OFFSET, recordOffset);
            page.setInt(slotOffset + SLOT_LENGTH, recordLength);
            
            setRecordCount(recordCount + 1);
            markDirty(); 
            return true;
        }
        
        else if (getFragmentationRatio() > 0.2) {
            compactPage();
            
            if (!hasSpace(requiredSpace)) {
                return false;
            }
            
            int freePointer = getFreeSpacePointer();
            int newFreePointer = freePointer - recordLength;
            int recordOffset = newFreePointer;
            
            page.write(recordOffset, recordData);
            setFreeSpacePointer(newFreePointer);
            
            int recordCount = getRecordCount();
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (recordCount * SLOT_SIZE);
            
            page.setInt(slotOffset + SLOT_OFFSET, recordOffset);
            page.setInt(slotOffset + SLOT_LENGTH, recordLength);
            
            setRecordCount(recordCount + 1);
            markDirty(); 
            return true;
        }

        return false;
    }

    /**
     * Computes the proportion of deleted slots within the current slot directory.
     *
     * @return fragmentation ratio in the range [0, 1]
     */
    private double getFragmentationRatio() {
        int total = getRecordCount();
        if (total == 0) return 0.0;
        
        int active = 0;
        for (int slot = 0; slot < total; slot++) {
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
            if (page.getInt(slotOffset + SLOT_OFFSET) != -1) {
                active++;
            }
        }
        return 1.0 - ((double) active / total);
    }
        
    /**
     * Updates the record at a given slot, relocating it when needed.
     *
     * @param slot slot number
     * @param record new record values
     * @return true when update succeeds, false otherwise
     */
    public boolean update(int slot, Object[] record) {
        if (slot < 0 || slot >= getRecordCount()) {
            return false; 
        }
        
        int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
        int currentOffset = page.getInt(slotOffset + SLOT_OFFSET);
        if (currentOffset == -1) {
            return false;
        }
        int currentLength = page.getInt(slotOffset + SLOT_LENGTH);
        byte[] newRecordData = serializeRecord(record);
        int newLength = newRecordData.length;
        
        if (newLength <= currentLength) {
            page.write(currentOffset, newRecordData);
            page.setInt(slotOffset + SLOT_LENGTH, newLength);
            markDirty(); // ✅ Fixed: use our own method
            return true;
        }
        else {
            if (hasSpace(newLength)) {
                int newOffset = getFreeSpacePointer() - newLength;
                page.write(newOffset, newRecordData);
                setFreeSpacePointer(newOffset);
                page.setInt(slotOffset + SLOT_OFFSET, newOffset);
                page.setInt(slotOffset + SLOT_LENGTH, newLength);
                markDirty(); 
                return true;
            }
            else if (getFragmentationRatio() >= 0.2) {
                compactPage();
                if (hasSpace(newLength)) {
                    int newOffset = getFreeSpacePointer() - newLength;
                    page.write(newOffset, newRecordData);
                    setFreeSpacePointer(newOffset);
                    page.setInt(slotOffset + SLOT_OFFSET, newOffset);
                    page.setInt(slotOffset + SLOT_LENGTH, newLength);
                    markDirty(); 
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
    }
  
    /**
     * Marks a slot as deleted and optionally compacts the page.
     *
     * @param slot slot number to delete
     * @return true when delete succeeds, false for invalid slot
     */
    public boolean delete(int slot) {
        int recordCount = getRecordCount();
        if (slot < 0 || slot >= recordCount) {
            return false; 
        }

        int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
        page.setInt(slotOffset + SLOT_OFFSET, -1);
        page.setInt(slotOffset + SLOT_LENGTH, 0);

        if (shouldCompact()) {
            compactPage();
        }
        markDirty(); 
        return true;
    }
    
    /**
     * Reads and deserializes a record by slot number.
     *
     * @param slot slot number
     * @return record values or null when slot is empty/invalid
     */
    public Object[] getRecord(int slot) {
        if (slot < 0 || slot >= getRecordCount()) {
            return null;
        }
        
        int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
        int recordOffset = page.getInt(slotOffset + SLOT_OFFSET);
        int recordLength = page.getInt(slotOffset + SLOT_LENGTH);
        
        if (recordOffset == -1) {
            return null;
        }
        
        byte[] recordData = new byte[recordLength];
        page.read(recordOffset, recordData);
        
        return deserializeRecord(recordData);
    }
    
    /**
     * Returns all non-deleted records together with their slot metadata.
     *
     * @return list of record-with-slot wrappers
     */
    public List<RecordWithSlot> getAllRecords() {
        List<RecordWithSlot> result = new ArrayList<>();
        int recordCount = getRecordCount();
        
        for (int slot = 0; slot < recordCount; slot++) {
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
            int recordOffset = page.getInt(slotOffset + SLOT_OFFSET);
            
            if (recordOffset != -1) {
                Object[] record = getRecord(slot);
                if (record != null) {
                    result.add(new RecordWithSlot(record, slot, blockId));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks whether the page currently has sufficient contiguous free space.
     *
     * @param size required bytes
     * @return true when enough space is available
     */
    public boolean hasSpace(int size) {
        int freeSpaceStart = DIRECTORY_OFFSET + HEADER_SIZE + (getRecordCount() * SLOT_SIZE);
        int freeSpaceEnd = getFreeSpacePointer();
        int availableSpace = freeSpaceEnd - freeSpaceStart;
        
        System.out.println("DEBUG hasSpace:");
        System.out.println("  requiredSpace: " + size);
        System.out.println("  freeSpaceStart: " + freeSpaceStart);  
        System.out.println("  freeSpaceEnd: " + freeSpaceEnd);
        System.out.println("  availableSpace: " + availableSpace);
        
        return size <= availableSpace;
    }
    
    /**
     * Returns the number of slots tracked in the directory.
     *
     * @return record count
     */
    public int getRecordCount() {
        return page.getInt(RECORD_COUNT_OFFSET);
    }
    
    /**
     * Persists record count in page header.
     *
     * @param count new slot count
     */
    private void setRecordCount(int count) {
        page.setInt(RECORD_COUNT_OFFSET, count);
    }
 
    /**
     * Reads free-space pointer from page header.
     *
     * @return free-space pointer offset
     */
    private int getFreeSpacePointer() {
        return page.getInt(FREE_POINTER_OFFSET);
    }
    
    /**
     * Writes free-space pointer in page header.
     *
     * @param pointer new pointer value
     */
    private void setFreeSpacePointer(int pointer) {
        if (pointer < DIRECTORY_OFFSET + HEADER_SIZE || pointer > Page.PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid free space pointer: " + pointer);
        }
        page.setInt(FREE_POINTER_OFFSET, pointer);
    }
    
    /**
     * Marks the owning block as dirty in the buffer pool.
     */
    private void markDirty() {
        if (bufferPool != null && blockId != null) {
            bufferPool.markDirtyBlock(blockId);
        }
    }
    
    /**
     * Serializes logical record values to binary form based on schema.
     *
     * @param record logical record values
     * @return encoded bytes
     */
    private byte[] serializeRecord(Object[] record) {
        int estimatedSize = calculateRecordSize(record);
        int maxRecordSize = Page.PAGE_SIZE - HEADER_SIZE - SLOT_SIZE - 100; // Leave some margin
        if (estimatedSize > maxRecordSize) {
            throw new IllegalArgumentException("Record too large: " + estimatedSize + 
                                              " bytes (max: " + maxRecordSize + ")");
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(estimatedSize);
        
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column column = schema.getColumn(i);
            Object value = i < record.length ? record[i] : null;
            
            switch (column.getType()) {
                case INTEGER:
                    buffer.putInt(value != null ? (Integer)value : 0);
                    break;
                    
                case VARCHAR:
                    String strValue = value != null ? value.toString() : "";
                    byte[] stringBytes = strValue.getBytes();
                    int length = Math.min(stringBytes.length, 255);
                    
                    buffer.put((byte)length);
                    buffer.put(stringBytes, 0, length);
                    break;
                    
            }
        }
        
        byte[] result = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(result);
        
        System.out.println("DEBUG: Serialized record size: " + result.length);
        return result;
    }

    /**
     * Deserializes binary record bytes into schema-aligned values.
     *
     * @param data encoded record bytes
     * @return decoded logical record values
     */
    private Object[] deserializeRecord(byte[] data) {

        ByteBuffer buffer = ByteBuffer.wrap(data);
        Object[] record = new Object[schema.getColumnCount()];
        
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column column = schema.getColumn(i);
            
            switch (column.getType()) {
                case INTEGER:
                    record[i] = buffer.getInt();
                    break;
                    
                case VARCHAR:
                    int length = buffer.get() & 0xFF; 
                    
                    byte[] stringBytes = new byte[length];
                    buffer.get(stringBytes, 0, length);
                    record[i] = new String(stringBytes);
                    break;
            }
        }
        
        return record;
    }
    
    /**
     * Estimates encoded record size according to schema and current values.
     *
     * @param record logical record values
     * @return estimated encoded size in bytes
     */
    private int calculateRecordSize(Object[] record) {
        int size = 0;
        
        for (int i = 0; i < schema.getColumnCount(); i++) {
            Column column = schema.getColumn(i);
            
            switch (column.getType()) {
                case INTEGER:
                    size += 4; // 4 bytes for int
                    break;
                    
                case VARCHAR:
                    // 1 byte for length + string bytes
                    String strValue = i < record.length && record[i] != null ? 
                                    record[i].toString() : "";
                    size += 1 + Math.min(strValue.getBytes().length, 255);
                    break;
            }
        }
        
        return size;
    }

    /**
     * Compacts live records to reclaim fragmented free space.
     */
    private void compactPage() {
        int recordCount = getRecordCount();
        if (recordCount == 0) return;

        List<byte[]> activeRecord = new ArrayList<>();
        List<Integer> activeSlots = new ArrayList<>();

        for (int slot = 0; slot < recordCount; slot++) {
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
            int recordOffset = page.getInt(slotOffset + SLOT_OFFSET);
            int recordLength = page.getInt(slotOffset + SLOT_LENGTH);
            
            if (recordOffset != -1) {
                byte[] recordData = new byte[recordLength];
                page.read(recordOffset, recordData);
                activeRecord.add(recordData);
                activeSlots.add(slot);
            }
        }

        if (activeRecord.isEmpty()) {
            setRecordCount(0);
            setFreeSpacePointer(Page.PAGE_SIZE);
            markDirty(); 
            return;
        } 

        for (int slot = 0; slot < recordCount; slot++) {
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
            page.setInt(slotOffset + SLOT_OFFSET, -1);
            page.setInt(slotOffset + SLOT_LENGTH, 0);
        }
    
        int newFreePointer = Page.PAGE_SIZE;
    
        for (int i = 0; i < activeRecord.size(); i++) {
            byte[] recordData = activeRecord.get(i);
            int originalSlot = activeSlots.get(i);
        
            newFreePointer -= recordData.length;
            page.write(newFreePointer, recordData);
            
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (originalSlot * SLOT_SIZE);
            page.setInt(slotOffset + SLOT_OFFSET, newFreePointer);
            page.setInt(slotOffset + SLOT_LENGTH, recordData.length);
        }
    
        setFreeSpacePointer(newFreePointer);
        markDirty(); 
    }

    /**
     * Determines whether deletion fragmentation has crossed the compaction threshold.
     *
     * @return true when compaction is recommended
     */
    private boolean shouldCompact() {
        int total = getRecordCount();
        if (total <= 2) return false;
        
        int deleted = 0;
        for (int slot = 0; slot < total; slot++) {
            int slotOffset = DIRECTORY_OFFSET + HEADER_SIZE + (slot * SLOT_SIZE);
            if (page.getInt(slotOffset + SLOT_OFFSET) == -1) {
                deleted++;
            }
        }
        
        return (double) deleted / total > 0.3; 
    }
    
    public static class RecordWithSlot {
        private Object[] record;
        private int slot;
        private Block block;  // Add this!
        
        /**
         * Creates a tuple of record data with physical location metadata.
         *
         * @param record logical values
         * @param slot slot number
         * @param block owning block
         */
        public RecordWithSlot(Object[] record, int slot, Block block) {
            this.record = record;
            this.slot = slot;
            this.block = block;
        }
        
        /**
         * Returns record values.
         *
         * @return logical values
         */
        public Object[] getRecord() {
            return record;
        }
        
        /**
         * Returns slot number.
         *
         * @return slot number
         */
        public int getSlot() {
            return slot;
        }
        
        /**
         * Returns owning block.
         *
         * @return block id
         */
        public Block getBlock() {
            return block;
        }
        
        /**
         * Converts this wrapper to a stable record identifier.
         *
         * @return record id
         */
        public RecordId toRecordId() {
            return new RecordId(block, slot);
        }

    }

    /**
     * Prints a coarse visualization of page layout and occupancy statistics.
     */
    public void visualizePage() {
    int pageSize = Page.PAGE_SIZE;
    int directorySpace = HEADER_SIZE + (getRecordCount() * SLOT_SIZE);
    int usedDataSpace = pageSize - getFreeSpacePointer();
    int freeSpace = getFreeSpacePointer() - directorySpace;
    
    System.out.println("=== PAGE LAYOUT ===");
    System.out.println("Total size: " + pageSize + " bytes");
    System.out.println("Records: " + getRecordCount());
    
    // Visual representation
    StringBuilder visual = new StringBuilder("[");
    
    // Header
    for (int i = 0; i < HEADER_SIZE; i += 100) {
        visual.append("H");
    }
    
    // Directory
    for (int i = HEADER_SIZE; i < directorySpace; i += 100) {
        visual.append("D");
    }
    
    // Free space
    for (int i = 0; i < freeSpace; i += 100) {
        visual.append(".");
    }
    
    // Used data
    for (int i = 0; i < usedDataSpace; i += 100) {
        visual.append("X");
    }
    
    visual.append("]");
    System.out.println(visual.toString());
    System.out.println("H: Header | D: Directory | .: Free | X: Data");
    
    // Print statistics
    System.out.println("Directory space: " + directorySpace + " bytes (" + 
                      (directorySpace * 100 / pageSize) + "%)");
    System.out.println("Free space: " + freeSpace + " bytes (" + 
                      (freeSpace * 100 / pageSize) + "%)");
    System.out.println("Used data space: " + usedDataSpace + " bytes (" + 
                      (usedDataSpace * 100 / pageSize) + "%)");
    
    // Fragmentation info
    System.out.println("Fragmentation: " + (getFragmentationRatio() * 100) + "%");
}
}