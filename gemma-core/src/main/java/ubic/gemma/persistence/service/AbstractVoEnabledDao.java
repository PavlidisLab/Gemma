package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
@ParametersAreNonnullByDefault
public abstract class AbstractVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractDao<O> implements BaseVoEnabledDao<O, VO> {

    /**
     * Amount of time in milliseconds after which a query (including post-processing) should be reported.
     * <p>
     * If there is no way to perform a given query under this amount of time, consider paginating results or optimizing
     * how Hibernate entities are loaded or cached.
     */
    protected static final int REPORT_SLOW_QUERY_AFTER_MS = 200;

    protected AbstractVoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * Load a value object for a given entity.
     * <p>
     * This should be fast and efficient, and avoid any database query or post-processing. If you need to perform
     * additional queries, consider overriding {@link #loadValueObject(Identifiable)} or {@link #loadValueObjects(Collection)}.
     */
    protected abstract VO doLoadValueObject( O entity );

    @Override
    public VO loadValueObject( O entity ) {
        return doLoadValueObject( entity );
    }

    @Override
    public VO loadValueObjectById( Long id ) {
        O entity = load( id );
        return entity == null ? null : doLoadValueObject( entity );
    }

    /**
     * The default implementation calls {@link #doLoadValueObject(Identifiable)} for each entity.
     */
    protected List<VO> doLoadValueObjects( Collection<O> entities ) {
        return entities.stream().map( this::doLoadValueObject ).collect( Collectors.toList() );
    }

    @Override
    public List<VO> loadValueObjects( Collection<O> entities ) {
        return doLoadValueObjects( entities );
    }

    @Override
    public List<VO> loadValueObjectsByIds( Collection<Long> ids ) {
        return doLoadValueObjects( load( ids ) );
    }

    /**
     * Should be overridden for any entity that requires special handling of larger amounts of VOs.
     *
     * @return VOs of all instances of the class this DAO manages.
     */
    @Override
    public List<VO> loadAllValueObjects() {
        return doLoadValueObjects( loadAll() );
    }
}
