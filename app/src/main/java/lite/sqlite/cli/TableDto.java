package lite.sqlite.cli;

import java.util.List;


public class TableDto {

    public List<String> columnNames;

    public List<List<String>> rowValues;

    public String message;

    public TableDto(List<String> columnNames, List<List<String>> rowValues) {
        this.columnNames = columnNames;
        this.rowValues = rowValues;
        this.message = "";
    }

    public TableDto(String message) {
        this.message = message;
    }
    
    // Static factory methods
    public static TableDto forError(String errorMessage) {
        return new TableDto("ERROR: " + errorMessage);
    }
    
    public static TableDto forUpdateResult(int affectedRows) {
        return new TableDto(affectedRows + " row(s) affected");
    }
}