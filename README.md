# Storage
The goal is to handle:
1. **Disk I/O management** - Minimizing slow disk operations **Most important**
2. **Memory management** - Efficiently using limited RAM
3. **Concurrency control** - Allowing multiple operations safely
4. **Data persistence** - Ensuring durability of change
The system is built on the fundamental concept of **pages** as the unit of data transfer between disk and memory, with a **buffer pool** managing which pages stay in memory.
Flow:
```
Query Processor
      ↓
Buffer Manager (Currently not implement txn)
      ↓
Buffer Pool ⟷ LRU-K Cache
      ↓
File Manager
      ↓
Operating System / Disk

```
### Key components + implementation
**Page** 
┌────────────────────────────┐
│         Page Header        │ 32 bytes
├────────────────────────────┤
│                            │
│                            │
│         Page Data          │ 4064 bytes
│                            │
│                            │
└────────────────────────────┘
```java
// Data access methods

public void setInt(int offset, int value)

public int getInt(int offset)

public void write(int offset, byte[] data)

public void read(int offset, byte[] data)

  

// State management

public void markDirty()

public void markClean()

public boolean isDirty()

  
// Pin management

public void pin()

public void unpin()

public boolean isPinned()
``` 

**LRU-K**
- Evict based on the frequency of access. Tracks K most recent accesses for each entry
- Enhance normal LRU due to finding the importance of each page, not just least recent used.
- Uses a hash table for O(1) lookups (map BlockId to node) and a doubly linked list for timeline tracking
```java
public V get(K key)          // O(1) lookup with access recording
public void put(K key, V value)  // O(1) insertion with eviction handling
private void evictLRU()      // Two-phase victim **selection**
```
- When the linked list reorder due to access time, the keys in hash table don't change. Hence retain the ability for look ups.