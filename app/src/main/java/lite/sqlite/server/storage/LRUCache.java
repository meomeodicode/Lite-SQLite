package lite.sqlite.server.storage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import javax.management.RuntimeErrorException;
import lite.sqlite.server.storage.DoublyLinkedList.Node;

public class LRUCache<K,V> {
    private static class CacheEntry<K,V> {
        K key;
        V value;
        Deque<Long> accessedTime;
        int maxHistory = 0;

        CacheEntry(K key, V value, int maxHistory) {
            this.key = key;
            this.value = value;
            this.maxHistory = maxHistory;
            this.accessedTime = new ArrayDeque<>(maxHistory);
            recordAccess();
        }

        public void recordAccess() {
            long accessTime = System.currentTimeMillis();
            if (accessedTime.size() >= maxHistory) {
                accessedTime.removeFirst();
            }
            accessedTime.addLast(accessTime);
        }

        public long getKthAccessTime() {
            if (accessedTime.isEmpty()) {
                return 0;
            }
            return accessedTime.peekFirst();
        }

        public long getAccessCount() {
            return accessedTime.size();
        }

        public long getBackwardKDistance(long currentTime) {
            if (accessedTime.size() < maxHistory) {
                return Long.MAX_VALUE;
            }
            return currentTime - getKthAccessTime();
        }

        @Override
        public String toString() {
            return String.format("LRUKEntry{%s -> %s, accesses=%d, kthTime=%d}", 
                               key, value, getAccessCount(), getKthAccessTime());
        }
    }

    private final DoublyLinkedList<CacheEntry<K,V>> lruList;
    private final ConcurrentHashMap<K, Node<CacheEntry<K,V>>> hashTable;
    private int capacity;
    private final int k;
    private final ReentrantReadWriteLock lock;
    
    int hits = 0, misses = 0;

    public LRUCache(int capacity, int k) {
        if (capacity < 0) {
            throw new RuntimeErrorException(null, "Invalid capacity");
        } 
        if (k <= 0) {
            throw new IllegalArgumentException("Negative k", null);
        }
        this.k = k;
        this.capacity = capacity;
        this.lruList = new DoublyLinkedList<>();
        this.hashTable = new ConcurrentHashMap<>(capacity);
        this.lock = new ReentrantReadWriteLock();
    }

    public V get(K key) {
        lock.writeLock().lock();
        try {
            Node<CacheEntry<K,V>> node = hashTable.get(key);
            if (node == null) {
                misses++;
                return null;
            }
            
            hits++;
            V nodeValue = node.data.value;
            lruList.moveToLast(node);
            node.data.recordAccess();
            return nodeValue;
        } catch (Exception e) {
            // throw new RuntimeErrorException(e, "Get error LRU cache");
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            Node<CacheEntry<K,V>> existingNode = hashTable.get(key);
            if (existingNode != null) {
                existingNode.data.value = value;
                existingNode.data.recordAccess();
                lruList.moveToLast(existingNode);
                return;
            }
            
            if (lruList.size() >= capacity) {
                evict();
            }
            
            CacheEntry<K,V> newEntry = new CacheEntry<K,V>(key, value, k);
            Node<CacheEntry<K,V>> newNode = lruList.addLast(newEntry);
            hashTable.put(key, newNode);     
        } catch (Exception e) {
            throw new RuntimeErrorException(null, "Error putting new lru entry");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Object[] evict() {
        if (lruList.isEmpty()) {
            return null;
        }
        
        Node<CacheEntry<K,V>> victim = null;
        long oldestAccessTime = Long.MAX_VALUE;
        long currentTime = System.currentTimeMillis();
        
        Node<CacheEntry<K,V>> current = lruList.getFirstNode();
        while (current != null) {
            CacheEntry<K,V> entry = current.data;
            boolean pinned = false;
            if (entry.value instanceof Frame) {
                Frame frame = (Frame) entry.value;
                pinned = frame.isPinned();
            }
            
            if (!pinned && entry.getAccessCount() < k) {
                long curAccessTime = entry.getKthAccessTime();
                if (victim == null || curAccessTime < oldestAccessTime) {
                    victim = current;
                    oldestAccessTime = curAccessTime;
                }
            }
            
            current = current.getNext();
            if (current == null || current == lruList.getLastNode().getNext()) {
                break; 
            }
        }

        if (victim == null) {
            current = lruList.getFirstNode();
            while (current != null) {
                CacheEntry<K,V> entry = current.getData();
                boolean pinned = false;
                if (entry.value instanceof Frame) {
                    Frame frame = (Frame) entry.value;
                    pinned = frame.isPinned();
                }
                
                if (!pinned) {
                    long curAccessTime = entry.getBackwardKDistance(currentTime);
                    if (victim == null || curAccessTime > oldestAccessTime) {
                        victim = current;
                        oldestAccessTime = curAccessTime;
                    }
                }
                
                current = current.getNext();
                if (current == null || current == lruList.getLastNode().getNext()) {
                    break;
                }
            }
        }

        if (victim != null) {
            CacheEntry<K,V> entry = victim.data;
            K key = entry.key;
            V value = entry.value;
            lruList.unlinkNode(victim);
            hashTable.remove(entry.key);
            return new Object[] {key, value};
        }
        return null;
    }

    public V remove(K key) {
        lock.writeLock().lock();
        try {
            Node<CacheEntry<K,V>> node = hashTable.remove(key);
            if (node != null) {
                CacheEntry<K,V> entry = lruList.unlinkNode(node);
                return entry.value;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void forEachValue(Consumer<V> action) {
        lock.readLock().lock();
        try {
            Node<CacheEntry<K,V>> current = lruList.getFirstNode();
            
            while (current != null) {
                CacheEntry<K,V> entry = current.getData();
                action.accept(entry.value);
                
                current = current.getNext();
                if (current == null || current == lruList.getLastNode().getNext()) {
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }   

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public double getHitRatio() {
        int total = hits + misses;
        return total == 0 ? 0 : (double) hits / total * 100.0;
    }
    
    public int size() {
        return lruList.size();
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            lruList.clear();
            hashTable.clear();
            hits = 0;
            misses = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
       
    public void printCacheState() {
        lock.readLock().lock();
        try {
            System.out.println("LRU-" + k + " Cache State:");
            System.out.printf("Size: %d/%d, Hits: %d, Misses: %d, Hit Ratio: %.2f%%\n", 
                size(), capacity, hits, misses, getHitRatio());
            
            Node<CacheEntry<K,V>> current = lruList.getFirstNode();
            int position = 0;
            while (current != null) {
                CacheEntry<K,V> entry = current.getData();
                System.out.printf("  [%d] %s (accesses: %d)\n", 
                    position++, entry.key, entry.getAccessCount());
                
                current = current.getNext();
                if (current == null || current == lruList.getLastNode().getNext()) {
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

}