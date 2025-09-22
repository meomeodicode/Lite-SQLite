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
        this.cache = new LRUCache<>(poolCapacity, 2); 
        this.freeFrames = new ArrayDeque<>(poolCapacity);
        this.lock = new ReentrantReadWriteLock();
        
        for (int i = 0; i < poolCapacity; i++) {
            freeFrames.push(new Frame());
        }
    }

    public Page pinBlock(BlockId blockId) throws IOException {
        lock.writeLock().lock();
        try {
            Frame frame = cache.get(blockId);
            
            if (frame != null) {
                frame.pin();
                return frame.getPage();
            }
            
            frame = allocateFrame();
            Page page = new Page(blockId.getBlockNum());
            fManager.read(blockId, page);
            frame.setBlockId(blockId);
            frame.setPage(page);
            frame.pin();
            cache.put(blockId, frame);
            System.out.println("WARN: No free frames available, relying on cache eviction.");
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
        lock.writeLock().lock();
        try {
            cache.forEachValue(frame -> {
                if (frame.getPage().isDirty()) {
                    try {
                        fManager.write(frame.getBlockId(), frame.getPage());
                        frame.getPage().markClean();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to flush frame", e);
                    }
                }
            }); 
        }
        finally {
            lock.writeLock().unlock();
        }

    }
    
    private Frame allocateFrame() throws IOException {
        if (!freeFrames.isEmpty()) {
            return freeFrames.pop();
        }
        return new Frame();
    }
    
    public double getHitRatio() {
        return cache.getHitRatio(); 
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