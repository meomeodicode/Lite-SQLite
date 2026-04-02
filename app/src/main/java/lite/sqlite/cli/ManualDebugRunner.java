package lite.sqlite.cli;

import java.io.File;

import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

public class ManualDebugRunner {

    private final QueryEngine engine;
    private final TablePrinter printer;

    public ManualDebugRunner() {
        this.engine = new QueryEngineImpl();
        this.printer = new TablePrinter();
    }

    public static void main(String[] args) {
        cleanupDatabaseFolder();
        ManualDebugRunner runner = new ManualDebugRunner();
        runner.run();
    }

    private void run() {
        try {
            step("Create table", "CREATE TABLE customers (id INTEGER, name VARCHAR(100), active INTEGER)");
            step("Insert row 1", "INSERT INTO customers (id, name, active) VALUES (1, 'John', 1)");
            step("Insert row 2", "INSERT INTO customers (id, name, active) VALUES (2, 'Mary', 0)");
            step("Insert row 3", "INSERT INTO customers (id, name, active) VALUES (3, 'Steve', 1)");
            step("Select all", "SELECT * FROM customers");
            step("Filter active customers", "SELECT name FROM customers WHERE active = 1");
            step("Create index", "CREATE INDEX idx_customers_id ON customers(id)");
            step("Find by id", "SELECT * FROM customers WHERE id = 3");
        } finally {
            engine.close();
        }
    }

    private void step(String title, String sql) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.println("SQL: " + sql);

        TableDto result;
        String normalized = sql.trim().toLowerCase();
        if (normalized.startsWith("select")) {
            result = engine.doQuery(sql);
        } else if (normalized.startsWith("create index")) {
            result = engine.doCreateIndex(sql);
        } else {
            result = engine.doUpdate(sql);
        }

        if (result.getErrorMessage() != null) {
            System.out.println(result.getErrorMessage());
            return;
        }

        if (result.getRows() != null && !result.getRows().isEmpty()) {
            printer.print(result);
        } else {
            System.out.println("OK");
        }
    }

    private static void cleanupDatabaseFolder() {
        File dbDir = new File("database");
        if (!dbDir.exists()) {
            return;
        }

        File[] files = dbDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(".tbl")) {
                file.delete();
            }
        }
    }
}
