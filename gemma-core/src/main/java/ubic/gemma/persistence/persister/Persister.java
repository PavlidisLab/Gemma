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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import java.util.Collection;

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
     * Persist all the objects in a collection. Non-nullable dependencies are checked and persisted first, if the
     * reference is detached, or converted into a reference to a persistent object identified by the objects business
     * key. Matching instances are not changed.
     *
     * @return The persistent versions of the objects.
     */
    @Secured({ "GROUP_USER" })
    Collection<?> persist( Collection<?> col );

    /**
     * Persist a single object. Non-nullable dependencies are checked and persisted first, if the reference is detached,
     * or converted into a reference to a persistent object identified by the objects business key. If a matching object
     * already exists, it will not be changed.
     *
     * @return the persistent version of the object.
     */
    @Secured({ "GROUP_USER" })
    Object persist( Object obj );

    /**
     * Special case for experiments.
     */
    @Secured({ "GROUP_USER" })
    ExpressionExperiment persist( ExpressionExperiment ee, ArrayDesignsForExperimentCache c );

    /**
     * Persist or update a single object. If the object already exists in the system, it will be replaced with the
     * supplied instance. This means that any existing data may be lost. Otherwise a new persistent instance will be
     * created from the supplied instance. Non-nullable dependencies will be replaced with existing persistent ones or
     * created anew: <strong>Associated objects will not be updated if they already exist</strong>. Therefore this
     * method has limited usefulness: when the provided object has new data but the associated objects are either new or
     * already existing. If you want to update associated objects you must update them explicitly (perhaps with a call
     * to persistOrUpdate on them).
     *
     * @return the persistent version of the object.
     */
    @Secured({ "GROUP_USER" })
    Object persistOrUpdate( Object obj );

    /**
     * Determine if a entity is transient (not persistent).
     *
     * @return true if the object is not (as far as we can tell) already persisted.
     */
    boolean isTransient( Object entity );

    @Secured({ "GROUP_USER" })
    ArrayDesignsForExperimentCache prepare( ExpressionExperiment entity );

}
