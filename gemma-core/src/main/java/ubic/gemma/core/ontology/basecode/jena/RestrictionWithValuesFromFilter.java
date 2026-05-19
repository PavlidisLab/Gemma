/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Resource;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Match {@link Restriction} with values from any of the given resources.
 * <p>
 * Phase 3 Jena 4 port: was {@code extends Filter<Restriction>}; Jena 4 drops
 * {@code Filter} in favour of {@link Predicate}.
 */
class RestrictionWithValuesFromFilter implements Predicate<Restriction> {

    private final Set<? extends Resource> resource;

    public RestrictionWithValuesFromFilter( Set<? extends Resource> resource ) {
        this.resource = resource;
    }

    @Override
    public boolean test( Restriction o ) {
        return resource.contains( JenaUtils.getRestrictionValue( o ) );
    }
}
