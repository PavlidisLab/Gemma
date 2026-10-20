package ubic.gemma.core.ontology.jena;

import org.apache.jena.rdf.model.Resource;

import java.util.function.Predicate;

/**
 * Detect bnodes, which are resources with null URIs.
 * <p>
 * Phase 3 Jena 4 port: was {@code extends Filter<T>}; Jena 4 drops {@code Filter}
 * in favour of {@link Predicate}, which {@code ExtendedIterator.filterKeep/Drop}
 * accept directly.
 *
 * @param <T>
 */
class BnodeFilter<T extends Resource> implements Predicate<T> {

    @Override
    public boolean test( T o ) {
        return o.getURI() == null;
    }
}
