package lite.sqlite.server;

import java.util.List;

import com.google.common.collect.Table;

import lite.sqlite.cli.TableDto;

/**
 * Interface for query execution engine.
 * This provides methods to execute SQL queries and updates.
 */
public interface QueryEngine {
    
    /**
     * Executes a SQL query (e.g., SELECT statement) and returns results.
     * 
     * @param sql the SQL query to execute
     * @return a TableDto containing the query results
     */
    TableDto doQuery(String sql);
    
    /**
     * Executes a SQL update statement (e.g., INSERT, UPDATE, DELETE, CREATE, etc.)
     * 
     * @param sql the SQL update to execute
     * @return a TableDto containing information about the operation
     */
    TableDto doUpdate(String sql);
    
    /**
     * Closes the query engine and releases resources.
     */
    default void close() {
        // Default implementation does nothing
    }
}
