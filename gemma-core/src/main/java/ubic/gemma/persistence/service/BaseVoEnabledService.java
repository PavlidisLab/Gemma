package ubic.gemma.persistence.service;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 01/06/17.
 * Interface for services that provide value object functionality.
 */
public interface BaseVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseReadOnlyService<O> {

    /**
     * @see BaseVoEnabledDao#loadValueObject(Identifiable)
     */
    @Nullable
    VO loadValueObject( O entity );

    /**
     * @see BaseVoEnabledDao#loadValueObjectById(Long)
     */
    @Nullable
    VO loadValueObjectById( Long entityId );

    /**
     * Loads value objects for all given entities.
     *
     * @param  entities the entities to be converted to value objects
     * @return          a collection of value objects representing he given entities.
     */
    List<VO> loadValueObjects( Collection<O> entities );

    /**
     * Load value objects by a given collection of IDs.
     */
    List<VO> loadValueObjectsByIds( Collection<Long> entityIds );

    /**
     * Loads value objects representing all the entities of specific type.
     *
     * @return a collection of value objects
     */
    List<VO> loadAllValueObjects();
}
