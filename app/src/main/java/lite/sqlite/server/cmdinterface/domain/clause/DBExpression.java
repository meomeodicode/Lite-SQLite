package lite.sqlite.server.cmdinterface.domain.clause;

/**
 * Represents an expression in a database query.
 * An expression can be a field reference, a constant, or a computed value.
 */
public class DBExpression {
    public static final int FIELD = 0;    // Expression references a field
    public static final int CONSTANT = 1; // Expression is a constant
    public static final int OPERATION = 2; // Expression is a computed value
    
    public static final int ADD = 1;
    public static final int SUBTRACT = 2;
    public static final int MULTIPLY = 3;
    public static final int DIVIDE = 4;
    public static final int COUNT = 5;
    public static final int SUM = 6;
    public static final int AVG = 7;
    public static final int MIN = 8;
    public static final int MAX = 9;
    
    private int type;
    private String fieldName;    // For FIELD type
    private DBConstant constant; // For CONSTANT type
    private int operation;       // For OPERATION type
    private DBExpression lhs;    // Left operand for binary operations
    private DBExpression rhs;    // Right operand for binary operations
    private DBExpression operand; // Operand for unary operations (aggregates)
    private String alias;        // Optional column alias
    
    public static DBExpression field(String fieldName) {
        DBExpression expr = new DBExpression();
        expr.type = FIELD;
        expr.fieldName = fieldName;
        return expr;
    }
    

    public static DBExpression constant(DBConstant constant) {
        DBExpression expr = new DBExpression();
        expr.type = CONSTANT;
        expr.constant = constant;
        return expr;
    }
    
    /**
     * Creates an integer constant expression.
     * 
     * @param value the integer value
     */
    public static DBExpression constant(int value) {
        return constant(new DBConstant(value));
    }
    
    /**
     * Creates a string constant expression.
     * 
     * @param value the string value
     */
    public static DBExpression constant(String value) {
        return constant(new DBConstant(value));
    }
    
    /**
     * Creates a binary operation expression.
     * 
     * @param lhs the left operand
     * @param operation the operation type
     * @param rhs the right operand
     */
    public static DBExpression operation(DBExpression lhs, int operation, DBExpression rhs) {
        DBExpression expr = new DBExpression();
        expr.type = OPERATION;
        expr.operation = operation;
        expr.lhs = lhs;
        expr.rhs = rhs;
        return expr;
    }
    
    /**
     * Creates an aggregate operation expression.
     * 
     * @param operation the aggregate operation type
     * @param operand the operand
     */
    public static DBExpression aggregate(int operation, DBExpression operand) {
        DBExpression expr = new DBExpression();
        expr.type = OPERATION;
        expr.operation = operation;
        expr.operand = operand;
        return expr;
    }
    
