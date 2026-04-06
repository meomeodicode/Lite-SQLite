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
    private List<String> updatedFieldNames;
    private List<DBConstant> updatedFieldValues;

    // Index
    private String indexName;
    private String indexFieldName;
    private boolean isUnique;

    private SchemaPresentation tableDTO;

    /**
     * Creates a statement visitor that captures parsed command metadata.
     *
     * @param parser generated MySQL parser instance
     */
    public MySqlStatementVisitor(MySQLStatementParser parser) {
        this.tableName = "";
        this.pred = new DBPredicate();
        this.selectedFields = new ArrayList<>();
        this.selectAll = false;
        this.indexName = "";
        this.indexFieldName = "";
        this.isUnique = false;
        this.insertFields = new ArrayList<>();
        this.insertedVals = new ArrayList<>();
        this.updatedFieldName = "";
        this.updatedFieldNames = new ArrayList<>();
        this.updatedFieldValues = new ArrayList<>();
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

        // Reset index state per statement to avoid leaking values across parses.
        this.indexName = "";
        this.indexFieldName = "";
        this.isUnique = false;

        if (ctx.indexName() != null) {
            this.indexName = sanitizeIdentifier(ctx.indexName().getText());
        }
        if (ctx.tableName() != null) {
            this.tableName = sanitizeIdentifier(ctx.tableName().getText());
        }

        String fullText = ctx.getText();
        this.isUnique = parseUniqueFromCreateIndex(fullText);
        this.indexFieldName = extractFirstIndexedColumn(fullText);

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
        parseDeleteStatement(ctx.getText());
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
        parseUpdateStatement(ctx.getText());
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
        if (commandType == CommandType.CREATE_INDEX && ctx.getText() != null) {
            this.indexName = ctx.getText();
        }
        return super.visitIndexName(ctx);
    }

    private String sanitizeIdentifier(String identifier) {
        if (identifier == null) {
            return "";
        }

        String trimmed = identifier.trim();
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void parseUpdateStatement(String updateText) {
        if (updateText == null) {
            return;
        }

        updatedFieldNames.clear();
        updatedFieldValues.clear();
        updatedFieldName = "";
        updatedFieldValue = null;

        String normalized = updateText.trim();
        String upper = normalized.toUpperCase();
        int setPos = upper.indexOf("SET");
        if (setPos < 0) {
            return;
        }

        String tablePart = normalized.substring("UPDATE".length(), setPos).trim();
        if (!tablePart.isEmpty()) {
            this.tableName = sanitizeIdentifier(tablePart);
        }

        int wherePos = upper.indexOf("WHERE", setPos + 3);
        String assignmentsPart = (wherePos >= 0)
            ? normalized.substring(setPos + 3, wherePos).trim()
            : normalized.substring(setPos + 3).trim();

        for (String assignment : splitAssignments(assignmentsPart)) {
            String[] parts = assignment.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String field = sanitizeIdentifier(parts[0].trim());
            if (field.isEmpty()) {
                continue;
            }

            DBConstant value = parseConstant(parts[1].trim());
            updatedFieldNames.add(field);
            updatedFieldValues.add(value);
        }

        if (!updatedFieldNames.isEmpty()) {
            updatedFieldName = updatedFieldNames.get(0);
            updatedFieldValue = DBExpression.constant(updatedFieldValues.get(0));
        }
    }

    private void parseDeleteStatement(String deleteText) {
        if (deleteText == null) {
            return;
        }

        String normalized = deleteText.trim();
        String upper = normalized.toUpperCase();
        int fromPos = upper.indexOf("FROM");
        if (fromPos < 0) {
            return;
        }

        int wherePos = upper.indexOf("WHERE", fromPos + 4);
        String tablePart = (wherePos >= 0)
            ? normalized.substring(fromPos + 4, wherePos).trim()
            : normalized.substring(fromPos + 4).trim();

        if (!tablePart.isEmpty()) {
            this.tableName = sanitizeIdentifier(tablePart);
        }
    }

    private List<String> splitAssignments(String assignmentsPart) {
        List<String> assignments = new ArrayList<>();
        if (assignmentsPart == null || assignmentsPart.isBlank()) {
            return assignments;
        }

        StringBuilder token = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < assignmentsPart.length(); i++) {
            char ch = assignmentsPart.charAt(i);
            if (ch == '\'') {
                inString = !inString;
            }

            if (ch == ',' && !inString) {
                String piece = token.toString().trim();
                if (!piece.isEmpty()) {
                    assignments.add(piece);
                }
                token.setLength(0);
                continue;
            }

            token.append(ch);
        }

        String tail = token.toString().trim();
        if (!tail.isEmpty()) {
            assignments.add(tail);
        }

        return assignments;
    }

    private DBConstant parseConstant(String literal) {
        if (literal == null) {
            return new DBConstant(null);
        }

        String trimmed = literal.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
            return new DBConstant(inner);
        }

        try {
            return new DBConstant(Integer.parseInt(trimmed));
        } catch (NumberFormatException ex) {
            return new DBConstant(trimmed);
        }
    }

    private boolean parseUniqueFromCreateIndex(String createIndexText) {
        if (createIndexText == null) {
            return false;
        }

        String normalized = createIndexText.replaceAll("\\s+", "").toUpperCase();
        return normalized.startsWith("CREATEUNIQUEINDEX");
    }

    private String extractFirstIndexedColumn(String createIndexText) {
        if (createIndexText == null) {
            return "";
        }

        int openParen = createIndexText.indexOf('(');
        int closeParen = createIndexText.lastIndexOf(')');
        if (openParen < 0 || closeParen <= openParen) {
            return "";
        }

        String fieldsText = createIndexText.substring(openParen + 1, closeParen).trim();
        if (fieldsText.isEmpty()) {
            return "";
        }

        String firstField = fieldsText.split(",", 2)[0].trim();
        String normalizedField = firstField.replaceAll("(?i)(ASC|DESC)$", "").trim();
        return sanitizeIdentifier(normalizedField);
    }

    private CreateIndexData buildCreateIndexData() {
        return new CreateIndexData(
            sanitizeIdentifier(indexName),
            sanitizeIdentifier(tableName),
            sanitizeIdentifier(indexFieldName),
            isUnique
        );
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
                return new UpdateData(updatedFieldNames, updatedFieldValues, pred, tableName);
            case DELETE:
                return new DeleteData(selectedFields, List.of(pred), tableName);
            case CREATE_TABLE:
                return new CreateTableData(tableName, tableDTO);
            case CREATE_INDEX:
                return buildCreateIndexData();
            default:
                return null;
        }
    }
}
