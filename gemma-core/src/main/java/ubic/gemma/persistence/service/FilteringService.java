package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @see FilteringDao
 * @author poirigui
 */
public interface FilteringService<O extends Identifiable> extends BaseService<O> {

    /**
     * Obtain an {@link ObjectFilter} for the entity this DAO is providing.
     *
     * It is the responsibility of the DAO to infer the ObjectFilter property type as well as the alias to use.
     *
     * If the ObjectFilter refers to a related entity (i.e. a {@link ubic.gemma.model.expression.bioAssay.BioAssay} of an
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}), then the DAO should inject the corresponding
     * DAO and delegate it the work of creating the ObjectFilter.
     *
     * In the frontend, that correspond to a FilterArg, but since the definition is not available here, we unpack its
     * attributes.
     *
     * @param property property name in the entity use as a left-hand side of the operator
     * @param operator an operator
     * @param value the corresponding, unparsed value, to the right-hand side of the operator
     * @return an object filter filled with the object alias, property, inferred type, operator and parsed value
     * @throws IllegalArgumentException if the property does not exist in {@link O}, or if the operator cannot be applied,
     * or if the value cannot apply to the property an operator see {@link ObjectFilter#parseObjectFilter(String, String, Class, ObjectFilter.Operator, String)}
     * for more details
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * Similar to {@link #getObjectFilter(String, ObjectFilter.Operator, String)}, but with a collection of values.
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * Obtain a {@link Sort} object for a property of the {@link O}.
     *
     * @param property a property of {@link O} to sort by
     * @param direction a sorting direction, or null if the default direction applies
     * @return a {@link Sort} object that can be used, for example, on {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}
     * @throws IllegalArgumentException if no such field exists in {@link O}
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;
}
