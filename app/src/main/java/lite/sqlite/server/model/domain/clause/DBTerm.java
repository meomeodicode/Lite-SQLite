package lite.sqlite.server.model.domain.clause;

import lite.sqlite.server.scan.RORecordScan;

/**
 * Represents a term in a database query predicate.
 * A term is a comparison between a field and a constant value,
 * or between two fields.
 * 
 * Currently only support for constant rhs
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

        if (rhsField != null || rhsConst == null || lhsField == null || !s.hasField(lhsField)) {
            return false; 
        }
        
        Object constValue = rhsConst.asJavaVal();
        
        if (constValue == null) {
            return false;
        }

        Object fieldValue = extractFieldValue(s, constValue);
        if (fieldValue == null) {
            return false;
        }

        return evaluateComparison(fieldValue, constValue);
    }

    private Object extractFieldValue(RORecordScan s, Object constValue) {
        if (constValue instanceof Integer) {
            return s.getInt(lhsField);
        }
        return s.getString(lhsField);
    }

    private boolean evaluateComparison(Object fieldValue, Object constValue) {
        if (fieldValue instanceof Integer && constValue instanceof Integer) {
            return compareComparable((Integer) fieldValue, (Integer) constValue);
        }

        String left = fieldValue.toString();
        String right = constValue.toString();
        if (operator == ComparisonOperator.LIKE) {
            return left.toLowerCase().contains(right.toLowerCase());
        }
        return compareComparable(left, right);
    }

    private <T extends Comparable<T>> boolean compareComparable(T left, T right) {
        switch (operator) {
            case EQUALS:
                return left.compareTo(right) == 0;
            case GREATER_THAN:
                return left.compareTo(right) > 0;
            case LESS_THAN:
                return left.compareTo(right) < 0;
            default:
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
