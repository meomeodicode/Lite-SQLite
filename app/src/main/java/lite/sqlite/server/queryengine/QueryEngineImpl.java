package lite.sqlite.server.queryengine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lite.sqlite.cli.TableDto;
import lite.sqlite.server.Parser;
import lite.sqlite.server.model.domain.clause.DBConstant;
import lite.sqlite.server.model.domain.clause.DBPredicate;
import lite.sqlite.server.model.domain.clause.DBTerm;
import lite.sqlite.server.model.domain.commands.CreateTableData;
import lite.sqlite.server.model.domain.commands.InsertData;
import lite.sqlite.server.model.domain.commands.QueryData;
import lite.sqlite.server.parser.ParserImpl;
import lite.sqlite.server.scan.RORecordScan;
import lite.sqlite.server.scan.RORecordScanImpl;
import lite.sqlite.server.storage.BasicFileManager;
import lite.sqlite.server.storage.buffer.BufferPool;
import lite.sqlite.server.storage.table.RecordId;
import lite.sqlite.server.storage.table.Table;
import lite.sqlite.server.storage.record.Record;
import lite.sqlite.server.storage.record.Schema;;


public class QueryEngineImpl implements QueryEngine {
    
    private final Map<String,Table> tables = new ConcurrentHashMap<>();
    private final BufferPool bufferPool;
    private final BasicFileManager fileManager;

    public QueryEngineImpl() {
        File dbDirectory = new File("database");
        this.fileManager = new BasicFileManager(dbDirectory);
        this.bufferPool = new BufferPool(50, fileManager);
    }

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
        
        if (!tables.containsKey(tableName)) {
            return TableDto.forError("Table" + tableName + "doesn't exist");
        }

        Table table = tables.get(tableName);
        Schema selectedSchema = table.getSchema();
        List<String> selectedColumns = queryData.getFields();
        
        if  (selectedColumns.isEmpty()) {
            return TableDto.forError("Table" + tableName + "has no fields");
        }
        else if (selectedColumns.get(0).equals("*")) {
            selectedColumns = selectedSchema.getColumnNames();
        }

        try {
            List<Record> records = new ArrayList<>();
            for (Record record: table) {
                records.add(record);
            }
            List<Record> filteredRows = applyWhereFilter(selectedSchema, records, queryData.getPredicate());        
            List<Integer> columnIndexes = new ArrayList<>();
            
            for (String columnName: selectedColumns) {
                int columnIdx = selectedSchema.getColumnIndex(columnName);
                columnIndexes.add(columnIdx);
            }

            List<List<String>> resultRows = new ArrayList<>();
            for (Record record : filteredRows) {
                List<String> resultRow = new ArrayList<>();
                Object[] values = record.getValues();
                
                for (int colIndex : columnIndexes) {
                    Object value = (colIndex < values.length) ? values[colIndex] : null;
                    resultRow.add(value != null ? value.toString() : "NULL");
                }
                resultRows.add(resultRow);
        }
        
        return new TableDto(selectedColumns, resultRows);
        
    } catch (Exception e) {
        return TableDto.forError("Error executing SELECT: " + e.getMessage());
    }
    }
    
    private List<Record> applyWhereFilter(Schema selectedSchema, List<Record> rows, DBPredicate predicate) {

        if (predicate == null || predicate.getTerms() == null || predicate.getTerms().isEmpty()) {
            return rows;
        }   
        List<Record> filteredRows = new ArrayList<>();

        for (Record row: rows) {
            RORecordScan recordScan = new RORecordScanImpl(row, selectedSchema);
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

    private TableDto executeCreateTable(CreateTableData createData) {

        String tableName = createData.getTableName();

        if (tables.containsKey(tableName)) {
            return TableDto.forError("Table '" + tableName + "' already exists");
        }

        Schema newSchema = createData.getSchemaPresentation().convertToSchema();
        Table newTable = new Table(newSchema, bufferPool, tableName);
        tables.put(tableName, newTable);        
        return TableDto.forUpdateResult(0);
    }


    private TableDto executeInsert(InsertData insertData) {
        System.out.println("DEBUG: executeInsert called");
        
        String tableName = insertData.getTableName();
        Table table = tables.get(tableName);

        System.out.println("DEBUG: Table name: " + tableName);

        if (!tables.containsKey(tableName)) {
            System.out.println("DEBUG: Table not found in schema");
            System.out.println("DEBUG: Available tables: " + tables.keySet());
            return TableDto.forError("Table '" + tableName + "' does not exist");
        }
        
        Schema schema = table.getSchema();
        List<String> schemaFields = schema.getColumnNames();
        System.out.println("DEBUG: Schema fields: " + schemaFields);
        
        List<String> insertFields = insertData.getFields();
        List<DBConstant> insertValues = insertData.getValues();
        
        if (insertFields.isEmpty()) {
            System.out.println("DEBUG: ERROR - Insert fields is empty!");
            return TableDto.forError("No fields specified for INSERT");
        }
        
        if (insertValues.isEmpty()) {
            System.out.println("DEBUG: ERROR - Insert values is empty!");
            return TableDto.forError("No values specified for INSERT");
        }
        
        try {
            Object[] recordData = new Object[schemaFields.size()];
            
            for (int i = 0; i < insertFields.size(); i++) {
                String fieldName = insertFields.get(i);
                int schemaIndex = schemaFields.indexOf(fieldName);

                if (schemaIndex == -1) {
                    return TableDto.forError("Column '" + fieldName + "' does not exist");
                }
                
                DBConstant value = insertValues.get(i);
                Object convertedValue = convertValue(value);
                recordData[schemaIndex] = convertedValue;
            }
            
            Record record = new Record(recordData);
            RecordId recordId = table.insertRecord(record);
            System.out.println("DEBUG: Record inserted with ID: " + recordId);
            return TableDto.forUpdateResult(1);
        }
        catch (Exception e) {
            return TableDto.forError("Error inserting record");
        }
    }

    private Object convertValue(DBConstant constant) {
        if (constant == null) return null;
        if (constant.asInt() != null) return constant.asInt();
        if (constant.asString() != null) return constant.asString();
        return constant.toString();
    }


}