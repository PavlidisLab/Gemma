package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 01/06/17.
 * Interface for DAOs providing value object functionality
 */
public interface BaseVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends BaseDao<O> {

    /**
     * Load a value object corresponding to an entity
     * 
     * @param entity
     * @return
     */
    VO loadValueObject( O entity );

    /**
     * Load value objects corresponding to entities 
     * @param entities
     * @return
     */
    Collection<VO> loadValueObjects( Collection<O> entities );

    /**
     * 
     * @return
     */
    Collection<VO> loadAllValueObjects();

    /**
     * 
     * @param offset
     * @param limit
     * @param orderBy
     * @param asc
     * @param filter
     * @return
     */
    Collection<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter );
}
