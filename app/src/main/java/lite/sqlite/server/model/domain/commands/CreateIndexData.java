package lite.sqlite.server.model.domain.commands;

import groovy.transform.ToString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@ToString
@AllArgsConstructor
@Getter
@Setter
public class CreateIndexData {
    private String idxname; 
    private String tblname;
    private String fieldname;
}
