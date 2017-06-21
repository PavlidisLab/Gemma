package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import java.util.Collection;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
public abstract class VoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractDao<O>
        implements BaseVoEnabledDao<O, VO> {

    protected VoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public abstract VO loadValueObject( O entity );

    @Override
    public abstract Collection<VO> loadValueObjects( Collection<O> entities );

    @Override
    public Collection<VO> loadAllValueObjects() {
        return loadValueObjects( loadAll() );
    }
}
