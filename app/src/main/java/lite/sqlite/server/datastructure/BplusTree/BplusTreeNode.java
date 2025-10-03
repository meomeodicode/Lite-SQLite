package lite.sqlite.server.datastructure.BplusTree;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter    
public class BplusTreeNode<K extends Comparable<K>,V> {
    
    private boolean isLeaf;
    private List<BplusTreeNode<K,V>> children;     
    private List<K> keys;
    private List<V> values;
    private BplusTreeNode<K,V> nextLeaf;

    public BplusTreeNode (boolean isLeaf, int maxDegree) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>(maxDegree - 1);
        if (isLeaf) {
            this.values = new ArrayList<>(maxDegree - 1);
            this.children = new ArrayList<>(); 
        } else {
            this.children = new ArrayList<>(maxDegree);
            this.values = new ArrayList<>(); 
        }
        this.nextLeaf = null;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public boolean isFull(int maxDegree) {
        return keys.size() >= maxDegree;
    }
}
