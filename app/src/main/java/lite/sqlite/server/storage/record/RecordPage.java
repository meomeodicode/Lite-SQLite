package lite.sqlite.server.storage.record;

import lite.sqlite.server.storage.Page;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import lite.sqlite.server.storage.Page;


public class RecordPage {
    // Page layout constants
    private static final int HEADER_OFFSET = 32;   
    private static final int SLOT_SIZE = 8;       
    
    // Slot directory entry offsets
    private static final int SLOT_OFFSET = 0;     
    private static final int SLOT_LENGTH = 4;     
    
    // Slot directory is at the beginning after header
    private static final int DIRECTORY_OFFSET = HEADER_OFFSET;
    
    // Page header offsets (custom for records)
    private static final int RECORD_COUNT_OFFSET = HEADER_OFFSET - 8;  
    private static final int FREE_POINTER_OFFSET = HEADER_OFFSET - 4;  
    
    private Page page;
    private Schema schema;
    
    public RecordPage(Page page, Schema schema) {
        this.page = page;
        this.schema = schema;
        
        if (getRecordCount() == 0) {
            setFreeSpacePointer(Page.PAGE_SIZE);
            setRecordCount(0);
        }
    }
    
    public boolean insert(Object[] record) {
        byte[] recordData = serializeRecord(record);
        
        int recordLength = recordData.length;
        int requiredSpace = recordLength + SLOT_SIZE; // Record + slot entry
        
        if (!hasSpace(requiredSpace)) {
            return false; // Not enough space
        }
        
        int freePointer = getFreeSpacePointer();
        int newFreePointer = freePointer - recordLength;
        int recordOffset = newFreePointer;
        
        page.write(recordOffset, recordData);
        setFreeSpacePointer(newFreePointer);
        
        int recordCount = getRecordCount();
        int slotOffset = DIRECTORY_OFFSET + (recordCount * SLOT_SIZE);
        
        page.setInt(slotOffset + SLOT_OFFSET, recordOffset);
        page.setInt(slotOffset + SLOT_LENGTH, recordLength);
        
        setRecordCount(recordCount + 1);
        page.markDirty();
        
        return true;
    }
    

    public boolean update(int slot, Object[] record) {
        if (slot < 0 || slot >= getRecordCount()) {
            return false; 
        }
        
        int slotOffset = DIRECTORY_OFFSET + (slot * SLOT_SIZE);
        int currentOffset = page.getInt(slotOffset + SLOT_OFFSET);
        int currentLength = page.getInt(slotOffset + SLOT_LENGTH);
        
        byte[] newRecordData = serializeRecord(record);
        int newLength = newRecordData.length;
        
        if (newLength <= currentLength) {
            page.write(currentOffset, newRecordData);
            
            if (newLength < currentLength) {
                page.setInt(slotOffset + SLOT_LENGTH, newLength);
            }
            
            page.markDirty();
            return true;
        }

        delete(slot);
        return insert(record);
    }
    
  
    public boolean delete(int slot) {
        int recordCount = getRecordCount();
        if (slot < 0 || slot >= recordCount) {
            return false; // Invalid slot
        }

        int slotOffset = DIRECTORY_OFFSET + (slot * SLOT_SIZE);
        page.setInt(slotOffset + SLOT_OFFSET, -1);
        page.setInt(slotOffset + SLOT_LENGTH, 0);
        
        // Mark page as dirty
        page.markDirty();
        
        return true;
    }
    
    public Object[] getRecord(int slot) {
        if (slot < 0 || slot >= getRecordCount()) {
            return null; // Invalid slot
        }
        
        int slotOffset = DIRECTORY_OFFSET + (slot * SLOT_SIZE);
        int recordOffset = page.getInt(slotOffset + SLOT_OFFSET);
        int recordLength = page.getInt(slotOffset + SLOT_LENGTH);
        
        if (recordOffset == -1) {
            return null;
        }
        
        // Read record data
        byte[] recordData = new byte[recordLength];
        page.read(recordOffset, recordData);
        
        // Deserialize and return
        return deserializeRecord(recordData);
    }
    
    public List<RecordWithSlot> getAllRecords() {
        List<RecordWithSlot> result = new ArrayList<>();
        int recordCount = getRecordCount();
        
        for (int slot = 0; slot < recordCount; slot++) {
            int slotOffset = DIRECTORY_OFFSET + (slot * SLOT_SIZE);
            int recordOffset = page.getInt(slotOffset + SLOT_OFFSET);
            
            if (recordOffset != -1) { // Not deleted
                Object[] record = getRecord(slot);
                result.add(new RecordWithSlot(record, slot));
            }
        }
        
        return result;
    }
    
    public boolean hasSpace(int size) {
        int freeSpaceStart = DIRECTORY_OFFSET + (getRecordCount() * SLOT_SIZE);
        int freeSpaceEnd = getFreeSpacePointer();
        int availableSpace = freeSpaceEnd - freeSpaceStart;
        
        return size <= availableSpace;
    }
    
    private int getRecordCount() {
        return page.getInt(RECORD_COUNT_OFFSET);
    }
    
    private void setRecordCount(int count) {
        page.setInt(RECORD_COUNT_OFFSET, count);
    }
 
    private int getFreeSpacePointer() {
        return page.getInt(FREE_POINTER_OFFSET);
    }
    
    private void setFreeSpacePointer(int pointer) {
        page.setInt(FREE_POINTER_OFFSET, pointer);
    }
    
    private byte[] serializeRecord(Object[] record) {
        // Simple serialization
        ByteBuffer buffer = ByteBuffer.allocate(calculateRecordSize(record));
        
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
                    
                // Add more types as needed
            }
        }
        
        return buffer.array();
    }

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
                    
                // Add more types as needed
            }
        }
        
        return size;
    }
    
    public static class RecordWithSlot {
        private Object[] record;
        private int slot;
        
        public RecordWithSlot(Object[] record, int slot) {
            this.record = record;
            this.slot = slot;
        }
        
        public Object[] getRecord() {
            return record;
        }
        
        public int getSlot() {
            return slot;
        }
    }
}