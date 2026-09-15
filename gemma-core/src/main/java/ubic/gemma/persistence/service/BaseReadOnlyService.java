package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Interface for read-only services.
 *
 * @author poirigui
 */
public interface BaseReadOnlyService<O extends Identifiable> {

    /**
     * @see BaseDao#getElementClass()
     */
    Class<? extends O> getElementClass();

    /**
     * @see BaseDao#find(Identifiable)
     */
    @Nullable
    @CheckReturnValue
    O find( O entity );

    /**
     * Does a search for the entity in the persistent storage, raising a {@link NullPointerException} if not found.
     *
     * @param entity the entity to be searched for
     * @return the version of entity retrieved from persistent storage
     * @throws NullPointerException if the entity is not found
     */
    @NonNull
    @CheckReturnValue
    O findOrFail( O entity ) throws NullPointerException;

    /**
     * @see BaseDao#load(Collection)
     */
    Collection<O> load( Collection<Long> ids );

    /**
     * Load multiple objects or fail with a {@link NullPointerException} if any of the objects does not exist in the
     * persistent storage.
     */
    Collection<O> loadOrFail( Collection<Long> ids ) throws NullPointerException;

    <T extends Exception> Collection<O> loadOrFail( Collection<Long> ids, Function<String, T> exceptionSupplier ) throws T;

    /**
     * @see BaseDao#load(Long)
     */
    @Nullable
    O load( Long id );

    /**
     * Load an entity of fail with a {@link NullPointerException} if it does not exist in the persistent storage.
     *
     * @param id the ID used to retrieve the entity
     * @return the entity as per {@link #load(Long)}, never null
     * @throws NullPointerException if the entity does not exist in the persistent storage
     * @see #load(Long)
     */
    @NonNull
    O loadOrFail( Long id ) throws NullPointerException;

    /**
     * Load an entity or fail with the supplied exception if it does not exist in the persistent storage.
     *
     * @throws T if the entity does not exist in the persistent storage
     * @see #load(Long)
     */
    @NonNull
    <T extends Exception> O loadOrFail( Long id, Supplier<T> exceptionSupplier ) throws T;

    /**
     * Load an entity or fail with the supplied exception if it does not exist in the persistent storage.
     * <p>
     * The message is generated automatically.
     *
     * @throws T if the entity does not exist in the persistent storage
     * @see #load(Long)
     */
    @NonNull
    <T extends Exception> O loadOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    /**
     * Load an entity or fail with the supplied exception and message.
     *
     * @throws T if the entity does not exist in the persistent storage
     * @see #load(Long)
     */
    @NonNull
    <T extends Exception> O loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    /**
     * @see BaseDao#loadAll()
     */
    Collection<O> loadAll();

    /**
     * @see BaseDao#countAll()
     */
    long countAll();

    /**
     * @see BaseDao#streamAll()
     */
    Stream<O> streamAll();

    /**
     * @see BaseDao#streamAll(boolean)
     */
    Stream<O> streamAll( boolean createNewSession );
}
