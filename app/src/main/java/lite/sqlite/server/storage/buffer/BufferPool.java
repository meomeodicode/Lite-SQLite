package lite.sqlite.server.storage.buffer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import lite.sqlite.server.storage.Block;
import lite.sqlite.server.storage.Frame;
import lite.sqlite.server.storage.LRUCache;
import lite.sqlite.server.storage.Page;
import lite.sqlite.server.storage.filemanager.FileManager;

public class BufferPool {

    private final int poolCapacity;
    private final FileManager fManager;
    private final LRUCache<Block, Frame> cache;
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

    public Page pinBlock(Block block) throws IOException {
        lock.writeLock().lock();
        try {
            Frame frame = cache.get(block);
            
            if (frame != null) {
                frame.pin();
                return frame.getPage();
            }
            
            frame = allocateFrame();
            Page page = new Page();
            fManager.read(block, page);
            frame.setBlockId(block); 
            frame.setPage(page);
            frame.pin();
            cache.put(block, frame);
            
            return page;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void unpinBlock(Block blockId) {
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
    
    public void flushBlock(Block blockId) throws IOException {
        lock.readLock().lock();
        try {
            Frame frame = cache.get(blockId);
            if (frame != null && frame.isDirty()) {
                fManager.write(blockId, frame.getPage());
                frame.setDirty(false);
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void flushAll() throws IOException {
        lock.writeLock().lock();
        try {
            cache.forEachValue(frame -> {
                if (frame.isDirty()) {
                    try {
                        fManager.write(frame.getBlockId(), frame.getPage());
                        frame.setDirty(false);
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
    
    public void markDirtyBlock(Block block) {
        lock.writeLock().lock();
        try {
            Frame frame = cache.get(block);
            if (frame != null) {
                frame.setDirty(true);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private Frame allocateFrame() throws IOException {
        
        if (!freeFrames.isEmpty()) {
            return freeFrames.pop();
        }
        
        Object[] victim = cache.evict();
        if (victim != null) {
            Block victimBlock = (Block) victim[0];
            Frame victimFrame = (Frame) victim[1];
            markDirtyBlock(victimBlock);
            if (victimFrame.isDirty()) {
                fManager.write(victimBlock, victimFrame.getPage());
                victimFrame.setDirty(true);
            }
            victimFrame.reset();
            return victimFrame;
        }
        
        throw new RuntimeException("No unpinned frames available");
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