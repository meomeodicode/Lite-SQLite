package lite.sqlite.server.storage.index;

import java.util.List;

import lite.sqlite.server.datastructure.BplusTree.BplusTree;
import lite.sqlite.server.storage.table.RecordId;
import lombok.Getter;

@Getter
public class TableIndex<K extends Comparable<K>> {
    private BplusTree<K, RecordId> tree;
    private String columnName;
    private String tableName;
    private String indexName;
    private boolean isUnique;
    private int maxDegree;
    
    public TableIndex(String indexName, String tableName, String columnName, boolean isUnique, int maxDegree) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.isUnique = isUnique;
        this.maxDegree = maxDegree;
        this.tree = new BplusTree<>(maxDegree);
    }
    
    /**
     * Inserts a key-to-record mapping into the index.
     * Enforces duplicate-key rejection only when the index is unique.
     *
     * @param key indexed key value
     * @param recordToInsert record identifier to store
     */
    public void insert(K key, RecordId recordToInsert) {
        if (isUnique) {
            RecordId recordWithThisKey = tree.searchUniqueIndex(key);
            if (recordWithThisKey != null) {
                throw new IllegalArgumentException(
                String.format("Duplicate key '%s' in unique index '%s' on table '%s'", 
                    key, indexName, tableName)
            );
            }
        }

        tree.insert(key, recordToInsert);
    }
    
    /**
     * Looks up one record id for a key (intended for unique indexes).
     *
     * @param key lookup key
     * @return one matching record id or null when absent
     */
    public RecordId search(K key) {
        return tree.searchUniqueIndex(key);
    }
    
    /**
     * Looks up all record ids for a key (intended for non-unique indexes).
     *
     * @param key lookup key
     * @return all matching record ids, possibly empty
     */
    public List<RecordId> searchAll(K key) {
        return tree.searchNonUniqueIndex(key);
    }
    @Override
    public String toString() {
        return String.format("Index[%s on %s.%s, unique=%s]", 
            indexName, tableName, columnName, isUnique);
    }
}
