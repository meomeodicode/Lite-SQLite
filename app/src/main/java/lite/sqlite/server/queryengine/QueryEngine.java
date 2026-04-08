package lite.sqlite.server.queryengine;
import lite.sqlite.cli.TableDto;
import lite.sqlite.events.model.MutationEvent;

/**
 * Interface for query execution engine.
 * This provides methods to execute SQL queries and updates.
 */
public interface QueryEngine {
    
    TableDto doQuery(String sql);
    TableDto doUpdate(String sql);
    TableDto doCreateIndex(String sql);
    void emitUpdateEvents(MutationEvent mutationEvent);
    default void close() {}
}
