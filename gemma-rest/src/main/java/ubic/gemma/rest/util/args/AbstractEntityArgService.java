package ubic.gemma.rest.util.args;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.security.access.ConfigAttribute;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractEntityArgService<T extends Identifiable, S extends FilteringService<T>> implements EntityArgService<T, S> {

    protected final S service;


    protected AbstractEntityArgService( S service ) {
        this.service = service;
    }

    @Override
    public Class<? extends T> getElementClass() {
        return service.getElementClass();
    }

    @Override
    public Set<String> getFilterableProperties() {
        return service.getFilterableProperties();
    }

    @Override
    public Class<?> getFilterablePropertyType( String p ) {
        return service.getFilterablePropertyType( p );
    }

    @Override
    public String getFilterablePropertyDescription( String p ) {
        return service.getFilterablePropertyDescription( p );
    }

    @Override
    public List<Object> getFilterablePropertyAllowedValues( String p ) {
        return service.getFilterablePropertyAllowedValues( p );
    }

    @Override
    public List<MessageSourceResolvable> getFilterablePropertyResolvableAvailableValuesLabels( String p ) {
        return service.getFilterablePropertyResolvableAvailableValuesLabels( p );
    }

    @Override
    public Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String p ) {
        return service.getFilterablePropertyConfigAttributes( p );
    }

    @Override
    @Nonnull
    public T getEntity( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException {
        return entityArg.getEntity( service );
    }

    @Override
    public List<T> getEntities( AbstractEntityArrayArg<?, T, S> entitiesArg ) throws NotFoundException, BadRequestException {
        return entitiesArg.getEntities( service );
    }

    @Override
    public Filters getFilters( AbstractEntityArg<?, T, S> entityArg ) throws BadRequestException {
        return entityArg.getFilters( service );
    }

    @Override
    public Filters getFilters( AbstractEntityArrayArg<?, T, S> entitiesArg ) throws BadRequestException {
        return entitiesArg.getFilters( service );
    }

    @Override
    public Filters getFilters( FilterArg<T> filterArg ) throws BadRequestException {
        return filterArg.getFilters( service );
    }

    @Override
    public Sort getSort( SortArg<T> sortArg ) throws BadRequestException {
        return sortArg.getSort( service );
    }
}
