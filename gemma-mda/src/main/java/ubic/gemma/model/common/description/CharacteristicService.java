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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

/**
 * @author paul
 * @version $Id$
 */
public interface CharacteristicService {

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public Characteristic create( Characteristic c );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void delete( java.lang.Long id );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void delete( Characteristic c );

    /**
     * <p>
     * Finds all characteristics whose parent object is of the specified class. Returns a map of characteristics to
     * parent objects.
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_VALUES_READ" })
    public Map<Characteristic, Object> findByParentClass( java.lang.Class parentClass );

    /**
     * <p>
     * Looks for an exact match of the give string to a valueUri in the characteritic database
     * </p>
     */
    public Collection<Characteristic> findByUri( java.lang.String searchString );

    /**
     * <p>
     * given a collection of strings that represent URI's will find all the characteristics that are used in the system
     * with URI's matching anyone in the given collection
     * </p>
     */
    public Collection<Characteristic> findByUri( Collection<String> uris );

    /**
     * <p>
     * Returns a collection of characteristcs that have a Value that match the given search string
     * </p>
     * <p>
     * (the value is usually a human readable form of the termURI
     * </p>
     */
    public Collection<Characteristic> findByValue( java.lang.String search );

    /**
     * <p>
     * Returns a map of the specified characteristics to their parent objects.
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_VALUES_READ" })
    public Map<Characteristic, Object> getParents( Collection<Characteristic> characteristics );

    /**
     * Browse through the characteristics, excluding GO annotations.
     * 
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve
     * @return
     */
    public List<Characteristic> browse( Integer start, Integer limit );

    /**
     * Browse through the characteristics, excluding GO annotations.
     * 
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve
     * @param sortField
     * @param descending
     * @return
     */
    public List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending );

    /**
     * @return how many Characteristics are in the system, excluding GO annotations.
     */
    public Integer count();

    /**
     * 
     */
    public Characteristic load( java.lang.Long id );

    /**
     * Note that Characteristics are not Securable. Thus make sure the current user has write access to the associated
     * object.
     * 
     * @param c
     * @see SecurityServiceImpl.isEditable
     */
    @Secured({ "GROUP_USER" })
    public void update( Characteristic c );

}
