package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Interface VO-enabled service with filtering capabilities.
 *
 * @param <O>
 * @param <VO>
 */
public interface FilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends BaseVoEnabledService<O, VO> {

    /**
     * Loads all value objects based on the given properties.
     *
     * @param offset  see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param limit   see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param orderBy see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param asc     see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @param filter  see ubic.gemma.web.services.rest.util.WebServiceWithFiltering#all
     * @return collection of value objects.
     */
    Slice<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter );
}