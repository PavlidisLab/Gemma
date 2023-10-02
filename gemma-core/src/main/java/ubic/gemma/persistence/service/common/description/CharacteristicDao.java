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

import lombok.Value;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BrowsingDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 */
public interface CharacteristicDao
        extends BrowsingDao<Characteristic>, FilteringVoEnabledDao<Characteristic, CharacteristicValueObject> {

    String OBJECT_ALIAS = "ch";

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start How far into the list to start
     * @param  limit Maximum records to retrieve (might be subject to security filtering)
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( int start, int limit );

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
    List<Characteristic> browse( int start, int limit, String sortField, boolean descending );

    Collection<? extends Characteristic> findByCategory( String query );

    /**
     * This search looks at direct annotations, factor values and biomaterials in that order.
     * <p>
     * Resulting EEs are filtered by ACLs.
     * <p>
     * The returned collection of EEs is effectively a {@link Set}, but since we cannot use since this should be
     * interchangable with {@link #findExperimentReferencesByUris(Collection, Taxon, int, boolean)}.
     * <p>
     * Ranking results by level guarantees correctness if a limit is used as datasets matched by direct annotation will
     * be considered before those matched by factor values or biomaterials. It is however expensive.
     *
     * @param uris       collection of URIs used for matching characteristics (via {@link Characteristic#getValueUri()})
     * @param taxon      taxon to restrict EEs to, or null to ignore
     * @param limit      limit how many results to return. Set to -1 for no limit.
     * @param rankByLevel rank results by level before limiting, has no effect if limit is -1
     * @return map of classes ({@link ExpressionExperiment}, {@link ubic.gemma.model.expression.experiment.FactorValue},
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterial}) to the matching URI to EEs which have an associated
     * characteristic using the given URI. The class lets us track where the annotation was.
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    /**
     * Similar to {@link #findExperimentsByUris(Collection, Taxon, int, boolean)}, but returns proxies with instead of
     * initializing all the EEs in bulk.
     *
     * @see org.hibernate.Session#load(Object, Serializable)
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentReferencesByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    Collection<Characteristic> findByUri( Collection<String> uris );

    Collection<Characteristic> findByUri( String searchString );

    /**
     * Return the characteristic with the most frequently used non-null value by URI.
     */
    Characteristic findBestByUri( String uri );

    /**
     * Represents a set of characteristics grouped by {@link Characteristic#getValueUri()} or {@link Characteristic#getValue()}.
     */
    @Value
    class CharacteristicUsageFrequency {
        String valueUri;
        String value;
        Long count;
    }

    /**
     * Find characteristics by value matching the provided LIKE pattern.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link #normalizeByValue(Characteristic)}.
     */
    Map<String, Characteristic> findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue( String value );

    /**
     * Count characteristics matching the provided value URIs.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link #normalizeByValue(Characteristic)}.
     */
    Map<String, Long> countCharacteristicsByValueUriGroupedByNormalizedValue( Collection<String> uris );

    /**
     * Normalize a characteristic by value.
     * <p>
     * This is obtained by taking the value URI or value if the former is null and converting it to lowercase.
     */
    String normalizeByValue( Characteristic characteristic );

    /**
     * Finds all Characteristics whose value match the given search term
     *
     * @param  search search
     * @return characteristics
     */
    Collection<Characteristic> findByValue( String search );

    /**
     * Obtain the parents (i.e. owners) of the given characteristics.
     * <p>
     * If a characteristic lacks a parent, its entry will be missing from the returned map.
     */
    Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> parentClasses, int maxResults );
}
