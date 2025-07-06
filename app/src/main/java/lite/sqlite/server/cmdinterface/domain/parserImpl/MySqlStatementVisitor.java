package lite.sqlite.server.cmdinterface.domain.parserImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementBaseVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;

import lite.sqlite.server.cmdinterface.domain.clause.DBConstant;
import lite.sqlite.server.cmdinterface.domain.clause.DBExpression;
import lite.sqlite.server.cmdinterface.domain.clause.DBPredicate;


public class MySqlStatementVisitor extends MySQLStatementBaseVisitor {

    enum COMMAND_TYPE {
        QUERY, MODIFY, INSERT, DELETE, CREATE_TABLE, CREATE_INDEX
    }
    private final MySQLStatementParser parser;
    private COMMAND_TYPE commandType;
    
    //Common
    private String tableName;
    private DBPredicate pred;
    
    //Select
    private List<String> selectedFields;
    //Insert
    private List<DBConstant> insertedVals;
    private List<String> insertFields;
    //Modify
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

    @Override
    public Object visitCreateIndex(MySQLStatementParser.CreateIndexContext ctx) {
        commandType = COMMAND_TYPE.CREATE_INDEX;
        return super.visitCreateIndex(ctx);
    }

    @Override
    public Object visitCreateTable(MySQLStatementParser.CreateTableContext ctx) {
        commandType = COMMAND_TYPE.CREATE_TABLE;
        return super.visitCreateTable(ctx);
    }

    @Override
    public Object visitTableName(MySQLStatementParser.TableNameContext ctx) {
        this.tableName = ctx.name().getText();
        return super.visitTableName(ctx);
    }
}
