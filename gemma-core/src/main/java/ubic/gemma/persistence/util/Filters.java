package ubic.gemma.persistence.util;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * Represents a conjunction of disjunctions of {@link ObjectFilter}.
 * @author poirigui
 */
public class Filters implements Iterable<ObjectFilter[]> {

    private final ArrayList<ObjectFilter[]> internalFilters;

    public Filters() {
        internalFilters = new ArrayList<>();
    }

    /**
     * Create a singleton {@link Filters} from a {@link ObjectFilter}.
     *
     * @param  filter the filter to create the {@link Filters} with
     * @return a {@link Filters} with the given object filter as only clause
     */
    public static Filters singleFilter( ObjectFilter filter ) {
        Filters filters = new Filters();
        filters.add( filter );
        return filters;
    }

    /**
     * Add a disjunction of one or more {@link ObjectFilter} clauses.
     */
    public void add( @NonNull ObjectFilter... filters ) {
        internalFilters.add( filters );
    }

    /**
     * Check if this contains an empty conjunction, or if all its clauses are empty.
     *
     * An empty {@link Filters} has no effect whatsoever on the result of a query.
     */
    public boolean isEmpty() {
        // hint: allMatch returns false if the stream is empty
        return internalFilters.stream().allMatch( l -> l.length == 0 );
    }

    /**
     * Obtain an iterator over the clauses contained in this conjunction.
     */
    @Override
    public Iterator<ObjectFilter[]> iterator() {
        return internalFilters.iterator();
    }

    @Override
    public String toString() {
        return internalFilters.stream()
                .map( conjunction -> Arrays.stream( conjunction )
                        .map( ObjectFilter::toString )
                        .collect( Collectors.joining( " or " ) ) )
                .collect( Collectors.joining( " and " ) );
    }
}
