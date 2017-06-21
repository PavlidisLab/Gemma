package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import java.util.Collection;

/**
 * Created by tesarst on 01/06/17.
 * Interface for services that provide value object functionality.
 */
public interface BaseVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseService<O> {

    /**
     * Loads a value object representing the entity with given id.
     *
     * @param entity the entity whose value object should be loaded.
     * @return value object representing the entity with matching id.
     */
    VO loadValueObject( O entity );

    /**
     * Loads value objects for all given entities.
     *
     * @param entities the entities to be converted to value objects
     * @return a collection of value objects representing he given entities.
     */
    Collection<VO> loadValueObjects( Collection<O> entities );

    /**
     * Loads value objects representing all the entities of specific type.
     *
     * @return collection
     */
    Collection<VO> loadAllValueObjects();
}
