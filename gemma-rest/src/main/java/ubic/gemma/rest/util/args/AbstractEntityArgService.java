package ubic.gemma.rest.util.args;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.security.access.ConfigAttribute;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.EntityNotFoundException;
import ubic.gemma.rest.util.MalformedArgException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEntityArgService<T extends Identifiable, S extends FilteringService<T>> implements EntityArgService<T, S> {

    static final String ERROR_FORMAT_ENTITY_NOT_FOUND = "The identifier was recognised to be '%1$s', but entity of type '%2$s' with '%1$s' equal to '%3$s' does not exist or is not accessible.";
    static final String ERROR_MSG_ENTITY_NOT_FOUND = "Entity with the given identifier does not exist or is not accessible.";

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
    public List<MessageSourceResolvable> getFilterablePropertyResolvableAllowedValuesLabels( String p ) {
        return service.getFilterablePropertyResolvableAllowedValuesLabels( p );
    }

    @Override
    public boolean isFilterablePropertyUsingSubquery( String p ) {
        return service.isFilterablePropertyUsingSubquery( p );
    }

    @Override
    public Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String p ) {
        return service.getFilterablePropertyConfigAttributes( p );
    }

    @Override
    public boolean isFilterablePropertyDeprecated( String p ) {
        return service.isFilterablePropertyDeprecated( p );
    }

    @Override
    @Nonnull
    public T getEntity( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException {
        return checkEntity( entityArg, entityArg.getEntity( service ) );
    }

    @Override
    public List<T> getEntities( AbstractEntityArg<?, T, S> entityArg ) throws NotFoundException, BadRequestException {
        List<T> result = entityArg.getEntities( service );
        if ( result.isEmpty() ) {
            // will raise a NotFoundException
            checkEntity( entityArg, null );
        }
        return result;
    }

    @Override
    public List<T> getEntities( AbstractEntityArrayArg<T, S> entitiesArg ) throws NotFoundException, BadRequestException {
        List<T> objects = new ArrayList<>( entitiesArg.getValue().size() );
        for ( String s : entitiesArg.getValue() ) {
            objects.add( getEntity( entityArgValueOf( entitiesArg.getEntityArgClass(), s ) ) );
        }
        return objects;
    }

    @Override
    public <A> Filters getFilters( AbstractEntityArg<A, T, S> entityArg ) throws BadRequestException {
        try {
            return Filters.by( service.getFilter( entityArg.getPropertyName(), entityArg.getPropertyType(), Filter.Operator.eq, entityArg.getValue() ) );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }
    }

    @Override
    public Filters getFilters( AbstractEntityArrayArg<T, S> entitiesArg ) throws BadRequestException {
        try {
            Filters.FiltersClauseBuilder clause = Filters.empty()
                    .and();
            for ( Map.Entry<String, List<String>> e : getArgsByPropertyName( entitiesArg ).entrySet() ) {
                if ( e.getValue().size() == 1 ) {
                    clause = clause.or( service.getFilter( e.getKey(), Filter.Operator.eq, e.getValue().iterator().next() ) );
                } else if ( e.getValue().size() > 1 ) {
                    clause = clause.or( service.getFilter( e.getKey(), Filter.Operator.in, e.getValue() ) );
                }
            }
            return clause.build();
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }
    }

    /**
     * Given a {@link AbstractEntityArrayArg}, construct a mapping of properties it refers to values those properties
     * are allowed to take in a filter.
     */
    protected Map<String, List<String>> getArgsByPropertyName( AbstractEntityArrayArg<T, S> entitiesArg ) {
        return entitiesArg.getValue().stream()
                .map( v -> Pair.of( v, entityArgValueOf( entitiesArg.getEntityArgClass(), v ) ) )
                .collect( Collectors.groupingBy( a -> a.getRight().getPropertyName(), Collectors.mapping( Pair::getLeft, Collectors.toList() ) ) );

    }

    @Override
    public Filters getFilters( FilterArg<T> filterArg ) throws BadRequestException {
        return filterArg.getFilters( service );
    }

    @Override
    public Sort getSort( SortArg<T> sortArg ) throws BadRequestException {
        return sortArg.getSort( service );
    }

    /**
     * Checks whether the given object is null, and throws an appropriate exception if necessary.
     *
     * @param entity  the object that should be checked for being null.
     * @return the same object as given.
     * @throws NotFoundException if the given entity is null.
     */
    protected T checkEntity( AbstractEntityArg<?, T, S> entityArg, @Nullable T entity ) throws NotFoundException {
        if ( entity == null ) {
            EntityNotFoundException cause = new EntityNotFoundException( String.format( ERROR_FORMAT_ENTITY_NOT_FOUND, entityArg.getPropertyName(), service.getElementClass(), entityArg.getValue() ) );
            throw new NotFoundException( ERROR_MSG_ENTITY_NOT_FOUND, cause );
        }
        return entity;
    }

    /**
     * Invoke either a static {@code valueOf} method or a suitable constructor to instantiate the argument.
     */
    protected AbstractEntityArg<?, T, S> entityArgValueOf( Class<? extends AbstractEntityArg<?, T, S>> entityArgClass, String s ) throws BadRequestException {
        try {
            return _entityArgValueOf( entityArgClass, s );
        } catch ( InvocationTargetException e ) {
            if ( e.getCause() instanceof BadRequestException ) {
                throw ( BadRequestException ) e.getCause();
            } else if ( e.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        } catch ( InstantiationException | IllegalAccessException | NoSuchMethodException e ) {
            throw new RuntimeException( String.format( "Could not call 'valueOf' nor a suitable constructor for %s", entityArgClass.getName() ), e );
        }
    }

    private AbstractEntityArg<?, T, S> _entityArgValueOf( Class<? extends AbstractEntityArg<?, T, S>> entityArgClass, String s ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        try {
            //noinspection unchecked
            return ( AbstractEntityArg<?, T, S> ) entityArgClass.getMethod( "valueOf", String.class ).invoke( null, s );
        } catch ( NoSuchMethodException ignored ) {
            return entityArgClass.getConstructor( String.class ).newInstance( s );
        }
    }
}
