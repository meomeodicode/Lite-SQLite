package lite.sqlite.server.storage;

import java.security.Timestamp;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.RuntimeErrorException;

import org.checkerframework.checker.units.qual.t;

import lite.sqlite.server.storage.DoublyLinkedList.Node;

public class LRUCache<K,V>  {

    private static class CacheEntry<K,V> {
        K key;
        V value;
        Deque<Long> accessedTime;
        int maxHistory = 0;

        CacheEntry(K key, V value, int maxHistory) {
            this.key = key;
            this.value = value;
            this.maxHistory = maxHistory;
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
        public long getLastAccessTime() {
            if (accessedTime.isEmpty()) {
                System.out.println("Empty accesses");
                return 0; 
            }
            return accessedTime.peekLast();
        }
        
        public long getOldestAccessTime() {
            if (accessedTime.isEmpty()) {
                System.out.println("Empty accesses");
                return 0; 
            }
            return accessedTime.getFirst();
        }

        public long countAccessTime() {
            return accessedTime.size();
        }

         @Override
        public String toString() {
            return String.format("LRUKEntry{%s -> %s, accesses=%d, kthTime=%d}", 
                               key, value, countAccessTime(), getKthAccessTime());
        }
    }
    private final DoublyLinkedList<CacheEntry<K,V>> lruList;
    private final ConcurrentHashMap<K, Node<CacheEntry<K,V>>> hashTable;
    private int capacity;
    private final ReentrantReadWriteLock lock;

    int hits = 0, misses = 0;
    public LRUCache(int capacity) {
        if (capacity < 0) {
            throw new RuntimeErrorException(null, "Invalid capacity");
        } 
        this.capacity = capacity;
        this.lruList = new DoublyLinkedList<>();
        this.hashTable = new ConcurrentHashMap<>(capacity);
        this.lock = new ReentrantReadWriteLock();
    }

    public V get(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<K,V> 
            if (node == null) {
                misses++;
                return null;
            }
            hits++;
            lruList.moveToLast(node);
            node.data.recordAccess();
        }
        catch (Exception e) {
            // throw new RuntimeErrorException(e, "Get error LRU cache");
        }
    }
}
