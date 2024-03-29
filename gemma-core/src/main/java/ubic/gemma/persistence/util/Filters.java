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
public class Filters implements Iterable<List<Filter>> {

    /**
     * Builder for a disjunctive sub-clause.
     */
    public class FiltersClauseBuilder {

        private final List<Filter> subClauses;
        private boolean built = false;

        private FiltersClauseBuilder() {
            subClauses = new ArrayList<>();
        }

        /**
         * Add a sub-clause.
         */
        @CheckReturnValue
        public FiltersClauseBuilder or( Filter filter ) {
            if ( built ) {
                throw new IllegalStateException( "This builder has already been built." );
            }
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

        @CheckReturnValue
        public <T> FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue, String originalProperty ) {
            return or( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty ) );
        }

        /**
         * Add a sub-clause explicitly.
         */
        @CheckReturnValue
        public <T> FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues ) {
            return or( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues ) );
        }

        @CheckReturnValue
        public <T> FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues, String originalProperty ) {
            return or( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues, originalProperty ) );
        }

        public Filters build() {
            Filters.this.clauses.add( Collections.unmodifiableList( subClauses ) );
            built = true;
            return Filters.this;
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
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue, String originalProperty ) {
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValue ) {
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValue, String originalProperty ) {
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Subquery requiredValue ) {
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue );
    }

    public static <T> Filters by( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Subquery requiredValue, String originalProperty ) {
        return empty().and( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty );
    }

    /**
     * Copy constructor.
     */
    public static Filters by( Filters filters ) {
        return empty().and( filters );
    }

    private final List<List<Filter>> clauses;

    private Filters() {
        this.clauses = new ArrayList<>();
    }

    /**
     * Start a new clause.
     */
    public FiltersClauseBuilder and() {
        return new FiltersClauseBuilder();
    }

    /**
     * Add a clause of one or more {@link Filter} sub-clauses to the conjunction.
     */
    public Filters and( Filter... filters ) {
        clauses.add( Arrays.asList( filters ) );
        return this;
    }

    /**
     * Add a clause of one explicit clause to the conjunction.
     */
    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, @Nullable T requiredValue, String originalProperty ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValue, originalProperty ) );
    }

    /**
     * Add a new clause of one explicit clause with a collection right hand side to to the conjunction.
     */
    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues ) );
    }

    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Collection<T> requiredValues, String originalProperty ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues, originalProperty ) );
    }

    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Subquery requiredValues ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues ) );
    }

    public <T> Filters and( @Nullable String objectAlias, String propertyName, Class<T> propertyType, Filter.Operator operator, Subquery requiredValues, String originalProperty ) {
        return and( Filter.by( objectAlias, propertyName, propertyType, operator, requiredValues, originalProperty ) );
    }

    /**
     * Add all the clauses of another filter to this.
     */
    public Filters and( Filters filters ) {
        for ( List<Filter> clause : filters.clauses ) {
            clauses.add( Collections.unmodifiableList( new ArrayList<>( clause ) ) );
        }
        return this;
    }

    /**
     * Check if this contains an empty conjunction, or if all its clauses are empty.
     * <p>
     * An empty {@link Filters} has no effect whatsoever on the result of a query.
     */
    public boolean isEmpty() {
        // hint: allMatch returns true if the stream is empty
        return clauses.stream().allMatch( List::isEmpty );
    }

    /**
     * Obtain an iterator over the clauses contained in this conjunction.
     */
    @Override
    public Iterator<List<Filter>> iterator() {
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
                .filter( conjunction -> !conjunction.isEmpty() )
                .map( conjunction -> conjunction.stream()
                        .map( f -> withOriginalProperties ? f.toOriginalString() : f.toString() )
                        .collect( Collectors.joining( " or " ) ) )
                .collect( Collectors.joining( " and " ) );
    }
}
