package lite.sqlite.server.parser;
import java.util.ArrayList;
import java.util.List;

import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;

import lite.sqlite.server.model.TableDefinition;
import lite.sqlite.server.model.domain.clause.DBConstant;
import lite.sqlite.server.model.domain.clause.DBExpression;
import lite.sqlite.server.model.domain.clause.DBPredicate;
import lite.sqlite.server.model.domain.commands.CommandType;
import lite.sqlite.server.model.domain.commands.CreateIndexData;
import lite.sqlite.server.model.domain.commands.CreateTableData;
import lite.sqlite.server.model.domain.commands.DeleteData;
import lite.sqlite.server.model.domain.commands.InsertData;
import lite.sqlite.server.model.domain.commands.QueryData;
import lite.sqlite.server.model.domain.commands.UpdateData;

public class MySqlStatementVisitor extends MySQLStatementBaseVisitor<Object> {

    private final MySQLStatementParser parser;
    private CommandType commandType;
    
    //Common
    private String tableName;
    private DBPredicate pred;
    
    //Select
    private List<String> selectedFields;

    //Insert
    private List<DBConstant> insertedVals;
    private List<String> insertFields;

    //Update
    private DBExpression updatedFieldValue;
    private String updatedFieldName;

    // Index
    private String indexName;
    private String indexFieldName;

    private TableDefinition schema;

    public MySqlStatementVisitor(MySQLStatementParser parser) {
        this.parser = parser;

        this.tableName = "";
        this.pred = new DBPredicate();

        this.selectedFields = new ArrayList<>();

        this.indexName = "";
        this.indexFieldName = "";

        this.insertFields = new ArrayList<>();
        this.insertedVals = new ArrayList<>();

        this.updatedFieldName = "";

        this.schema = new TableDefinition();
    }

    // Command Type Visitors
    @Override
    public Object visitCreateIndex(MySQLStatementParser.CreateIndexContext ctx) {
        commandType = CommandType.CREATE_INDEX;
        return super.visitCreateIndex(ctx);
    }

    @Override
    public Object visitCreateTable(MySQLStatementParser.CreateTableContext ctx) {
        System.out.println("DEBUG: visitCreateTable called");
        System.out.println("DEBUG: Context text: " + ctx.getText());
        
        this.commandType = CommandType.CREATE_TABLE;
        
        // Extract table name
        if (ctx.tableName() != null) {
            this.tableName = ctx.tableName().getText();
            System.out.println("DEBUG: Found table name: " + this.tableName);
        }
        return super.visitCreateTable(ctx);
    }


    @Override
    public Object visitDelete(MySQLStatementParser.DeleteContext ctx) {
        commandType = CommandType.DELETE;
        return super.visitDelete(ctx);
    }

    @Override
    public Object visitInsert(MySQLStatementParser.InsertContext ctx) {
        commandType = CommandType.INSERT;
        
        insertFields.clear();
        insertedVals.clear();
        
        String insertText = ctx.getText();
        int firstParen = insertText.indexOf('(');
        int firstCloseParen = insertText.indexOf(')', firstParen);
        
        if (firstParen != -1 && firstCloseParen != -1) {
            String columnsPart = insertText.substring(firstParen + 1, firstCloseParen);
            String[] columns = columnsPart.split(",");
            for (String column : columns) {
                String cleanColumn = column.trim();
                if (!cleanColumn.isEmpty()) {
                    insertFields.add(cleanColumn);
                    System.out.println("DEBUG: Added field: " + cleanColumn);
                }
            }
        }
        
        return super.visitInsert(ctx);
    }

    @Override
    public Object visitUpdate(MySQLStatementParser.UpdateContext ctx) {
        commandType = CommandType.MODIFY;
        return super.visitUpdate(ctx);
    }

