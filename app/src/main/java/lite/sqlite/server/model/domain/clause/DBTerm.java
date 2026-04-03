package lite.sqlite.server.model.domain.clause;

import lite.sqlite.server.scan.RORecordScan;

/**
 * Represents a term in a database query predicate.
 * A term is a comparison between a field and a constant value,
 * or between two fields.
 */
public class DBTerm {
    private String lhsField;       
    private String rhsField;       
    private DBConstant rhsConst;   
    private ComparisonOperator operator;          
    
    /**
     * Creates a new term comparing a field to a constant value.
     * 
     * @param lhs the field name on the left side
     * @param op the comparison operator
     * @param rhs the constant value on the right side
     */
    public DBTerm(String lhs, ComparisonOperator op, DBConstant rhs) {
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
    public DBTerm(String lhs, ComparisonOperator op, String rhs) {
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
                    return false;
                }
                
                // Compare integers
                Integer constInt = (Integer)constValue;
                switch (operator) {
                    case EQUALS:
                        return fieldValue.equals(constInt);
                    case GREATER_THAN:
                        return fieldValue.compareTo(constInt) > 0;
                    case LESS_THAN:
                        return fieldValue.compareTo(constInt) < 0;
                    default: return false;
                }
            } else {
                // Assume string comparison
                String fieldValue = s.getString(lhsField);
                if (fieldValue == null) {
                    return false;
                }
                
                // Convert constant to string for comparison
                String constStr = constValue.toString();
                if (constValue instanceof String) {
                    // If it's already a string, use it directly (don't add extra quotes from toString)
                    constStr = (String)constValue;
                }
                
                switch (operator) {
                    case EQUALS:
                        return fieldValue.equals(constStr);
                    case GREATER_THAN:
                        return fieldValue.compareTo(constStr) > 0;
                    case LESS_THAN:
                        return fieldValue.compareTo(constStr) < 0;
                    case LIKE:
                        return fieldValue.toLowerCase().contains(constStr.toLowerCase());
                    default: return false;
                }
            }
        } catch (Exception e) {
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
        if (operator == ComparisonOperator.EQUALS && lhsField.equals(fldname) && rhsConst != null)
            return rhsConst;
        return null;
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
    
    public ComparisonOperator getOperator() {
        return operator;
    }
    
    @Override
    public String toString() {
        String opStr;
        switch (operator) {
            case EQUALS:
                opStr = "=";
                break;
            case GREATER_THAN:
                opStr = ">";
                break;
            case LESS_THAN:
                opStr = "<";
                break;
            case LIKE:
                opStr = "LIKE";
                break;
            default:
                opStr = "op" + operator;
                break;
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
