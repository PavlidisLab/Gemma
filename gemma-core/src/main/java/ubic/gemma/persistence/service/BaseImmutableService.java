package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

/**
 * Base service class for an immutable entity.
 * <p>
 * Immutable entities can be created, deleted but never updated.
 * @author poirigui
 */
public interface BaseImmutableService<O extends Identifiable> extends BaseReadOnlyService<O> {

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
    @CheckReturnValue
    Collection<O> create( Collection<O> entities );

    /**
     * Creates the given entity in the persistent storage.
     *
     * @param entity the entity to be created.
     * @return object referencing the persistent instance of the given entity.
     */
    @CheckReturnValue
    O create( O entity );

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
}
