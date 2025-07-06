package lite.sqlite.cli;

import java.util.List;

/**
 * Data Transfer Object for table data.
 * This class represents the result of a SQL query or update.
 */
public class TableDto {
    /**
     * Column names for the result table.
     */
    public List<String> columns;
    
    /**
     * Rows of data, each row is a list of values.
     */
    public List<List<String>> rows;
    
    /**
     * Optional message to display (e.g., error message or success message).
     */
    public String message;
    
    /**
     * Number of rows affected by an update operation.
     */
    public int affectedRows;
    
    /**
     * Creates a new TableDto for query results.
     * 
     * @param columns the column names
     * @param rows the data rows
     * @return a TableDto instance
     */
    public static TableDto forQueryResult(List<String> columns, List<List<String>> rows) {
        TableDto dto = new TableDto();
        dto.columns = columns;
        dto.rows = rows;
        dto.message = "";
        return dto;
    }
    
    /**
     * Creates a new TableDto for update results.
     * 
     * @param affectedRows the number of rows affected
     * @return a TableDto instance
     */
    public static TableDto forUpdateResult(int affectedRows) {
        TableDto dto = new TableDto();
        dto.affectedRows = affectedRows;
        dto.message = affectedRows + " row(s) affected";
        return dto;
    }
    
    /**
     * Creates a new TableDto for error messages.
     * 
     * @param errorMessage the error message
     * @return a TableDto instance
     */
    public static TableDto forError(String errorMessage) {
        TableDto dto = new TableDto();
        dto.message = "ERROR: " + errorMessage;
        return dto;
    }
}
