package lite.sqlite.server.storage;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;


public class Page {
    public static final int PAGE_SIZE = 4096; // 4KB - matches hardware page size
    private static final int HEADER_SIZE = 32; // Reserve space for metadata
    private static final int DATA_SIZE = PAGE_SIZE - HEADER_SIZE;
    
    // Header offsets
    private static final int CHECKSUM_OFFSET = 0;    // 4 bytes
    private static final int PAGE_ID_OFFSET = 4;     // 4 bytes  
    private static final int PAGE_TYPE_OFFSET = 8;   // 4 bytes
    private static final int DBMS_VERSION_OFFSET = 12; // 4 bytes
    private static final int TRANSACTION_ID_OFFSET = 16; // 8 bytes
    private static final int FREE_SPACE_OFFSET = 24; // 4 bytes
    private static final int RECORD_COUNT_OFFSET = 28; // 4 bytes
    
    // Page types
    public static final int PAGE_TYPE_TABLE_DATA = 1;
    public static final int PAGE_TYPE_INDEX = 2;
    public static final int PAGE_TYPE_METADATA = 3;

    private final ByteBuffer buffer;
    private final int pageId;
    private boolean dirty = false;
    private int pinCount = 0;

    public Page(int pageId) {
        this.pageId = pageId;
        this.buffer = ByteBuffer.allocate(PAGE_SIZE);
        initializeHeader();
    }

     public int getPageId() {
        return buffer.getInt(PAGE_ID_OFFSET);
    }
    
    public int getPageType() {
        return buffer.getInt(PAGE_TYPE_OFFSET);
    }
    
    public void setPageType(int pageType) {
        buffer.putInt(PAGE_TYPE_OFFSET, pageType);
        markDirty();
    }
    
    public int getDbmsVersion() {
        return buffer.getInt(DBMS_VERSION_OFFSET);
    }
    
    public long getTransactionId() {
        return buffer.getLong(TRANSACTION_ID_OFFSET);
    }
    
    public void setTransactionId(long transactionId) {
        buffer.putLong(TRANSACTION_ID_OFFSET, transactionId);
        markDirty();
    }
    
    public int getFreeSpace() {
        return buffer.getInt(FREE_SPACE_OFFSET);
    }

    private void setFreeSpace(int freeSpace) {
        buffer.putInt(FREE_SPACE_OFFSET, freeSpace);
        markDirty();
    }
    
    public int getRecordCount() {
        return buffer.getInt(RECORD_COUNT_OFFSET);
    }
    
    private void setRecordCount(int count) {
        buffer.putInt(RECORD_COUNT_OFFSET, count);
        markDirty();
    }

        // Page state management
    public boolean isDirty() {
        return dirty;
    }
    
    public void markDirty() {
        this.dirty = true;
        updateCheckSum(); 
    }

    private void initializeHeader() {
        buffer.putInt(PAGE_ID_OFFSET, pageId);
        buffer.putInt(PAGE_TYPE_OFFSET, PAGE_TYPE_TABLE_DATA); // Default to table data
        buffer.putInt(DBMS_VERSION_OFFSET, 1); // Version 1.0
        buffer.putLong(TRANSACTION_ID_OFFSET, 0L); // No transaction initially
        buffer.putInt(FREE_SPACE_OFFSET, DATA_SIZE); // All data space is free initially
        buffer.putInt(RECORD_COUNT_OFFSET, 0); // No records initially
        updateCheckSum();
    }

    private int calculateCheckSum() {
        CRC32 crc = new CRC32();
        byte[] data = new byte[PAGE_SIZE-4];
        buffer.position(4);
        buffer.get(data);
        
        crc.update(data);
        return (int)crc.getValue();
    }

    private void updateCheckSum() {
        int checkSum = calculateCheckSum();
        buffer.putInt(CHECKSUM_OFFSET, checkSum);
    }

    public boolean verifyChecksum() {
        int storedChecksum = buffer.getInt(CHECKSUM_OFFSET);
        
        CRC32 crc = new CRC32();
        byte[] data = new byte[PAGE_SIZE - 4];
        buffer.position(4);
        buffer.get(data);
        buffer.rewind();
        
        crc.update(data);
        return storedChecksum == (int) crc.getValue();
    }

    public void markClean() {
        this.dirty = false;
    }

    public void pin() {
        pinCount++;
    }
    
    public void unpin() {
        if (pinCount > 0) {
            pinCount--;
        }
    }
    
    public boolean isPinned() {
        return pinCount > 0;
    }
    
    public int getPinCount() {
        return pinCount;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }
    
    public static int getDataSize() {
        return DATA_SIZE;
    }
    
    @Override
    public String toString() {
        return String.format("Page{id=%d, type=%d, records=%d, freeSpace=%d, dirty=%s, pinned=%d}", 
                           getPageId(), getPageType(), getRecordCount(), 
                           getFreeSpace(), isDirty(), getPinCount());
    }

    public void write(int offset, byte[] data) {
        if (offset + data.length > DATA_SIZE) {
            throw new IllegalArgumentException("Data exceeds page boundary");
        }
        
        buffer.position(HEADER_SIZE + offset);
        buffer.put(data);
        markDirty();
    }
    
    public void read(int offset, byte[] data) {
        if (offset + data.length > DATA_SIZE) {
            throw new IllegalArgumentException("Read exceeds page boundary");
        }
        buffer.position(HEADER_SIZE + offset);
        buffer.get(data);
    }
    
    public ByteBuffer contents() {
        buffer.position(0);
        return buffer;
    }
    
    public int getInt(int offset) {
        if (offset + 4 > DATA_SIZE) {
            throw new IllegalArgumentException("Offset exceeds page data boundary");
        }
        return buffer.getInt(HEADER_SIZE + offset);
    }
    
    public void setInt(int offset, int value) {
        if (offset + 4 > DATA_SIZE) {
            throw new IllegalArgumentException("Offset exceeds page data boundary");
        }
        buffer.putInt(HEADER_SIZE + offset, value);
        markDirty();
    }

}
