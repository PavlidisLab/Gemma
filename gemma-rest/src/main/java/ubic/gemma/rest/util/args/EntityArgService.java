package ubic.gemma.rest.util.args;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.security.access.ConfigAttribute;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
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

    @Nonnull
    T getEntity( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException;

    List<T> getEntities( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException;

    List<T> getEntities( AbstractEntityArrayArg<T, S> entitiesArg ) throws NotFoundException, BadRequestException;

    <A> Filters getFilters( AbstractEntityArg<A, T, S> entityArg ) throws BadRequestException;

    Filters getFilters( AbstractEntityArrayArg<T, S> entitiesArg ) throws BadRequestException;

    Filters getFilters( FilterArg<T> filterArg ) throws BadRequestException;

    Sort getSort( SortArg<T> sortArg ) throws BadRequestException;
}
