package lite.sqlite.cli;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableDto {
    
    private List<String> columnNames;
    private List<List<String>> rowValues;
    private String errorMessage;
    private int updateCount = -1; 

    public TableDto(List<String> columnNames, List<List<String>> rowValues) {
        this.columnNames = columnNames;
        this.rowValues = rowValues;
        this.errorMessage = "";
    }

    public TableDto(String message) {
        this.errorMessage = message;
    }
    
    // Static factory methods
    public static TableDto forError(String errorMessage) {
        return new TableDto("ERROR: " + errorMessage);
    }
    
    public static TableDto forUpdateResult(int affectedRows) {
        return new TableDto(affectedRows + " row(s) affected");
    }
    
}