package lite.sqlite.server.model.domain.clause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a predicate in a database query, composed of one or more terms.
 * Terms in a predicate are combined with logical AND (conjunction).
 */
public class DBPredicate {
    private List<DBTerm> terms = new ArrayList<DBTerm>();

    /**
     * Creates a new empty predicate.
     */
    public DBPredicate() {
    }

    /**
     * Creates a new predicate from a string expression.
     * This is a simplified constructor for basic predicates.
     */
    public DBPredicate(String expression) {
        // For now, store the expression as a simple string
        // You can extend this to parse the expression into proper DBTerms
        if (expression != null && !expression.trim().isEmpty()) {
            // Simple implementation - you can enhance this later
            this.add(new DBTerm("temp_field", DBTerm.EQUALS, new DBConstant(expression)));
        }
    }

    /**
     * Creates a new predicate with a single term.
     * 
     * @param t the term
     */
    public DBPredicate(DBTerm t) {
        terms.add(t);
    }

    /**
     * Conjoins this predicate with another predicate.
     * 
     * @param pred the predicate to conjoin with
     */
    public void conjoinWith(DBPredicate pred) {
        terms.addAll(pred.terms);
    }

    /**
     * Checks if the predicate is satisfied by the current record in the scan.
     * 
     * @param s the record scan
     * @return true if the predicate is satisfied, false otherwise
     */
    public boolean isSatisfied(RORecordScan s) {
        for (DBTerm t : terms)
            if (!t.isSatisfied(s)) return false;
        return true;
    }

    /**
     * If this predicate equates a field to a constant, return that constant.
     * Otherwise, return null.
     * 
     * @param fldname the field name to check
     * @return the constant value if the predicate equates the field to a constant, null otherwise
     */
    public DBConstant equatesWithConstant(String fldname) {
        for (DBTerm t : terms) {
            DBConstant c = t.equatesWithConstant(fldname);
            if (c != null)
                return c;
        }
        return null;
    }    
    public String toString() {
        Iterator<DBTerm> iter = terms.iterator();
        if (!iter.hasNext()) return "";
        String result = iter.next().toString();
        while (iter.hasNext()) result += " and " + iter.next().toString();
        return result;
    }
    
    /**
     * Returns the list of terms in this predicate.
     * 
     * @return the list of terms
     */
    public List<DBTerm> getTerms() {
        return terms;
    }
    
    /**
     * Adds a term to this predicate.
     * 
     * @param term the term to add
     */
    public void add(DBTerm term) {
        terms.add(term);
    }
    
    /**
     * Checks if this predicate is empty (has no terms).
     * 
     * @return true if the predicate is empty, false otherwise
     */
    public boolean isEmpty() {
        return terms.isEmpty();
    }
}
