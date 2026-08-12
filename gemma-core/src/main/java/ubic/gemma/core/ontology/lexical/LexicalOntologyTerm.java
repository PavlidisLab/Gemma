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

    /**
     * OBO/IAO definition predicate. Cellosaurus has no definition field, but its {@code comment:} plays the
     * same role, so exposing it here means {@code OntologyService.getDefinition} — and therefore the
     * existing /annotations/search enrichment — surfaces it with no special-casing for lexical sources.
     */
    private static final String DEFINITION_URI = "http://purl.obolibrary.org/obo/IAO_0000115";

    private final LexicalTermMetadata metadata;

    public LexicalOntologyTerm( String uri, @Nullable String label ) {
        this( uri, label, LexicalTermMetadata.EMPTY );
    }

    public LexicalOntologyTerm( String uri, @Nullable String label, @Nullable LexicalTermMetadata metadata ) {
        super( uri, label );
        this.metadata = metadata == null ? LexicalTermMetadata.EMPTY : metadata;
    }

    /**
     * Descriptive metadata from the source vocabulary — species, cell-line type, donor sex, strain type,
     * and any problematic-entry flag. Never null; {@link LexicalTermMetadata#EMPTY} when the source says
     * nothing.
     */
    public LexicalTermMetadata getMetadata() {
        return metadata;
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
        AnnotationProperty p = getAnnotation( propertyUri );
        return p != null ? Collections.singleton( p ) : Collections.emptySet();
    }

    @Nullable
    @Override
    public AnnotationProperty getAnnotation( String propertyUri ) {
        if ( DEFINITION_URI.equals( propertyUri ) && metadata.comment() != null ) {
            return new LexicalAnnotationProperty( DEFINITION_URI, metadata.comment() );
        }
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
