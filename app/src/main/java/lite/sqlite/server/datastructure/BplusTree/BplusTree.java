package lite.sqlite.server.datastructure.BplusTree;

import java.util.AbstractMap;
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
            
            int pos = findPosition(key, node);
            keys.add(pos, key);
            values.add(pos, value);
            
           if (node.isFull(maxDegree)) {
                insertResult =  splitNode(node, true);
            }
            
        }
        else {
            int childIndex = findPosition(key, node);
            BplusTreeNode<K,V> childrenToInsert = node.getChildren().get(childIndex);
            SplitInfo splitResult = insertRecursively(childrenToInsert, key, value);
            if (splitResult != null) {
                int insertPos = findPosition(splitResult.promotedKey, node);
                node.getKeys().add(insertPos, splitResult.promotedKey);
                if (node.isFull(maxDegree)) {
                    insertResult = splitNode(node, false);
                }
            }
        }
        return insertResult;
    } 


    private SplitInfo splitNode(BplusTreeNode<K,V> node, boolean isLeaf) {

        int mid = maxDegree / 2;
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

            node.setKeys(oldKeys);
            node.setValues(oldValues);
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
        
            oldKeys.subList(mid, oldKeys.size()).clear();
            oldChildren.subList(mid + 1, oldChildren.size()).clear();
        }

        return new SplitInfo(promotedKey, newNode);
    }

    public V search(K key) {
        if (root == null) {
            return null;
        }
        
        BplusTreeNode<K,V> node = root;
        while (!node.isLeaf()) {
            int childIndex = findPosition(key, node);
            node = node.getChildren().get(childIndex);
        }
        
        List<K> keys = node.getKeys();
        List<V> values = node.getValues();
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).compareTo(key) == 0) {
                return values.get(i);
            }
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

    private int findPosition(K key, BplusTreeNode<K,V> currentNode) {

        List<K> keys = currentNode.getKeys();
        int left = 0;
        int right = keys.size()-1;
        while (left <= right) {
            int mid = left + (right - left)/2;
            int comparison = keys.get(mid).compareTo(key);

            if (comparison == 0) {
                return mid;
            }
            else if (comparison < 0) {
                right = mid-1;
            } 
            else {
                left = mid+1;
            }
        }
        return left;
    }

    public void printTree() {
        System.out.println("=== B+ Tree Structure ===");
        printNode(root, 0);
    }

    private void printNode(BplusTreeNode<K,V> node, int level) {
        if (node == null) return;
        
        String indent = "  ".repeat(level);
        String type = node.isLeaf() ? "LEAF" : "INTERNAL";
        System.out.println(indent + type + ": " + node.getKeys());
        
        if (!node.isLeaf()) {
            for (BplusTreeNode<K,V> child : node.getChildren()) {
                printNode(child, level + 1);
            }
        }
    }
}
