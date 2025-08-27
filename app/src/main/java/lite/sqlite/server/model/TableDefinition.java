package lite.sqlite.server.model;
import java.util.ArrayList;
import java.util.List;


public class TableDefinition {
    private final List<String> fieldNames;
    private final List<String> fieldTypes;

    public TableDefinition() {
        this.fieldNames = new ArrayList<>();
        this.fieldTypes = new ArrayList<>();
    }

    public void addField(String name, String type) {
        this.fieldNames.add(name);
        this.fieldTypes.add(type);
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public List<String> getFieldTypes() {
        return fieldTypes;
    }
}