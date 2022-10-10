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

import lombok.Data;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;
import ubic.gemma.persistence.service.BrowsingDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 */
public interface CharacteristicDao
        extends BrowsingDao<Characteristic>, BaseVoEnabledDao<Characteristic, CharacteristicValueObject> {

    String OBJECT_ALIAS = "ch";

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start How far into the list to start
     * @param  limit Maximum records to retrieve (might be subject to security filtering)
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( Integer start, Integer limit );

    /**
     * Browse through the characteristics, excluding GO annotations, with sorting.
     *
     * @param  start      query offset
     * @param  limit      maximum amount of entries
     * @param  descending order direction
     * @param  sortField  order field
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending );

    Collection<? extends Characteristic> findByCategory( String query );

    /**
     * @param  classes            constraint of who the 'owner' of the Characteristic has to be.
     * @param  characteristicUris uris
     * @return characteristics
     */
    Collection<Characteristic> findByUri( Collection<Class<?>> classes, @Nullable Collection<String> characteristicUris );

    /**
     * This search looks at direct annotations, factor values and biomaterials in that order. Duplicate EEs are avoided
     * (and will thus be associated via the first uri that resulted in a hit).
     * <p>
     * Resulting EEs are filtered by ACLs.
     *
     * @param  uris       collection of URIs used for matching characteristics (via {@link Characteristic#getValueUri()})
     * @param  taxon      taxon to restrict EEs to, or null to ignore
     * @param  limit      approximate limit to how many results to return (just used to avoid extra queries; the limit
     *                    may be exceeded). Set to 0 for no limit.
     * @return map of classes ({@link ExpressionExperiment}, {@link ubic.gemma.model.expression.experiment.FactorValue},
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterial}) to the matching URI to IDs of experiments which have
     * an associated characteristic using the given uriString. The class lets us track where the annotation was.
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit );

    Collection<Characteristic> findByUri( Collection<String> uris );

    Collection<Characteristic> findByUri( String searchString );

    /**
     * Represents a set of characteristics grouped by {@link Characteristic#getValueUri()} or {@link Characteristic#getValue()}.
     */
    @Data
    class CharacteristicByValueUriOrValueCount {
        private final String valueUri;
        private final String value;
        private final Long count;
    }

    /**
     * Count characteristics by value matching the provided LIKE pattern.
     * <p>
     * The key in the mapping is either the group's shared value URI or value of the former is null or empty, in
     * lowercase.
     */
    Map<String, CharacteristicByValueUriOrValueCount> countCharacteristicValueLikeByNormalizedValue( String value );

    /**
     * Count characteristics by value URI in a given collection.
     *
     * @see #countCharacteristicValueUriInByNormalizedValue(Collection)
     */
    Map<String, CharacteristicByValueUriOrValueCount> countCharacteristicValueUriInByNormalizedValue( Collection<String> uris );

    /**
     * Finds all Characteristics whose value match the given search term
     *
     * @param  search search
     * @return characteristics
     */
    Collection<Characteristic> findByValue( String search );

    /**
     * @param  characteristics characteristics
     * @param  parentClass     parent class
     * @return a map of the specified characteristics to their parent objects.
     */
    Map<Characteristic, Object> getParents( Class<?> parentClass, @Nullable Collection<Characteristic> characteristics );

    /**
     * Optimized version that only retrieves the IDs of the owning objects. The parentClass has to be kept track of by
     * the caller.
     */
    Map<Characteristic, Long> getParentIds( Class<?> parentClass, @Nullable Collection<Characteristic> characteristics );
}
