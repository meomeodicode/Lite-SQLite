package lite.sqlite.server.storage.record;

public class Column {
    private String name;
    private DataType type;
    private int maxLength;  
    
    public Column(String name, DataType type) {
        this(name, type, 0);
    }
    
    public Column(String name, DataType type, int maxLength) {
        this.name = name;
        this.type = type;
        this.maxLength = maxLength;
    }
    
    public String getName() {
        return name;
    }
    
    public DataType getType() {
        return type;
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    @Override
    public String toString() {
        if (type == DataType.VARCHAR && maxLength > 0) {
            return name + " " + type + "(" + maxLength + ")";
        }
        return name + " " + type;
    }
}
