package lite.sqlite.server.model.domain.clause;

import java.io.Serializable;

public class DBConstant implements Comparable<DBConstant>, Serializable {
    private static final long serialVersionUID = 1L;
    private Integer ival;
    private String sval;

    public DBConstant(Integer val) {
        this.ival = val;
    }

    public DBConstant(String str) {
        this.sval = str;
    }

    public Integer asInt() {
        return ival;
    }

    public String asString() {
        return sval;
    }

    public boolean equals(Object obj) {
        DBConstant tmpObj = (DBConstant) obj;
        return (ival != null) ? ival.equals(tmpObj.ival) : sval.equals(tmpObj.sval);
    }

    public int compareTo(DBConstant tmpObj) {
        return (ival != null) ? ival.compareTo(tmpObj.ival) : sval.compareTo(tmpObj.sval);
    }
    
}
