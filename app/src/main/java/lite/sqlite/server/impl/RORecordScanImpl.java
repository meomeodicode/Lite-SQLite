package lite.sqlite.server.impl;

import java.util.List;

import lite.sqlite.server.RORecordScan;
import lite.sqlite.server.model.TableDefinition;

public class RORecordScanImpl implements RORecordScan {
    
    private final List<Object> currentRow;
    private final TableDefinition tableSchema;
    
    public RORecordScanImpl(List<Object> row, TableDefinition schema) {
        this.currentRow = row;
        this.tableSchema = schema;
    }

    @Override
    public Integer getInt(String fldname) {
        Object value = getFieldValue(fldname);
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else {
                return Integer.parseInt(value.toString());
            }
        } catch (NumberFormatException e) {
            return null; // Not a valid integer
        }
    }
    
    @Override
    public String getString(String fldname) {
        Object value = getFieldValue(fldname);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public boolean hasField(String fldname) {
        return tableSchema.getFieldNames().contains(fldname);
    }
    
    /**
     * Helper method to get the raw field value by name.
     */
    private Object getFieldValue(String fldname) {
        List<String> fieldNames = tableSchema.getFieldNames();
        int fieldIndex = fieldNames.indexOf(fldname);
        
        if (fieldIndex == -1 || fieldIndex >= currentRow.size()) {
            return null;
        }
        
        return currentRow.get(fieldIndex);
    }
    
}
