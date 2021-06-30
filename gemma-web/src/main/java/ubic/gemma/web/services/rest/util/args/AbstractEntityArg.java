package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.ws.rs.core.Response;

/**
 * Class representing and API call argument that can represent various identifiers of different types.
 * E.g a taxon can be represented by Long number (ID) or multiple String properties (scientific/common name).
 *
 * @param <A>  the type that the argument is expected to mutate to.
 * @param <O>  the persistent object type.
 * @param <S>  the service for the object type.
 * @author tesarst
 */
public abstract class AbstractEntityArg<A, O extends Identifiable, S extends BaseService<O>> extends AbstractArg<A> {

    private static final String ERROR_FORMAT_ENTITY_NOT_FOUND = "The identifier was recognised to be '%1$s', but entity of type '%2$s' with '%1$s' equal to '%3$s' does not exist or is not accessible.";
    private static final String ERROR_MSG_ENTITY_NOT_FOUND = "Entity with the given identifier does not exist or is not accessible.";
    /**
     * The value in a Type that the argument represents.
     * Should only be used by the implementations of this class, which is why there is no setter for it,
     * as the whole reason behind this class is to delegate the functionality from the web service controllers.
     */
    protected A value;

    String nullCause = "No cause specified.";

    AbstractEntityArg( A value ) {
        super( value );
    }

    @Override
    public String toString() {
        if ( this.value == null )
            return "";
        return String.valueOf( this.value );
    }

    /**
     * @return true, if the value of this argument is null.
     */
    public boolean isNull() {
        return this.value == null;
    }

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return an object whose identifier matches the value of this mutable argument.
     */
    public abstract O getPersistentObject( S service );

    /**
     * @return the name of the property on the Identifiable object that this object represents.
     */
    public abstract String getPropertyName( S service );

    /**
     * Checks whether the given object is null, and throws an appropriate exception if necessary.
     *
     * @param response the object that should be checked for being null.
     * @return the same object as given.
     * @throws GemmaApiException if the given response is null.
     */
    protected O check( O response ) {
        if ( response == null ) {
            throwNotFound();
        }
        return response;
    }

    /**
     * Throws a GemmaApiException informing that the object this argument represents was not found.
     */
    void throwNotFound() {
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                ERROR_MSG_ENTITY_NOT_FOUND );
        WellComposedErrorBody.addExceptionFields( errorBody, new EntityNotFoundException( this.nullCause ) );
        throw new GemmaApiException( errorBody );
    }

    /**
     * Composes a null cause from the given values.
     *
     * @param identifierName the name of the identifier that the MutableArg refers to.
     * @param entityName     the name of the entity that this MutableArg represents.
     */
    void setNullCause( String identifierName, String entityName ) {
        this.nullCause = String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, identifierName, entityName, this.value );
    }

}
