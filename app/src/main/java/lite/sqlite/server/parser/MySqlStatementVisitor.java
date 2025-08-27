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
        commandType = CommandType.CREATE_TABLE;
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
        return super.visitSelect(ctx);
    }

    // Command Attributes for Query & Delete

    @Override
    public Object visitTableName(MySQLStatementParser.TableNameContext ctx) {
        this.tableName = ctx.name().getText();
        return super.visitTableName(ctx);
    }

    @Override
    public Object visitProjection(MySQLStatementParser.ProjectionContext ctx) {
        if (ctx.expr() != null) {
            this.selectedFields.add(ctx.expr().getText());
        }
        return super.visitProjection(ctx);
    }

    @Override
    public Object visitColumnName(MySQLStatementParser.ColumnNameContext ctx) {
        if (commandType == CommandType.INSERT && ctx.getText() != null) {
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
            // Simple predicate parsing - you can extend this for more complex expressions
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
            // Simple parsing - extract field name and type from text
            String text = ctx.getText();
            String[] parts = text.split("\\s+");
            if (parts.length >= 2) {
                String fieldName = parts[0];
                String fieldType = parts[1];
                this.schema.addField(fieldName, fieldType);
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