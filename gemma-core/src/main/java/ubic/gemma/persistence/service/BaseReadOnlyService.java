package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Interface for read-only services.
 * @author poirigui
 */
public interface BaseReadOnlyService<O extends Identifiable> {

    Class<? extends O> getElementClass();

    /**
     * Does a search for the entity in the persistent storage
     *
     * @param entity the entity to be searched for
     * @return the version of entity retrieved from the persistent storage, if found, otherwise null.
     */
    @Nullable
    @CheckReturnValue
    O find( O entity );

    /**
     * Does a search for the entity in the persistent storage, raising a {@link NullPointerException} if not found.
     * @param entity the entity to be searched for
     * @return the version of entity retrieved from persistent storage
     * @throws NullPointerException if the entity is not found
     */
    @Nonnull
    @CheckReturnValue
    O findOrFail( O entity ) throws NullPointerException;

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
     * Load an entity or fail with the supplied exception.
     * @throws T if the entity does not exist in the persistent storage
     */
    @Nonnull
    <T extends Exception> O loadOrFail( Long id, Supplier<T> exceptionSupplier ) throws T;

    /**
     * Load an entity or fail with the supplied exception; the message is generated automatically.
     * @throws T if the entity does not exist in the persistent storage
     */
    @Nonnull
    <T extends Exception> O loadOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    /**
     * Load an entity or fail with the supplied exception and message.
     * @throws T if the entity does not exist in the persistent storage
     */
    @Nonnull
    <T extends Exception> O loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    /**
     * Loads all the entities of specific type.
     *
     * @return collection of all entities currently available in the persistent storage.
     */
    Collection<O> loadAll();

    long countAll();

    Stream<O> streamAll();

    Stream<O> streamAll( boolean createNewSession );
}
