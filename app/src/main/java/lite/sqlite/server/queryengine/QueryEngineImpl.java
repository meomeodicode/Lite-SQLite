package lite.sqlite.server.queryengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lite.sqlite.cli.TableDto;
import lite.sqlite.server.Parser;
import lite.sqlite.server.model.TableDefinition;
import lite.sqlite.server.model.domain.clause.DBConstant;
import lite.sqlite.server.model.domain.clause.DBPredicate;
import lite.sqlite.server.model.domain.clause.DBTerm;
import lite.sqlite.server.model.domain.commands.CreateTableData;
import lite.sqlite.server.model.domain.commands.InsertData;
import lite.sqlite.server.model.domain.commands.QueryData;
import lite.sqlite.server.parser.ParserImpl;
import lite.sqlite.server.scan.RORecordScan;
import lite.sqlite.server.scan.RORecordScanImpl;

public class QueryEngineImpl implements QueryEngine {
    
    private Map<String,TableDefinition> schema = new ConcurrentHashMap<>();
    private Map<String, List<List<Object>>> tableData = new ConcurrentHashMap<>();

    public QueryEngineImpl() {}

    @Override
    public TableDto doQuery(String sql) {
        try {
            Parser parser = new ParserImpl(sql);
            Object command = parser.queryCmd();
            
            if (command instanceof QueryData) {
                return executeSelect((QueryData) command);
            } else {
                return TableDto.forError("Invalid query command");
            }
        } catch (Exception e) {
            return TableDto.forError("Query error: " + e.getMessage());
        }
    }

    @Override
    public TableDto doUpdate(String sql) {
        try {
            Parser parser = new ParserImpl(sql);
            Object command = parser.updateCmd();
            
            if (command instanceof CreateTableData) {
                return executeCreateTable((CreateTableData) command);
            } else if (command instanceof InsertData) {
                return executeInsert((InsertData) command);
            } else {
                return TableDto.forError("Unknown update command");
            }
        } catch (Exception e) {
            return TableDto.forError("Update error: " + e.getMessage());
        }
    }

    private TableDto executeSelect(QueryData queryData)
    {
        String tableName = queryData.getTable();
        
        if (!schema.containsKey(tableName)) {
            return TableDto.forError("Table" + tableName + "doesn't exist");
        }

        TableDefinition tableDefinition = schema.get(tableName);

        List<String> selectedColumns = queryData.getFields();

        if  (selectedColumns.isEmpty())
        {
            return TableDto.forError("Table" + tableName + "has no fields");
        }

        List<Integer> columnIndexes = getColumnIndexes(tableDefinition, selectedColumns);
        List<List<Object>> rows = tableData.get(tableName);
        List<List<Object>> filteredRows = applyWhereFilter(rows, tableDefinition, queryData.getPredicate());
        
        List<String> resultColumns = new ArrayList<>();
        List<List<String>> resultRows = new ArrayList<>();

        if (selectedColumns.size() == 1 && "*".equals(selectedColumns.get(0))) {
            resultColumns = new ArrayList<>(tableDefinition.getFieldNames());
        } else {
            resultColumns = new ArrayList<>(selectedColumns);
        }

        for (List<Object> row : filteredRows) {
            List<String> resultRow = new ArrayList<>();
            for (int colIndex : columnIndexes) {
                Object value = (colIndex < row.size()) ? row.get(colIndex) : null;
                resultRow.add(value != null ? value.toString() : "NULL");
            }
            resultRows.add(resultRow);
        }
        
        return new TableDto(resultColumns, resultRows);

    }
    
    private List<List<Object>> applyWhereFilter(List<List<Object>> rows, TableDefinition tableDefinition, DBPredicate predicate) {
        if (predicate == null || predicate.getTerms() == null || predicate.getTerms().isEmpty()) {
            return rows;
        }   
        List<List<Object>> filteredRows = new ArrayList<>();

        for (List<Object> row: rows) {
            RORecordScan recordScan = new RORecordScanImpl(row, tableDefinition);
            boolean matchesAllTerms = true;

            for (DBTerm term : predicate.getTerms()) {
                if (!term.isSatisfied(recordScan)) {
                    matchesAllTerms = false;
                    break;
                }
            }
            
            if (matchesAllTerms) {
                filteredRows.add(row);
            }
        }
        return filteredRows;
    }

