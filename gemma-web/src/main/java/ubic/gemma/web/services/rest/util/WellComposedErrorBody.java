package ubic.gemma.web.services.rest.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tesarst on 17/05/17.
 * The object acting as a payload for the ResponseErrorObject.
 */
public class WellComposedErrorBody {
    public final String code;
    public final String message;
    public Map<String, String> errors = null;

    public WellComposedErrorBody( String code, String message ) {
        this.code = code;
        this.message = message;
    }

    public void addErrorsField( String key, String value ) {
        if ( this.errors == null ) {
            this.errors = new HashMap<>();
        }
        this.errors.put( key, value );
    }
};
