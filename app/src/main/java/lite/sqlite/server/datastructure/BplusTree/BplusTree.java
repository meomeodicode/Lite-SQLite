package lite.sqlite.server.datastructure.BplusTree;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class BplusTree<K extends Comparable<K>,V> {
    
    private BplusTreeNode<K,V> root;
    private int maxDegree;

    public BplusTree(int maxDegree) {
        
        if (maxDegree < 3) {
            throw new IllegalArgumentException("Max degree must be larger than 3");
        }
        this.maxDegree = maxDegree;
        this.root = new BplusTreeNode<>(true, maxDegree);
    }

    private class SplitInfo {
        K promotedKey;
        BplusTreeNode<K,V> newNode;
        SplitInfo(K promotedKey, BplusTreeNode<K,V> newNode) {
            this.promotedKey = promotedKey;
            this.newNode = newNode;
        }
    }

    public void insert(K key, V value) {
        if (root == null) {
            root = new BplusTreeNode<>(true, maxDegree);
        }
        SplitInfo nodeSplit = insertRecursively(root, key, value);

        if (nodeSplit != null) {
            BplusTreeNode<K,V> newRoot = new BplusTreeNode<>(false, maxDegree);
            newRoot.getChildren().add(root);
            newRoot.getChildren().add(nodeSplit.newNode);
            newRoot.getKeys().add(nodeSplit.promotedKey);
            this.root = newRoot;
        }
    };

    private SplitInfo insertRecursively(BplusTreeNode<K,V> node, K key, V value) {
    
        SplitInfo insertResult = null;
        if (node.isLeaf()) {
            List<K> keys = node.getKeys();
            List<V> values = node.getValues();

            int pos = findPositionToInsert(key, node);
            keys.add(pos, key);
            values.add(pos, value);
            
        if (node.isFull(maxDegree)) {
                insertResult =  splitNode(node, true);
            }
            
        }
        else {
            int childIndex = findPositionToInsert(key, node);
            BplusTreeNode<K,V> childToInsert = node.getChildren().get(childIndex);
            SplitInfo splitResult = insertRecursively(childToInsert, key, value);
            if (splitResult != null) {
                int insertPos = findPositionToInsert(splitResult.promotedKey, node);
                node.getKeys().add(insertPos, splitResult.promotedKey);
                node.getChildren().add(insertPos+1, splitResult.newNode);
                if (node.isFull(maxDegree)) {
                    insertResult = splitNode(node, false);
                }
            }
        }
        return insertResult;
    } 


    private SplitInfo splitNode(BplusTreeNode<K,V> node, boolean isLeaf) {

        int mid = (maxDegree-1) / 2;
        BplusTreeNode<K,V> newNode = new BplusTreeNode<>(isLeaf, maxDegree);
        List<K> oldKeys = node.getKeys();
        K promotedKey = oldKeys.get(mid);

        if (isLeaf) {
            List<V> oldValues = node.getValues();

            for (int i = mid; i < oldKeys.size(); i++) {
                newNode.getKeys().add(oldKeys.get(i));
                newNode.getValues().add(oldValues.get(i));
            }
            oldKeys.subList(mid, oldKeys.size()).clear();
            oldValues.subList(mid, oldValues.size()).clear();
            newNode.setNextLeaf(node.getNextLeaf());
            node.setNextLeaf(newNode); 
        }
        else {
            List<BplusTreeNode<K,V>> oldChildren = node.getChildren();
            for (int i = mid + 1; i < oldKeys.size(); i++) {
                newNode.getKeys().add(oldKeys.get(i));
            }
        
            for (int i = mid + 1; i < oldChildren.size(); i++) {
                newNode.getChildren().add(oldChildren.get(i));
            }
        
            oldKeys.subList(mid , oldKeys.size()).clear();
            oldChildren.subList(mid+1, oldChildren.size()).clear();
        }

        return new SplitInfo(promotedKey, newNode);
    }

    /**
     * Traverses from root to a leaf that can satisfy the given search key.
     *
     * @param key target key
     * @param firstMatch when true, chooses the left-most child path that can still contain {@code key}
     * @return candidate leaf node for lookup
     */
    private BplusTreeNode<K,V> findLeafForSearch(K key, boolean firstMatch) {
        BplusTreeNode<K,V> node = root;
        while (!node.isLeaf()) {
            int childIndex = firstMatch
                ? findFirstChildForKey(key, node)
                : findPositionToInsert(key, node);
            node = node.getChildren().get(childIndex);
        }
        return node;
    }

    /**
     * Returns one value for a key lookup. For duplicate keys this returns an arbitrary
     * matching value from the located leaf (suitable for unique indexes).
     *
     * @param key lookup key
     * @return one matching value or null when absent
     */
    public V searchUniqueIndex(K key) {
        BplusTreeNode<K,V> node = findLeafForSearch(key, false);
        int keyIndex = Collections.binarySearch(node.getKeys(), key);
        if (keyIndex >= 0) {
            return node.getValues().get(keyIndex);
        }
        return null;
    }

    /**
     * Returns all values whose key equals {@code key}. This method may traverse adjacent
     * linked leaves because duplicate keys can span leaf split boundaries.
     *
     * @param key lookup key
     * @return list of all matching values, possibly empty
     */
    public List<V> searchNonUniqueIndex(K key) {
        List<V> matches = new ArrayList<>();
        BplusTreeNode<K,V> node = findLeafForSearch(key, true);
        int idx = lowerBound(node.getKeys(), key);

        while (node != null) {
            List<K> keys = node.getKeys();
            List<V> values = node.getValues();
            while (idx < keys.size() && keys.get(idx).compareTo(key) == 0) {
                matches.add(values.get(idx));
                idx++;
            }

            if (idx < keys.size()) {
                break;
            }

            node = node.getNextLeaf();
            idx = 0;
            if (node == null || node.getKeys().isEmpty()) {
                break;
            }
            if (node.getKeys().get(0).compareTo(key) != 0) {
                break;
            }
        }

        return matches;
    }

    /**
     * Convenience alias for non-unique lookups.
     *
     * @param key lookup key
     * @return list of all matching values, possibly empty
     */
    public List<V> searchAll(K key) {
        return searchNonUniqueIndex(key);
    }

    // public BplusTreeNode search(K key) {};
    // public BplusTreeNode delete() {};
    // public Byte[] serialization() {};
    // public BplusTree deserialization() {}; 

    public int getMinKeys() {
        return (int) Math.ceil(maxDegree / 2.0) - 1;
    }

    private int findPositionToInsert(K key, BplusTreeNode<K,V> currentNode) {
        List<K> keys = currentNode.getKeys();
        int left = 0;
        int right = keys.size()-1;

        while (left <= right) {
            int mid = left + (right - left)/2;
            int comparison = keys.get(mid).compareTo(key);

            if (comparison == 0) {
                return currentNode.isLeaf() ? mid : mid + 1;
            }
            else if (comparison < 0) {
                left = mid+1;
            } 
            else {
                right = mid-1;
            }
        }
        return left;
    }

    /**
     * Finds the first child index in an internal node that may contain {@code key}.
     * Used to reach the left-most leaf for duplicate-key scans.
     *
     * @param key lookup key
     * @param node internal node
     * @return child pointer index
     */
    private int findFirstChildForKey(K key, BplusTreeNode<K,V> node) {
        List<K> keys = node.getKeys();
        int i = 0;
        while (i < keys.size() && keys.get(i).compareTo(key) < 0) {
            i++;
        }
        return i;
    }

    /**
     * Computes the first index i where keys[i] >= key.
     *
     * @param keys sorted key list
     * @param key lookup key
     * @return lower-bound index in [0, keys.size()]
     */
    private int lowerBound(List<K> keys, K key) {
        int left = 0;
        int right = keys.size();

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (keys.get(mid).compareTo(key) < 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }

    public void printTree() {
        System.out.println("B+ Tree Structure:");
        if (root == null) {
            System.out.println("Empty tree");
            return;
        }
        printNodeDetailed(root, "", true);
    }

    private void printNodeDetailed(BplusTreeNode<K,V> node, String prefix, boolean isTail) {
        String nodeType = node.isLeaf() ? "LEAF" : "INTERNAL";
        System.out.println(prefix + (isTail ? "└── " : "├── ") + nodeType + ": " + node.getKeys());
        
        if (!node.isLeaf()) {
            List<BplusTreeNode<K,V>> children = node.getChildren();
            for (int i = 0; i < children.size() - 1; i++) {
                printNodeDetailed(children.get(i), prefix + (isTail ? "    " : "│   "), false);
            }
            if (children.size() > 0) {
                printNodeDetailed(children.get(children.size() - 1), prefix + (isTail ? "    " : "│   "), true);
            }
        }
    }
}
