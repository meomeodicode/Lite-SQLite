package lite.sqlite.server.model.domain.clause;

import lite.sqlite.server.scan.RORecordScan;

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
    
        public boolean isSatisfied(RORecordScan s) {
        if (!s.hasField(lhsField)) {
            return false;
        }

        if (rhsField != null) {
            return false;
        }
        

        Object constValue = rhsConst.asJavaVal();
        

        boolean isIntegerComparison = (constValue instanceof Integer);
        
        try {
            // Perform type-specific comparison
            if (isIntegerComparison) {
                // Get the field as an integer
                Integer fieldValue = s.getInt(lhsField);
                if (fieldValue == null) {
                    System.err.println("DEBUG: Field '" + lhsField + "' is not an integer or is NULL.");
                    return false;
                }
                
                // Compare integers
                Integer constInt = (Integer)constValue;
                switch (operator) {
                    case 0: return fieldValue.equals(constInt);      // =
                    case 1: return fieldValue.compareTo(constInt) > 0;  // >
                    case 2: return fieldValue.compareTo(constInt) < 0;  // <
                    // Add other operators as needed
                    default: return false;
                }
            } else {
                // Assume string comparison
                String fieldValue = s.getString(lhsField);
                if (fieldValue == null) {
                    System.err.println("DEBUG: Field '" + lhsField + "' is NULL or cannot be retrieved as a string.");
                    return false;
                }
                
                // Convert constant to string for comparison
                String constStr = constValue.toString();
                if (constValue instanceof String) {
                    // If it's already a string, use it directly (don't add extra quotes from toString)
                    constStr = (String)constValue;
                }
                
                switch (operator) {
                    case 0: return fieldValue.equals(constStr);      // =
                    case 1: return fieldValue.compareTo(constStr) > 0;  // >
                    case 2: return fieldValue.compareTo(constStr) < 0;  // <
                    // Add other operators as needed
                    default: return false;
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Error in comparison: " + e.getMessage());
            e.printStackTrace();
            return false;
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

    public String getLhsField() {
        return lhsField;
    }

    public String getRhsField() {
        return rhsField;
    }

    public DBConstant getRhsConstant() {
        return rhsConst;
    }
    
    public int getOperator() {
        return operator;
    }
    
    @Override
    public String toString() {
        String opStr;
        switch (operator) {
            case 0: opStr = "="; break;
            case 1: opStr = ">"; break;
            case 2: opStr = "<"; break;
            default: opStr = "op" + operator; break;
        }
        
        String rightSide;
        if (rhsField != null) {
            rightSide = rhsField;
        } else {
            rightSide = (rhsConst != null) ? rhsConst.toString() : "null";
        }
        
        return lhsField + " " + opStr + " " + rightSide;
    }
}
