package ubic.gemma.persistence.util;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a conjunction of disjunctions of {@link ObjectFilter}.
 * @author poirigui
 */
public class Filters implements Iterable<ObjectFilter[]> {

    /**
     * Builder for a disjunctive sub-clause.
     */
    public static class FiltersClauseBuilder {

        private final Filters filters;

        private final List<ObjectFilter> subClauses = new ArrayList<>();

        private FiltersClauseBuilder( Filters filters ) {
            this.filters = filters;
        }

        /**
         * Add a sub-clause.
         */
        @CheckReturnValue
        public FiltersClauseBuilder or( ObjectFilter objectFilter ) {
            subClauses.add( objectFilter );
            return this;
        }

        /**
         * Add a sub-clause explicitly.
         */
        @CheckReturnValue
        public FiltersClauseBuilder or( @Nullable String objectAlias, String propertyName, Class<?> propertyType, ObjectFilter.Operator operator, Object requiredValue ) {
            return or( new ObjectFilter( objectAlias, propertyName, propertyType, operator, requiredValue ) );
        }

        public Filters build() {
            return this.filters.and( subClauses.toArray( new ObjectFilter[0] ) );
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
     * Create a singleton {@link Filters} from a {@link ObjectFilter}.
     *
     * @param  filter the filter to create the {@link Filters} with
     * @return a {@link Filters} with the given object filter as only clause
     */
    public static Filters singleFilter( ObjectFilter filter ) {
        return new Filters().and( filter );
    }

    /**
     * Create a singleton {@link Filters} from an explicit clause.
     */
    public static Filters singleFilter( @Nullable String objectAlias, String propertyName, Class<?> propertyType, ObjectFilter.Operator operator, Object requiredValue ) {
        return singleFilter( new ObjectFilter( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    private final ArrayList<ObjectFilter[]> clauses;

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
     * Add a clause of one or more {@link ObjectFilter} sub-clauses to the conjunction.
     */
    public Filters and( ObjectFilter... filters ) {
        clauses.add( filters );
        return this;
    }

    /**
     * Add a clause of one explicit clause to the conjunction.
     */
    public Filters and( @Nullable String objectAlias, String propertyName, Class<?> propertyType, ObjectFilter.Operator operator, Object requiredValue ) {
        return and( new ObjectFilter( objectAlias, propertyName, propertyType, operator, requiredValue ) );
    }

    /**
     * Check if this contains an empty conjunction, or if all its clauses are empty.
     * <p>
     * An empty {@link Filters} has no effect whatsoever on the result of a query.
     */
    public boolean isEmpty() {
        // hint: allMatch returns false if the stream is empty
        return clauses.isEmpty();
    }

    /**
     * Obtain an iterator over the clauses contained in this conjunction.
     */
    @Override
    public Iterator<ObjectFilter[]> iterator() {
        return clauses.iterator();
    }

    @Override
    public String toString() {
        return clauses.stream()
                .filter( conjunction -> conjunction.length > 0 )
                .map( conjunction -> {
                    if ( conjunction.length == 1 ) {
                        return conjunction[0].toString();
                    } else {
                        return "(" + Arrays.stream( conjunction )
                                .map( ObjectFilter::toString )
                                .collect( Collectors.joining( " or " ) ) + ")";
                    }
                } )
                .collect( Collectors.joining( " and " ) );
    }
}
