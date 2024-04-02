package ubic.gemma.rest.util.args;

import org.apache.commons.lang.StringUtils;
import ubic.gemma.rest.util.MalformedArgException;

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
            throw new MalformedArgException( "" );
        }
        return new QueryArg( s );
    }
}
