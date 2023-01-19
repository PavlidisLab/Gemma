package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

/**
 * Interface representing and API call argument that can represent various identifiers of different types. E.g a taxon
 * can be represented by Long number (ID) or multiple String properties (scientific/common name).
 *
 * @param <T> the type that the argument is expected to mutate to as per {@link Arg}
 * @param <O> the persistent object type.
 * @param <S> the service for the object type.
 */
public interface EntityArg<T, O extends Identifiable, S extends FilteringService<O>> extends Arg<T> {

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return an object whose identifier matches the value of this mutable argument.
     * @throws NotFoundException   if the service cannot provide the entity
     * @throws BadRequestException if the service lacks the ability of providing the entity, which is typically due to
     *                             an incomplete request
     */
    @Nonnull
    O getEntity( S service ) throws NotFoundException, BadRequestException;

    /**
     * Obtain filters suitable for restricting results of a query to the entity represented by this argument.
     *
     * @throws BadRequestException if the filter represented by this is invalid (i.e. a property is not found in the
     *                             entity)
     * @see FilteringService#loadPreFilter(Filters, Sort, int, int)
     * @see FilteringService#loadPreFilter(Filters, Sort)
     */
    Filters getFilters( S service ) throws BadRequestException;
}
