package ubic.gemma.core.ontology.lexical;

import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyIndividual;
import ubic.gemma.core.ontology.model.OntologyRestriction;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.simple.OntologyTermSimple;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A term from a flat lexical vocabulary (Cellosaurus, MGI strains, ...).
 * <p>
 * {@link OntologyTermSimple} throws {@link UnsupportedOperationException} from its hierarchy/annotation
 * accessors. For a flat vocabulary those relations legitimately don't exist, so this subclass returns
 * empty collections instead — defensive against any consumer that walks a returned term (the search
 * fan-out expands via the service-level {@code getParents}/{@code getChildren}, but a returned term can
 * still be inspected directly).
 * <p>
 * The predicate-targeted overloads {@code getAnnotations(uri)} / {@code getAnnotation(uri)} matter as much
 * as the collection-wide ones: those are what the REST layer actually calls. {@code OntologyService
 * .getDefinition} probes {@code IAO_0000115} and {@code /annotations/search}'s match attribution probes the
 * six OBO/IAO synonym predicates, all through the single-URI overload. Leaving those inherited made every
 * Cellosaurus / MGI hit throw on enrichment.
 */
public class LexicalOntologyTerm extends OntologyTermSimple {

    public LexicalOntologyTerm( String uri, @Nullable String label ) {
        super( uri, label );
    }

    @Override
    public Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return Collections.emptySet();
    }

    @Override
    public Collection<OntologyTerm> getChildren( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return Collections.emptySet();
    }

    @Override
    public Collection<OntologyRestriction> getRestrictions() {
        return Collections.emptySet();
    }

    @Override
    public Collection<OntologyIndividual> getIndividuals( boolean direct ) {
        return Collections.emptySet();
    }

    @Override
    public Collection<AnnotationProperty> getAnnotations() {
        return Collections.emptySet();
    }

    @Override
    public Collection<AnnotationProperty> getAnnotations( String propertyUri ) {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public AnnotationProperty getAnnotation( String propertyUri ) {
        return null;
    }

    @Override
    public Collection<String> getAlternativeIds() {
        return Collections.emptySet();
    }

    @Override
    public boolean isRoot() {
        // A flat vocabulary term has no parents, so it is trivially a root.
        return true;
    }
}
