package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Property;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Filter that retain only the restrictions on any of the given properties.
 * <p>
 * Phase 3 Jena 4 port: was {@code extends Filter<Restriction>}; Jena 4 drops
 * {@code Filter} in favour of {@link Predicate}.
 */
class RestrictionWithOnPropertyFilter implements Predicate<Restriction> {
    private final Set<? extends Property> properties;

    public RestrictionWithOnPropertyFilter( Set<? extends Property> properties ) {
        this.properties = properties;
    }

    @Override
    public boolean test( Restriction o ) {
        return properties.contains( o.getOnProperty() );
    }
}
