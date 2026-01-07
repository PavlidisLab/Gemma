package ubic.gemma.model.common.search;

import lombok.Value;
import ubic.gemma.model.common.IdentifiableValueObject;

import javax.annotation.Nullable;
import java.util.stream.Collectors;

@Value
public class SearchResultValueObject<T extends IdentifiableValueObject<?>> {

    Class<?> resultClass;
    double score;
    String highlightedText;
    IdentifiableValueObject<?> resultObject;
    /**
     * A URL for the result.
     * <p>
     * If there is a single result, the frontend might decide to redirect immediately to this page.
     */
    @Nullable
    String resultObjectUrl;
    /**
     * Indicate if the result URL is external (i.e., outside of Gemma).
     */
    boolean resultObjectUrlExternal;

    public SearchResultValueObject( SearchResult<T> result ) {
        this( result, null, false );
    }

    public SearchResultValueObject( SearchResult<T> result, @Nullable String resultObjectUrl, boolean resultObjectUrlExternal ) {
        this.resultClass = result.getResultType();
        this.score = result.getScore();
        this.resultObjectUrlExternal = resultObjectUrlExternal;
        if ( result.getHighlights() != null ) {
            this.highlightedText = result.getHighlights().entrySet().stream()
                    .map( e -> String.format( "Tagged %s: %s", e.getKey(), e.getValue() ) )
                    .collect( Collectors.joining( "<br/>" ) );
        } else {
            this.highlightedText = null;
        }
        this.resultObject = result.getResultObject();
        this.resultObjectUrl = resultObjectUrl;
    }
}
