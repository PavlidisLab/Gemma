package ubic.gemma.rest.util;

import javax.ws.rs.core.Response;

/**
 * Entrypoint for custom AssertJ assertions.
 * @author poirigui
 */
public class Assertions extends org.assertj.core.api.Assertions {

    public static ResponseAssert assertThat( Response response ) {
        return new ResponseAssert( response );
    }
}