package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;

/**
 * Bridges {@link Arg} operating on entities with their corresponding {@link FilteringService}.
 * @param <T>
 */
public interface EntityArgService<T extends Identifiable, S extends FilteringService<T>> {

    @Nonnull
    T getEntity( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException;

    List<T> getEntities( AbstractEntityArrayArg<?, T, S> entitiesArg ) throws NotFoundException, BadRequestException;

    Filters getFilters( AbstractEntityArg<?, T, S> entityArg ) throws BadRequestException;

    Filters getFilters( AbstractEntityArrayArg<?, T, S> entitiesArg ) throws BadRequestException;

    Filters getFilters( FilterArg<T> filterArg ) throws BadRequestException;

    Sort getSort( SortArg<T> sortArg ) throws BadRequestException;
}
