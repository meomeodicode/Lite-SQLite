package lite.sqlite.server.model.domain.commands;
import java.util.List;

import lite.sqlite.server.model.domain.clause.DBPredicate;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class QueryData {
    private List<String> fields;
    private String table;
    private DBPredicate pred;
    
    public List<String> getFields() {
        return fields;
    }
    
    public String getTable() {
        return table;
    }
    
    public DBPredicate getPredicate() {
        return pred;
    }
}