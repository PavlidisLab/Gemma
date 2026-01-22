package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <O> the Object type that this service is handling.
 * @author tesarst
 */
public interface BaseService<O extends Identifiable> extends BaseImmutableService<O> {

    /**
     * @see BaseDao#save(Collection)
     */
    @CheckReturnValue
    Collection<O> save( Collection<O> entities );

    /**
     * @see BaseDao#save(Identifiable)
     */
    @CheckReturnValue
    O save( O entity );

    /**
     * Updates all entities in the given collection in the persistent storage.
     *
     * @param entities the entities to be updated.
     * @see BaseDao#update(Collection)
     */
    void update( Collection<O> entities );

    /**
     * Updates the given entity in the persistent storage.
     *
     * @param entity the entity to be updated.
     * @see BaseDao#update(Identifiable)
     */
    void update( O entity );
}