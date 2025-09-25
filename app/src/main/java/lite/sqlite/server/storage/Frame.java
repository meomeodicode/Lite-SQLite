package lite.sqlite.server.storage;

public class Frame {
    private Block blockId;          
    private Page page;              
    private int pinCount = 0;            
    private boolean dirty;            
    private long lastAccessTime;     
    
    public Frame() {
        this.page = null;  
        this.pinCount = 0;
        this.dirty = false;
        this.blockId = null;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
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
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void assignToBlock(Block blockId) {
        this.blockId = blockId;
        if (this.page == null) {
            this.page = new Page();
        }
    }
    
    public Block getBlockId() {
        return blockId;
    }
    
    public void setBlockId(Block blockId) {
        this.blockId = blockId;
    }

    public Page getPage() {
        lastAccessTime = System.currentTimeMillis();
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    

    public boolean isReplaceable() {
        return pinCount == 0;
    }
    
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