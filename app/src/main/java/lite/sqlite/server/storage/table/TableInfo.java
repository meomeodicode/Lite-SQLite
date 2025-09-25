package lite.sqlite.server.storage.table;

import lite.sqlite.server.storage.record.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableInfo {
    private String tableName;
    private Schema schema;
    private String filename;
    private long createdTime;
    private long lastModified;
    private int recordCount;

    public TableInfo(String tableName, Schema schema, String filename) {
        this.tableName = tableName;
        this.schema = schema;
        this.filename = filename;
        this.createdTime = System.currentTimeMillis();
        this.lastModified = createdTime;
        this.recordCount = 0;
    }

    public void touch() {
        this.lastModified = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("TableInfo{name='%s', file='%s', columns=%d, records~%d}", 
                           tableName, filename, schema.getColumnCount(), recordCount);
    }
}
