// package lite.sqlite.server.storage.buffer;

// import java.io.IOException;
// import java.util.ArrayDeque;
// import java.util.Deque;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.locks.ReentrantReadWriteLock;

// import lite.sqlite.server.storage.BlockId;
// import lite.sqlite.server.storage.Frame;
// import lite.sqlite.server.storage.Page;
// import lite.sqlite.server.storage.filemanager.FileManager;

// public class BufferPool {

//     private final int poolCapacity;
//     private final FileManager fManager;

//     private final ConcurrentHashMap<BlockId, Frame> bufferMap;
//     private final Deque<Frame> lruList;

//     private ReentrantReadWriteLock lock; 

//     private int hits = 0;
//     private int misses = 0;

//     // public BufferPool(int poolCapacity, FileManager fManager) {
//     //     this.poolCapacity = poolCapacity;
//     //     this.fManager = fManager;
//     //     this.bufferMap = new ConcurrentHashMap<>();
//     //     this.freeFrames = new ArrayDeque<>(poolCapacity);
//     //     this.lruList = new ArrayDeque<>(poolCapacity);
//     //     this.lock = new ReentrantReadWriteLock();
        
//     //     // Initialize free frames
//     //     for (int i = 0; i < poolCapacity; i++) {
//     //         freeFrames.push(new Frame());
//     //     }
//     // }

//     // public Page pinBlock(BlockId blockId) throws IOException {
//     //     lock.writeLock().lock();
//     // }

// }
