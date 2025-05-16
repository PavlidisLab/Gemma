package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.rest.util.MalformedArgException;

@Schema(type = "string", description = "Filter results matching the given full-text query.",
        externalDocs = @ExternalDocumentation(url = "https://lucene.apache.org/core/3_6_2/queryparsersyntax.html"))
public class QueryArg implements Arg<String> {

    private final String value;

    private QueryArg( String value ) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * @throws MalformedArgException if the query string is blank
     */
    public static QueryArg valueOf( String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "The query cannot be empty." );
        }
        return new QueryArg( s );
    }
}
