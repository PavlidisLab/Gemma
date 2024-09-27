package ubic.gemma.persistence.util;

/**
 * Mode to use when filtering with a subquery.
 * <p>
 * By default, {@link #ANY} is used, resulting in a clause of the form {@code id in {subquery}}.
 * @see Subquery
 */
public enum SubqueryMode {
    /**
     * Produce a clause of the form {@code id in {subquery}}.
     */
    ANY,
    /**
     * Produce a clause of the form {@code id not in {not(subquery)}}.
     * <p>
     * In this case, the subquery's filter is negated with {@link Filter#not(Filter)}.
     */
    ALL,
    /**
     * Produce a clause of the form {@code id not in {subquery}}.
     */
    NONE
}
