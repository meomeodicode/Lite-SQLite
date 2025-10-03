package lite.sqlite.server.datastructure.BplusTree;
import java.util.Collections;
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
                System.out.println("Inserting" + insertResult.promotedKey);
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
                    System.out.println("Splitted node" + splitResult.promotedKey);
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

    private BplusTreeNode<K,V> findLeafToInsert(K key) {
        BplusTreeNode<K,V> tmpRoot = root;
        while(!tmpRoot.isLeaf()) {
            int i = findPositionToInsert(key, tmpRoot);
            tmpRoot = tmpRoot.getChildren().get(i);
        } 
        return tmpRoot;
    }

    public V search(K key) {
        BplusTreeNode<K,V> node = root;
        while (!node.isLeaf()) {
            int childIndex = findPositionToInsert(key, node);
            System.out.println("Searching " + key + " in " + node.getKeys() + " -> child " + childIndex);
            node = node.getChildren().get(childIndex);
        }
        
        int keyIndex = Collections.binarySearch(node.getKeys(), key);
        if (keyIndex >= 0) {
            return node.getValues().get(keyIndex);
        }
        return null;
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
