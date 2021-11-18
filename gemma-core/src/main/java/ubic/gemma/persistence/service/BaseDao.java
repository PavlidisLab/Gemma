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

import java.util.Collection;
import java.util.List;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <T> type
 * @author paul
 */
public interface BaseDao<T> {

    /**
     * Create all the given entities in the persistent storage.
     *
     * @param entities the entities to be crated.
     * @return collection of entities representing the instances in the persistent storage that were created.
     */
    List<T> create( Collection<T> entities );

    /**
     * Create an object. If the entity type is immutable, this may also remove any existing entities identified by an
     * appropriate 'find' method.
     *
     * @param entity the entity to create
     * @return the persistent version of the entity
     */
    T create( T entity );

    /**
     * Loads entities with given ids form the persistent storage.
     *
     * @param ids the ids of entities to be loaded. If some id's are not found, they are skipped.
     * @return collection of entities with given ids.
     */
    List<T> load( Collection<Long> ids );

    /**
     * Loads the entity with given id from the persistent storage.
     *
     * @param id the id of entity to load.
     * @return the entity with given id, or null if such entity does not exist. Note that passing a null identifier will
     * return null.
     */
    T load( Long id );

    /**
     * Loads all instanced of specific class from the persistent storage.
     *
     * @return a collection containing all instances that are currently accessible.
     */
    List<T> loadAll();

    /**
     * Counts all instances of specific class in the persitent storage.
     *
     * @return number that is the amount of instances currently accessible.
     */
    long countAll();

    /**
     * Remove all entities in the collection.
     * @param entities
     */
    void remove( Collection<T> entities );

    /**
     * Remove a persistent instance based on its id. The implementer is trusted to know what type of object to remove.
     * Note that this method is to be avoided for Securables, because it will leave cruft in the ACL tables. We may fix
     * this by having this method return the removed object.
     *
     * @param id the id of the entity to be removed
     */
    void remove( Long id );

    /**
     * Remove a persistent instance
     *
     * @param entity the entity to be removed
     */
    void remove( T entity );

    /**
     * Remove all entities from persistent storage.
     */
    void removeAll();

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
    T find( T entity );

    /**
     * Calls the find method, and if this method returns null, creates a new instance in the persistent storage.
     *
     * @param entity the entity to look for and persist if not found.
     * @return the given entity, guaranteed to be representing an entity present in the persistent storage.
     */
    T findOrCreate( T entity );

    /**
     * Thaw a list of entities.
     *
     * The implementation can mutate the collection in-place if desirable. If so, we suggest using {@link java.util.Collections#copy(List, List)}
     * to perform this operation efficiently.
     *
     * @param entities
     */
    void thaw( List<T> entities );

    /**
     * Thaw a single entity.
     * @param entity
     */
    void thaw( T entity );
}