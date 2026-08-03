package ubic.gemma.core.ontology.lexical;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A single term from a flat lexical vocabulary: a URI, a primary label, and zero or more synonyms.
 * <p>
 * This is the unit consumed by {@link LexicalOntologyIndex} and produced by the parsers of
 * {@link AbstractLexicalOntologyService} subclasses (e.g. the Cellosaurus OBO parser). Unlike a full
 * ontology term it carries no hierarchy — flat vocabularies such as Cellosaurus have none.
 */
public record LexicalTerm( String uri, @Nullable String label, List<String> synonyms ) {

    public LexicalTerm {
        synonyms = synonyms == null ? List.of() : List.copyOf( synonyms );
    }

    public LexicalTerm( String uri, @Nullable String label ) {
        this( uri, label, List.of() );
    }
}
