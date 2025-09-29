package lite.sqlite.server.scan;

import lite.sqlite.server.storage.record.Schema;
import lite.sqlite.server.storage.record.Record;

public class RORecordScanImpl implements RORecordScan {
    
    private Record currentRow;
    private Schema tableSchema;
    
    public RORecordScanImpl(Record row, Schema schema) {
        this.currentRow = row;
        this.tableSchema = schema;
    }

    @Override
    public Integer getInt(String fieldName) {
        Object value = getFieldValue(fieldName);
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
            return null;
        }
    }
    
    @Override
    public String getString(String fieldName) {
        Object value = getFieldValue(fieldName);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public boolean hasField(String fieldName) {
        return tableSchema.getColumnNames().contains(fieldName);
    }
    
    private Object getFieldValue(String fieldName) {
        int fieldIndex = tableSchema.getColumnIndex(fieldName);
        if (fieldIndex == -1 || fieldIndex >= currentRow.size()) {
            return null;
        }
        return currentRow.getValue(fieldIndex);
    }
    
}
