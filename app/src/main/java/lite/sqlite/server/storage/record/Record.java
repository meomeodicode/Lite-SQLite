package lite.sqlite.server.storage.record;

import java.util.Arrays;

public class Record {
    private final Object[] values;
    
    public Record(Object[] values) {
        this.values = Arrays.copyOf(values, values.length);
    }
    
    public Object getValue(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= values.length) {
            throw new IndexOutOfBoundsException("Field index out of range: " + fieldIndex);
        }
        return values[fieldIndex];
    }
    
    public Object[] getValues() {
        return Arrays.copyOf(values, values.length);
    }
    
    public int size() {
        return values.length;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Record)) return false;
        Record other = (Record) obj;
        return Arrays.equals(values, other.values);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
    
    @Override
    public String toString() {
        return "Record" + Arrays.toString(values);
    }
}