package ubic.gemma.persistence.service.BaseReadOnlyService;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseDao;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Interface for read-only services.
 * @param <O>
 * @author poirigui
 */
public interface BaseReadOnlyService<O extends Identifiable> {

    Class<? extends O> getElementClass();

    /**
     * @see BaseDao#getIdentifierPropertyName()
     */
    String getIdentifierPropertyName();

    /**
     * Does a search for the entity in the persistent storage
     *
     * @param entity the entity to be searched for
     * @return the version of entity retrieved from the persistent storage, if found, otherwise null.
     */
    @Nullable
    @CheckReturnValue
    O find( O entity );

    @Nonnull
    @CheckReturnValue
    O findOrFail( O entity );

    /**
     * Loads objects with given ids.
     *
     * @param ids the ids of objects to be loaded.
     * @return collection containing object with given IDs.
     */
    Collection<O> load( Collection<Long> ids );

    /**
     * Loads object with given ID.
     *
     * @param id the ID of entity to be loaded.
     * @return the entity with matching ID, or null if the entity does not exist or if the passed ID was null
     */
    @Nullable
    O load( Long id );

    /**
     * Convenience for running {@link #load(Long)} and checking if the result is null.
     * @param id the ID used to retrieve the entity
     * @return the entity as per {@link #load(Long)}, never null
     * @throws NullPointerException if the entity does not exist in the persistent storage
     */
    @Nonnull
    O loadOrFail( Long id ) throws NullPointerException;

    /**
     * Loads all the entities of specific type.
     *
     * @return collection of all entities currently available in the persistent storage.
     */
    Collection<O> loadAll();

    long countAll();
}
