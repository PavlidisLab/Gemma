package ubic.gemma.persistence.service;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by tesarst on 01/06/17.
 * Interface for DAOs providing value object functionality
 */
public interface BaseVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends BaseDao<O> {

    VO loadValueObject( O entity );

    Collection<VO> loadValueObjects( Collection<O> entities );

    Collection<VO> loadAllValueObjects();

    Collection<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            ArrayList<ObjectFilter[]> filter );
}
