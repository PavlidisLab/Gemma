package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O>
 * @param <VO>
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    private final String objectAlias;

    protected AbstractFilteringVoEnabledDao( String objectAlias, Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.objectAlias = objectAlias;
    }

    @Override
    public final String getObjectAlias() {
        return objectAlias;
    }

    @Override
    public Class<O> getElementClass() {
        return elementClass;
    }

    @Override
    public final List<VO> loadValueObjects( Collection<O> entities ) {
        if ( entities.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), "id", Long.class, ObjectFilter.Operator.in, EntityUtils.getIds( entities ) ) ), null );
    }

    @Override
    public final List<VO> loadValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), "id", Long.class, ObjectFilter.Operator.in, ids ) ), null );
    }

    @Override
    public final List<VO> loadAllValueObjects() {
        return loadValueObjectsPreFilter( null, null );
    }
}