package lite.sqlite.server.model.domain.commands;
import java.util.List;

import lite.sqlite.server.model.domain.clause.DBPredicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class UpdateData {
    private List<String> fields;
    private DBPredicate predicate;
    private String tblName;
    
    public String getTableName() {
        return tblName;
    }
}