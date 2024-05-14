package ubic.gemma.rest.util;

import org.springframework.validation.Errors;

import javax.ws.rs.core.Response;

/**
 * Entrypoint for custom AssertJ assertions.
 * @author poirigui
 */
public class Assertions {

    public static ResponseAssert assertThat( Response response ) {
        return new ResponseAssert( response );
    }

    public static ErrorsAssert assertThat( Errors errors ) {
        return new ErrorsAssert( errors );
    }
}
