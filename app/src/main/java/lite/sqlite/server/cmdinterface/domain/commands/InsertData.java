package lite.sqlite.server.cmdinterface.domain.commands;
import java.util.List;

import lite.sqlite.server.cmdinterface.domain.clause.DBConstant;
import lite.sqlite.server.cmdinterface.domain.clause.DBPredicate;
import lombok.ToString;

@ToString
public class InsertData {
    private List<String> fields;
    private List<DBConstant> val;
    private String tblName;
}