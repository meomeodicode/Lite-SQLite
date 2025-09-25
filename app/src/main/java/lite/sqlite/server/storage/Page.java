package lite.sqlite.server.storage;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Page {
    public static final int PAGE_SIZE = 4096;
    public static final Charset CHARSET = StandardCharsets.US_ASCII;
    
    private final ByteBuffer buffer;
    
    public Page() {
        buffer = ByteBuffer.allocateDirect(PAGE_SIZE);
    }
    
    public Page(int blocksize) {
        buffer = ByteBuffer.allocateDirect(blocksize);
    }
    
    public Page(byte[] b) {
        buffer = ByteBuffer.wrap(b);
    }
    
    public int getInt(int offset) {
        return buffer.getInt(offset);
    }
    
    public void setInt(int offset, int n) {
        buffer.putInt(offset, n);
    }
    
    public byte[] getBytes(int offset) {
        buffer.position(offset);
        int length = buffer.getInt();
        byte[] b = new byte[length];
        buffer.get(b);
        return b;
    }
    
    public void setBytes(int offset, byte[] b) {
        buffer.position(offset);
        buffer.putInt(b.length);
        buffer.put(b);
    }
    
    public String getString(int offset) {
        byte[] b = getBytes(offset);
        return new String(b, CHARSET);
    }
    
    public void setString(int offset, String s) {
        byte[] b = s.getBytes(CHARSET);
        setBytes(offset, b);
    }
    
    public void write(int offset, byte[] data) {
        if (offset < 0 || offset + data.length > buffer.capacity()) {
            throw new IllegalArgumentException(
                String.format("Data exceeds page boundary: offset=%d, dataLength=%d, pageSize=%d", 
                             offset, data.length, buffer.capacity()));
        }
        buffer.position(offset);
        buffer.put(data);
    }
    
    public void read(int offset, byte[] data) {
        if (offset < 0 || offset + data.length > buffer.capacity()) {
            throw new IllegalArgumentException(
                String.format("Read exceeds page boundary: offset=%d, dataLength=%d, pageSize=%d", 
                             offset, data.length, buffer.capacity()));
        }
        buffer.position(offset);
        buffer.get(data);
    }
    
    public static int maxLength(int strlen) {
        float bytesPerChar = CHARSET.newEncoder().maxBytesPerChar();
        return Integer.BYTES + (strlen * (int)bytesPerChar);
    }
    
    public int capacity() {
        return buffer.capacity();
    }
    
    public ByteBuffer contents() {
        buffer.position(0);
        return buffer;
    }
    
    @Override
    public String toString() {
        return String.format("Page{capacity=%d, position=%d}", 
                           buffer.capacity(), buffer.position());
    }
}