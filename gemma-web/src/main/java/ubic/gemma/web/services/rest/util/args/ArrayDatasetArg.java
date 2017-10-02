package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayDatasetArg extends ArrayEntityArg {
    private static final String ERROR_MSG_DETAIL = "Provide a string that contains at least one ID or short name, or multiple separated (',') character. All identifier must be same type, i.e. do not combine IDs and short names.";
    private static final String ERROR_MSG = ArrayArg.ERROR_MSG + " Dataset identifiers";

    private ArrayDatasetArg( List<String> values ) {
        super( values );
    }

    private ArrayDatasetArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request arrayDataset argument
     * @return an instance of ArrayDatasetArg representing an array of Dataset identifiers from the input string,
     * or a malformed ArrayDatasetArg that will throw an {@link GemmaApiException} when accessing its value, if the
     * input String can not be converted into an array of Dataset identifiers.
     */
    @SuppressWarnings("unused")
    public static ArrayStringArg valueOf( final String s ) {
        if ( Strings.isNullOrEmpty( s ) ) {
            return new ArrayDatasetArg( String.format( ERROR_MSG, s ),
                    new IllegalArgumentException( ERROR_MSG_DETAIL ) );
        }
        String[] array = s.split( "," );
        for (int i = 0; i < array.length; i++)
            array[i] = array[i].trim();
        return new ArrayDatasetArg( Arrays.asList( array ) );
    }

    @Override
    public ArrayList<ObjectFilter[]> combineFilters( ArrayList<ObjectFilter[]> filters, BaseVoEnabledService service ) {
        if ( filters == null ) {
            filters = new ArrayList<>();
        }
        String name = this.getPropertyName( service );
        Class<?> type = String.class;
        if(name.equals( "id" )){
            type = Long.class;
        }
        ObjectFilter filter;
        try {
            filter = new ObjectFilter( name, type, this.getValue(), ObjectFilter.in, ObjectFilter.DAO_EE_ALIAS );
        } catch ( ParseException e ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    FilterArg.ERROR_MSG_MALFORMED_REQUEST );
            WellComposedErrorBody.addExceptionFields( error, e );
            throw new GemmaApiException( error );
        }
        filters.add( new ObjectFilter[] { filter } );
        return filters;
    }

    private String getPropertyName( BaseVoEnabledService service ) {
        String value = this.getValue().get( 0 );
        DatasetArg arg = DatasetArg.valueOf( value );
        //noinspection unchecked // Make sure that the passed service is EEService
        String identifier = arg.getPropertyName( service );
        if ( Strings.isNullOrEmpty( identifier ) ) {
            throw new GemmaApiException( new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    "Identifier " + value + " not recognized." ) );
        }
        return identifier;
    }

}
