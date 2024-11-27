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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <T> type
 * @author paul
 */
public interface BaseDao<T> {

    /**
     * Obtain the element class of {@link T}.
     */
    Class<? extends T> getElementClass();

    /**
     * Obtain the identifiable property name for {@link T}.
     */
    String getIdentifierPropertyName();

    /**
     * Crates all the given entities in the persistent storage.
     *
     * @param entities the entities to be crated.
     * @return collection of entities representing the instances in the persistent storage that were created.
     */
    @CheckReturnValue
    Collection<T> create( Collection<T> entities );

    /**
     * Create an object. If the entity type is immutable, this may also remove any existing entities identified by an
     * appropriate 'find' method.
     *
     * @param entity the entity to create
     * @return the persistent version of the entity
     */
    @CheckReturnValue
    T create( T entity );

    /**
     * Save all the given entities in the persistent storage.
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
     * Create or update an entity whether it is transient.
     * <p>
     * Unlike {@link #update(Object)}, this method does not attach the given entity to the persistence context and the
     * returned value must be used instead.
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
     */
    Collection<T> load( Collection<Long> ids );

    /**
     * Loads the entity with given id from the persistent storage.
     *
     * @param id the id of entity to load.
     * @return the entity with given ID, or null if such entity does not exist or if the passed ID was null
     *
     * @see org.hibernate.Session#get(Class, Serializable)
     */
    @Nullable
    T load( Long id );

    /**
     * Loads all instanced of specific class from the persistent storage.
     *
     * @return a collection containing all instances that are currently accessible.
     */
    Collection<T> loadAll();

    /**
     * Load references for all the given IDs.
     * <p>
     * Entities already in the session will be returned directly.
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
    @Nonnull
    T loadReference( Long id );

    /**
     * Reload an entity from the persistent storage.
     * <p>
     * This does nothing if the entity is already in the session.
     * @throws org.hibernate.ObjectNotFoundException if the entity does not exist.
     */
    @Nonnull
    T reload( T entity ) throws ObjectNotFoundException;

    /**
     * Reload an entity from the persistent storage.
     * <p>
     * This does nothing for entities already in the session.
     */
    @Nonnull
    Collection<T> reload( Collection<T> entities ) throws ObjectNotFoundException;

    /**
     * Counts all instances of specific class in the persitent storage.
     *
     * @return number that is the amount of instances currently accessible.
     */
    long countAll();

    void remove( Collection<T> entities );

    /**
     * Remove a persistent instance based on its ID.
     *
     * The implementer is trusted to know what type of object to remove.
     *
     * Note that this method is to be avoided for {@link gemma.gsec.model.Securable}, because it will leave cruft in the
     * ACL tables. We may fix this by having this method return the removed object.
     *
     * @param id the ID of the entity to be removed
     */
    void remove( Long id );

    /**
     * Remove a persistent instance
     *
     * @param entity the entity to be removed
     */
    void remove( T entity );

    /**
     * @param entities Update the entities. Not supported if the entities are immutable.
     */
    void update( Collection<T> entities );

    /**
     * @param entity Update the entity. Not supported if the entity is immutable.
     */
    void update( T entity );

    /**
     * Does a look up for the given entity in the persistent storage, usually looking for a specific identifier ( either
     * id or a string property).
     *
     * @param entity the entity to look for.
     * @return an entity that was found in the persistent storage, or null if no such entity was found.
     */
    @Nullable
    @CheckReturnValue
    T find( T entity );

    /**
     * Calls the find method, and if this method returns null, creates a new instance in the persistent storage.
     *
     * @param entity the entity to look for and persist if not found.
     * @return the given entity, guaranteed to be representing an entity present in the persistent storage.
     */
    @CheckReturnValue
    T findOrCreate( T entity );
}