package ubic.gemma.web.services.rest.util.args;

import com.sun.istack.NotNull;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.ws.rs.core.Response;

/**
 * Class representing and API call argument that can represent various identifiers of different types.
 * E.g a taxon can be represented by Long number (ID) or multiple String properties (scientific/common name, abbreviation).
 *
 * @param <A>  the type that the argument is expected to mutate to.
 * @param <O>  the persistent object type.
 * @param <S>  the service for the object type.
 * @param <VO> the value object type.
 * @author tesarst
 */
public abstract class MutableArg<A, O extends Identifiable, S extends BaseVoEnabledService<O, VO>, VO extends IdentifiableValueObject<O>> {

    static final String ERROR_FORMAT_ENTITY_NOT_FOUND = "The identifier was recognised to be '%1$s', but entity of type '%2$s' with '%1$s' of given value does not exist or is not accessible.";
    private static final String ERROR_MSG_ENTITY_NOT_FOUND = "Entity with the given identifier does not exist or is not accessible.";
    /**
     * The value in a Type that the argument represents.
     * Should only be used by the implementations of this class, which is why there is no setter for it,
     * as the whole reason behind this class is to delegate the functionality from the web service controllers.
     */
    A value;

    String nullCause = "No cause specified.";

    /**
     * @return the reason that the object represented by the argument was null.
     */
    public final String getNullCause() {
        return nullCause;
    }

    /**
     * @return true, if the value of this argument is null.
     */
    public boolean isNull() {
        return this.value == null;
    }

    /**
     * Calls appropriate backend logic to retrieve the value object of the persistent object that this argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return the value object whose identifier matches the value of this mutable argument.
     */
    public final VO getValueObject( S service ) {
        O object = this.value == null ? null : this.getPersistentObject( service );
        return check(service.loadValueObject( object ));
    }

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return an object whose identifier matches the value of this mutable argument.
     */
    @NotNull
    public abstract O getPersistentObject( S service );

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
     * Checks whether the response Value Object is null, and throws an appropriate exception if necessary.
     * This double check on the VO is necessary, as some services will allow to return the persistent object, but not
     * the value object (interceptors have different rules for them).
     *
     * @param response the Value Object that should be checked for being null.
     * @return the same object as given.
     * @throws GemmaApiException if the given response is null.
     */
    protected VO check( VO response ) {
        if ( response == null ) {
            throwNotFound();
        }
        return response;
    }

    /**
     * Throws a GemmaApiException informing that the object this argument represents was not found.
     */
    private void throwNotFound() {
        WellComposedErrorBody errorBody = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                ERROR_MSG_ENTITY_NOT_FOUND );
        WellComposedErrorBody.addExceptionFields( errorBody, new EntityNotFoundException( getNullCause() ) );
        throw new GemmaApiException( errorBody );
    }

}
