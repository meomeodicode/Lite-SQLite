package lite.sqlite.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for printing tables to the console.
 */
public class TablePrinter {
    
    /**
     * Prints a TableDto to the console in a formatted table.
     * 
     * @param tableDto the data to print
     */
    public void print(TableDto tableDto) {
        if (tableDto == null || tableDto.columns == null || tableDto.rows == null) {
            System.out.println("No data to display");
            return;
        }
        
        // Handle update results with no table data
        if (tableDto.columns.isEmpty() && tableDto.affectedRows > 0) {
            System.out.println(tableDto.affectedRows + " row(s) affected");
            return;
        }
        
        // Calculate column widths
        List<Integer> columnWidths = calculateColumnWidths(tableDto);
        
        // Print header
        printRow(tableDto.columns, columnWidths);
        printSeparator(columnWidths);
        
        // Print rows
        for (List<String> row : tableDto.rows) {
            printRow(row, columnWidths);
        }
        
        // Print row count
        System.out.println(tableDto.rows.size() + " row(s) returned");
    }
    
    /**
     * Calculates the width of each column based on content.
     * 
     * @param tableDto the table data
     * @return a list of column widths
     */
    private List<Integer> calculateColumnWidths(TableDto tableDto) {
        List<Integer> widths = new ArrayList<>();
        
        // Initialize with header widths
        for (String columnName : tableDto.columns) {
            widths.add(Math.max(columnName.length(), 5));
        }
        
        // Update with data widths
        for (List<String> row : tableDto.rows) {
            for (int i = 0; i < row.size() && i < widths.size(); i++) {
                String value = row.get(i);
                if (value != null) {
                    widths.set(i, Math.max(widths.get(i), value.length()));
                }
            }
        }
        
        // Add padding
        for (int i = 0; i < widths.size(); i++) {
            widths.set(i, widths.get(i) + 2);
        }
        
        return widths;
    }
    
    /**
     * Prints a row of data with proper formatting.
     * 
     * @param values the values in the row
     * @param widths the widths of each column
     */
    private void printRow(List<String> values, List<Integer> widths) {
        StringBuilder builder = new StringBuilder();
        builder.append("|");
        
        for (int i = 0; i < values.size() && i < widths.size(); i++) {
            String value = values.get(i);
            if (value == null) {
                value = "NULL";
            }
            
            int width = widths.get(i);
            builder.append(" ");
            builder.append(value);
            
            // Pad with spaces
            for (int j = value.length() + 1; j < width; j++) {
                builder.append(" ");
            }
            
            builder.append("|");
        }
        
        System.out.println(builder.toString());
    }
    
    /**
     * Prints a separator line.
     * 
     * @param widths the widths of each column
     */
    private void printSeparator(List<Integer> widths) {
        StringBuilder builder = new StringBuilder();
        builder.append("+");
        
        for (int width : widths) {
            for (int i = 0; i < width; i++) {
                builder.append("-");
            }
            builder.append("+");
        }
        
        System.out.println(builder.toString());
    }
}
