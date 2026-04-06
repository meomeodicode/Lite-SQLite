package lite.sqlite.server.model.domain.clause;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lite.sqlite.server.scan.RORecordScan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DBTerm Tests")
class DBTermTest {

    @Test
    @DisplayName("Integer equals comparison should pass")
    void testIntegerEquals() {
        DBTerm term = new DBTerm("age", ComparisonOperator.EQUALS, new DBConstant(20));
        RORecordScan scan = scan(Map.of("age", 20));
        assertTrue(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("String less-than comparison should pass")
    void testStringLessThan() {
        DBTerm term = new DBTerm("name", ComparisonOperator.LESS_THAN, new DBConstant("mike"));
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertTrue(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("LIKE should be case-insensitive")
    void testLikeCaseInsensitive() {
        DBTerm term = new DBTerm("name", ComparisonOperator.LIKE, new DBConstant("ALI"));
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertTrue(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Null constant object should return false")
    void testNullConstantObject() {
        DBTerm term = new DBTerm("name", ComparisonOperator.EQUALS, (DBConstant) null);
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertFalse(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Constant with null value should return false")
    void testConstantNullValue() {
        DBTerm term = new DBTerm("name", ComparisonOperator.EQUALS, new DBConstant(null));
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertFalse(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Unknown field should return false")
    void testUnknownField() {
        DBTerm term = new DBTerm("missing", ComparisonOperator.EQUALS, new DBConstant("x"));
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertFalse(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Field-to-field comparisons are currently unsupported")
    void testFieldToFieldUnsupported() {
        DBTerm term = new DBTerm("left", ComparisonOperator.EQUALS, "right");
        RORecordScan scan = scan(Map.of("left", "a"), Map.of("right", "a"));
        assertFalse(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Non-numeric field with integer constant should return false")
    void testIntegerComparisonTypeMismatch() {
        DBTerm term = new DBTerm("age", ComparisonOperator.EQUALS, new DBConstant(20));
        RORecordScan scan = scan(Map.of("age", "twenty"));
        assertFalse(term.isSatisfied(scan));
    }

    @Test
    @DisplayName("Unsupported operator should return false")
    void testUnsupportedOperatorReturnsFalse() {
        DBTerm term = new DBTerm("name", ComparisonOperator.CONTAINS_NULL, new DBConstant("a"));
        RORecordScan scan = scan(Map.of("name", "alice"));
        assertFalse(term.isSatisfied(scan));
    }

    private RORecordScan scan(Map<String, Object> stringValues) {
        return scan(stringValues, new HashMap<>());
    }

    private RORecordScan scan(Map<String, Object> stringValues, Map<String, Object> intValues) {
        return new FakeScan(stringValues, intValues);
    }

    private static class FakeScan implements RORecordScan {
        private final Map<String, Object> values;

        FakeScan(Map<String, Object> lhsValues, Map<String, Object> rhsValues) {
            this.values = new HashMap<>();
            this.values.putAll(lhsValues);
            this.values.putAll(rhsValues);
        }

        @Override
        public Integer getInt(String fldname) {
            Object value = values.get(fldname);
            if (value == null) {
                return null;
            }
            if (value instanceof Integer) {
                return (Integer) value;
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        public String getString(String fldname) {
            Object value = values.get(fldname);
            return value == null ? null : value.toString();
        }

        @Override
        public boolean hasField(String fldname) {
            return values.containsKey(fldname);
        }
    }
}
