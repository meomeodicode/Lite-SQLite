package lite.sqlite.server.model.domain.clause;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;

@Getter
public class DBConstant implements Comparable<DBConstant>, Serializable {

    private Object val;

    public DBConstant(Object val) {
        this.val = val;
    }

    public Integer asInt() {
        return (val instanceof Integer) ? (Integer) val : null;
    }

    public String asString() {
        // Return null if val is not a string, to be consistent with asInt()
        return (val instanceof String) ? (String) val : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DBConstant that = (DBConstant) obj;
        // Use Objects.equals for safe comparison of potentially null values.
        return Objects.equals(val, that.val);
    }

@Override
    public int compareTo(DBConstant other) {
        if (other == null || other.val == null) {
            return (this.val == null) ? 0 : 1; // An object is greater than null
        }
        if (this.val == null) {
            return -1; // Null is less than an object
        }

        // Ensure both values are of a comparable type
        if (!(this.val instanceof Comparable) || !(other.val instanceof Comparable)) {
            throw new ClassCastException("Cannot compare non-comparable types.");
        }

        // Check for incompatible types (e.g., Integer vs. String)
        if (this.val.getClass() != other.val.getClass()) {
            throw new ClassCastException("Cannot compare " + this.val.getClass().getSimpleName() +
                                         " with " + other.val.getClass().getSimpleName());
        }

        @SuppressWarnings("unchecked")
        Comparable<Object> thisComparable = (Comparable<Object>) this.val;
        
        return thisComparable.compareTo(other.val);
    }

    public Object asJavaVal() {
        return val;
    }

    @Override
    public String toString() {
        if (val instanceof String) {
            return "'" + val + "'"; // Enclose strings in single quotes
        }
        if (val == null) {
            return "NULL";
        }
        return val.toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(val);
    }
}