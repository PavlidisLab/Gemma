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
package ubic.gemma.persistence.service.common.description;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;
import ubic.gemma.persistence.service.BrowsingDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 */
public interface CharacteristicDao
        extends BrowsingDao<Characteristic>, BaseVoEnabledDao<Characteristic, CharacteristicValueObject> {

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve (might be subject to security filtering)
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( Integer start, Integer limit );

    /**
     * Browse through the characteristics, excluding GO annotations, with sorting.
     *
     * @param start      query offset
     * @param limit      maximum amount of entries
     * @param descending order direction
     * @param sortField  order field
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending );

    Collection<? extends Characteristic> findByCategory( String query );

    /**
     * @param classes            constraint of who the 'owner' of the Characteristic has to be.
     * @param characteristicUris uris
     * @return characteristics
     */
    Collection<Characteristic> findByUri( Collection<Class<?>> classes, Collection<String> characteristicUris );

    /**
     * @param classesToFilterOn constraint of who the 'owner' of the Characteristic has to be.
     * @param uriString         uri string
     * @return characteristics
     */
    Collection<Characteristic> findByUri( Collection<Class<?>> classesToFilterOn, String uriString );

    Collection<Characteristic> findByUri( Collection<String> uris );

    Collection<Characteristic> findByUri( String searchString );

    /**
     * @param classes constraint of who the 'owner' of the Characteristic has to be.
     * @param string  value
     * @return characteristics
     */
    Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string );

    /**
     * Finds all Characteristics whose value match the given search term
     *
     * @param search search
     * @return characteristics
     */
    Collection<Characteristic> findByValue( String search );

    /**
     * @param characteristics characteristics
     * @param parentClass     parent class
     * @return a map of the specified characteristics to their parent objects.
     */
    Map<Characteristic, Object> getParents( Class<?> parentClass, Collection<Characteristic> characteristics );

}
