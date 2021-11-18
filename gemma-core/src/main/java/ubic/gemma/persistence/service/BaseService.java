package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import java.util.Collection;
import java.util.List;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <O> the Object type that this service is handling.
 * @author tesarst
 */
public interface BaseService<O extends Identifiable> {

    /**
     * Does a search for the entity in the persistent storage
     *
     * @param entity the entity to be searched for
     * @return the version of entity retrieved from the persistent storage, if found.
     */
    O find( O entity );

    /**
     * Does a search for the entity in the persistent storage, and if not found, creates it.
     *
     * @param entity the entity to look for, and create if not found.
     * @return the entity retrieved from the persistent storage, either found or created.
     */
    O findOrCreate( O entity );

    /**
     * Creates all the given entities in a persistent storage
     *
     * @param entities the entities to be created.
     * @return collection of objects referencing the persistent instances of given entities.
     */
    @SuppressWarnings("unused")
    // Consistency
    List<O> create( Collection<O> entities );

    /**
     * Creates the given entity in the persistent storage.
     *
     * @param entity the entity to be created.
     * @return object referencing the persistent instance of the given entity.
     */
    O create( O entity );

    /**
     * Loads objects with given ids.
     *
     * @param ids the ids of objects to be loaded.
     * @return collection containing object with given ids.
     */
    List<O> load( Collection<Long> ids );

    /**
     * Loads object with given id.
     *
     * @param id the id of entity to be loaded.
     * @return the entity with matching id, or null if the entity does not exist.
     */
    O load( Long id );

    /**
     * Load and thaw using {@link BaseDao#thaw(List)} in a single transaction.
     * @param ids
     * @return
     */
    List<O> loadAndThaw( Collection<Long> ids );

    /**
     * Load and thaw using {@link BaseDao#thaw(Object)} in a single transaction.
     * @param id
     * @return
     */
    O loadAndThaw( Long id );

    /**
     * Loads all the entities of specific type.
     *
     * @return collection of all entities currently available in the persistent storage.
     */
    List<O> loadAll();

    long countAll();

    /**
     * Removes all the given entities from persistent storage.
     *
     * @param entities the entities to be removed.
     */
    void remove( Collection<O> entities );

    /**
     * Removes the entity with given id from the persistent storage.
     *
     * @param id the id of entity to be removed.
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
    void removeAll();

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

    /**
     * Reload and thaw a collection of entities.
     * @deprecated use {@link #loadAndThaw(Collection)} instead, this is inefficient as it will reload the entities from
     *             the database to ensure that the loading and thawing operations are performed in the same Hibernate
     *             session.
     */
    @Deprecated
    List<O> thaw( Collection<O> entities );

    /**
     * Reload and thaw an entity.
     * @deprecated use {@link #loadAndThaw(Long)} instead, this is inefficient as it will reload the entity from the
     *             database to ensure that the loading and thawing operations are performed in the same Hibernate
     *             session.
     */
    @Deprecated
    O thaw( O entity );
}