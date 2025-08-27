package lite.sqlite.server.model.domain.clause;

/**
 * Interface for read-only record scanning.
 * Represents a scan over records, allowing values to be retrieved by field name.
 */
public interface RORecordScan {
    /**
     * Gets the value of the specified field from the current record as an Integer.
     * 
     * @param fldname the name of the field
     * @return the field value as an Integer, or null if not applicable
     */
    Integer getInt(String fldname);
    
    /**
     * Gets the value of the specified field from the current record as a String.
     * 
     * @param fldname the name of the field
     * @return the field value as a String, or null if not applicable
     */
    String getString(String fldname);
    
    /**
     * Checks if the scan contains a field with the specified name.
     * 
     * @param fldname the name of the field
     * @return true if the field exists, false otherwise
     */
    boolean hasField(String fldname);
}
