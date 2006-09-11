/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence;

import org.springframework.dao.DataAccessException;

/**
 * Class to handle many crud operations on complex Gemma-domain entities (reading is only supported using 'find(entity)'
 * methods).
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface CrudHelper<E> {

    /**
     * Locate an object in the system, based on the business key from the template entity passed in.
     * 
     * @param entity
     * @return
     * @throws DataAccessException if the entity passed in is already persistent.
     */
    public E find( E entity ) throws DataAccessException;

    /**
     * Make a new entity persistent, checking the business key first. If an entity is passed that matches a object in
     * the database, the persistent version will be returned. Any data in the existing versions <em>not</em> be
     * updated. Otherwise, the entity will be created. Any cascading behavior resulting from the 'create' is not
     * directly addressed by this method but is assumed to be handled by the relational manager - associations are not
     * checked.
     * 
     * @param entity
     * @return
     * @throws DataAccessException
     */
    public E findOrCreate( E entity ) throws DataAccessException;

    /**
     * Make a new entity persistent, checking the business key first. If an entity is passed that matches a object in
     * the database, the persistent version will be returned. Any data in the existing versions <em>will</em> be
     * updated with data from the version that was passed in. Otherwise, the entity will be created.
     * <p>
     * Any cascading behavior resulting from the 'create' is not directly addressed by this method but is assumed to be
     * handled by the relational manager - associations are not checked. If the entity passed in is associated with
     * transient objects, they will not be persisted unless there is a back-end cascade. Therefore non-compositional
     * associations should be passed in as already persisted. If you don't want this behavior, use findOrCreate (which
     * doesn't do an update), and fill in the associations manually.
     * <p>
     * If you want the associations checked and persisted explicitly, use cascadeCreateOrUpdate.
     * 
     * @param entity
     * @return
     * @throws DataAccessException
     */
    public E createOrUpdate( E entity ) throws DataAccessException;

    /**
     * Make a new entity persistent, including all of its associated objects, checking all business keys first to make
     * sure the same data isn't inserted twice. If an entity (or associated object) is encountered that matches a object
     * in the database, it will be replaced with the persistent version. Any data in the existing versions will be
     * overwritten.
     * 
     * @param entity
     * @return
     * @throws DataAccessException If the entity passed is already persistent
     */
    public E cascadeCreateOrUpdate( E entity ) throws DataAccessException;

    /**
     * Make a new entity persistent, including all of its associated objects, but business keys are <em>not</em>
     * checked. It is assumed that the same data does not already exist in the system. Any uniqueness constraints this
     * involves in are not directly addressed by this method, they are assumed to be handled by the back end (e.g.,
     * Hibernate).
     * <p>
     * This method should only be used if you are sure the data is new in the system.
     * 
     * @param entity
     * @return
     * @throws IllegalArgumentException If the entity passed is already persistent, or any of the associated objects are
     *         persistent
     */
    public E strictCreate( E entity ) throws IllegalArgumentException;

    /**
     * Update the state of a persistent entity, including all of its associated objects. If any of the associated
     * objects are not persistent, they are made persistent. The state of the existing objects are overwritten as in
     * normal update.
     * 
     * @param entity
     * @throws DataAccessException If the entity that is passed in is not persistent
     */
    public void cascadeUpdate( E entity ) throws DataAccessException;

    /**
     * Update the state of a persistent entity, including all of its associated objects, all of which must already be
     * persistent. The state of the existing objects are overwritten as in normal update.
     * <p>
     * If you have updated a collection associated with the entity (one-to-many for example), this method will not
     * result in the collection being updated unless this is handled by the relational backend (e.g., Hibernate).
     * Instead use
     * 
     * @param entity
     * @throws DataAccessException If any of the associated objects are not persistent
     */
    public void strictUpdate( E entity ) throws DataAccessException;

    /**
     * Delete an entity from the persistent store. Associated objects are not explicitly altered unless they maintain a
     * reference to the entity that would result in a foreign key constraint violation. Any cascading behavior is not
     * directly addressed by this method but is assumed to be handled by the back end (e.g., Hibernate).
     * 
     * @param entity
     * @throws IllegalArgumentException if the entity is not persistent.
     */
    public void delete( E entity ) throws IllegalArgumentException;

}
