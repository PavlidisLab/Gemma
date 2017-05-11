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
package ubic.gemma.persistence.service.common.measurement;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.measurement.Unit;

/**
 * @author paul
 * @version $Id$
 */
public interface UnitService {

    /**
     * Creates a new instance of ubic.gemma.model.common.measurement.Unit and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.common.measurement.Unit instances to create.
     * @return the created instances.
     */
    @Secured( { "GROUP_USER" })
    public java.util.Collection<Unit> create( java.util.Collection<Unit> entities );

    /**
     * Creates an instance of ubic.gemma.model.common.measurement.Unit and adds it to the persistent store.
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.common.measurement.Unit create( ubic.gemma.model.common.measurement.Unit unit );

    public Unit find( Unit unit );

    @Secured( { "GROUP_USER" })
    public Unit findOrCreate( Unit unit );

    /**
     * Loads an instance of ubic.gemma.model.common.measurement.Unit from the persistent store.
     */
    public ubic.gemma.model.common.measurement.Unit load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.common.measurement.Unit}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection<Unit> loadAll();

    /**
     * Removes the instance of ubic.gemma.model.common.measurement.Unit having the given <code>identifier</code> from
     * the persistent store.
     */
    @Secured( { "GROUP_ADMIN" })
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    @Secured( { "GROUP_ADMIN" })
    public void remove( java.util.Collection<Unit> entities );

    /**
     * Removes the instance of ubic.gemma.model.common.measurement.Unit from the persistent store.
     */
    @Secured( { "GROUP_USER" })
    public void remove( ubic.gemma.model.common.measurement.Unit unit );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    @Secured( { "GROUP_USER" })
    public void update( java.util.Collection<Unit> entities );

    /**
     * Updates the <code>unit</code> instance in the persistent store.
     */
    @Secured( { "GROUP_USER" })
    public void update( ubic.gemma.model.common.measurement.Unit unit );

}