    @Override
    public Object visitSelect(MySQLStatementParser.SelectContext ctx) {
        commandType = CommandType.QUERY;
        selectedFields.clear();
        
        String selectText = ctx.getText();
        System.out.println("DEBUG: Parsing SELECT: " + selectText);
        
        try {
        int selectIndex = selectText.indexOf("SELECT");
        int fromIndex = selectText.indexOf("FROM");
        
        if (selectIndex != -1 && fromIndex != -1 && selectIndex < fromIndex) {
            String fieldsPart = selectText.substring(selectIndex + 6, fromIndex); 
            System.out.println("DEBUG: Raw fields part: '" + fieldsPart + "'");
            
            if (fieldsPart.contains("*")) {
                selectedFields.add("*");
                System.out.println("DEBUG: Added SELECT *");
            } else {
                String[] fields = fieldsPart.split("AND");
                for (String field : fields) {
                    String cleanField = field.trim();
                    if (!cleanField.isEmpty()) {
                        selectedFields.add(cleanField);
                        System.out.println("DEBUG: Added select field: " + cleanField);
                    }
                }
            }
        }
    } catch (Exception e) {
        System.out.println("DEBUG: Error extracting SELECT fields: " + e.getMessage());
        selectedFields.add("*"); 
    }
    return super.visitSelect(ctx);
    }

    @Override
    public Object visitTableName(MySQLStatementParser.TableNameContext ctx) {
        this.tableName = ctx.name().getText();
        return super.visitTableName(ctx);
    }

    @Override
    public Object visitProjection(MySQLStatementParser.ProjectionContext ctx) {        
        if (ctx.expr() != null) {
            System.out.println(ctx.expr());
            this.selectedFields.add(ctx.expr().getText());
        }
        return super.visitProjection(ctx);
    }

    @Override
    public Object visitColumnName(MySQLStatementParser.ColumnNameContext ctx) {
        if (commandType == CommandType.INSERT && ctx.getText() != null) {
            System.out.println("column names:" + ctx.getText());
            this.insertFields.add(ctx.getText());
        }
        return super.visitColumnName(ctx);
    }

    @Override
    public Object visitAssignmentValues(MySQLStatementParser.AssignmentValuesContext ctx) {
        if (ctx.assignmentValue() != null) {
            for (var assignmentValue : ctx.assignmentValue()) {
                if (assignmentValue.expr() != null) {
                    this.insertedVals.add(new DBConstant(assignmentValue.expr().getText()));
                }
            }
        }
        return super.visitAssignmentValues(ctx);
    }

    @Override
    public Object visitWhereClause(MySQLStatementParser.WhereClauseContext ctx) {
        if (ctx.expr() != null) {
            this.pred = new DBPredicate(ctx.expr().getText());
        }
        return super.visitWhereClause(ctx);
    }

    @Override
    public Object visitIndexName(MySQLStatementParser.IndexNameContext ctx) {
        if (ctx.getText() != null) {
            this.indexName = ctx.getText();
        }
        return super.visitIndexName(ctx);
    }

    @Override
    public Object visitColumnDefinition(MySQLStatementParser.ColumnDefinitionContext ctx) {
        if (ctx.getText() != null) {
            System.out.println("Visit column:" + ctx.getText());
            String text = ctx.getText();
            
            String fieldName = null;
            String fieldType = null;
            
            for (int i = 1; i < text.length(); i++) {
                if (Character.isUpperCase(text.charAt(i))) {
                    fieldName = text.substring(0, i);
                    fieldType = text.substring(i);
                    break;
                }
            }
            
            if (fieldName != null && fieldType != null) {
                System.out.println("DEBUG: Parsed field: " + fieldName + " type: " + fieldType);
                this.schema.addField(fieldName, fieldType);
            } else {
                System.out.println("DEBUG: Could not parse: " + text);
            }
        }
        return super.visitColumnDefinition(ctx);
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getSelectedFields() {
        return selectedFields;
    }

    public DBPredicate getPredicate() {
        return pred;
    }

    public List<DBConstant> getInsertedValues() {
        return insertedVals;
    }

    public List<String> getInsertFields() {
        return insertFields;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexFieldName() {
        return indexFieldName;
    }

    public TableDefinition getSchema() {
        return schema;
    }

    public String getUpdatedFieldName() {
        return updatedFieldName;
    }

    public DBExpression getUpdatedFieldValue() {
        return updatedFieldValue;
    }
    
    public Object getValue() {
        switch (commandType) {
            case QUERY:
                return new QueryData(selectedFields, tableName, pred);
            case INSERT:
                return new InsertData(insertFields, insertedVals, tableName);
            case MODIFY:
                return new UpdateData(java.util.List.of(updatedFieldName), pred, tableName);
            case DELETE:
                return new DeleteData(selectedFields, java.util.List.of(pred), tableName);
            case CREATE_TABLE:
                return new CreateTableData(tableName, schema);
            case CREATE_INDEX:
                return new CreateIndexData(indexName, tableName, indexFieldName);
            default:
                return null;
        }
    }}