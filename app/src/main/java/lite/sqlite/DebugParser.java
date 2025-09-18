// package lite.sqlite;

// import lite.sqlite.cli.TableDto;
// import lite.sqlite.cli.TablePrinter;
// import lite.sqlite.server.queryengine.QueryEngineImpl;

// package lite.sqlite.server.storage;

// public class LRUCacheTest {
//     public static void main(String[] args) {
//         // Create LRU-2 cache with capacity 3
//         LRUCache<String, String> cache = new LRUCache<>(3, 2);
        
//         System.out.println("=== Testing LRU-K Cache Implementation ===");
        
//         // Test 1: Basic operations
//         System.out.println("\nTest 1: Basic Put/Get");
//         cache.put("key1", "value1");
//         cache.put("key2", "value2");
//         cache.put("key3", "value3");
        
//         System.out.println("Get key1: " + cache.get("key1"));
//         System.out.println("Get key2: " + cache.get("key2"));
//         System.out.println("Get key3: " + cache.get("key3"));
//         cache.printCacheState();
        
//         // Test 2: Eviction with sequential scan pattern
//         System.out.println("\nTest 2: Sequential Scan Pattern");
        
//         // First sequence: A B C D (should evict someone)
//         cache.put("A", "A-value");
//         cache.put("B", "B-value");
//         cache.put("C", "C-value");
//         cache.put("D", "D-value"); // Should evict key1 (LRU)
        
//         System.out.println("After sequential scan, key1 exists: " + (cache.get("key1") != null));
//         cache.printCacheState();
        
//         // Test 3: Access pattern with key popularity
//         System.out.println("\nTest 3: Popular Key Pattern");
        
//         // Access A multiple times to make it popular
//         System.out.println("Accessing A multiple times");
//         cache.get("A");
//         cache.get("A");
//         cache.get("A");
        
//         // Access B,C,D once each
//         cache.get("B");
//         cache.get("C");
//         cache.get("D");
        
//         // Add new key E (should not evict A due to its popularity)
//         cache.put("E", "E-value");
        
//         System.out.println("After adding E, check which key was evicted:");
//         System.out.println("A exists: " + (cache.get("A") != null));
//         System.out.println("B exists: " + (cache.get("B") != null));
//         System.out.println("C exists: " + (cache.get("C") != null));
//         System.out.println("D exists: " + (cache.get("D") != null));
//         System.out.println("E exists: " + (cache.get("E") != null));
        
//         cache.printCacheState();
        
//         // Test 4: Hit ratio statistics
//         System.out.println("\nTest 4: Hit Ratio");
//         System.out.printf("Final Hit Ratio: %.2f%% (hits=%d, misses=%d)\n", 
//                          cache.getHitRatio(), cache.getHits(), cache.getMisses());
//     }
// }