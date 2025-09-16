package lite.sqlite.server.storage;

public class Frame {
    private BlockId blockId;          
    private Page page;              
    private int pinCount;            
    private boolean dirty;            
    private long lastAccessTime;     
    
    public Frame() {
        this.page = null;  // Create page when needed
        this.pinCount = 0;
        this.dirty = false;
        this.blockId = null;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    // Pin/Unpin methods
    public void pin() {
        pinCount++;
        lastAccessTime = System.currentTimeMillis();
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
    
    // Dirty flag management
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    // Block assignment
    public void assignToBlock(BlockId blockId) {
        this.blockId = blockId;
        // Create page when assigned to block
        if (this.page == null) {
            this.page = new Page(blockId.getBlockNum());
        }
    }
    
    public BlockId getBlockId() {
        return blockId;
    }
    
    // Page access
    public Page getPage() {
        lastAccessTime = System.currentTimeMillis();
        return page;
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Check if this frame is available for replacement
     */
    public boolean isReplaceable() {
        return pinCount == 0;
    }
    
    /**
     * Reset frame to empty state
     */
    public void reset() {
        this.blockId = null;
        this.page = null;
        this.pinCount = 0;
        this.dirty = false;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("Frame{block=%s, pinCount=%d, dirty=%b}", 
                           blockId, pinCount, dirty);
    }
}