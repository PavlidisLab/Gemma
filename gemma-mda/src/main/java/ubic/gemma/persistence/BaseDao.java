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
package ubic.gemma.persistence;

import java.util.Collection;

/**
 * Interface that supports basic CRUD operations.
 *
 * @param <T>
 * @author paul
 */
public interface BaseDao<T> {

    Collection<? extends T> create( Collection<? extends T> entities );

    /**
     * Create an object. If the entity type is immutable, this may also delete any existing entities identified by an
     * appropriate 'find' method.
     */
    T create( T entity );

    Collection<? extends T> load( Collection<Long> ids );

    T load( Long id );

    Collection<? extends T> loadAll();

    void remove( Collection<? extends T> entities );

    /**
     * Remove a persistent instance based on its id. The implementer is trusted to know what type of object to remove.
     * Note that this method is to be avoided for Securables, because it will leave cruft in the ACL tables. We may fix
     * this by having this method return the removed object.
     */
    void remove( Long id );

    /**
     * Remove a persistent instance
     */
    void remove( T entity );

    /**
     * Update the entities. Not supported if the entities are immutable.
     */
    void update( Collection<? extends T> entities );

    /**
     * Update the entity. Not supported if the entity is immutable.
     */
    void update( T entity );

}