    private List<Integer> getColumnIndexes(TableDefinition tableDefinition, List<String> selectedColumns)
    {
        List<Integer> indexes = new ArrayList<>();
        List<String> fieldNames = tableDefinition.getFieldNames();

        if (selectedColumns.size() == 1 && "*".equals(selectedColumns.get(0))) {
            for (int i = 0; i < fieldNames.size(); i++) {
                indexes.add(i);
            }
        } else {
            for (String columnName : selectedColumns) {
                int index = fieldNames.indexOf(columnName);
                indexes.add(index); 
            }
        }
        
        return indexes;
    }

    private TableDto executeCreateTable(CreateTableData createData) {

        String tableName = createData.getTableName();
        System.out.println("DEBUG: CREATE TABLE - Table name: " + tableName);
        System.out.println("DEBUG: CREATE TABLE - Schema: " + createData.getSchema());
        System.out.println("DEBUG: CREATE TABLE - Schema fields: " + createData.getSchema().getFieldNames());
        
        if (schema.containsKey(tableName)) {
            return TableDto.forError("Table '" + tableName + "' already exists");
        }
        
        schema.put(tableName, createData.getSchema());
        tableData.put(tableName, new ArrayList<>());
        
        System.out.println("DEBUG: CREATE TABLE - Stored schema: " + schema.get(tableName).getFieldNames());
        
        return TableDto.forUpdateResult(0);
    }

    private TableDto executeInsert(InsertData insertData) {
        System.out.println("DEBUG: executeInsert called");
        String tableName = insertData.getTableName();
        System.out.println("DEBUG: Table name: " + tableName);

        if (!schema.containsKey(tableName)) {
            System.out.println("DEBUG: Table not found in schema");
            System.out.println("DEBUG: Available tables: " + schema.keySet());
            return TableDto.forError("Table '" + tableName + "' does not exist");
        }
        
        TableDefinition tableDefinition = schema.get(tableName);
        List<String> schemaFields = tableDefinition.getFieldNames();
        System.out.println("DEBUG: Schema fields: " + schemaFields);
        
        List<Object> newRow = new ArrayList<>(Collections.nCopies(schemaFields.size(), null));
        
        List<String> insertFields = insertData.getFields();
        List<DBConstant> insertValues = insertData.getValues();
        
        System.out.println("DEBUG: Insert fields: " + insertFields);
        System.out.println("DEBUG: Insert values: " + insertValues);
        System.out.println("DEBUG: Fields size: " + insertFields.size());
        System.out.println("DEBUG: Values size: " + insertValues.size());
        
        if (insertFields.isEmpty()) {
            System.out.println("DEBUG: ERROR - Insert fields is empty!");
            return TableDto.forError("No fields specified for INSERT");
        }
        
        if (insertValues.isEmpty()) {
            System.out.println("DEBUG: ERROR - Insert values is empty!");
            return TableDto.forError("No values specified for INSERT");
        }
        
        for (int i = 0; i < insertFields.size(); i++) {
            String fieldName = insertFields.get(i);
            System.out.println("DEBUG: Processing field: " + fieldName);

            int schemaIndex = schemaFields.indexOf(fieldName);
            System.out.println("DEBUG: Field index in schema: " + schemaIndex);

            if (schemaIndex == -1) {
                return TableDto.forError("Column '" + fieldName + "' does not exist");
            }
            
            DBConstant value = insertValues.get(i);
            Object convertedValue = convertValue(value);
            System.out.println("DEBUG: Converting value: " + value + " -> " + convertedValue);
            newRow.set(schemaIndex, convertedValue);
        }
        
        System.out.println("DEBUG: New row: " + newRow);
        tableData.get(tableName).add(newRow);
        System.out.println("DEBUG: Total rows in table: " + tableData.get(tableName).size());
        
        return TableDto.forUpdateResult(1);
    }

    private Object convertValue(DBConstant constant) {
        if (constant == null) return null;
        if (constant.asInt() != null) return constant.asInt();
        if (constant.asString() != null) return constant.asString();
        return constant.toString();
    }


}