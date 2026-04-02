package lite.sqlite;

import lite.sqlite.cli.CLI;
import lite.sqlite.cli.ConsoleCLI;
import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

public class App {

    public static void main(String[] args) {
        QueryEngine engine = new QueryEngineImpl();
        CLI cli = new ConsoleCLI(engine);
        cli.start();
    }
}
