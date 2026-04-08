package lite.sqlite;

import lite.sqlite.cli.CLI;
import lite.sqlite.cli.ConsoleCLI;
import lite.sqlite.config.AppConfig;
import lite.sqlite.config.EventEmitterFactory;
import lite.sqlite.server.queryengine.QueryEngine;
import lite.sqlite.server.queryengine.QueryEngineImpl;

public class App {

    public static void main(String[] args) {
        AppConfig.initialize();
        QueryEngine engine = new QueryEngineImpl(EventEmitterFactory.create());
        CLI cli = new ConsoleCLI(engine);
        cli.start();
    }
}
