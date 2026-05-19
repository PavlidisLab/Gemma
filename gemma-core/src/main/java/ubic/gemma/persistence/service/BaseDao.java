/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service;

import org.hibernate.ObjectNotFoundException;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <T> type
 * @author paul
 */
public interface BaseDao<T extends Identifiable> {

    /**
     * Obtain the element class of {@link T}.
     */
    Class<? extends T> getElementClass();

    /**
     * Create all the given entities in the persistent storage.
     *
     * @param entities the entities to be crated.
     * @return collection of entities representing the entities in the persistent storage that were created.
     */
    @CheckReturnValue
    Collection<T> create( Collection<T> entities );

    /**
     * Create an entity in the persistent storage.
     *
     * @param entity the entity to create
     * @return the persistent version of the entity
     * @see org.hibernate.Session#persist(Object)
     */
    @CheckReturnValue
    T create( T entity );

    /**
     * Create (if transient) or update all the given entities in the persistent storage.
     * <p>
     * Unlike {@link #update(Collection)}, this method does not attach the given entities to the persistence context;
     * the returned values must be used instead.
     *
     * @see org.hibernate.Session#persist(Object)
     * @see org.hibernate.Session#merge(Object)
     */
    @CheckReturnValue
    Collection<T> save( Collection<T> entities );

    /**
     * Create (if transient) or update an entity.
     * <p>
     * Unlike {@link #update(Identifiable)}, this method does not attach the given entity to the persistence context and
     * the returned value must be used instead.
     *
     * @see org.hibernate.Session#persist(Object)
     * @see org.hibernate.Session#merge(Object)
     */
    @CheckReturnValue
    T save( T entity );

    /**
     * Loads entities with given ids form the persistent storage.
     *
     * @param ids the IDs of entities to be loaded. If some IDs are not found or null, they are skipped.
     * @return collection of entities with given ids.
     * @see org.hibernate.Session#get(Class, Serializable)
     */
    Collection<T> load( Collection<Long> ids );

    /**
     * Loads the entity with given id from the persistent storage.
     *
     * @param id the id of entity to load.
     * @return the entity with given ID, or null if such entity does not exist or if the passed ID was null
     * @see org.hibernate.Session#get(Class, Serializable)
     */
    @Nullable
    T load( Long id );

    /**
     * Loads all entities of type {@link T} from the persistent storage.
     *
     * @return a collection containing all entities that are currently accessible.
     */
    Collection<T> loadAll();

    /**
     * Load references of entities of type {@link T} for all the given IDs.
     * <p>
     * Entities already in the current session will be returned directly.
     */
    Collection<T> loadReference( Collection<Long> ids );

    /**
     * Load reference for an entity.
     * <p>
     * If the entity is already in the session, it will be returned instead. Note that unlike {@link #load(Long)}, this
     * method will not return null if the entity does not exist.
     * <p>
     * You may freely access the {@link Identifiable#getId()} field without triggering proxy initialization.
     *
     * @see org.hibernate.Session#load(Object, Serializable)
     */
    @NonNull
    T loadReference( Long id );

    /**
     * Reload an entity from the persistent storage.
     * <p>
     * This does nothing if the entity is already in the session.
     *
     * @throws org.hibernate.ObjectNotFoundException if the entity does not exist.
     */
    @NonNull
    T reload( T entity ) throws ObjectNotFoundException;

    /**
     * Reload an entity from the persistent storage.
     * <p>
     * This does nothing for entities already in the session.
     */
    @NonNull
    Collection<T> reload( Collection<T> entities ) throws ObjectNotFoundException;

    /**
     * Counts all entities of {@link T} in the persistent storage.
     *
     * @return number that is the amount of entities currently accessible.
     */
    long countAll();

    /**
     * Stream all entities of {@link T} from the persistent storage.
     * <p>
     * Note that the stream will only be valid while the current database session is active. To create a stream that can
     * outlive the current session, use {@link #streamAll(boolean)} passing {@code true}.
     */
    Stream<T> streamAll();

    /**
     * Stream all entities of type {@link T} from the persistent storage.
     * <p>
     * If creating a new session, the caller is responsible for closing the stream to avoid resource leaks. It is
     * recommended to use try-with-resource statement.
     *
     * @param createNewSession whether to create a new session for the stream, it will be closed when the stream is
     *                         closed
     */
    Stream<T> streamAll( boolean createNewSession );

    /**
     * Remove all given persistent entities.
     */
    void remove( Collection<T> entities );

    /**
     * Remove a persistent entity.
     *
     * @param entity the entity to be removed
     */
    void remove( T entity );

    /**
     * Update the given entities.
     * <p>
     * Not supported if the entity is immutable or loaded in read-only mode.
     */
    void update( Collection<T> entities );

    /**
     * Update the given entity.
     * <p>
     * Not supported if the entity is immutable or loaded in read-only mode.
     */
    void update( T entity );

    /**
     * Does a look-up for the given entity in the persistent storage, usually looking for a specific identifier if
     * {@link Identifiable#getId()} is set or a business key.
     *
     * @param entity the entity to look for.
     * @return an entity that was found in the persistent storage, or null if no such entity was found.
     */
    @Nullable
    @CheckReturnValue
    T find( T entity );

    /**
     * Calls the {@link #find(Identifiable)} method, and if this method returns null, creates a new entity in the
     * persistent storage pas per {@link #create(Identifiable)}.
     *
     * @param entity the entity to look for and persist if not found.
     * @return the given entity, guaranteed to be representing an entity present in the persistent storage.
     */
    @CheckReturnValue
    T findOrCreate( T entity );
}