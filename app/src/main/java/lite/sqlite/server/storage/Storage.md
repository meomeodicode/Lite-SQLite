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
#### Page 
A page is a fixed-size block of data, either in memory or on disk.
Pages read from disk are placed in [[Database Buffer Pool]] -> reducing disk IO.
Once a page is loaded, it's not just the rows you are asking for, it's all the rows stored on that page -> If row  size is small, more of them can fit into a single page -> More useful data, less disk IO.
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

**Page structure**: 
To balance learning and complexity, each page in Lite-SQLite uses a **slotted page**structure with **4096 bytes** per page:

```
┌─────────────┬─────────────────┬─────────────┬─────────────────┐
│ Page Header │ Slot Directory  │ Free Space  │ Record Data     │
│ (32 bytes)  │ (grows →)       │             │ (← grows)       │
└─────────────┴─────────────────┴─────────────┴─────────────────┘
0            32                              free_ptr          4096
```
- Each slot entry is 8 bytes.
	- Slot Entry Structure:
		[Offset 0-3]: Record offset (where record data starts).
		[Offset 4-7]: Record length (size of record in bytes).
- Page header is 32 bytes

#### How the tables are stored?

Records are stored from the end of the page, growing backwards to the slot directory. Current implementation use variable-length record and apply row-based storage.
	- 
	- Slot Entry Structure:
		[Offset 0-3]: Record offset (where record data starts)
		[Offset 4-7]: Record length (size of record in bytes)
		
		Example:
		Slot 0: [3500, 120] → Record at offset 3500, length 120 bytes
		Slot 1: [3380, 150] → Record at offset 3380, length 150 bytes

- Complete records example: 
	Schema: [id: INTEGER, name: VARCHAR, email: VARCHAR]
	Record: [1, "John", "john@example.com"]
	
	Serialized Format:
	[0x00,0x00,0x00,0x01] [0x04,'J','o','h','n'] [0x10,'j','o','h','n','@','e','x','a','m','p','l','e','.','c','o','m']
	 ↑ id = 1              ↑ name = "John"         ↑ email = "john@example.com"
	 4 bytes               5 bytes                 17 bytes
	Total: 26 bytes

#### Buffer
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


