package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseReadOnlyService.BaseReadOnlyService;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <O> the Object type that this service is handling.
 * @author tesarst
 */
public interface BaseService<O extends Identifiable> extends BaseReadOnlyService<O> {

    /**
     * Does a search for the entity in the persistent storage, and if not found, creates it.
     *
     * @param entity the entity to look for, and create if not found.
     * @return the entity retrieved from the persistent storage, either found or created.
     */
    @CheckReturnValue
    O findOrCreate( O entity );

    /**
     * Creates all the given entities in a persistent storage
     *
     * @param entities the entities to be created.
     * @return collection of objects referencing the persistent instances of given entities.
     */
    @SuppressWarnings("unused")
    // Consistency
    Collection<O> create( Collection<O> entities );

    /**
     * Creates the given entity in the persistent storage.
     *
     * @param entity the entity to be created.
     * @return object referencing the persistent instance of the given entity.
     */
    O create( O entity );

    /**
     * @see BaseDao#save(Collection)
     */
    Collection<O> save( Collection<O> entities );

    /**
     * @see BaseDao#save(Object)
     */
    O save( O entity );

    /**
     * Removes all the given entities from persistent storage.
     *
     * @param entities the entities to be removed.
     */
    void remove( Collection<O> entities );

    /**
     * Removes the entity with given ID from the persistent storage.
     *
     * @param id the ID of entity to be removed.
     */
    void remove( Long id );

    /**
     * Removes the given entity from the persistent storage.
     *
     * @param entity the entity to be removed.
     */
    void remove( O entity );

    /**
     * Remove all entities from the persistent storage.
     */
    void removeAllInBatch();

    /**
     * Updates all entities in the given collection in the persistent storage.
     *
     * @param entities the entities to be updated.
     */
    void update( Collection<O> entities );

    /**
     * Updates the given entity in the persistent storage.
     *
     * @param entity the entity to be updated.
     */
    void update( O entity );
}