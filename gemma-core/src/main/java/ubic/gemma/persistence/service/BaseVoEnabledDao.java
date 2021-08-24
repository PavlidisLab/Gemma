package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 01/06/17.
 * Interface for DAOs providing value object functionality
 *
 * @deprecated This interface is deprecated because providing generic DAOs makes little sense since once VO cannot
 * satisfy all the possible usage context. For example, we might need a different VO for the RESTful API than the
 * JavaScript frontend. Instead, the VO should be defined and created where needed.
 */
@Deprecated
public interface BaseVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends BaseDao<O> {

    /**
     * Load a value object corresponding to an entity
     *
     * @param entity the entity to turn into a value object
     * @return a value object
     */
    @Deprecated
    VO loadValueObject( O entity );

    /**
     * Load value objects corresponding to entities
     *
     * @param entities the entities to turn into value objects
     * @return a collection of value objects
     */
    @Deprecated
    List<VO> loadValueObjects( Collection<O> entities );

    @Deprecated
    List<VO> loadAllValueObjects();
}
