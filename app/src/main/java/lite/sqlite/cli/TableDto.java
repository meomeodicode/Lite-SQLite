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
    
    public static TableDto forError(String errorMessage) {
        return new TableDto("ERROR: " + errorMessage);
    }
    
    public static TableDto forUpdateResult(int affectedRows) {
        return new TableDto(List.of("result"), List.of(List.of(affectedRows + " row(s) affected")));
    } 
    
    public List<List<String>> getRows() {
        return rowValues;
    } 
}