package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
public abstract class AbstractVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractDao<O> implements BaseVoEnabledDao<O, VO> {

    protected static final int REPORT_SLOW_QUERY_AFTER_MS = 20;

    protected AbstractVoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public abstract VO loadValueObject( O entity );

    @Override
    public List<VO> loadValueObjects( Collection<O> entities ) {
        return entities.stream().map( this::loadValueObject ).collect( Collectors.toList() );
    }

    /**
     * Should be overridden for any entity that requires special handling of larger amounts of VOs.
     *
     * @return VOs of all instances of the class this DAO manages.
     */
    @Override
    public List<VO> loadAllValueObjects() {
        return this.loadValueObjects( this.loadAll() );
    }
}
