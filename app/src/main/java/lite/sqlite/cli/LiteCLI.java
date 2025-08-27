package lite.sqlite.cli;

import java.util.Scanner;

import lite.sqlite.server.QueryEngine;

public class LiteCLI {
    private void cliLoop(QueryEngine db)
    {
    Scanner scanner = new Scanner(System.in).useDelimiter(";");
    TablePrinter tblPrinter = new TablePrinter();
    while (true) {
      System.out.print("liter_sqlLite> ");
      String sql = scanner.next().replace("\n", " ").replace("\r", "").trim();
      TableDto result;
      if (sql.isEmpty()) 
      {
        System.out.print("Empty sql");
        break;
      }

      if (sql.startsWith("select"))
      {
        result = db.doQuery(sql);
      }
      else if (sql.startsWith("update"))
      {
        result = db.doUpdate(sql);
      }
      else if (sql.startsWith("insert"))
      {
        result = db.doUpdate(sql);
      }
      else if (sql.startsWith("create"))
      {
        result = db.doUpdate(sql);
      }
      else if (sql.startsWith("delete"))
      {
        result = db.doUpdate(sql);
      }
      else
      {
        result = new TableDto("Unknown command: " + sql);
      }
      
      // Print the result
      if (result.message != null && !result.message.isEmpty()) {
        System.out.println(result.message);
      } else {
        tblPrinter.print(result);
      }
    }
    scanner.close();
    }

    public static void run(String[] args) {
        String dirname = (args.length == 0) ? "tinydb" : args[0];
        QueryEngine db = new SQLiteQueryEngine(dirname);
        LiteCLI cli = new LiteCLI();
        cli.cliLoop(db);
        if (db instanceof SQLiteQueryEngine) {
            ((SQLiteQueryEngine) db).close();
        }
    }
    
}
