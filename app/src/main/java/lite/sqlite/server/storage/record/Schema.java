package lite.sqlite.server.storage.record;

import java.util.ArrayList;
import java.util.List;

public class Schema {
        private List<Column> columns;
    
    public Schema() {
        this.columns = new ArrayList<>();
    }
    
    public void addColumn(String name, DataType type) {
        columns.add(new Column(name, type));
    }
    
    public void addColumn(String name, DataType type, int maxLength) {
        columns.add(new Column(name, type, maxLength));
    }
    
    public Column getColumn(int index) {
        return columns.get(index);
    }
    
    public Column getColumn(String name) {
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }
    
    public int getColumnIndex(String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }
    
    public int getColumnCount() {
        return columns.size();
    }
    
    public List<Column> getColumns() {
        return new ArrayList<>(columns);
    }
    
    public List<String> getColumnNames() {
        List<String> names = new ArrayList<>();
        for (Column column : columns) {
            names.add(column.getName());
        }
        return names;
    }
    

    public int calculateRecordSize(Object[] record) {
        int size = 0;
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            switch (column.getType()) {
                case INTEGER:
                    size += 4; 
                    break;
                case VARCHAR:
                    String strValue = i < record.length && record[i] != null ? 
                                    record[i].toString() : "";
                    size += 1 + Math.min(strValue.getBytes().length, 255); 
                    break;
            }
        }
        return size;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns.get(i));
        }
        return sb.toString();
    }
}
