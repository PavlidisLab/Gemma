package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.List;

/**
 * Interface for DAO that provide filtering capabilities on their entity using {@link ObjectFilter}.
 *
 * This interface does not yet provide loading capabilities using filters, but you can use {@link FilteringVoEnabledDao}
 * meanwhile or {@link ubic.gemma.persistence.util.ObjectFilterQueryUtils} utilities to generate the corresponding HQL.
 *
 * @param <O> the entity type being filtered
 */
public interface FilteringDao<O extends Identifiable> extends BaseDao<O> {

    /**
     * Obtain the object alias used to identify {@link O} in Hibernate queries.
     *
     * This is used in the RESTful API to generate {@link ObjectFilter} with the correct object alias.
     */
    String getObjectAlias();

    /**
     * Obtain an {@link ObjectFilter} for the entity this DAO is providing.
     *
     * It is the responsibility of the DAO to infer the ObjectFilter property type as well as the alias to use.
     *
     * If the ObjectFilter refers to a related entity (i.e. a bioassay of an expression experiment), then the DAO should
     * inject the corresponding DAO and delegate it the work of creating the ObjectFilter.
     *
     * In the frontend, that correspond to a FilterArg, but since the definition is not available here, we unpack its
     * attributes.
     *
     * @param property property name in the entity use as a left-hand side of the operator
     * @param operator an operator
     * @param value the corresponding, unparsed value, to the right-hand side of the operator
     * @return an object filter filled with the object alias, property, inferred type, operator and parsed value
     * @throws ObjectFilterException if the property does not exist in the entity, or the operator cannot be applied, or
     * if the value cannot apply to the property and operator
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws ObjectFilterException;

    /**
     * Similar to {@link #getObjectFilter(String, ObjectFilter.Operator, String)}, but with a collection of values.
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws ObjectFilterException;
}
