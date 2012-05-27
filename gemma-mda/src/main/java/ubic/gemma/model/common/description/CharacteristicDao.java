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

import ubic.gemma.persistence.BrowsingDao;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 * @version $Id$
 */
public interface CharacteristicDao extends BrowsingDao<Characteristic> {

    /**
     * Finds all characteristics whose parent object is of the specified class. Returns a map of characteristics to
     * parent objects.
     */
    public Map<Characteristic, Collection<?>> findByParentClass( java.lang.Class<?> parentClass );

    /**
     * 
     */
    public Collection<Characteristic> findByUri( java.lang.String searchString );

    /**
     * 
     */
    public Collection<Characteristic> findByUri( Collection<String> uris );

    /**
     * <p>
     * Finds all Characteristics whose value match the given search term
     * </p>
     */
    public Collection<Characteristic> findByValue( java.lang.String search );

    /**
     * <p>
     * Returns a map of the specified characteristics to their parent objects.
     * </p>
     */
    public Map getParents( java.lang.Class<?> parentClass, Collection<Characteristic> characteristics );

    /**
     * Browse through the characteristics, excluding GO annotations.
     * 
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve (might be subject to security filtering)
     */
    @Override
    public List<Characteristic> browse( Integer start, Integer limit );

    /**
     * Browse through the characteristics, excluding GO annotations, with sorting.
     * 
     * @param start
     * @param limit
     * @param sortField
     * @param descending
     * @return
     */
    @Override
    public List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending );

    /**
     * @return how many Characteristics are in the system, excluding GO annotations.
     */
    @Override
    public Integer count();

}
