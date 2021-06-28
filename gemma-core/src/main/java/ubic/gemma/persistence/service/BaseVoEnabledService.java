package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;

/**
 * Created by tesarst on 01/06/17.
 * Interface for services that provide value object functionality.
 *
 * @deprecated This interface is deprecated because providing generic DAOs makes little sense since once VO cannot
 * satisfy all the possible usage context. For example, we might need a different VO for the RESTful API than the
 * JavaScript frontend. Instead, the VO should be defined and created where needed.
 */
@Deprecated
public interface BaseVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseService<O> {

    /**
     * Loads a value object representing the entity with given id.
     *
     * @param  entity the entity whose value object should be loaded.
     * @return        value object representing the entity with matching id.
     */
    @Deprecated
    VO loadValueObject( O entity );

    /**
     * Loads value objects for all given entities.
     *
     * @param  entities the entities to be converted to value objects
     * @return          a collection of value objects representing he given entities.
     */
    @Deprecated
    Collection<VO> loadValueObjects( Collection<O> entities );

    /**
     * Loads value objects representing all the entities of specific type.
     *
     * @return a collection of value objects
     */
    @Deprecated
    Collection<VO> loadAllValueObjects();

    /**
     * Loads all value objects based on the given properties.
     *
     * @param  offset  see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param  limit   see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param  orderBy see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param  asc     see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param  filter  see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @return         collection of value objects.
     */
    @Deprecated
    Collection<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter );
}