    /**
     * Evaluates the expression using the current record in the scan.
     * 
     * @param s the record scan
     * @return the result of the expression as a DBConstant
     */
    public DBConstant evaluate(RORecordScan s) {
        if (type == FIELD) {
            Integer intVal = s.getInt(fieldName);
            if (intVal != null) {
                return new DBConstant(intVal);
            } else {
                String strVal = s.getString(fieldName);
                return new DBConstant(strVal);
            }
        } else if (type == CONSTANT) {
            return constant;
        } else if (type == OPERATION) {
            // Binary operations
            if (lhs != null && rhs != null) {
                DBConstant lhsVal = lhs.evaluate(s);
                DBConstant rhsVal = rhs.evaluate(s);
                if (lhsVal.asInt() != null && rhsVal.asInt() != null) {
                    int result;
                    switch (operation) {
                        case ADD: result = lhsVal.asInt() + rhsVal.asInt(); break;
                        case SUBTRACT: result = lhsVal.asInt() - rhsVal.asInt(); break;
                        case MULTIPLY: result = lhsVal.asInt() * rhsVal.asInt(); break;
                        case DIVIDE: 
                            if (rhsVal.asInt() == 0) {
                                // Division by zero - return special value
                                return new DBConstant("ERROR: Division by zero");
                            }
                            result = lhsVal.asInt() / rhsVal.asInt(); 
                            break;
                        default: 
                            return new DBConstant("ERROR: Unsupported operation");
                    }
                    return new DBConstant(result);
                } else {
                    // If any operand is a string, convert both to strings for display
                    String result = "ERROR: Cannot perform operation on these types";
                    if (operation == ADD) {
                        // String concatenation for ADD operation
                        String lhsStr = (lhsVal.asString() != null) ? lhsVal.asString() : String.valueOf(lhsVal.asInt());
                        String rhsStr = (rhsVal.asString() != null) ? rhsVal.asString() : String.valueOf(rhsVal.asInt());
                        result = lhsStr + rhsStr;
                    }
                    return new DBConstant(result);
                }
            }
            // Unary/aggregate operations
            else if (operand != null) {
                // Note: Real implementations of aggregates would need to process multiple records
                // This is a simplified version that works on a single record
                DBConstant val = operand.evaluate(s);
                switch (operation) {
                    case COUNT: return new DBConstant(1); // Always count as 1 for a single record
                    case SUM: return val; // For a single record, sum is just the value
                    case AVG: return val; // For a single record, avg is just the value
                    case MIN: return val; // For a single record, min is just the value
                    case MAX: return val; // For a single record, max is just the value
                    default: return new DBConstant("ERROR: Unsupported aggregate");
                }
            } else {
                return new DBConstant("ERROR: Invalid expression");
            }
        }
        return new DBConstant("ERROR: Invalid expression type");
    }
    
    /**
     * Sets an alias for this expression.
     * 
     * @param alias the alias
     * @return this expression, for method chaining
     */
    public DBExpression as(String alias) {
        this.alias = alias;
        return this;
    }
    
    /**
     * Returns the alias for this expression, or null if none is set.
     * 
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }
    
    /**
     * Returns the type of this expression.
     * 
     * @return the expression type
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the field name if this is a field reference expression.
     * 
     * @return the field name, or null if this is not a field reference
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Returns the constant value if this is a constant expression.
     * 
     * @return the constant value, or null if this is not a constant
     */
    public DBConstant getConstant() {
        return constant;
    }
    
    /**
     * Returns the operation type if this is an operation expression.
     * 
     * @return the operation type
     */
    public int getOperation() {
        return operation;
    }
    
    @Override
    public String toString() {
        if (type == FIELD) {
            return fieldName + (alias != null ? " AS " + alias : "");
        } else if (type == CONSTANT) {
            String constStr;
            if (constant.asInt() != null) {
                constStr = constant.asInt().toString();
            } else {
                constStr = "'" + constant.asString() + "'";
            }
            return constStr + (alias != null ? " AS " + alias : "");
        } else if (type == OPERATION) {
            if (lhs != null && rhs != null) {
                String opStr;
                switch (operation) {
                    case ADD: opStr = "+"; break;
                    case SUBTRACT: opStr = "-"; break;
                    case MULTIPLY: opStr = "*"; break;
                    case DIVIDE: opStr = "/"; break;
                    default: opStr = "?"; break;
                }
                return "(" + lhs.toString() + " " + opStr + " " + rhs.toString() + ")" + 
                       (alias != null ? " AS " + alias : "");
            } else if (operand != null) {
                String opStr;
                switch (operation) {
                    case COUNT: opStr = "COUNT"; break;
                    case SUM: opStr = "SUM"; break;
                    case AVG: opStr = "AVG"; break;
                    case MIN: opStr = "MIN"; break;
                    case MAX: opStr = "MAX"; break;
                    default: opStr = "?"; break;
                }
                return opStr + "(" + operand.toString() + ")" + 
                       (alias != null ? " AS " + alias : "");
            }
        }
        return "INVALID_EXPRESSION";
    }
}