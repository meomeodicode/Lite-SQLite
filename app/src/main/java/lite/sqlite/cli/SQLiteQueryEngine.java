package lite.sqlite.cli;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import lite.sqlite.server.DatabaseManager;
import lite.sqlite.server.IQueryEngine;

/**
 * SQLite implementation of the IQueryEngine interface.
 * This class executes SQL queries against a SQLite database.
 */
public class SQLiteQueryEngine implements IQueryEngine {
    private DatabaseManager databaseManager;
    
    /**
     * Creates a new SQLiteQueryEngine with the specified DatabaseManager.
     * 
     * @param databaseManager the database manager to use
     */
    public SQLiteQueryEngine(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public TableDto doQuery(String sql) {
        try {
            // To be implemented: Use DatabaseManager to execute the query
            // For now, we'll return an empty result
            
            List<String> columns = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();
            
            /*
             * Sample implementation (to be completed by user)
             *
             * try (Connection connection = databaseManager.getConnection();
             *      Statement statement = connection.createStatement();
             *      ResultSet resultSet = statement.executeQuery(sql)) {
             *     
             *     // Get column names
             *     ResultSetMetaData metaData = resultSet.getMetaData();
             *     int columnCount = metaData.getColumnCount();
             *     
             *     for (int i = 1; i <= columnCount; i++) {
             *         columns.add(metaData.getColumnName(i));
             *     }
             *     
             *     // Get rows
             *     while (resultSet.next()) {
             *         List<String> row = new ArrayList<>();
             *         for (int i = 1; i <= columnCount; i++) {
             *             String value = resultSet.getString(i);
             *             row.add(value);
             *         }
             *         rows.add(row);
             *     }
             * }
             */
            
            return TableDto.forQueryResult(columns, rows);
        } catch (Exception e) {
            return TableDto.forError(e.getMessage());
        }
    }

    @Override
    public TableDto doUpdate(String sql) {
        try {
            // To be implemented: Use DatabaseManager to execute the update
            // For now, we'll return a dummy result
            
            int affectedRows = 0;
            
            /*
             * Sample implementation (to be completed by user)
             * 
             * try (Connection connection = databaseManager.getConnection();
             *      Statement statement = connection.createStatement()) {
             *     
             *     affectedRows = statement.executeUpdate(sql);
             * }
             */
            
            return TableDto.forUpdateResult(affectedRows);
        } catch (Exception e) {
            return TableDto.forError(e.getMessage());
        }
    }
}
