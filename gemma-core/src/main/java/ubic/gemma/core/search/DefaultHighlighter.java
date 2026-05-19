package ubic.gemma.core.search;

import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Default {@link Highlighter} implementation that returns the matched value verbatim under its
 * field name. Restored as part of HS-7 search restoration Step 5 (see SEARCH_RECCE.md).
 *
 * <p>This is the pragmatic Step-5 replacement for the pre-strip {@code DefaultHighlighter} which
 * leaned on Lucene 5's {@code org.apache.lucene.search.highlight.Highlighter} for span-aware
 * snippet generation. With Hibernate Search 7 the highlight projection lives in the engine DSL
 * ({@code f.highlight(field).asArray()}), but using it requires marking each field with
 * {@code highlightable = Highlightable.ANY} in the entity mapping. We have not done that pass yet
 * (it is paired with the reindex in Step 6), so the Step-5 path is "post-hoc highlighting": the
 * search source projects the projectable text fields out of the index, and routes each value
 * through {@link #highlight(String, String)}. The default behaviour is to return the value
 * verbatim, which is exactly what a no-op highlighter does &mdash; preserving the
 * "highlighter requested" intent without span tagging.</p>
 *
 * <p>The hook also implements {@link OntologyHighlighter} so the ontology source (when it lands)
 * can produce per-term snippets through the same interface.</p>
 *
 * @author paul
 * @author poirigui
 */
public class DefaultHighlighter implements Highlighter, OntologyHighlighter {

    @Override
    public Map<String, String> highlight( String value, String field ) {
        return Collections.singletonMap( field, value );
    }

    @Override
    public Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field ) {
        return Collections.singletonMap( field, termLabel );
    }
}
