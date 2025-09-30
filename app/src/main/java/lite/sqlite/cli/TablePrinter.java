package lite.sqlite.cli;

import java.util.ArrayList;
import java.util.List;

public class TablePrinter {

    public void print(TableDto tableDto) {
        
        System.out.println("DEBUG: TablePrinter received: columns=" + tableDto.getColumnNames() + 
                         ", rows=" + (tableDto.getRows() != null ? tableDto.getRows().size() : "null"));
        
        List<String> headers = tableDto.getColumnNames();
        List<List<String>> rows = tableDto.getRows();
        
        if (headers == null || rows == null) {
            System.out.println("No data available to display");
            return;
        }

        List<Integer> columnWidths = new ArrayList<>();
        
        for (String header : headers) {
            columnWidths.add(header.length());
        }
        

        for (List<String> row : rows) {
            for (int i = 0; i < row.size() && i < headers.size(); i++) {
                String value = row.get(i);
                if (value != null) {
                    columnWidths.set(i, Math.max(columnWidths.get(i), value.length()));
                }
            }
        }
        
        // Add padding
        for (int i = 0; i < columnWidths.size(); i++) {
            columnWidths.set(i, columnWidths.get(i) + 2);
        }
        
        // Print table
        printSeparator(columnWidths);
        printRow(headers, columnWidths);
        printSeparator(columnWidths);
        
        for (List<String> row : rows) {
            printRow(row, columnWidths);
        }
        
        printSeparator(columnWidths);
        System.out.println(rows.size() + " row(s) returned");
    }
    
    private void printSeparator(List<Integer> columnWidths) {
        for (Integer width : columnWidths) {
            System.out.print("+");
            for (int i = 0; i < width; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }
    
    private void printRow(List<String> values, List<Integer> columnWidths) {
        for (int i = 0; i < values.size() && i < columnWidths.size(); i++) {
            String value = values.get(i);
            int width = columnWidths.get(i);
            
            System.out.print("| ");
            System.out.print(value);
            
            // Pad with spaces
            for (int j = 0; j < width - value.length() - 2; j++) {
                System.out.print(" ");
            }
            
            System.out.print(" ");
        }
        System.out.println("|");
    }
}
