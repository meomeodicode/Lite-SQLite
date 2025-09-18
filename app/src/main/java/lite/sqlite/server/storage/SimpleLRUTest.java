package lite.sqlite.server.storage;

public class SimpleLRUTest {
    public static void main(String[] args) {
        LRUCache<String, String> cache = new LRUCache<>(3, 2);
        
        System.out.println("=== Clean LRU Test (Capacity = 3) ===");
        
        // Step 1: Add exactly 3 items
        System.out.println("\n1. Adding A, B, C (should fit exactly)");
        cache.put("A", "value-A");
        cache.put("B", "value-B");
        cache.put("C", "value-C");
        cache.printCacheState();
        
        // Step 2: Add 4th item (should evict oldest)
        System.out.println("\n2. Adding D (should evict A - the oldest)");
        cache.put("D", "value-D");
        cache.printCacheState();
        
        // Check what exists
        System.out.println("\nChecking what exists:");
        System.out.println("A exists: " + (cache.get("A") != null) + " (should be FALSE - evicted)");
        System.out.println("B exists: " + (cache.get("B") != null) + " (should be TRUE)");
        System.out.println("C exists: " + (cache.get("C") != null) + " (should be TRUE)");
        System.out.println("D exists: " + (cache.get("D") != null) + " (should be TRUE)");
        
        // Step 3: Access B multiple times to make it popular
        System.out.println("\n3. Making B popular (accessing 3 times)");
        cache.get("B");
        cache.get("B");
        cache.get("B");
        cache.printCacheState();
        
        // Step 4: Add E (should NOT evict B due to popularity)
        System.out.println("\n4. Adding E (should evict C or D, NOT B)");
        cache.put("E", "value-E");
        cache.printCacheState();
        
        System.out.println("\nFinal check:");
        System.out.println("B exists: " + (cache.get("B") != null) + " (should be TRUE - was popular)");
        System.out.println("C exists: " + (cache.get("C") != null));
        System.out.println("D exists: " + (cache.get("D") != null));
        System.out.println("E exists: " + (cache.get("E") != null) + " (should be TRUE - just added)");
        
        System.out.println("\nExpected: Size should be 3/3, not 6/3 or 12/3!");
        cache.printCacheState();
    }
}