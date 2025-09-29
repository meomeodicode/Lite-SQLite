package lite.sqlite.server.model.domain.commands;

import javax.xml.validation.Schema;

import lite.sqlite.server.model.SchemaPresentation;
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
    private SchemaPresentation schemaPresentation; 
}
