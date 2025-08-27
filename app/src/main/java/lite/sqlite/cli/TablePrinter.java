package lite.sqlite.cli;

import java.util.ArrayList;
import java.util.List;

public class TablePrinter {
    
    /**
     * Prints a TableDto to the console in a formatted table.
     * 
     * @param tableDto the data to print
     */
    public void print(TableDto tableDto) {
        if (tableDto == null) {
            System.out.println("No data to display");
            return;
        }
        
        // Handle message results (like error or update messages)
        if (tableDto.message != null && !tableDto.message.isEmpty()) {
            System.out.println(tableDto.message);
            return;
        }
        
        // Handle query results
        if (tableDto.columnNames == null || tableDto.rowValues == null) {
            System.out.println("No data to display");
            return;
        }
        
        List<Integer> columnWidths = calculateColumnWidths(tableDto);
        
        // Print header
        printRow(tableDto.columnNames, columnWidths);
        printSeparator(columnWidths);
        
        // Print rows
        for (List<String> row : tableDto.rowValues) {
            printRow(row, columnWidths);
        }
        
        // Print row count
        System.out.println(tableDto.rowValues.size() + " row(s) returned");
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
        for (String columnName : tableDto.columnNames) {
            widths.add(Math.max(columnName.length(), 5));
        }
        
        // Update with data widths
        for (List<String> row : tableDto.rowValues) {
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
