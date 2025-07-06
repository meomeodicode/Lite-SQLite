package lite.sqlite.server.cmdinterface.domain.clause;

/**
 * Represents a term in a database query predicate.
 * A term is a comparison between a field and a constant value,
 * or between two fields.
 */
public class DBTerm {
    public static final int EQUALS = 0;
    public static final int GREATER_THAN = 1;
    public static final int LESS_THAN = 2;
    public static final int LIKE = 3;
    
    private String lhsField;       
    private String rhsField;       
    private DBConstant rhsConst;   
    private int operator;          
    
    /**
     * Creates a new term comparing a field to a constant value.
     * 
     * @param lhs the field name on the left side
     * @param op the comparison operator
     * @param rhs the constant value on the right side
     */
    public DBTerm(String lhs, int op, DBConstant rhs) {
        this.lhsField = lhs;
        this.operator = op;
        this.rhsConst = rhs;
    }
    
    /**
     * Creates a new term comparing two fields.
     * 
     * @param lhs the field name on the left side
     * @param op the comparison operator
     * @param rhs the field name on the right side
     */
    public DBTerm(String lhs, int op, String rhs) {
        this.lhsField = lhs;
        this.operator = op;
        this.rhsField = rhs;
    }
    
    /**
     * Checks if the term is satisfied by the current record in the scan.
     * 
     * @param s the record scan
     * @return true if the term is satisfied, false otherwise
     */
    public boolean isSatisfied(RORecordScan s) {
        if (!s.hasField(lhsField))
            return false;
        
        // If field-to-field comparison
        if (rhsField != null) {
            if (!s.hasField(rhsField))
                return false;
            
            Integer lhsInt = s.getInt(lhsField);
            Integer rhsInt = s.getInt(rhsField);
            
            if (lhsInt != null && rhsInt != null) {
                return compareInts(lhsInt, rhsInt);
            } else {
                String lhsStr = s.getString(lhsField);
                String rhsStr = s.getString(rhsField);
                return compareStrings(lhsStr, rhsStr);
            }
        }
        // If field-to-constant comparison
        else {
            Integer lhsInt = s.getInt(lhsField);
            if (lhsInt != null && rhsConst.asInt() != null) {
                return compareInts(lhsInt, rhsConst.asInt());
            } else {
                String lhsStr = s.getString(lhsField);
                return compareStrings(lhsStr, rhsConst.asString());
            }
        }
    }
    
    /**
     * If this term equates a field to a constant, return that constant.
     * Otherwise, return null.
     * 
     * @param fldname the field name to check
     * @return the constant value if the term equates the field to a constant, null otherwise
     */
    public DBConstant equatesWithConstant(String fldname) {
        if (operator == EQUALS && lhsField.equals(fldname) && rhsConst != null)
            return rhsConst;
        return null;
    }
    
    private boolean compareInts(int lhs, int rhs) {
        switch (operator) {
            case EQUALS: return lhs == rhs;
            case GREATER_THAN: return lhs > rhs;
            case LESS_THAN: return lhs < rhs;
            default: return false;
        }
    }
    
    private boolean compareStrings(String lhs, String rhs) {
        if (lhs == null || rhs == null)
            return false;
            
        switch (operator) {
            case EQUALS: return lhs.equals(rhs);
            case GREATER_THAN: return lhs.compareTo(rhs) > 0;
            case LESS_THAN: return lhs.compareTo(rhs) < 0;
            case LIKE: return lhs.toLowerCase().contains(rhs.toLowerCase());
            default: return false;
        }
    }
    
    /**
     * Returns the left side field name.
     * 
     * @return the left side field name
     */
    public String getLhsField() {
        return lhsField;
    }
    
    /**
     * Returns the right side field name if this term compares two fields.
     * 
     * @return the right side field name, or null if this term compares a field to a constant
     */
    public String getRhsField() {
        return rhsField;
    }
    
    /**
     * Returns the right side constant if this term compares a field to a constant.
     * 
     * @return the right side constant, or null if this term compares two fields
     */
    public DBConstant getRhsConstant() {
        return rhsConst;
    }
    
    /**
     * Returns the operator for this term.
     * 
     * @return the operator code (EQUALS, GREATER_THAN, LESS_THAN, or LIKE)
     */
    public int getOperator() {
        return operator;
    }
    
    @Override
    public String toString() {
        String op;
        switch (operator) {
            case EQUALS: op = "="; break;
            case GREATER_THAN: op = ">"; break;
            case LESS_THAN: op = "<"; break;
            case LIKE: op = "LIKE"; break;
            default: op = "?"; break;
        }
        
        if (rhsField != null)
            return lhsField + " " + op + " " + rhsField;
        else
            return lhsField + " " + op + " " + rhsConst;
    }
}
