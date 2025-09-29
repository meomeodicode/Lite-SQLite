package lite.sqlite.server.model;


import java.util.ArrayList;
import java.util.List;

import lite.sqlite.server.storage.record.DataType;
import lite.sqlite.server.storage.record.Schema;

public class SchemaPresentation {
    private String tableName;
    private List<String> fieldNames;
    private List<String> fieldTypes;

    public SchemaPresentation() {
        this.fieldNames = new ArrayList<>();
        this.fieldTypes = new ArrayList<>();
    }

    public SchemaPresentation(String tableName) {
        this();
        this.tableName = tableName;
    }

    public SchemaPresentation(String tableName, List<String> fieldNames, List<String> fieldTypes) {
        this.tableName = tableName;
        this.fieldNames = new ArrayList<>(fieldNames);
        this.fieldTypes = new ArrayList<>(fieldTypes);
    }

    public void addField(String fieldName, String fieldType) {
        fieldNames.add(fieldName);
        fieldTypes.add(fieldType);
    }

    public List<String> getFieldNames() {
        return new ArrayList<>(fieldNames);
    }

    public List<String> getFieldTypes() {
        return new ArrayList<>(fieldTypes);
    }

    public void setFieldNames(List<String> fieldNames) {
        this.fieldNames = new ArrayList<>(fieldNames);
    }
    
    public void setFieldTypes(List<String> fieldTypes) {
        this.fieldTypes = new ArrayList<>(fieldTypes);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getFieldCount() {
        return fieldNames.size();
    }

    public boolean hasField(String fieldName) {
        return fieldNames.contains(fieldName);
    }
    
    public int getFieldIndex(String fieldName) {
        return fieldNames.indexOf(fieldName);
    }
    
    public String getFieldType(String fieldName) {
        int index = getFieldIndex(fieldName);
        return (index >= 0 && index < fieldTypes.size()) ? fieldTypes.get(index) : null;
    }

    public String getFieldType(int index) {
        return (index >= 0 && index < fieldTypes.size()) ? fieldTypes.get(index) : null;
    }

    public boolean isValid() {
        return tableName != null && !tableName.isEmpty() && 
               fieldNames.size() == fieldTypes.size() && 
               fieldNames.size() > 0;
    }

    public Schema convertToSchema() { 
        Schema schema = new Schema();
        List<String> fieldNames = this.getFieldNames();
        List<String> fieldTypes = this.getFieldTypes();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldType = fieldTypes.get(i).toUpperCase();
            
            if (fieldType.startsWith("VARCHAR")) {
                int length = 255; 
                if (fieldType.contains("(")) {
                    try {
                        String lengthStr = fieldType.substring(fieldType.indexOf("(") + 1, fieldType.indexOf(")"));
                        length = Integer.parseInt(lengthStr);
                    } catch (Exception e) {
                    }
                }
                schema.addColumn(fieldName, DataType.VARCHAR, length);
            } else if (fieldType.equals("INTEGER") || fieldType.equals("INT")) {
                schema.addColumn(fieldName, DataType.INTEGER);
            } else {
                // Default for any other unknown type
                schema.addColumn(fieldName, DataType.VARCHAR, 255);
            }
        }
        
        return schema;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SchemaPresentation{tableName='").append(tableName).append("', fields=[");
        for (int i = 0; i < fieldNames.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(fieldNames.get(i)).append(":").append(fieldTypes.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }
}
