package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Array of identifiers of an Identifiable entity
 */
public abstract class ArrayEntityArg<O extends Identifiable, VO extends IdentifiableValueObject<O>, S extends BaseVoEnabledService<O, VO>>
        extends ArrayStringArg {

    Class<?> argValueClass = null;
    String argValueName = null;
    private Class<? extends MutableArg> argClass;

    ArrayEntityArg( List<String> values, Class<? extends MutableArg> argClass ) {
        super( values );
        this.argClass = argClass;
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

    /**
     * Combines the given filters with the properties in this array to create a final filter to be used for VO retrieval.
     * Note that this does not check whether objects with identifiers in this array arg do actually exist. This merely creates
     * a set of filters that should be used to impose restrictions in the database query.
     * You can call this#getPersistentObjects which does try to retrieve the corresponding objects, and consequently
     * does yield a 404 error if an object for any of the identifiers in this array arg does not exist.
     *
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @param filters the filters list to add the new filter to. Can be null.
     * @return the same array list as given, with a new added element, or a new ArrayList, in case the given filters
     * was null.
     */
    public ArrayList<ObjectFilter[]> combineFilters( ArrayList<ObjectFilter[]> filters, S service ) {
        if ( filters == null ) {
            filters = new ArrayList<>();
        }
        ObjectFilter filter;
        try {
            filter = new ObjectFilter( this.getPropertyName( service ), this.getPropertyType( service ),
                    this.getValue(), ObjectFilter.in, this.getObjectDaoAlias() );
        } catch ( ParseException e ) {
            throw this.convertParseException( e );
        }
        filters.add( new ObjectFilter[] { filter } );
        return filters;
    }

    /**
     * Retrieves the persistent objects for all the identifiers in this array arg.
     * Note that if any of the values in the array do not map to an object (i.e. an object with such identifier does not exist),
     * a 404 error will be thrown.
     *
     * @param service the service that will be used to retrieve the persistent objects.
     * @return a collection of persistent objects matching the identifiers on this array arg.
     */
    public Collection<O> getPersistentObjects( S service ) {
        Collection<O> objects = new ArrayList<>( this.getValue().size() );
        for ( String s : this.getValue() ) {
            try {
                MutableArg<?, O, VO, S> arg;
                // noinspection unchecked // Could not avoid using reflection, because java does not allow abstract static methods.
                arg = ( MutableArg<?, O, VO, S> ) argClass.getMethod( "valueOf", String.class ).invoke( null, s );
                objects.add( arg.getPersistentObject( service ) );
            } catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e ) {
                e.printStackTrace();
            }
        }
        return objects;
    }

    /**
     * @return the DAO alias of the object class that the identifiers in this array represents.
     */
    protected abstract String getObjectDaoAlias();

    /**
     * The implementation should set the {@link #argValueClass} and {@link #argValueName} properties.
     *
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     */
    protected abstract void setPropertyNameAndType( S service );

    /**
     * Reads the given MutableArgs property name and checks whether it is null or empty.
     *
     * @param arg     the MutableArg to retrieve the property name from.
     * @param value   one of the values of the property that has been passed into this array arg.
     * @param service service that may be used to retrieve the property from the MutableArg.
     * @param <T>     type of the given MutableArg.
     * @return the name of the property that the values in this arrayArg refer to.
     */
    <T extends MutableArg<?, O, VO, S>> String checkPropertyNameString( T arg, String value, S service ) {
        String identifier = arg.getPropertyName( service );
        if ( Strings.isNullOrEmpty( identifier ) ) {
            throw new GemmaApiException( new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    "Identifier " + value + " not recognized." ) );
        }
        return identifier;
    }

    /**
     * Converts the given parse exception into a GemmaApiException with a well composed error body.
     *
     * @param e the exception to be converted.
     * @return a properly populated GemmaApiException describing the given exception.
     */
    GemmaApiException convertParseException( ParseException e ) {
        WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                FilterArg.ERROR_MSG_MALFORMED_REQUEST );
        WellComposedErrorBody.addExceptionFields( error, e );
        return new GemmaApiException( error );
    }

    /**
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @return the name of the property that the values in this array represent.
     */
    private String getPropertyName( S service ) {
        if ( this.argValueName == null ) {
            this.setPropertyNameAndType( service );
        }
        return this.argValueName;
    }

    /**
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @return the type of the property that the values in this array represent.
     */
    private Class<?> getPropertyType( S service ) {
        if ( argValueClass == null ) {
            this.getPropertyName( service );
        }
        return argValueClass;
    }

}
