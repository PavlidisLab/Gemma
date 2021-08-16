package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.NotYetImplementedException;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
@Deprecated
public abstract class AbstractVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractDao<O> implements BaseVoEnabledDao<O, VO> {

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

    /**
     * Should be overridden for any entity that is expected to have pre-filtered VOs available
     *
     * @param  filter  see this#formRestrictionClause(ArrayList)
     * @param  limit   limit
     * @param  asc     ordering asc? false for desc
     * @param  offset  offset
     * @param  orderBy order by property
     * @return a collection of VOs that are guaranteed to be filtered and ordered by the input parameters
     *                 without the
     *                 need to
     *                 further be checked by ACLs.
     */
    @Override
    public List<VO> loadValueObjectsPreFilter( int offset, int limit, String orderBy, boolean asc,
            List<ObjectFilter[]> filter ) {
        throw new NotYetImplementedException( "This entity does not have pre-filtered VO retrieval implemented yet" );
    }
}
