package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filters;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;

/**
 * Array of identifiers of an {@link Identifiable} entity
 *
 * @param <A> the type of the value used to retrieve the entity, which is typically a {@link String}
 * @param <O> the type of the resulting entity
 * @param <S> the type of the filtering service providing the entity
 */
public interface EntityArrayArg<A, O extends Identifiable, S extends FilteringService<O>> extends Arg<List<A>> {

    /**
     * Retrieves the persistent objects for all the identifiers in this array arg.
     * Note that if any of the values in the array do not map to an object (i.e. an object with such identifier does not
     * exist),
     * a 404 error will be thrown.
     *
     * @param service the service that will be used to retrieve the persistent objects.
     * @return a collection of persistent objects matching the identifiers on this array arg.
     */
    List<O> getEntities( S service ) throws NotFoundException, BadRequestException;

    /**
     * Obtain a {@link Filters} for all the entities represented by this argument.
     * <p>
     * By applying this to a query, only the entities defined in this argument will be retrieved.
     * @throws BadRequestException if the filter represented by this is invalid (i.e. a property is not found in the
     *                             entity)
     */
    Filters getFilters( S service ) throws BadRequestException;
}
