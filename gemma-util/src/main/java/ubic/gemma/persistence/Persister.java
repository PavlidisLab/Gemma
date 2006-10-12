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

import java.util.Collection;

/**
 * Interface defining the ability to create domain objects in bulk or singly. Classes that implement this interface
 * should expect:
 * <ul>
 * <li>To be passed an object graph that is valid - that is, all non-nullable properties and associations are filled
 * in.
 * <li>The objects passed might include objects that are already persistent in the system.
 * </ul>
 * 
 * @author keshav
 * @author Paul Pavlidis
 * @version $Id$
 */
public interface Persister {

    /**
     * Persist all the objects in a collection. Non-nullable dependencies are checked and persisted first, if the
     * reference is detached, or converted into a reference to a persistent object identified by the objects business
     * key.
     * 
     * @param col
     * @return The persistent versions of the objects.
     */
    public Collection<?> persist( Collection<?> col );

    /**
     * Persist a single object. Non-nullable dependencies are checked and persisted first, if the reference is detached,
     * or converted into a reference to a persistent object identified by the objects business key.
     * 
     * @param obj
     * @resutln the persistent version of the object.
     */
    public Object persist( Object obj );

}
