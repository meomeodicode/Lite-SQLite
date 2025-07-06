package lite.sqlite.cli;

import java.util.Scanner;

import lite.sqlite.server.IQueryEngine;

public class LiteCLI {
    private void cliLoop(IQueryEngine db)
    {
    Scanner scanner = new Scanner(System.in).useDelimiter(";");
    TablePrinter tblPrinter = new TablePrinter();
    while (true) {
      System.out.print("liter_sqlLite> ");
      String sql = scanner.next().replace("\n", " ").replace("\r", "").trim();

      TableDto result;
        if (sql.startsWith("exit")) {
            break;
        } else if (sql.startsWith("select")) {
            result = db.doQuery(sql);
        } else {
            result = db.doUpdate(sql);
        }

        if (result.message.isEmpty()) {
            tblPrinter.print(result);
        } else {
            System.out.println(result.message);
        }
    }
    scanner.close();
}

    public static void run(String[] args) {
    String dirname = (args.length == 0) ? "tinydb" : args[0];
    IQueryEngine db = new BasicQueryEngine(dirname);
    cliLoop(db);
    db.close();
  }
    
}
