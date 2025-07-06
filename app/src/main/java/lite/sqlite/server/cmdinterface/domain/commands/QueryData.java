package lite.sqlite.server.cmdinterface.domain.commands;
import java.util.List;

import lite.sqlite.server.cmdinterface.domain.clause.DBPredicate;
import lombok.ToString;

@ToString
public class QueryData {
    private List<String> fields;
    private String table;
    private DBPredicate pred;
}