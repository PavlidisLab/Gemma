package ubic.gemma.core.ontology.jena;

import java.util.function.Predicate;

/**
 * Iterator filter based on a {@link Predicate}.
 * <p>
 * Phase 3 Jena 4 port: was {@code extends Filter<T>}; Jena 4 drops {@code Filter}
 * in favour of {@link Predicate}, which {@code ExtendedIterator.filterKeep/Drop}
 * accept directly. Kept as a thin wrapper so call sites continue to read.
 *
 * @param <T>
 */
class PredicateFilter<T> implements Predicate<T> {
    private final Predicate<T> predicate;

    public PredicateFilter( Predicate<T> predicate ) {
        this.predicate = predicate;
    }

    @Override
    public boolean test( T o ) {
        return predicate.test( o );
    }
}
