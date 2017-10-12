package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayPlatformArg extends ArrayEntityArg<ArrayDesign, ArrayDesignValueObject, ArrayDesignService> {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple, separated by (',') character. All identifiers must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Platform identifiers";

    private ArrayPlatformArg( List<String> values ) {
        super( values );
    }

    private ArrayPlatformArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayPlatform argument
     * @return an instance of ArrayPlatformArg representing an array of Platform identifiers from the input string,
     * or a malformed ArrayPlatformArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Platform identifiers.
     */
    @SuppressWarnings("unused")
    public static ArrayStringArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayPlatformArg( String.format( ERROR_MSG, s ),
                    new IllegalArgumentException( ERROR_MSG_DETAIL ) );
        }
        return new ArrayPlatformArg( Arrays.asList( splitString( s ) ) );
    }

    @Override
    protected String getObjectDaoAlias() {
        return ObjectFilter.DAO_AD_ALIAS;
    }

    @Override
    protected String getPropertyName( ArrayDesignService service ) {
        String value = this.getValue().get( 0 );
        PlatformArg arg = PlatformArg.valueOf( value );
        return checkPropertyNameString( arg, value, service );
    }

}
