package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;

/**
 * @author poirigui
 */
public interface FilteringService<O extends Identifiable> extends BaseService<O> {

    /**
     * @see FilteringDao#getObjectAlias()
     */
    String getObjectAlias();

    /**
     * @see FilteringDao#getObjectFilter(String, ObjectFilter.Operator, String)
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws ObjectFilterException;

    /**
     * @see FilteringDao#getObjectFilter(String, ObjectFilter.Operator, Collection)
     */
    ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws ObjectFilterException;
}
