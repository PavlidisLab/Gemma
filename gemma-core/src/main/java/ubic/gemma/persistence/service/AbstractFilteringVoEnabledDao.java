package ubic.gemma.persistence.service;

import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

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
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( objectAlias, getIdPropertyName(), Long.class, ObjectFilter.Operator.eq, entity.getId() ) ), null ).stream()
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
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( objectAlias, getIdPropertyName(), Long.class, ObjectFilter.Operator.eq, id ) ), null ).stream()
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
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( objectAlias, getIdPropertyName(), Long.class, ObjectFilter.Operator.in, EntityUtils.getIds( entities ) ) ), null );
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
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( objectAlias, getIdPropertyName(), Long.class, ObjectFilter.Operator.in, ids ) ), null );
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

    @Override
    public final ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value );
    }

    @Override
    public final ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values );
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
        if ( maxDepth == 0 )
            return;
        ClassMetadata classMetadata = getSessionFactory().getClassMetadata( entityClass );
        String[] propertyNames = classMetadata.getPropertyNames();
        Type[] propertyTypes = classMetadata.getPropertyTypes();
        if ( classMetadata.getIdentifierPropertyName() != null ) {
            destination.add( prefix + classMetadata.getIdentifierPropertyName() );
        }
        for ( int i = 0; i < propertyNames.length; i++ ) {
            if ( propertyTypes[i].isCollectionType() ) {
                // only collection of supported scalars
                Class<?> elementClass = ( ( CollectionType ) propertyTypes[i] ).getElementType( ( SessionFactoryImplementor ) getSessionFactory() ).getReturnedClass();
                if ( ObjectFilter.getConversionService().canConvert( String.class, elementClass ) ) {
                    destination.add( prefix + propertyNames[i] );
                }
            } else if ( propertyTypes[i].isEntityType() ) {
                addFilterableProperties( prefix + propertyNames[i] + ".", propertyTypes[i].getReturnedClass(), destination, maxDepth - 1 );
            } else if ( ObjectFilter.getConversionService().canConvert( String.class, propertyTypes[i].getReturnedClass() ) ) {
                destination.add( prefix + propertyNames[i] );
            }
        }
    }

    /**
     * Meta-information for a filterable property.
     */
    @Value
    protected static class FilterablePropertyMeta {
        String objectAlias;
        String propertyName;
        Class<?> propertyType;
    }

    /**
     * Obtain various meta-information used to infer what to use in a {@link ObjectFilter} or {@link Sort}.
     *
     * This is used by {@link #getObjectFilter(String, ObjectFilter.Operator, String)} and {@link #getSort(String, Sort.Direction)}.
     *
     * @throws IllegalArgumentException if no such propertyName exists in {@link O}
     * @see #getObjectFilter(String, ObjectFilter.Operator, String)
     * @see #getObjectFilter(String, ObjectFilter.Operator, Collection)
     * @see #getSort(String, Sort.Direction)
     */
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        return new FilterablePropertyMeta( objectAlias, propertyName, resolveObjectFilterPropertyType( propertyName, elementClass ) );
    }

    /**
     * Helper to resolve the type of a property in a given class.
     */
    protected Class<?> resolveObjectFilterPropertyType( String propertyName, Class<?> clazz ) {
        if ( ( StringUtils.countMatches( propertyName, '.' ) + 1 ) > FILTERABLE_PROPERTIES_MAX_DEPTH ) {
            throw new IllegalArgumentException( String.format( "At most %d levels can be used for filtering.",
                    FILTERABLE_PROPERTIES_MAX_DEPTH ) );
        }
        try {
            return getFilterablePropertyType( propertyName, clazz );
        } catch ( NoSuchFieldException e ) {
            String availableProperties = getFilterableProperties().stream().sorted().collect( Collectors.joining( ", " ) );
            throw new IllegalArgumentException( String.format( "Could not resolve property '%s' on %s. Available properties are: %s.", propertyName, clazz.getName(), availableProperties ), e );
        }
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
    private Class<?> getFilterablePropertyType( String property, Class<?> cls ) throws NoSuchFieldException {
        ClassMetadata classMetadata = getSessionFactory().getClassMetadata( cls );

        String[] parts = property.split( "\\.", 2 );

        // ID is kept separately from properties
        if ( parts[0].equals( classMetadata.getIdentifierPropertyName() ) ) {
            return classMetadata.getIdentifierType().getReturnedClass();
        }

        String[] propertyNames = classMetadata.getPropertyNames();
        Type[] propertyTypes = classMetadata.getPropertyTypes();
        int i = ArrayUtils.indexOf( propertyNames, parts[0] );
        if ( i == -1 ) {
            throw new NoSuchFieldException( String.format( "No such field %s in %s.", property, cls.getName() ) );
        }

        Type propertyType = propertyTypes[i];

        Class<?> subCls;

        if ( parts.length > 1 ) {
            if ( propertyType.isCollectionType() ) {
                subCls = ( ( CollectionType ) propertyType ).getElementType( ( SessionFactoryImplementor ) getSessionFactory() ).getReturnedClass();
                if ( ObjectFilter.getConversionService().canConvert( String.class, subCls ) ) {
                    return subCls;
                } else {
                    throw new NoSuchFieldException( String.format( "element type of %s in %s is not supported.", property, cls.getName() ) );
                }
            } else if ( propertyType.isEntityType() ) {
                subCls = propertyType.getReturnedClass();
            } else {
                throw new NoSuchFieldException( String.format( "%s is not an entity or collection type in %s.", property, cls.getName() ) );
            }
            return getFilterablePropertyType( parts[1], subCls );
        } else if ( ObjectFilter.getConversionService().canConvert( String.class, propertyType.getReturnedClass() ) ) {
            return propertyType.getReturnedClass();
        } else {
            throw new NoSuchFieldException( String.format( "%s is not an entity or collection type in %s.", property, cls.getName() ) );
        }
    }

    private String getIdPropertyName() {
        return getSessionFactory().getClassMetadata( elementClass ).getIdentifierPropertyName();
    }
}