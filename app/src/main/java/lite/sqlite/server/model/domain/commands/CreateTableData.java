package lite.sqlite.server.model.domain.commands;

import lite.sqlite.server.model.TableDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
@Setter
public class CreateTableData {
    private String tableName;
    private TableDefinition schema; 
}
