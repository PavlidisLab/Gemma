package ubic.gemma.persistence.service;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.*;
import org.springframework.beans.factory.InitializingBean;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O> the entity type
 * @param <VO> the corresponding VO type
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO>, InitializingBean {

    /**
     * Maximum depth to explore when enumerating filterable properties via {@link #getFilterableProperties()}.
     */
    private static final int FILTERABLE_PROPERTIES_MAX_DEPTH = 3;

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
    private final Set<String> filterableProperties = new HashSet<>();

    /**
     * Subset of {@link #filterableProperties} that should use a subquery for filtering.
     */
    private final Set<String> filterablePropertiesViaSubquery = new HashSet<>();

    /**
     * Aliases for filterable properties.
     */
    private final Set<FilterablePropertyAlias> filterablePropertyAliases = new HashSet<>();

    protected AbstractFilteringVoEnabledDao( @Nullable String objectAlias, Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.objectAlias = objectAlias;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void afterPropertiesSet() {
        log.debug( String.format( "Configuring filterable properties for %s...", getElementClass().getName() ) );
        StopWatch timer = StopWatch.createStarted();
        configureFilterableProperties( new FilterablePropertiesConfigurer() );
        String message = String.format( "Done configuring for %s. %d properties were registered in %d ms.", getElementClass().getName(), filterableProperties.size(), timer.getTime() );
        if ( timer.getTime() > 100 ) {
            log.warn( message );
        } else {
            log.debug( message );
        }
    }

    @Override
    public final Set<String> getFilterableProperties() {
        return filterableProperties;
    }

    /**
     * Configurer for filterable properties and their aliases.
     */
    protected class FilterablePropertiesConfigurer {

        private final Map<String, Class<?>> entityByPrefix = new HashMap<>();

        public void registerProperty( String propertyName ) {
            registerProperty( propertyName, false );
        }

        /**
         * Register a given property.
         * @throws IllegalArgumentException if the property is already registered
         */
        public void registerProperty( String propertyName, boolean useSubquery ) {
            if ( getFilterablePropertyMeta( propertyName ) == null ) {
                throw new IllegalArgumentException( "Property %s does not have any associated meta information." );
            }
            if ( filterableProperties.add( propertyName ) ) {
                if ( useSubquery ) {
                    filterablePropertiesViaSubquery.add( propertyName );
                }
                log.trace( String.format( "Registered property %s.", propertyName ) );
            } else {
                throw new IllegalArgumentException( String.format( "Filterable property %s is already registered.",
                        propertyName ) );
            }
        }

        /**
         * Register all the given properties.
         * @throws IllegalArgumentException if any of the given properties is already registered
         */
        public void registerProperties( String... propertyNames ) throws IllegalArgumentException {
            List<String> propsMissingMeta = Arrays.stream( propertyNames )
                    .filter( p -> getFilterablePropertyMeta( p ) == null )
                    .collect( Collectors.toList() );
            if ( !propsMissingMeta.isEmpty() ) {
                throw new IllegalArgumentException( String.format( "The following properties are missing meta information: %s.",
                        String.join( ", ", propsMissingMeta ) ) );
            }
            List<String> props = Arrays.asList( propertyNames );
            if ( CollectionUtils.containsAny( filterableProperties, props ) ) {
                throw new IllegalArgumentException( String.format( "The following filterable properties are already registered: %s.",
                        props.stream().filter( filterableProperties::contains ).collect( Collectors.joining( ", " ) ) ) );
            }
            filterableProperties.addAll( props );
            log.trace( String.format( "Registered properties: %s.", String.join( ", ", propertyNames ) ) );
        }

        /**
         * @throws IllegalArgumentException if no properties match the given name
         */
        public void unregisterProperty( String propertyName ) {
            if ( filterableProperties.remove( propertyName ) ) {
                log.trace( String.format( "Unregistered property %s.", propertyName ) );
            } else {
                throw new IllegalArgumentException( String.format( "No such filterable properties %s.", propertyName ) );
            }
        }

        /**
         * Unregister all the properties matching the given predicate.
         * @throws IllegalArgumentException if no properties match the given predicate
         */
        public void unregisterProperties( Predicate<? super String> predicate ) throws IllegalArgumentException {
            int sizeBefore = filterableProperties.size();
            if ( filterableProperties.removeIf( predicate ) ) {
                log.trace( String.format( "Unregistered %d properties using a predicate.", sizeBefore - filterableProperties.size() ) );
            } else {
                throw new IllegalArgumentException( "No filterable properties matched the supplied predicate." );
            }
        }

        public void registerEntity( String prefix, Class<?> entityClass, int maxDepth ) throws IllegalArgumentException {
            registerEntity( prefix, entityClass, maxDepth, false );
        }

        /**
         * Register an entity available at a given prefix.
         * <p>
         * This method recursively register the properties of a given entity up to the maximum depth.
         * @param prefix      a prefix under which the entity is made available
         * @param entityClass a class for the entity, which must be registered mapped by Hibernate
         * @param maxDepth    maximum depth for visiting properties. For example, zero would expose no property but the
         *                    entity itself, 1 would expose the properties of the alias, 2 would expose the properties
         *                    of any entity directly related to the given entity, etc.
         * @param useSubquery whether to use a subquery when filtering by this entity (and its descendant)
         * @throws IllegalArgumentException if no entity of the given type is registered under the given prefix or if
         * the prefix is invalid
         */
        public void registerEntity( String prefix, Class<?> entityClass, int maxDepth, boolean useSubquery ) throws IllegalArgumentException {
            if ( !prefix.isEmpty() && !prefix.endsWith( "." ) ) {
                throw new IllegalArgumentException( "A non-empty prefix must end with a '.' character." );
            }
            if ( maxDepth <= 0 ) {
                throw new IllegalArgumentException( String.format( "Maximum depth for adding filterable properties of %s %s must be strictly positive.",
                        entityClass.getName(), summarizePrefix( prefix ) ) );
            }
            ClassMetadata classMetadata = getSessionFactory().getClassMetadata( entityClass );
            if ( classMetadata == null ) {
                throw new IllegalArgumentException( String.format( "Cannot add filterable properties for unmapped class %s %s.",
                        entityClass.getName(), summarizePrefix( prefix ) ) );
            }
            Class<?> prevValue = entityByPrefix.putIfAbsent( prefix, entityClass );
            if ( prevValue != null ) {
                throw new IllegalArgumentException( String.format( "An entity of type %s is already registered %s.",
                        prevValue.getName(), summarizePrefix( prefix ) ) );
            }
            String[] propertyNames = classMetadata.getPropertyNames();
            Type[] propertyTypes = classMetadata.getPropertyTypes();
            if ( classMetadata.getIdentifierPropertyName() != null ) {
                registerProperty( prefix + classMetadata.getIdentifierPropertyName(), useSubquery );
            }
            for ( int i = 0; i < propertyNames.length; i++ ) {
                String propertyName = propertyNames[i];
                Type propertyType = propertyTypes[i];
                if ( propertyType.isEntityType() ) {
                    if ( maxDepth > 1 ) {
                        registerEntity( prefix + propertyName + ".", propertyType.getReturnedClass(), maxDepth - 1, useSubquery );
                    } else {
                        log.trace( String.format( "Max depth reached, will not recurse into %s", propertyName ) );
                    }
                } else if ( propertyType.isCollectionType() ) {
                    // special case for collection size, regardless of its type
                    registerProperty( prefix + propertyName + ".size", useSubquery );
                } else if ( propertyType instanceof MaterializedBlobType || propertyType instanceof MaterializedClobType || propertyType instanceof MaterializedNClobType ) {
                    log.trace( String.format( "Property %s%s of type %s was excluded in %s: BLOBs and CLOBs are not exposed by default.",
                            prefix, propertyName, propertyType.getName(), entityClass.getName() ) );
                } else if ( Filter.getConversionService().canConvert( String.class, propertyType.getReturnedClass() ) ) {
                    registerProperty( prefix + propertyName, useSubquery );
                } else {
                    log.warn( String.format( "Property %s%s of type %s in %s is not supported and will be skipped.",
                            prefix, propertyName, propertyType.getReturnedClass().getName(), entityClass.getName() ) );
                }
            }
            log.trace( String.format( "Registered entity %s %s.", entityClass.getName(), summarizePrefix( prefix ) ) );
        }

        /**
         * Unregister an entity at a given prefix previously registered via {@link #registerEntity(String, Class, int)}.
         * <p>
         * Note that since {@link #registerEntity(String, Class, int)} works recursively, you can unregister an entity
         * registered under a longer prefix. For example, if you registered an {@link ubic.gemma.model.analysis.Analysis}
         * under {@code analysis.}, you can then unregister an {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}
         * under {@code analysis.experimentAnalyzed}.
         * @throws IllegalArgumentException if no entity of the given type is registered under the given prefix or if
         * the prefix is invalid
         */
        public void unregisterEntity( String prefix, Class<?> entityClass ) {
            if ( !prefix.isEmpty() && !prefix.endsWith( "." ) ) {
                throw new IllegalArgumentException( "A non-empty prefix must end with a '.' character." );
            }
            if ( entityByPrefix.remove( prefix, entityClass ) ) {
                if ( filterableProperties.removeIf( s -> s.startsWith( prefix ) ) ) {
                    // remove entities registered under sub-prefixes
                    entityByPrefix.keySet().removeIf( p -> p.startsWith( prefix ) );
                } else {
                    log.warn( String.format( "While unregistering %s %s, no properties were removed. Is it possible that a parent prefix was already removed?",
                            entityClass.getName(), summarizePrefix( prefix ) ) );
                }
                log.trace( String.format( "Registered entity %s under %s.", entityClass.getName(), summarizePrefix( prefix ) ) );
            } else {
                throw new IllegalArgumentException( String.format( "No entity of type %s is registered %s.",
                        entityClass.getName(), summarizePrefix( prefix ) ) );
            }
        }

        /**
         * @see #registerAlias(String, String, Class, String, int, boolean)
         */
        public void registerAlias( String prefix, @Nullable String objectAlias, Class<?> propertyType, @Nullable String aliasFor, int maxDepth ) {
            registerAlias( prefix, objectAlias, propertyType, aliasFor, maxDepth, false );
        }

        /**
         * Register an alias for a property.
         * <p>
         * This also registers a property under the given prefix as per {@link #registerEntity(String, Class, int)}.
         * @param objectAlias internal alias used to refer to the entity as per {@link Filter#getObjectAlias()}.
         * @see #registerEntity(String, Class, int)
         */
        public void registerAlias( String prefix, @Nullable String objectAlias, Class<?> propertyType, @Nullable String aliasFor, int maxDepth, boolean useSubquery ) {
            filterablePropertyAliases.add( new FilterablePropertyAlias( prefix, objectAlias, propertyType, aliasFor ) );
            registerEntity( prefix, propertyType, maxDepth, useSubquery );
            log.trace( String.format( "Registered alias for %s (%s) %s.", objectAlias, propertyType.getName(), summarizePrefix( prefix ) ) );
        }

        private String summarizePrefix( String prefix ) {
            return prefix.isEmpty() ? "as root" : String.format( "under prefix '%s'", prefix );
        }
    }

    /**
     * Register filterable properties.
     */
    @OverridingMethodsMustInvokeSuper
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        configurer.registerEntity( "", getElementClass(), FILTERABLE_PROPERTIES_MAX_DEPTH );
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
    public List<Object> getFilterablePropertyAllowedValues( String propertyName ) throws IllegalArgumentException {
        return getFilterablePropertyMeta( propertyName ).allowedValues;
    }

    @Override
    public boolean getFilterablePropertyIsUsingSubquery( String property ) throws IllegalArgumentException {
        if ( !filterableProperties.contains( property ) ) {
            throw new IllegalArgumentException( String.format( "Unknown filterable property %s.", property ) );
        }
        return filterablePropertiesViaSubquery.contains( property );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, String value ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value, property ), property, null );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, String value, SubqueryMode subqueryMode ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value, property ), property, subqueryMode );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values, property ), property, null );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, Collection<String> values, SubqueryMode subqueryMode ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.parse( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values, property ), property, subqueryMode );
    }

    @Override
    public final <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, T value ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.by( propertyMeta.objectAlias, propertyMeta.propertyName, propertyType, operator, value, property ), property, null );
    }

    @Override
    public final <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, Collection<T> values ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return nestIfSubquery( Filter.by( propertyMeta.objectAlias, propertyMeta.propertyName, propertyType, operator, values, property ), property, null );
    }

    private Filter nestIfSubquery( Filter f, String propertyName, @Nullable SubqueryMode subqueryMode ) {
        if ( !filterablePropertiesViaSubquery.contains( propertyName ) ) {
            if ( subqueryMode != null ) {
                throw new IllegalArgumentException( propertyName + " cannot be filtered via a subquery." );
            }
            return f;
        }
        String entityName = getSessionFactory().getClassMetadata( getElementClass() ).getEntityName();
        List<Subquery.Alias> aliases;
        if ( f.getObjectAlias() != null ) {
            aliases = null;
            for ( FilterablePropertyAlias fpa : filterablePropertyAliases ) {
                if ( f.getObjectAlias().equals( fpa.getObjectAlias() ) ) {
                    aliases = SubqueryUtils.guessAliases( fpa.prefix, fpa.getObjectAlias() );
                    break;
                }
            }
            if ( aliases == null ) {
                throw new IllegalArgumentException( String.format( "Could not find a filterable property alias for %s.", f.getObjectAlias() ) );
            }
        } else {
            // the property refers to the root entity, no need for aliases
            aliases = Collections.emptyList();
        }
        Filter.Operator subqueryOp;
        if ( subqueryMode != null ) {
            switch ( subqueryMode ) {
                case ANY:
                    subqueryOp = Filter.Operator.inSubquery;
                    break;
                case ALL:
                    subqueryOp = Filter.Operator.notInSubquery;
                    f = Filter.not( f );
                    break;
                case NONE:
                    subqueryOp = Filter.Operator.notInSubquery;
                    break;
                default:
                    throw new IllegalArgumentException( "Unknown subquery mode." );
            }
        } else {
            subqueryOp = Filter.Operator.inSubquery;
        }
        return Filter.by( objectAlias, getIdentifierPropertyName(), Long.class, subqueryOp,
                    new Subquery( entityName, getIdentifierPropertyName(), aliases, f ),
                    propertyName );
    }

    @Override
    public final Sort getSort( String property, @Nullable Sort.Direction direction ) {
        FilterablePropertyMeta propertyMeta = getFilterablePropertyMeta( property );
        return Sort.by( propertyMeta.objectAlias, propertyMeta.propertyName, direction, property );
    }

    /**
     * Meta-information for a filterable property.
     */
    @With
    @Value
    protected static class FilterablePropertyMeta {
        @Nullable
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
        List<Object> allowedValues;
    }

    @Value
    @EqualsAndHashCode(of = "prefix")
    private static class FilterablePropertyAlias {
        String prefix;
        @Nullable
        String objectAlias;
        Class<?> propertyType;
        @Nullable
        String aliasFor;
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
        // replace longer prefix first
        List<FilterablePropertyAlias> aliases = filterablePropertyAliases.stream()
                .sorted( Comparator.comparing( f -> f.prefix.length(), Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        for ( FilterablePropertyAlias alias : aliases ) {
            if ( propertyName.startsWith( alias.prefix ) && !propertyName.equals( alias.prefix + "size" ) ) {
                String fieldName = propertyName.replaceFirst( "^" + Pattern.quote( alias.prefix ), "" );
                return getFilterablePropertyMeta( alias.objectAlias, fieldName, alias.propertyType )
                        .withDescription( alias.aliasFor != null ? String.format( "alias for %s.%s", alias.aliasFor, fieldName ) : null );
            }
        }
        return getFilterablePropertyMeta( objectAlias, propertyName, getElementClass() );
    }

    protected FilterablePropertyMeta getFilterablePropertyMeta( @Nullable String objectAlias, String propertyName, Class<?> clazz ) throws IllegalArgumentException {
        Key key = new Key( clazz, propertyName );
        PartialFilterablePropertyMeta partialMeta = partialFilterablePropertiesMeta.computeIfAbsent( key, ignored -> {
            try {
                return resolveFilterablePropertyMetaInternal( propertyName, clazz );
            } catch ( NoSuchFieldException e ) {
                throw new IllegalArgumentException( String.format( "Could not resolve property %s on %s.", propertyName, clazz.getName() ), e );
            }
        } );
        return new FilterablePropertyMeta( objectAlias, propertyName, partialMeta.propertyType, null, partialMeta.allowedValues );
    }

    /**
     * Partial property meta.
     * <p>
     * This is used by {@link #resolveFilterablePropertyMetaInternal(String, Class)} which uses a recursion that
     * does not need to track information about object alias, full property path, etc.
     */
    @Value
    private static class PartialFilterablePropertyMeta {
        Class<?> propertyType;
        @Nullable
        List<Object> allowedValues;
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
    private PartialFilterablePropertyMeta resolveFilterablePropertyMetaInternal( String property, Class<?> cls ) throws NoSuchFieldException {
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
                return resolveFilterablePropertyMetaInternal( parts[1], propertyType.getReturnedClass() );
            } else if ( propertyType.isCollectionType() && "size".equals( parts[1] ) ) {
                return new PartialFilterablePropertyMeta( Integer.class, null ); /* special case for collection size */
            } else {
                throw new NoSuchFieldException( String.format( "%s is not an entity type in %s.", property, cls.getName() ) );
            }
        }

        Class<?> actualType = ( Class<?> ) propertyType.getReturnedClass();

        // available values, only for enumerated types
        List<Object> allowedValues;
        if ( isEnumType( propertyType ) ) {
            EnumType et = ( EnumType ) ( ( CustomType ) propertyType ).getUserType();
            //noinspection unchecked,rawtypes
            allowedValues = new ArrayList<>( EnumSet.allOf( et.returnedClass() ) );
        } else {
            allowedValues = null;
        }

        if ( Filter.getConversionService().canConvert( String.class, actualType ) ) {
            return new PartialFilterablePropertyMeta( actualType, allowedValues );
        } else {
            throw new NoSuchFieldException( String.format( "%s is not of a supported type or a collection of supported types %s.", property, cls.getName() ) );
        }
    }

    private static boolean isEnumType( Type type ) {
        return type instanceof CustomType && ( ( CustomType ) type ).getUserType() instanceof EnumType;
    }
}