package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

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
     * Obtain the element class of this.
     *
     * The purpose of this interface is to provide introspectable properties to construct {@link ObjectFilter} and
     * {@link Sort} objects.
     */
    Class<? extends O> getElementClass();

    /**
     * Obtain the object alias used to identify {@link O} in Hibernate queries.
     *
     * Note: this can be null, in which case it refers to the root entity. We don't recommend returning null as it might
     * cause ambiguous queries, unless you implement {@link AbstractCriteriaFilteringVoEnabledDao}.
     *
     * This is used in the RESTful API to generate {@link ObjectFilter} with the correct object alias.
     */
    String getObjectAlias();
}
