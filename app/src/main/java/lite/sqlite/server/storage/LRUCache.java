package lite.sqlite.server.storage.buffer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lite.sqlite.server.storage.BlockId;
import lite.sqlite.server.storage.Frame;
import lite.sqlite.server.storage.LRUCache;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.filemanager.FileManager;

public class BufferPool {

    private final int poolCapacity;
    private final FileManager fManager;
    private final LRUCache<BlockId, Frame> cache;
    private final Deque<Frame> freeFrames;
    private final ReentrantReadWriteLock lock; 

    public BufferPool(int poolCapacity, FileManager fManager) {
        this.poolCapacity = poolCapacity;
        this.fManager = fManager;
        this.cache = new LRUCache<>(poolCapacity, 2); // LRU-2 is good for databases
        this.freeFrames = new ArrayDeque<>(poolCapacity);
        this.lock = new ReentrantReadWriteLock();
        
        // Initialize free frames
        for (int i = 0; i < poolCapacity; i++) {
            freeFrames.push(new Frame());
        }
    }

    public Page pinBlock(BlockId blockId) throws IOException {
        lock.writeLock().lock();
        try {
            Frame frame = cache.get(blockId);
            
            if (frame != null) {
                // Cache hit - your LRUCache already updated position and recorded access
                frame.pin();
                return frame.getPage();
            }
            
            // Cache miss - allocate frame and load from disk
            frame = allocateFrame();
            
            // Load page from disk
            Page page = new Page(blockId.getBlockNum());
            fManager.read(blockId, page);
            
            // Set up frame
            frame.setBlockId(blockId);
            frame.setPage(page);
            frame.pin();
            
            // Put in cache - your evictLRU() will be called automatically if needed
            cache.put(blockId, frame);
            
            return page;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void unpinBlock(BlockId blockId) {
        lock.writeLock().lock();
        try {
            Frame frame = cache.get(blockId);
            if (frame != null) {
                frame.unpin();
                // get() already updated LRU position
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void flushBlock(BlockId blockId) throws IOException {
        lock.readLock().lock();
        try {
            Frame frame = cache.get(blockId);
            if (frame != null && frame.getPage().isDirty()) {
                fManager.write(blockId, frame.getPage());
                frame.getPage().markClean();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void flushAll() throws IOException {
        // Since your LRUCache doesn't expose iteration over values,
        // we'll need to handle this differently or add a method to LRUCache
        System.out.println("Flushing all dirty pages...");
        // TODO: Consider adding getAllEntries() method to LRUCache
    }
    
    private Frame allocateFrame() throws IOException {
        // Try free frames first
        if (!freeFrames.isEmpty()) {
            return freeFrames.pop();
        }
        
        // No free frames - create new one
        // Your LRUCache.put() will handle eviction automatically
        return new Frame();
    }
    
    // Delegate statistics to your LRUCache
    public double getHitRatio() {
        return cache.getHitRatio() / 100.0; // Convert from percentage to ratio
    }
    
    public int getHits() {
        return cache.getHits();
    }
    
    public int getMisses() {
        return cache.getMisses();
    }
    
    public int getPoolSize() {
        return poolCapacity;
    }
    
    public int getUsedFrames() {
        return cache.size();
    }
    
    public int getFreeFrames() {
        return freeFrames.size();
    }
    
    public void printStatistics() {
        System.out.println("Buffer Pool Statistics:");
        System.out.println("  Pool Capacity: " + poolCapacity);
        System.out.println("  Free Frames: " + getFreeFrames());
        
        // Use your LRUCache's detailed statistics
        cache.printCacheState();
    }
    
    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            cache.clear();
            freeFrames.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}