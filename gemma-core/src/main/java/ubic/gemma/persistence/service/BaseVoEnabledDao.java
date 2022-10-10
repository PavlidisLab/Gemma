package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

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
     * @param entity the entity to turn into a value object
     * @return a value object
     */
    VO loadValueObject( O entity );

    VO loadValueObjectById( Long id );

    /**
     * Load value objects corresponding to entities
     *
     * @param entities the entities to turn into value objects
     * @return a collection of value objects
     */
    List<VO> loadValueObjects( Collection<O> entities );

    List<VO> loadValueObjectsByIds( Collection<Long> ids );

    List<VO> loadAllValueObjects();
}
