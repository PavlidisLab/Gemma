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
package ubic.gemma.persistence.persister;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;

/**
 * Interface defining the ability to create domain objects in bulk or singly. Classes that implement this interface
 * should expect:
 * <ul>
 * <li>To be passed an object graph that is valid - that is, all non-nullable properties and associations are filled in.
 * <li>The objects passed might include objects that are already persistent in the system.
 * </ul>
 *
 * @author keshav
 * @author Paul Pavlidis
 */
public interface Persister {

    /**
     * Persist a single object. Non-nullable dependencies are checked and persisted first, if the reference is detached,
     * or converted into a reference to a persistent object identified by the objects business key. If a matching object
     * already exists, it will not be changed.
     *
     * @param obj the object
     * @return the persistent version of the object.
     */
    @Secured({ "GROUP_USER" })
    @CheckReturnValue
    <T extends Identifiable> T persist( T obj );

    /**
     * Persist all the objects in a collection. Non-nullable dependencies are checked and persisted first, if the
     * reference is detached, or converted into a reference to a persistent object identified by the objects business
     * key. Matching instances are not changed.
     *
     * @param col the collection of objects
     * @return The persistent versions of the objects.
     */
    @Secured({ "GROUP_USER" })
    @CheckReturnValue
    <T extends Identifiable> List<T> persist( Collection<T> col );

    /**
     * Persist or update a single object. If the object already exists in the system, it will be replaced with the
     * supplied instance. This means that any existing data may be lost. Otherwise a new persistent instance will be
     * created from the supplied instance. Non-nullable dependencies will be replaced with existing persistent ones or
     * created anew: <strong>Associated objects will not be updated if they already exist</strong>. Therefore this
     * method has limited usefulness: when the provided object has new data but the associated objects are either new or
     * already existing. If you want to update associated objects you must update them explicitly (perhaps with a call
     * to persistOrUpdate on them).
     *
     * @param obj the object
     * @return the persistent version of the object.
     */
    @Secured({ "GROUP_USER" })
    @CheckReturnValue
    <T extends Identifiable> T persistOrUpdate( T obj );
}
