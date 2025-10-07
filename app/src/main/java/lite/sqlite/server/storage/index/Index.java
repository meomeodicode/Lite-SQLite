package lite.sqlite.server.storage.index;

import lite.sqlite.server.datastructure.BplusTree.BplusTree;
import lite.sqlite.server.storage.table.RecordId;

public class Index<K extends Comparable<K>> {
    
    private BplusTree<K, RecordId> tree;
    private String columnName;
    private String tableName;
    private String indexName;
    private boolean isUnique;
    private int maxDegree;
    
    public Index(String indexName, String tableName, String columnName, boolean isUnique, int maxDegree) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.isUnique = isUnique;
        this.maxDegree = maxDegree;
        this.tree = new BplusTree<>(maxDegree);
    }
    
    public void insert(K key, RecordId value) {
        if (isUnique && tree.search(key) != null) {
            throw new IllegalArgumentException(
                String.format("Duplicate key '%s' in unique index '%s' on table '%s'", 
                    key, indexName, tableName)
            );
        }
        tree.insert(key, value);
    }
    
    public RecordId search(K key) {
        return tree.search(key);
    }
    
    
    // Getters
    public String getColumnName() { return columnName; }
    public String getTableName() { return tableName; }
    public String getIndexName() { return indexName; }
    public boolean isUnique() { return isUnique; }
    public int getMaxDegree() { return maxDegree; }
    
    @Override
    public String toString() {
        return String.format("Index[%s on %s.%s, unique=%s]", 
            indexName, tableName, columnName, isUnique);
    }
}