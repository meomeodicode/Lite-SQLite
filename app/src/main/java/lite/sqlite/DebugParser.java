package lite.sqlite;

import lite.sqlite.cli.TableDto;
import lite.sqlite.cli.TablePrinter;
import lite.sqlite.server.queryengine.QueryEngineImpl;

public class DebugParser {
    public static void main(String[] args) {
        QueryEngineImpl engine = new QueryEngineImpl();
        TablePrinter printer = new TablePrinter();
        
        System.out.println("=== Testing Lite-SQLite Query Engine ===\n");
        
        System.out.println("1. CREATE TABLE users (id INT, name VARCHAR(50))");
        TableDto result1 = engine.doUpdate("CREATE TABLE users (id INT, name VARCHAR(50))");
        printer.print(result1);
        System.out.println();
        
        System.out.println("2. INSERT INTO users (id, name) VALUES (1, 'Alice')");
        TableDto result2 = engine.doUpdate("INSERT INTO users (id, name) VALUES (1, 'Alice')");
        printer.print(result2);
        System.out.println();
        
        System.out.println("3. INSERT INTO users (id, name) VALUES (2, 'Bob')");
        TableDto result3 = engine.doUpdate("INSERT INTO users (id, name) VALUES (2, 'Bob')");
        printer.print(result3);
        System.out.println();
        
        System.out.println("4. SELECT * FROM users");
        TableDto result4 = engine.doQuery("SELECT * FROM users");
        printer.print(result4);
        System.out.println();
    
    }
}