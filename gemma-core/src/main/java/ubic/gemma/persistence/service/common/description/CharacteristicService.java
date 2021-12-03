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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.FilteringService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author paul
 */
public interface CharacteristicService extends BaseVoEnabledService<Characteristic, CharacteristicValueObject>, FilteringService<Characteristic> {

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start How far into the list to start
     * @param  limit Maximum records to retrieve
     * @return       characteristics
     */
    List<Characteristic> browse( Integer start, Integer limit );

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start      How far into the list to start
     * @param  limit      Maximum records to retrieve
     * @param  sortField  sort field
     * @param  descending sor order
     * @return            characteristics
     */
    List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending );

    /**
     * This search looks at direct annotations, factor values and biomaterials in that order. Duplicate EEs are
     * avoided (and will thus be associated via the first uri that resulted in a hit).
     * 
     * @param  uriString uri strings
     * @param  t         taxon to limit to; null for no limit
     * @param  limit     try to limit to this many results. No guarantee the limit won't be exceeded. Negative values
     *                   implies no limit.
     * @return           map of classes (Experiment, FactorValue, BioMaterial) to the URI to the IDs of experiments
     *                   which have an
     *                   associated characteristic using the given uriString. The class lets us track where the
     *                   annotation was.
     */
    Map<Class<?>, Map<String, Collection<Long>>> findExperimentsByUris( Collection<String> uriString, Taxon t, int limit );

    /**
     * given a collection of strings that represent URI's will find all the characteristics that are used in the system
     * with URI's matching anyone in the given collection
     *
     * @param  uris uris
     * @return      characteristics
     */
    Collection<Characteristic> findByUri( Collection<String> uris );

    /**
     * Looks for an exact match of the give string to a valueUri in the characteristic database
     *
     * @param  searchString search string
     * @return              characteristics
     */
    Collection<Characteristic> findByUri( String searchString );

    /**
     * Returns a collection of characteristics that have a Value that match the given search string. The value is
     * usually a human readable form of the termURI
     *
     * @param  search search
     * @return        characteristics
     */
    Collection<Characteristic> findByValue( String search );

    /**
     * @param  characteristics characteristics
     * @return                 a map of the specified characteristics to their annotated objects.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_VALUES_READ" })
    Map<Characteristic, Object> getParents( Collection<Characteristic> characteristics );

    /**
     * @param  characteristics characteristics
     * @param  classes         classes
     * @return                 a map of the specified characteristics to their parent objects, constrained to be among
     *                         the classes
     *                         given.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_VALUES_READ" })
    Map<Characteristic, Object> getParents( Collection<Class<?>> classes, Collection<Characteristic> characteristics );

    //    /**
    //     * @param classes constraint
    //     * @param string  string value
    //     * @return characteristics
    //     */
    //    Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string );

    Collection<? extends Characteristic> findByCategory( String queryPrefix );

    @Override
    @Secured({ "GROUP_USER" })
    Characteristic create( Characteristic c );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Characteristic c );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Characteristic c );

    /**
     * Optimized version that only retrieves the IDs of the owning object. The caller has to keep track of the
     * parentClass
     * 
     * @param  parentClass     the type of object sought associated with the characteristic
     * @param  characteristics
     * @return
     */
    Map<Characteristic, Long> getParentIds( Class<?> parentClass, Collection<Characteristic> characteristics );

}
