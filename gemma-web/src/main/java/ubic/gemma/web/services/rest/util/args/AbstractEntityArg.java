package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

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

    AbstractEntityArg( A value ) {
        super( value );
    }

    /**
     * @return the name of the entity this argument represents.
     */
    protected abstract String getEntityName();

    /**
     * @return the name of the property on the Identifiable object that this object represents.
     */
    protected abstract String getPropertyName();

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @throws NotFoundException if the service cannot provide the entity
     * @return an object whose identifier matches the value of this mutable argument.
     */
    public abstract O getEntity( S service ) throws NotFoundException;

    /**
     * Checks whether the given object is null, and throws an appropriate exception if necessary.
     *
     * @param entity the object that should be checked for being null.
     * @return the same object as given.
     * @throws NotFoundException if the given entity is null.
     */
    protected O checkEntity( O entity ) throws NotFoundException {
        if ( entity == null ) {
            EntityNotFoundException cause = new EntityNotFoundException( String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, getPropertyName(), getEntityName(), this.getValue() ) );
            throw new NotFoundException( ERROR_MSG_ENTITY_NOT_FOUND, cause );
        }
        return entity;
    }

}
