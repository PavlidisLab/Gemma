package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.rest.util.EntityNotFoundException;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

/**
 * @author tesarst
 */
public abstract class AbstractEntityArg<T, O extends Identifiable, S extends FilteringVoEnabledService<O, ?>> extends AbstractArg<T> implements EntityArg<T, O, S> {

    private static final String ERROR_FORMAT_ENTITY_NOT_FOUND = "The identifier was recognised to be '%1$s', but entity of type '%2$s' with '%1$s' equal to '%3$s' does not exist or is not accessible.";
    private static final String ERROR_MSG_ENTITY_NOT_FOUND = "Entity with the given identifier does not exist or is not accessible.";

    private final Class<O> entityClass;

    protected AbstractEntityArg( Class<O> entityClass, T value ) {
        super( value );
        this.entityClass = entityClass;
    }

    /**
     * @return the name of the property on the Identifiable object that this object represents.
     */
    protected abstract String getPropertyName( S service );

    /**
     * Convert a given value to string so that it can be passed to {@link FilteringVoEnabledService#getFilter(String, Filter.Operator, String)}
     */
    protected String getFilterRequiredValue() {
        return String.valueOf( getValue() );
    }

    /**
     * Obtain a {@link Filters} that restrict a query to the entity represented by this argument.
     */
    @Override
    public Filters getFilters( S service ) throws BadRequestException {
        try {
            return Filters.by( service.getFilter( this.getPropertyName( service ), Filter.Operator.eq, getFilterRequiredValue() ) );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }
    }

    /**
     * Checks whether the given object is null, and throws an appropriate exception if necessary.
     *
     * @param service service that will be used to provide more details in the error message
     * @param entity  the object that should be checked for being null.
     * @return the same object as given.
     * @throws NotFoundException if the given entity is null.
     */
    protected O checkEntity( S service, @Nullable O entity ) throws NotFoundException {
        if ( entity == null ) {
            EntityNotFoundException cause = new EntityNotFoundException( String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, getPropertyName( service ), entityClass.getName(), this.getValue() ) );
            throw new NotFoundException( ERROR_MSG_ENTITY_NOT_FOUND, cause );
        }
        return entity;
    }
}
