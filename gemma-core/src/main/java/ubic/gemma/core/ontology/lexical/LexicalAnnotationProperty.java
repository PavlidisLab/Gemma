package ubic.gemma.core.ontology.lexical;

import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.simple.AbstractOntologyResourceSimple;

import javax.annotation.Nullable;

/**
 * A plain {@link AnnotationProperty} for flat lexical vocabularies.
 * <p>
 * The only other implementation is Jena-backed and package-private, so a term that is not built from an
 * {@code OntModel} has no way to expose an annotation. This is that way: it lets a
 * {@link LexicalOntologyTerm} answer {@code getAnnotation(IAO_0000115)} with the source's descriptive
 * comment, which is what makes {@code OntologyService.getDefinition} — and every caller already built on
 * it — work for Cellosaurus and MGI hits without special-casing them.
 */
public class LexicalAnnotationProperty extends AbstractOntologyResourceSimple implements AnnotationProperty {

    @Nullable
    private final String contents;

    public LexicalAnnotationProperty( String uri, @Nullable String contents ) {
        super( uri, localNameOf( uri ), localNameOf( uri ) );
        this.contents = contents;
    }

    private static String localNameOf( String uri ) {
        int i = Math.max( uri.lastIndexOf( '/' ), uri.lastIndexOf( '#' ) );
        return i >= 0 && i + 1 < uri.length() ? uri.substring( i + 1 ) : uri;
    }

    @Override
    public boolean isObsolete() {
        // A definition annotation is never independently obsoleted; obsolescence lives on the term.
        return false;
    }

    @Nullable
    @Override
    public String getComment() {
        // The contents ARE the comment for this kind of resource; there is no separate rdfs:comment.
        return contents;
    }

    @Override
    public String getProperty() {
        return getLocalName();
    }

    @Nullable
    @Override
    public String getContents() {
        return contents;
    }
}
