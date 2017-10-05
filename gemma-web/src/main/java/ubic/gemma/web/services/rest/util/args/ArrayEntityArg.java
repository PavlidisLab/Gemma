package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Array of identifiers of an Identifiable entity
 */
public abstract class ArrayEntityArg extends ArrayStringArg {

    ArrayEntityArg( List<String> values ) {
        super( values );
    }

    ArrayEntityArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Splits string by the ',' comma character and trims the resulting strings.
     *
     * @param arg the string to process
     * @return trimmed strings exploded from the input.
     */
    static String[] splitString( String arg ) {
        String[] array = arg.split( "," );
        for ( int i = 0; i < array.length; i++ )
            array[i] = array[i].trim();
        return array;
    }

    static String checkPropertyNameString( MutableArg arg, String value, BaseVoEnabledService service ) {
        String identifier = arg.getPropertyName( service );
        if ( Strings.isNullOrEmpty( identifier ) ) {
            throw new GemmaApiException( new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    "Identifier " + value + " not recognized." ) );
        }
        return identifier;
    }

    /**
     * Combines the given filters with the properties in this array to create a final filter to be used for VO retrieval.
     *
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @param filters the filters list to add the new filter to. Can be null.
     * @return the same array list as given, with a new added element, or a new ArrayList, in case the given filters
     * was null.
     */
    public ArrayList<ObjectFilter[]> combineFilters( ArrayList<ObjectFilter[]> filters, BaseVoEnabledService service ) {
        if ( filters == null ) {
            filters = new ArrayList<>();
        }
        String name = this.getPropertyName( service );
        Class<?> type = String.class;
        if ( name.equals( "id" ) ) {
            type = Long.class;
        }
        ObjectFilter filter;
        try {
            filter = new ObjectFilter( name, type, this.getValue(), ObjectFilter.in, getObjectDaoAlias() );
        } catch ( ParseException e ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    FilterArg.ERROR_MSG_MALFORMED_REQUEST );
            WellComposedErrorBody.addExceptionFields( error, e );
            throw new GemmaApiException( error );
        }
        filters.add( new ObjectFilter[] { filter } );
        return filters;
    }

    /**
     * @return the name of the property that the values in this array represent.
     */
    protected abstract String getPropertyName(BaseVoEnabledService service);

    /**
     * @return the DAO alias of the object class that the identifiers in this array represents.
     */
    protected abstract String getObjectDaoAlias();

}
