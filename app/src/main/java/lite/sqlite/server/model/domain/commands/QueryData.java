package lite.sqlite.server.model.domain.commands;
import java.util.List;

import lite.sqlite.server.model.domain.clause.DBPredicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Data
public class QueryData {
    private List<String> fields;
    private String table;
    private DBPredicate predicate;
    private Boolean selectAll;
}