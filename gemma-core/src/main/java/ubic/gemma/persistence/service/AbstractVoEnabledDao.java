package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
public abstract class AbstractVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractDao<O> implements BaseVoEnabledDao<O, VO> {

    /**
     * Amount of time in milliseconds after which a query (including post-processing) should be reported.
     * <p>
     * If there is no way to perform a given query under this amount of time, consider paginating results or optimizing
     * how Hibernate entities are loaded or cached.
     */
    protected static final int REPORT_SLOW_QUERY_AFTER_MS = 1000;

    protected AbstractVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * Load a value object for a given entity.
     * <p>
     * This should be fast and efficient, and avoid any database query or post-processing. If you need to perform
     * additional queries, implement {@link #postProcessValueObjects(List)} instead.
     */
    @Nullable
    protected abstract VO doLoadValueObject( O entity );

    /**
     * Load all the value objects for the given entities.
     * <p>
     * The default is to apply {@link #doLoadValueObject(Identifiable)} on each entry and weed out null elements.
     * <p>
     * This method should be fast and any post-processing should happen in {@link #postProcessValueObjects(List)}.
     */
    public List<VO> doLoadValueObjects( Collection<O> entities ) {
        return entities.stream()
                .map( this::doLoadValueObject )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }

    /**
     * Post-process VOs in bulk.
     * <p>
     * Use this as an opportunity to load extra informations that could not be populated in the initial
     * {@link #doLoadValueObject(Identifiable)} or {@link #doLoadValueObjects(Collection)}
     */
    protected void postProcessValueObjects( List<VO> vos ) {

    }

    @Override
    public final VO loadValueObject( O entity ) {
        VO vo = doLoadValueObject( entity );
        if ( vo != null ) {
            postProcessValueObjects( Collections.singletonList( vo ) );
            return vo;
        } else {
            return null;
        }
    }

    @Override
    public final VO loadValueObjectById( Long id ) {
        O entity = load( id );
        return entity == null ? null : loadValueObject( entity );
    }

    /**
     * The default implementation calls {@link #loadValueObject(Identifiable)} for each entity and filters out nulls.
     */
    @Override
    public final List<VO> loadValueObjects( Collection<O> entities ) {
        List<VO> results = doLoadValueObjects( entities );
        postProcessValueObjects( results );
        return results;
    }

    @Override
    public final List<VO> loadValueObjectsByIds( Collection<Long> ids ) {
        return loadValueObjects( load( ids ) );
    }

    /**
     * Should be overridden for any entity that requires special handling of larger amounts of VOs.
     *
     * @return VOs of all instances of the class this DAO manages.
     */
    @Override
    public final List<VO> loadAllValueObjects() {
        return loadValueObjects( loadAll() );
    }
}
