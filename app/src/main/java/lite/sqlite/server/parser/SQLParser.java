package lite.sqlite.server.parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;
import org.apache.shardingsphere.sql.parser.mysql.parser.MySQLLexer;

import lite.sqlite.server.Parser;
import lite.sqlite.server.model.domain.commands.QueryData;

public class SQLParser implements Parser {
    
    MySqlStatementVisitor sqlStatementVisitor;

    public SQLParser(String sql) {
        MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(sql));
        MySQLStatementParser parser = new MySQLStatementParser(new CommonTokenStream(lexer));

        sqlStatementVisitor = new MySqlStatementVisitor(parser);
        sqlStatementVisitor.visit(parser.execute());
    }

    @Override
    public QueryData queryCmd() {
        return (QueryData) sqlStatementVisitor.getValue();
    }

    @Override
    public Object updateCmd() {
        return sqlStatementVisitor.getValue();
    }
}
