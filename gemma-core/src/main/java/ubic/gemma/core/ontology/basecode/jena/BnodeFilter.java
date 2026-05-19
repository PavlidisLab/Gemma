/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

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
