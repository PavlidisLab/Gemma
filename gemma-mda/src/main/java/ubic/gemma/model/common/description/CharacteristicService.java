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
package ubic.gemma.model.common.description;

import org.springframework.security.access.annotation.Secured;

/**
 * @author paul
 * @version $Id$
 */
public interface CharacteristicService {

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.common.description.Characteristic create(
            ubic.gemma.model.common.description.Characteristic c );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void delete( java.lang.Long id );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void delete( ubic.gemma.model.common.description.Characteristic c );

    /**
     * <p>
     * Finds all characteristics whose parent object is of the specified class. Returns a map of characteristics to
     * parent objects.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<Characteristic, Object> findByParentClass( java.lang.Class parentClass );

    /**
     * <p>
     * Looks for an exact match of the give string to a valueUri in the characteritic database
     * </p>
     */
    public java.util.Collection<Characteristic> findByUri( java.lang.String searchString );

    /**
     * <p>
     * given a collection of strings that represent URI's will find all the characteristics that are used in the system
     * with URI's matching anyone in the given collection
     * </p>
     */
    public java.util.Collection<Characteristic> findByUri( java.util.Collection<String> uris );

    /**
     * <p>
     * Returns a collection of characteristcs that have a Value that match the given search string
     * </p>
     * <p>
     * (the value is usually a human readable form of the termURI
     * </p>
     */
    public java.util.Collection<Characteristic> findByValue( java.lang.String search );

    /**
     * <p>
     * Returns the parent object of the specified Characteristic.
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public java.lang.Object getParent( ubic.gemma.model.common.description.Characteristic characteristic );

    /**
     * <p>
     * Returns a map of the specified characteristics to their parent objects.
     * </p>
     */
    public java.util.Map<Characteristic, Object> getParents( java.util.Collection<Characteristic> characteristics );

    /**
     * 
     */
    public ubic.gemma.model.common.description.Characteristic load( java.lang.Long id );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void update( ubic.gemma.model.common.description.Characteristic c );

}
