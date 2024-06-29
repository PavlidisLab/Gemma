package ubic.gemma.rest.util.args;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.security.access.ConfigAttribute;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Bridges {@link Arg} operating on entities with their corresponding {@link FilteringService}.
 */
public interface EntityArgService<T extends Identifiable, S extends FilteringService<T>> {

    /**
     * @see FilteringVoEnabledService#getElementClass()
     */
    Class<? extends T> getElementClass();

    /**
     * @see FilteringVoEnabledService#getFilterableProperties()
     */
    Set<String> getFilterableProperties();

    /**
     * @see FilteringVoEnabledService#getFilterablePropertyType(String)
     */
    Class<?> getFilterablePropertyType( String p );

    /**
     * @see FilteringVoEnabledService#getFilterablePropertyDescription(String)
     */
    String getFilterablePropertyDescription( String p );

    /**
     * @see FilteringVoEnabledService#getFilterablePropertyAllowedValues(String)
     */
    List<Object> getFilterablePropertyAllowedValues( String p );

    boolean getFilterablePropertyIsUsingSubquery( String p );

    /**
     * @see FilteringVoEnabledService#getFilterablePropertyConfigAttributes(String)
     */
    Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String roles );

    /**
     * @see FilteringVoEnabledService#getFilterablePropertyResolvableAllowedValuesLabels(String)
     */
    List<MessageSourceResolvable> getFilterablePropertyResolvableAllowedValuesLabels( String p ) throws BadRequestException;

    /**
     * Retrieve the entity represented by this argument.
     * @throws NotFoundException   if the entity does not exist
     * @throws BadRequestException if the argument is malformed
     */
    T getEntity( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException;

    /**
     * Retrieve the entities represented by this argument.
     * <p>
     * Note that this will never return an empty array.
     * <p>
     * This is intended for cases where an argument could match more than one entity.
     * @throws NotFoundException   if no entity matching the argument exist
     * @throws BadRequestException if the argument is malformed
     */
    List<T> getEntities( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException;

    /**
     * Retrieve each entity represented by the array argument, raising a {@link NotFoundException} if any of them is
     * missing.
     * @throws NotFoundException   if any entity is missing
     * @throws BadRequestException if the argument is malformed
     */
    List<T> getEntities( AbstractEntityArrayArg<T, S> entitiesArg ) throws NotFoundException, BadRequestException;

    /**
     * Translate the provided entity argument into a {@link Filters}.
     * <p>
     * This will generate clause in the form of {@code property = value}.
     * @throws BadRequestException if the argument is malformed
     */
    <A> Filters getFilters( AbstractEntityArg<A, T, S> entityArg ) throws BadRequestException;

    /**
     * Translate the provided entity argument into a {@link Filters}.
     * <p>
     * This will generate clause in the form of {@code property in (values...)}.
     * @throws BadRequestException if the argument is malformed
     */
    Filters getFilters( AbstractEntityArrayArg<T, S> entitiesArg ) throws BadRequestException;

    /**
     * Obtain a {@link Filters} from a filter argument.
     * @throws BadRequestException if the argument is malformed
     */
    Filters getFilters( FilterArg<T> filterArg ) throws BadRequestException;

    /**
     * Obtain a {@link Sort} from a sort argument.
     * @throws BadRequestException if the argument is malformed
     */
    Sort getSort( SortArg<T> sortArg ) throws BadRequestException;
}
