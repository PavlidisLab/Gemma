package ubic.gemma.rest.util;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.internal.Objects;

/**
 * Assertions for JSON based on {@link JsonPath}.
 * @author poirigui
 */
public class JsonAssert extends AbstractAssert<JsonAssert, String> {

    public static InstanceOfAssertFactory<String, JsonAssert> json() {
        return new InstanceOfAssertFactory<>( String.class, JsonAssert::new );
    }

    private final Objects objects = Objects.instance();

    public JsonAssert( String json ) {
        super( json, JsonAssert.class );
    }

    /**
     * Ensure that a given path exists.
     */
    public JsonAssert hasPath( String jsonPath ) {
        try {
            JsonPath.compile( jsonPath ).read( actual );
        } catch ( PathNotFoundException e ) {
            failWithMessage( "No JSON objects were matched for path: " + jsonPath );
        }
        return myself;
    }

    /**
     * Ensure that a given path does not exist.
     */
    public JsonAssert doesNotHavePath( String jsonPath ) {
        try {
            JsonPath.compile( jsonPath ).read( actual );
            failWithMessage( "At least one JSON objects were matched by path: " + jsonPath );
        } catch ( PathNotFoundException e ) {
            // success
        }
        return myself;
    }

    /**
     * Ensure that a JSON path has the given value.
     */
    public <T> JsonAssert hasPathWithValue( String jsonPath, Object value ) {
        objects.assertEqual( info, JsonPath.compile( jsonPath ).read( actual ), value );
        return myself;
    }
}
