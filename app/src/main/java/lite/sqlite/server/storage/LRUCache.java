package lite.sqlite.server.storage;

import java.security.Timestamp;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.RuntimeErrorException;

import org.checkerframework.checker.units.qual.t;
import org.codehaus.groovy.runtime.metaclass.MetaMethodIndex.CacheEntry;

import com.github.benmanes.caffeine.cache.Cache;

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
            this.accessedTime = new ArrayDeque(maxHistory);
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

        public long getAccessCount() {
            return accessedTime.size();
        }

        public long getBackwardKDistance(long currentTime) {
            if (accessedTime.size() < maxHistory) {
                return Long.MAX_VALUE; // +inf for entries with < K accesses
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
        if (k<=0) {
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
        }
        catch (Exception e) {
            // throw new RuntimeErrorException(e, "Get error LRU cache");
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void moveToLast(Node<CacheEntry<K,V>> node) {
        if (node == null) {
            throw new RuntimeErrorException(null, "Null pointer");
        }
        lruList.removeNode(node);
        lruList.addLast(node.data);
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
            CacheEntry<K,V> newEntry = new CacheEntry<K,V>(key,value, k);
            lruList.addLast(newEntry);
            hashTable.put(key, existingNode);
        }
        catch (Exception e) {
            throw new RuntimeErrorException(null, "Error putting new lru entry");
        }
    }

    private void evictLRU() {
        if (lruList.isEmpty()) {
            return;
        }
        Node<CacheEntry<K,V>> victim = null;
        long oldestKthTime = System.currentTimeMillis();
        long maxAccessGap = Long.MIN_VALUE;
        boolean foundLessThankAccesses = false;
        Node<CacheEntry<K,V>> current = lruList.getFirstNode();
        while (current != null) {
            CacheEntry<K,V> entry = current.data;
            long curKAccessTime = entry.getBackwardKDistance(oldestKthTime);
            if (curKAccessTime > maxAccessGap) {
            }
            current = current.getNext();
            if (current == null || current == lruList.getLastNode().getNext()) {
                break; 
            }
        }

        if (!foundLessThankAccesses) {
            current = lruList.getFirstNode();
            while (current != null) {
                CacheEntry<K,V> entry = current.getData();
                long kthAccessTime = entry.getKthAccessTime();
                
                if (victim == null || kthAccessTime < oldestKthTime) {
                    oldestKthTime = kthAccessTime;
                    victim = current;
                }
                
                // Move to next node
                current = current.getNext();
                if (current == null || current == lruList.getLastNode().getNext()) {
                    break;
                }
            }
        }

        if (victim != null) {
            CacheEntry<K,V> entry = lruList.removeNode(victim);
            hashTable.remove(entry.key);
        }

    }

}
