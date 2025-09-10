package lite.sqlite.server;
import lite.sqlite.cli.TableDto;

/**
 * Interface for query execution engine.
 * This provides methods to execute SQL queries and updates.
 */
public interface QueryEngine {
    
    TableDto doQuery(String sql);
    TableDto doUpdate(String sql);
    default void close() {
    }
}
