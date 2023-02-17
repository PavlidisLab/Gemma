package ubic.gemma.persistence.util;

import lombok.EqualsAndHashCode;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a conjunction of disjunctions of {@link Filter}.
 * @author poirigui
 */
@EqualsAndHashCode(of = { "clauses" })
public class Filters implements Iterable<Filter[]> {

    /**
     * Builder for a disjunctive sub-clause.
     */
    public static class FiltersClauseBuilder {

        private final Filters filters;

        private final List<Filter> subClauses = new ArrayList<>();

        private FiltersClauseBuilder( Filters filters ) {
            this.filters = filters;
        }

        /**
         * Add a sub-clause.
         */
        @CheckReturnValue
        public FiltersClauseBuilder or( Filter filter ) {
            subClauses.add( filter );
            return this;
        }

        /**
         * Add a sub-clause explicitly.
         */
        @CheckReturnValue
        public <T> FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue ) {
            return or( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue ) );
        }

        /**
         * Add a sub-clause explicitly.
         */
        @CheckReturnValue
        public <T> FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues ) {
            return or( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues ) );
        }

        public Filters build() {
            return this.filters.and( subClauses.toArray( new Filter[0] ) );
        }

        /**
         * Shortcut for build and starting a new clause.
         */
        public FiltersClauseBuilder and() {
            return build().and();
        }
    }

    /**
     * Create an empty filter.
     */
    public static Filters empty() {
        return new Filters();
    }

    /**
     * Create a singleton {@link Filters} from a {@link Filter}.
     *
     * @param subClauses an array of sub-clause to create the {@link Filters} with
     * @return a {@link Filters} with the given filter as only clause
     */
    public static Filters by( Filter... subClauses ) {
        return empty().and( subClauses );
    }

    /**
     * Create a singleton {@link Filters} from an explicit clause.
     */
    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue ) {
        return empty().and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValue ) {
        return empty().and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    /**
     * Copy constructor.
     */
    public static Filters by( Filters filters ) {
        return empty().and( filters );
    }

    private final ArrayList<Filter[]> clauses;

    private Filters() {
        this.clauses = new ArrayList<>();
    }

    /**
     * Start a new clause.
     */
    public FiltersClauseBuilder and() {
        return new FiltersClauseBuilder( this );
    }

    /**
     * Add a clause of one or more {@link Filter} sub-clauses to the conjunction.
     */
    public Filters and( Filter... filters ) {
        clauses.add( filters );
        return this;
    }

    /**
     * Add a clause of one explicit clause to the conjunction.
     */
    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    /**
     * Add a new clause of one explicit clause with a collection right hand side to to the conjunction.
     */
    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues ) );
    }

    /**
     * Add all the clauses of another filter to this.
     */
    public Filters and( Filters filters ) {
        clauses.addAll( filters.clauses );
        return this;
    }

    /**
     * Check if this contains an empty conjunction, or if all its clauses are empty.
     * <p>
     * An empty {@link Filters} has no effect whatsoever on the result of a query.
     */
    public boolean isEmpty() {
        // hint: allMatch returns true if the stream is empty
        return clauses.stream().allMatch( arr -> arr.length == 0 );
    }

    /**
     * Obtain an iterator over the clauses contained in this conjunction.
     */
    @Override
    public Iterator<Filter[]> iterator() {
        return clauses.iterator();
    }

    @Override
    public String toString() {
        return toString( false );
    }

    public String toOriginalString() {
        return toString( true );
    }

    private String toString( boolean withOriginalProperties ) {
        return clauses.stream()
                .filter( conjunction -> conjunction.length > 0 )
                .map( conjunction -> Arrays.stream( conjunction )
                        .map( Filter::toOriginalString )
                        .collect( Collectors.joining( " or " ) ) )
                .collect( Collectors.joining( " and " ) );
    }
}
