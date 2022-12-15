package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for VO-enabled DAO with filtering capabilities.
 * @author poirigui
 */
public interface FilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledDao<O, VO>, BaseDao<O> {

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
     * or if the value cannot apply to the property an operator see {@link Filter#parse(String, String, Class, Filter.Operator, String)}
     * for more details
     */
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * Similar to {@link #getFilter(String, Filter.Operator, String)}, but with a collection of values.
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * Obtain a {@link Sort} object for a property of the {@link O}.
     *
     * @param property a property of {@link O} to sort by
     * @param direction a sorting direction, or null if the default direction applies
     * @return a {@link Sort} object that can be used, for example, on {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}
     * @throws IllegalArgumentException if no such field exists in {@link O}
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;

    /**
     * Load VOs with ordering, filtering and offset/limit.
     * <p>
     * Consider using {@link FilteringVoEnabledService#getFilter(String, Filter.Operator, String)} and {@link FilteringVoEnabledService#getSort(String, Sort.Direction)}
     * to produce the filters and sort safely from user input.
     *
     * @param filters filters applied on the search. The properties mentioned in the {@link Filter}
     *                must exist and be visible to Hibernate. You can use nested properties such as "curationDetails.lastUpdated".
     * @param sort    an object property and direction to order by. This property must exist and be visible to
     *                Hibernate. You can use nested properties such as "curationDetails.lastUpdated".
     * @param offset  an offset from which entities are retrieved when sorted according to the sort argument, or 0 to
     *                ignore
     * @param limit   a limit on the number of returned results, or -1 to ignore
     * @return a slice of the relevant data
     */
    Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Load VOs with minimal ordering and filtering.
     *
     * Use this as an alternative to {@link #loadValueObjectsPreFilter(Filters, Sort, int, int)} if you do not
     * intend to provide pagination capabilities.
     *
     * @see #loadValueObjectsPreFilter(Filters, Sort, int, int)
     */
    List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * Count VOs matching the given filters.
     */
    long countValueObjectsPreFilter( @Nullable Filters filters );
}
