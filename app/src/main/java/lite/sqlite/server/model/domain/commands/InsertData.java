package lite.sqlite.server.model.domain.commands;
import java.util.List;

import lite.sqlite.server.model.domain.clause.DBConstant;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class InsertData {
    private List<String> fields;
    private List<DBConstant> val;
    private String tblName;
    
    public List<String> getFields() {
        return fields;
    }
    
    public List<DBConstant> getValues() {
        return val;
    }
    
    public String getTableName() {
        return tblName;
    }
}