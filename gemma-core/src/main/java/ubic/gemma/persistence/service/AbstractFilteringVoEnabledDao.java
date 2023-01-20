package ubic.gemma.persistence.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O> the entity type
 * @param <VO> the corresponding VO type
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    /**
     * Maximum depth to explore when enumerating filterable properties via {@link #getFilterableProperties()}.
     */
    protected static final int FILTERABLE_PROPERTIES_MAX_DEPTH = 3;

    /**
     * Cached partial filterable properties meta, computed as we go.
     */
    private static final Map<Key, PartialFilterablePropertyMeta> partialFilterablePropertiesMeta = new ConcurrentHashMap<>();

    @Value
    private static class Key {
        Class<?> clazz;
        String propertyName;
    }

    private final String objectAlias;

    /**
     * Cached filterable properties, computed once on startup.
     */
    private final Set<String> filterableProperties;

    protected AbstractFilteringVoEnabledDao( @Nullable String objectAlias, Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.objectAlias = objectAlias;
        Set<String> result = new HashSet<>();
        addFilterableProperties( "", elementClass, result, FILTERABLE_PROPERTIES_MAX_DEPTH );
        this.filterableProperties = Collections.unmodifiableSet( result );
    }

    /**
     * {@inheritDoc}
     * <p>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final VO loadValueObject( O entity ) {
        return loadValueObjectsPreFilter( Filters.by( objectAlias, getIdPropertyName(), Long.class, Filter.Operator.eq, entity.getId() ), null ).stream()
                .findFirst()
                .orElse( null );
    }

    /**
     * {@inheritDoc}
     * <p>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final VO loadValueObjectById( Long id ) {
        return loadValueObjectsPreFilter( Filters.by( objectAlias, getIdPropertyName(), Long.class, Filter.Operator.eq, id ), null ).stream()
                .findFirst()
                .orElse( null );
    }

    /**
     * {@inheritDoc}
     * <p>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadValueObjects( Collection<O> entities ) {
        if ( entities.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.by( objectAlias, getIdPropertyName(), Long.class, Filter.Operator.in, EntityUtils.getIds( entities ) ), null );
    }

    /**
     * {@inheritDoc}
     * <p>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.by( objectAlias, getIdPropertyName(), Long.class, Filter.Operator.in, ids ), null );
    }

    /**
     * {@inheritDoc}
     * <p>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadAllValueObjects() {
        return loadValueObjectsPreFilter( null, null );
    }

    @Override
    public Set<String> getFilterableProperties() {
        return filterableProperties;
    }

    @Override
    public Class<?> getFilterablePropertyType( String propertyName ) throws IllegalArgumentException {
        return getFilterablePropertyMeta( propertyName ).propertyType;
    }

    @Nullable
    @Override
    public String getFilterablePropertyDescription( String propertyName ) throws IllegalArgumentException {
        return getFilterablePropertyMeta( propertyName ).description;
    }

    @Nullable
    @Override
    public List<Object> getFilterablePropertyAvailableValues( String propertyName ) {
        return getFilterablePropertyMeta( propertyName ).availableValues;
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, String value ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values );
    }

    @Override
    public final Sort getSort( String property, @Nullable Sort.Direction direction ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return Sort.by( propertyMeta.objectAlias, propertyMeta.propertyName, direction );
    }

    /**
     * Helper that inspects a class and add all the filterable properties with the given prefix.
     */
    protected void addFilterableProperties( String prefix, Class<?> entityClass, Set<String> destination, int maxDepth ) {
        if ( maxDepth <= 0 ) {
            throw new IllegalArgumentException( String.format( "Maximum depth for adding filterable properties of %s to %s must be strictly positive.",
                    entityClass.getName(), prefix ) );
        }
        ClassMetadata classMetadata = getSessionFactory().getClassMetadata( entityClass );
        String[] propertyNames = classMetadata.getPropertyNames();
        Type[] propertyTypes = classMetadata.getPropertyTypes();
        if ( classMetadata.getIdentifierPropertyName() != null ) {
            destination.add( prefix + classMetadata.getIdentifierPropertyName() );
        }
        for ( int i = 0; i < propertyNames.length; i++ ) {
            if ( propertyTypes[i].isEntityType() ) {
                if ( maxDepth > 1 ) {
                    addFilterableProperties( prefix + propertyNames[i] + ".", propertyTypes[i].getReturnedClass(), destination, maxDepth - 1 );
                }
            } else if ( propertyTypes[i].isCollectionType() ) {
                // special case for collection size, regardless of its type
                destination.add( prefix + propertyNames[i] + ".size" );
            } else if ( Filter.getConversionService().canConvert( String.class, propertyTypes[i].getReturnedClass() ) ) {
                destination.add( prefix + propertyNames[i] );
            }
        }
    }

    /**
     * Meta-information for a filterable property.
     */
    @With
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    protected static class FilterablePropertyMeta {
        String objectAlias;
        String propertyName;
        Class<?> propertyType;
        /**
         * A short description for this parameter if clarifications are needed.
         * <p>
         * This will appear in the generated OpenAPI specification.
         */
        @Nullable
        String description;
        /**
         * A short list of allowed values if that can be determined, or null.
         */
        @Nullable
        List<Object> availableValues;
    }

    /**
     * Obtain various meta-information used to infer what to use in a {@link Filter} or {@link Sort}.
     * <p>
     * This is used by {@link #getFilter(String, Filter.Operator, String)} and {@link #getSort(String, Sort.Direction)}.
     *
     * @throws IllegalArgumentException if no such propertyName exists in {@link O}
     * @see #getFilter(String, Filter.Operator, String)
     * @see #getFilter(String, Filter.Operator, Collection)
     * @see #getSort(String, Sort.Direction)
     */
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        return getFilterablePropertyMeta( objectAlias, propertyName, elementClass );
    }

    protected FilterablePropertyMeta getFilterablePropertyMeta( @Nullable String objectAlias, String propertyName, Class<?> clazz ) throws IllegalArgumentException {
        Key key = new Key( clazz, propertyName );
        PartialFilterablePropertyMeta partialMeta = partialFilterablePropertiesMeta.computeIfAbsent( key, ignored -> {
            try {
                return resolveFilterablePropertyMetaInternal( propertyName, clazz, FILTERABLE_PROPERTIES_MAX_DEPTH );
            } catch ( NoSuchFieldException e ) {
                throw new IllegalArgumentException( String.format( "Could not resolve property %s on %s.", propertyName, clazz.getName() ), e );
            }
        } );
        return new FilterablePropertyMeta( objectAlias, propertyName, partialMeta.propertyType, null, partialMeta.availableValues );
    }

    /**
     * Partial property meta.
     * <p>
     * This is used by {@link #resolveFilterablePropertyMetaInternal(String, Class, int)} which uses a recursion that
     * does not need to track information about object alias, full property path, etc.
     */
    @Value
    private static class PartialFilterablePropertyMeta {
        Class<?> propertyType;
        @Nullable
        List<Object> availableValues;
    }

    /**
     * Checks if property of given name exists in the given class. If the given string specifies
     * nested properties (E.g. curationDetails.troubled), only the substring before the first dot is evaluated and the
     * rest of the string is processed in a new recursive iteration.
     *
     * @param property the property to check for. If the string contains dot characters ('.'), only the part
     *                 before the first dot will be evaluated. Substring after the dot will be checked against the
     *                 type of the field retrieved from the substring before the dot.
     * @param cls      the class to check the property on.
     * @return the class of the property last in the line of nesting.
     */
    private PartialFilterablePropertyMeta resolveFilterablePropertyMetaInternal( String property, Class<?> cls, int maxDepth ) throws NoSuchFieldException {
        if ( maxDepth == 0 ) {
            throw new IllegalArgumentException( String.format( "At most %d levels can be used for filtering.",
                    FILTERABLE_PROPERTIES_MAX_DEPTH ) );
        }

        ClassMetadata classMetadata = getSessionFactory().getClassMetadata( cls );

        String[] parts = property.split( "\\.", 2 );

        // ID is kept separately from properties
        if ( parts[0].equals( classMetadata.getIdentifierPropertyName() ) ) {
            return new PartialFilterablePropertyMeta( classMetadata.getIdentifierType().getReturnedClass(), null );
        }

        String[] propertyNames = classMetadata.getPropertyNames();
        Type[] propertyTypes = classMetadata.getPropertyTypes();
        int i = ArrayUtils.indexOf( propertyNames, parts[0] );
        if ( i == -1 ) {
            throw new NoSuchFieldException( String.format( "No such field %s in %s.", parts[0], cls.getName() ) );
        }

        Type propertyType = propertyTypes[i];

        // recurse only on entity type
        if ( parts.length > 1 ) {
            if ( propertyType.isEntityType() ) {
                return resolveFilterablePropertyMetaInternal( parts[1], propertyType.getReturnedClass(), maxDepth - 1 );
            } else if ( propertyType.isCollectionType() && "size".equals( parts[1] ) ) {
                return new PartialFilterablePropertyMeta( Integer.class, null ); /* special case for collection size */
            } else {
                throw new NoSuchFieldException( String.format( "%s is not an entity type in %s.", property, cls.getName() ) );
            }
        }

        Class<?> actualType = ( Class<?> ) propertyType.getReturnedClass();

        // available values, only for enumerated types
        List<Object> availableValues;
        if ( propertyType instanceof CustomType && ( ( CustomType ) propertyType ).getUserType() instanceof EnumType ) {
            EnumType et = ( EnumType ) ( ( CustomType ) propertyType ).getUserType();
            //noinspection unchecked,rawtypes
            availableValues = new ArrayList<>( EnumSet.allOf( et.returnedClass() ) );
        } else {
            availableValues = null;
        }

        if ( Filter.getConversionService().canConvert( String.class, actualType ) ) {
            return new PartialFilterablePropertyMeta( actualType, availableValues );
        } else {
            throw new NoSuchFieldException( String.format( "%s is not of a supported type or a collection of supported types %s.", property, cls.getName() ) );
        }
    }

    private String getIdPropertyName() {
        return getSessionFactory().getClassMetadata( elementClass ).getIdentifierPropertyName();
    }
}