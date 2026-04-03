package lite.sqlite.server.parser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;

import lite.sqlite.server.model.SchemaPresentation;
import lite.sqlite.server.model.domain.clause.ComparisonOperator;
import lite.sqlite.server.model.domain.clause.DBConstant;
import lite.sqlite.server.model.domain.clause.DBExpression;
import lite.sqlite.server.model.domain.clause.DBPredicate;
import lite.sqlite.server.model.domain.clause.DBTerm;
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
    private boolean selectAll;

    //Insert
    private List<DBConstant> insertedVals;
    private List<String> insertFields;

    //Update
    private DBExpression updatedFieldValue;
    private String updatedFieldName;

    // Index
    private String indexName;
    private String indexFieldName;

    private SchemaPresentation tableDTO;

    /**
     * Creates a statement visitor that captures parsed command metadata.
     *
     * @param parser generated MySQL parser instance
     */
    public MySqlStatementVisitor(MySQLStatementParser parser) {
        this.parser = parser;
        this.tableName = "";
        this.pred = new DBPredicate();
        this.selectedFields = new ArrayList<>();
        this.selectAll = false;
        this.indexName = "";
        this.indexFieldName = "";
        this.insertFields = new ArrayList<>();
        this.insertedVals = new ArrayList<>();
        this.updatedFieldName = "";
        this.tableDTO = new SchemaPresentation();
    }

    // Command Type Visitors
    /**
     * Handles CREATE INDEX statements and extracts index metadata.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitCreateIndex(MySQLStatementParser.CreateIndexContext ctx) {
        commandType = CommandType.CREATE_INDEX;
        String fullText = ctx.getText();
        int openParen = fullText.lastIndexOf('(');
        int closeParen = fullText.lastIndexOf(')');
        
        if (openParen >= 0 && closeParen > openParen) {
            this.indexFieldName = fullText.substring(openParen + 1, closeParen);
        } else {
            this.indexFieldName = "";
        }
        
        return super.visitCreateIndex(ctx);
    }

    /**
     * Handles CREATE TABLE statements and captures table name.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitCreateTable(MySQLStatementParser.CreateTableContext ctx) {
        this.commandType = CommandType.CREATE_TABLE;
        
        // Extract table name
        if (ctx.tableName() != null) {
            this.tableName = ctx.tableName().getText();
        }
        return super.visitCreateTable(ctx);
    }


    /**
     * Marks command type as DELETE.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitDelete(MySQLStatementParser.DeleteContext ctx) {
        commandType = CommandType.DELETE;
        return super.visitDelete(ctx);
    }

    /**
     * Handles INSERT statements and extracts explicit column list.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
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
                }
            }
        }
        
        return super.visitInsert(ctx);
    }

    /**
     * Marks command type as UPDATE/MODIFY.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitUpdate(MySQLStatementParser.UpdateContext ctx) {
        commandType = CommandType.MODIFY;
        return super.visitUpdate(ctx);
    }

    /**
     * Handles SELECT statements and resets projection state.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitSelect(MySQLStatementParser.SelectContext ctx) {
        commandType = CommandType.QUERY;
        selectedFields.clear();
        selectAll = false;
        return super.visitSelect(ctx);
    }

    /**
     * Captures shorthand projection list usage (SELECT *) while still traversing
     * child projection nodes for mixed projection lists.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitProjections(MySQLStatementParser.ProjectionsContext ctx) {
        if (ctx.unqualifiedShorthand() != null) {
            this.selectAll = true;
        }
        return super.visitProjections(ctx);
    }

    /**
     * Captures table name encountered in statement context.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitTableName(MySQLStatementParser.TableNameContext ctx) {
        this.tableName = ctx.name().getText();
        return super.visitTableName(ctx);
    }

    /**
     * Captures one projection expression from a SELECT list.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitProjection(MySQLStatementParser.ProjectionContext ctx) {        

        if (ctx.qualifiedShorthand() != null) {
            this.selectAll = true;
            return super.visitProjection(ctx);
        }

        if (ctx.expr() != null) {
            this.selectedFields.add(ctx.expr().getText());
        }
        return super.visitProjection(ctx);
    }

    /**
     * Captures column names used in INSERT statements.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitColumnName(MySQLStatementParser.ColumnNameContext ctx) {
        if (commandType == CommandType.INSERT && ctx.getText() != null) {
            this.insertFields.add(ctx.getText());
        }
        return super.visitColumnName(ctx);
    }

    /**
     * Captures literal assignment values for INSERT statements.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitAssignmentValues(MySQLStatementParser.AssignmentValuesContext ctx) {
        if (ctx.assignmentValue() != null) {
            for (var assignmentValue : ctx.assignmentValue()) {
                String value = assignmentValue.expr().getText();
                if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                this.insertedVals.add(new DBConstant(value));
            }
        }
        return super.visitAssignmentValues(ctx);
    }

    /**
     * Parses a simple WHERE clause into a single predicate term.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitWhereClause(MySQLStatementParser.WhereClauseContext ctx) {
        String clause = ctx.getText();
        
        String expression = clause;
        if (expression.toUpperCase().startsWith("WHERE")) {
            expression = expression.substring(5).trim();
        }
        
        String lhsField = null;
        ComparisonOperator operator = null;
        Object rhsValue = null;
        
        if (expression.contains("=")) {
            String[] parts = expression.split("=", 2);
            lhsField = parts[0].trim();
            String valueStr = parts[1].trim();
            operator = ComparisonOperator.EQUALS;
            
            if (valueStr.startsWith("'") && valueStr.endsWith("'")) {
                rhsValue = valueStr.substring(1, valueStr.length() - 1);
            } else {
                try {
                    rhsValue = Integer.parseInt(valueStr);
                } catch (NumberFormatException e) {
                    rhsValue = valueStr;
                }
            }
        } else if (expression.contains(">")) {
            String[] parts = expression.split(">", 2);
            lhsField = parts[0].trim();
            String valueStr = parts[1].trim();
            operator = ComparisonOperator.GREATER_THAN;
            
            try {
                rhsValue = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                rhsValue = valueStr;
            }
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<", 2);
            lhsField = parts[0].trim();
            String valueStr = parts[1].trim();
            operator = ComparisonOperator.LESS_THAN;
            
            try {
                rhsValue = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                rhsValue = valueStr;
            }
        }
        
        if (lhsField != null && operator != null && rhsValue != null) {
            this.pred = new DBPredicate(new DBTerm(lhsField, operator, new DBConstant(rhsValue)));
        } else {
            this.pred = new DBPredicate(); 
        }
        
        return super.visitWhereClause(ctx);
    }

    /**
     * Captures index name token.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitIndexName(MySQLStatementParser.IndexNameContext ctx) {
        if (ctx.getText() != null) {
            this.indexName = ctx.getText();
        }
        return super.visitIndexName(ctx);
    }

    /**
     * Parses one column definition and adds it to the CREATE TABLE schema presentation.
     *
     * @param ctx parser context
     * @return delegated visitor result
     */
    @Override
    public Object visitColumnDefinition(MySQLStatementParser.ColumnDefinitionContext ctx) {
        if (ctx.getText() != null) {
            String text = ctx.getText().trim();

            String fieldName = null;
            String fieldType = null;

            Pattern compactColumnPattern = Pattern.compile(
                "^([a-zA-Z_][a-zA-Z0-9_]*)(varchar\\(\\d+\\)|integer|int)$",
                Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = compactColumnPattern.matcher(text);
            if (matcher.matches()) {
                fieldName = matcher.group(1);
                fieldType = matcher.group(2);
            }
            
            if (fieldName != null && fieldType != null) {
                this.tableDTO.addField(fieldName, fieldType);
            }
        }
        return super.visitColumnDefinition(ctx);
    }

    /**
     * Returns detected command type.
     *
     * @return command type
     */
    public CommandType getCommandType() {
        return commandType;
    }

    /**
     * Returns parsed table name.
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns parsed SELECT field list.
     *
     * @return projected fields
     */
    public List<String> getSelectedFields() {
        return selectedFields;
    }

    /**
     * Returns parsed predicate.
     *
     * @return predicate
     */
    public DBPredicate getPredicate() {
        return pred;
    }

    /**
     * Returns parsed INSERT values.
     *
     * @return constant values
     */
    public List<DBConstant> getInsertedValues() {
        return insertedVals;
    }

    /**
     * Returns parsed INSERT field names.
     *
     * @return field names
     */
    public List<String> getInsertFields() {
        return insertFields;
    }

    /**
     * Returns parsed index name.
     *
     * @return index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Returns parsed index field name.
     *
     * @return index field name
     */
    public String getIndexFieldName() {
        return indexFieldName;
    }

    /**
     * Returns parsed CREATE TABLE schema presentation.
     *
     * @return schema DTO
     */
    public SchemaPresentation getTableDTO() {
        return tableDTO;
    }

    /**
     * Returns parsed UPDATE field name.
     *
     * @return field name
     */
    public String getUpdatedFieldName() {
        return updatedFieldName;
    }

    /**
     * Returns parsed UPDATE value expression.
     *
     * @return update expression
     */
    public DBExpression getUpdatedFieldValue() {
        return updatedFieldValue;
    }
    
    /**
     * Materializes parsed state into a command DTO based on command type.
     *
     * @return command object or null when command type is not recognized
     */
    public Object getValue() {
        switch (commandType) {
            case QUERY:
                return new QueryData(selectedFields, tableName, pred, selectAll);
            case INSERT:
                return new InsertData(insertFields, insertedVals, tableName);
            case MODIFY:
                return new UpdateData(List.of(updatedFieldName), pred, tableName);
            case DELETE:
                return new DeleteData(selectedFields, List.of(pred), tableName);
            case CREATE_TABLE:
                return new CreateTableData(tableName, tableDTO);
            case CREATE_INDEX:
                return new CreateIndexData(indexName, tableName, indexFieldName, false);
            default:
                return null;
        }
    }}
