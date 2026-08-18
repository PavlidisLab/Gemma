package ubic.gemma.core.ontology.lexical;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A single term from a flat lexical vocabulary: a URI, a primary label, zero or more synonyms, and
 * whatever descriptive metadata the source carries.
 * <p>
 * This is the unit consumed by {@link LexicalOntologyIndex} and produced by the parsers of
 * {@link AbstractLexicalOntologyService} subclasses (e.g. the Cellosaurus OBO parser). Unlike a full
 * ontology term it carries no hierarchy — flat vocabularies such as Cellosaurus have none.
 * <p>
 * Only the label and synonyms are indexed; {@link #metadata()} is descriptive and rides along so a caller
 * can see what it resolved. See {@link LexicalTermMetadata}.
 */
public record LexicalTerm( String uri, @Nullable String label, List<String> synonyms,
                           LexicalTermMetadata metadata ) {

    public LexicalTerm {
        synonyms = synonyms == null ? List.of() : List.copyOf( synonyms );
        metadata = metadata == null ? LexicalTermMetadata.EMPTY : metadata;
    }

    public LexicalTerm( String uri, @Nullable String label, List<String> synonyms ) {
        this( uri, label, synonyms, LexicalTermMetadata.EMPTY );
    }

    public LexicalTerm( String uri, @Nullable String label ) {
        this( uri, label, List.of(), LexicalTermMetadata.EMPTY );
    }
}
