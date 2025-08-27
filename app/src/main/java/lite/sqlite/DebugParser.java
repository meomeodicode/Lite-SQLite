package lite.sqlite;

import lite.sqlite.server.parser.SQLParser;

public class DebugParser {
    public static void main(String[] args) {
        // Test different SQL statements
        String testSqls = "SELECT * FROM users";
        SQLParser parser = new SQLParser(testSqls);
            System.out.println("Testing SQL: " + testSqls);
        try {
            Object result = parser.queryCmd();
            System.out.println("Result: " + result);
            System.out.println("Type: " + result.getClass().getSimpleName());
            System.out.println("---");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("---");
        }
    }
}