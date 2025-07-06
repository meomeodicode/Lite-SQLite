package lite.sqlite.cli;

import java.util.Scanner;
import lite.sqlite.server.IQueryEngine;
import lite.sqlite.server.TableDto;

/**
 * Console implementation of the CLI interface.
 * This class provides a command-line interface for interacting with the database.
 */
public class ConsoleCLI implements CLI {
    private final IQueryEngine queryEngine;
    private boolean running;
    private Scanner scanner;
    private TablePrinter tablePrinter;
    
    /**
     * Creates a new ConsoleCLI with the specified query engine.
     * 
     * @param queryEngine the query engine to use
     */
    public ConsoleCLI(IQueryEngine queryEngine) {
        this.queryEngine = queryEngine;
        this.tablePrinter = new TablePrinter();
    }
    
    @Override
    public void start() {
        scanner = new Scanner(System.in).useDelimiter(";");
        running = true;
        
        System.out.println("Welcome to Lite SQLite CLI");
        System.out.println("Enter SQL commands terminated by semicolon (;)");
        System.out.println("Type 'exit;' to quit");
        System.out.println();
        
        cliLoop();
    }
    
    @Override
    public void stop() {
        running = false;
        if (scanner != null) {
            scanner.close();
        }
        System.out.println("Goodbye!");
    }
    
    /**
     * Main CLI loop that processes user input.
     */
    private void cliLoop() {
        while (running) {
            System.out.print("litesql> ");
            String sql = scanner.next().replace("\\n", " ").replace("\\r", "").trim();
            
            if (sql.equalsIgnoreCase("exit")) {
                stop();
                break;
            }
            
            TableDto result;
            try {
                if (sql.toLowerCase().startsWith("select")) {
                    result = queryEngine.doQuery(sql);
                } else {
                    result = queryEngine.doUpdate(sql);
                }
                
                if (result.message.isEmpty()) {
                    tablePrinter.print(result);
                } else {
                    System.out.println(result.message);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            
            System.out.println();
        }
    }
}