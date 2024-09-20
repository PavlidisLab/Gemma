package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for filtering-capable DAO.
 * <p>
 * Filtering DAOs have the capability of using {@link Filters} and {@link Sort} abstractions for browsing entities.
 * There are also extra conveniences for loading IDs {@link #loadIds(Filters, Sort)} and counting entities
 * {@link #count(Filters)} without having to retrieve too much from the persistent storage.
 * <p>
 * This interface also provides introspection capabilities for enumerating and getting some meta information about
 * filterable properties.
 *
 * @author poirigui
 */
public interface FilteringDao<O extends Identifiable> extends BaseDao<O> {
    /**
     * List all properties availble for filtering.
     */
    Set<String> getFilterableProperties();

    /**
     * Obtain the type of the given filterable property.
     * @throws IllegalArgumentException if no such property exists, the cause will be a {@link NoSuchFieldException}
     */
    Class<?> getFilterablePropertyType( String propertyName ) throws IllegalArgumentException;

    /**
     * Obtain a short description for a given filterable property.
     * @throws IllegalArgumentException if no such property exists
     */
    @Nullable
    String getFilterablePropertyDescription( String propertyName ) throws IllegalArgumentException;

    /**
     * Obtain a short list of allowed values for the given property, or null if unrestricted.
     * <p>
     * The returned values must be of the type outlined by {@link #getFilterablePropertyType(String)}, generally a list
     * of {@link String} or {@link Integer} to denote possible values for an enumerated type.
     */
    @Nullable
    List<Object> getFilterablePropertyAllowedValues( String property ) throws IllegalArgumentException;

    /**
     * Indicate if the given property is using a subquery for filtering.
     * <p>
     * When this is the case, the filter will only check if at least one related entity is matching.
     * @throws IllegalArgumentException if the property does not exist
     */
    boolean getFilterablePropertyIsUsingSubquery( String property ) throws IllegalArgumentException;

    /**
     * Obtain an {@link Filter} for the entity this DAO is providing.
     * <p>
     * It is the responsibility of the DAO to infer the filter property type as well as the alias to use.
     * <p>
     * If the filter refers to a related entity (i.e. a {@link ubic.gemma.model.expression.bioAssay.BioAssay} of an
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}), then the DAO should inject the corresponding
     * DAO and delegate it the work of creating the filter.
     * <p>
     * In the frontend, that correspond to a FilterArg, but since the definition is not available here, we unpack its
     * attributes.
     *
     * @param property property name in the entity use as a left-hand side of the operator
     * @param operator an operator
     * @param value the corresponding, unparsed value, to the right-hand side of the operator
     * @return a filter filled with the object alias, property, inferred type, operator and parsed value
     * @throws IllegalArgumentException if the property does not exist in {@link O}, or if the operator cannot be applied,
     * or if the value cannot apply to the property an operator see {@link Filter#parse(String, String, Class, Filter.Operator, String, String)}
     * for more details
     */
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    Filter getFilter( String property, Filter.Operator operator, String value, SubqueryMode subqueryMode );

    /**
     * Similar to {@link #getFilter(String, Filter.Operator, String)}, but with a collection of values.
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    Filter getFilter( String property, Filter.Operator operator, Collection<String> values, SubqueryMode subqueryMode );

    /**
     * Obtain a {@link Filter} with an already parsed value.
     * @see #getFilter(String, Filter.Operator, String)
     */
    <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, T value ) throws IllegalArgumentException;

    /**
     * Obtain a {@link Filter} with an already parsed collection of values.
     * @see #getFilter(String, Filter.Operator, Collection)
     */
    <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, Collection<T> values ) throws IllegalArgumentException;

    /**
     * Obtain a {@link Sort} object for a property of the {@link O}.
     *
     * @param property a property of {@link O} to sort by
     * @param direction a sorting direction, or null if the default direction applies
     * @return a {@link Sort} object that can be used, for example, on {@link FilteringVoEnabledDao#loadValueObjects(Filters, Sort, int, int)}
     * @throws IllegalArgumentException if no such field exists in {@link O}
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;

    /**
     * Load IDs of entities matching the given filters.
     */
    List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * Load entities matching the given filters.
     */
    List<O> load( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * Load a slice of entities matching the given filters.
     */
    Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Count VOs matching the given filters.
     */
    long count( @Nullable Filters filters );
}
