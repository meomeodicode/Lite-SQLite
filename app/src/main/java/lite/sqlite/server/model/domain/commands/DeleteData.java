package lite.sqlite.server.model.domain.commands;
import java.util.List;

import lite.sqlite.server.model.domain.clause.DBPredicate;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class DeleteData {
    private List<String> fields;
    private List<DBPredicate> predicate;
    private String tblName;
    
    public List<String> getFields() {
        return fields;
    }
    
    public List<DBPredicate> getPredicate() {
        return predicate;
    }
    
    public String getTableName() {
        return tblName;
    }
}