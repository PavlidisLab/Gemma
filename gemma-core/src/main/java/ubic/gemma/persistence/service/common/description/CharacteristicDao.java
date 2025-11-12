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

import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
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

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param start How far into the list to start
     * @param limit Maximum records to retrieve (might be subject to security filtering)
     * @return characteristics
     */
    @Override
    List<Characteristic> browse( int start, int limit );

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
    List<Characteristic> browse( int start, int limit, String sortField, boolean descending );

    Collection<Characteristic> findByParentClasses( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, @Nullable String category, int maxResults );

    Collection<Characteristic> findByCategory( String value );

    Collection<Characteristic> findByCategoryLike( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    Collection<Characteristic> findByCategoryUri( String uri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * This search looks at direct annotations, factor values and biomaterials in that order.
     * <p>
     * Resulting EEs are filtered by ACLs.
     * <p>
     * The returned collection of EEs is effectively a {@link Set}, but since we cannot use since this should be
     * interchangable with {@link #findExperimentReferencesByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)}.
     * <p>
     * Ranking results by level guarantees correctness if a limit is used as datasets matched by direct annotation will
     * be considered before those matched by factor values or biomaterials. It is however expensive.
     *
     * @param uris              collection of URIs used for matching characteristics (via {@link Characteristic#getValueUri()})
     * @param includeSubjects   lookup subjects (or values for regular characteristics)
     * @param includePredicates lookup predicates (only applicable to {@link Statement}s)
     * @param includeObjects    lookup objects (only applicable to {@link Statement}s)
     * @param taxon             taxon to restrict EEs to, or null to ignore
     * @param limit             limit how many results to return. Set to -1 for no limit.
     * @param rankByLevel       rank results by level before limiting, has no effect if limit is -1
     * @return map of classes ({@link ExpressionExperiment}, {@link ubic.gemma.model.expression.experiment.FactorValue},
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterial}) to the matching URI to EEs which have an associated
     * characteristic using the given URI. The class lets us track where the annotation was.
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    /**
     * Similar to {@link #findExperimentsByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)}, but returns proxies with instead of
     * initializing all the EEs in bulk.
     *
     * @see org.hibernate.Session#load(Object, Serializable)
     */
    Map<Class<? extends Identifiable>, Map<String, Set<@MayBeUninitialized ExpressionExperiment>>> findExperimentReferencesByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel );

    /**
     * Find characteristics with the given URI.
     *
     * @param category         restrict the category of the characteristic, or null to ignore
     * @param parentClasses    only return characteristics that have parents of these classes, or null to ignore
     * @param includeNoParents include characteristics that have no parents
     * @param maxResults       maximum number of results to return, or -1 for no limit
     */
    Collection<Characteristic> findByUri( String uri, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Return the characteristic with the most frequently used non-null value by URI.
     */
    Characteristic findBestByUri( String uri );

    /**
     * Find characteristics by URI.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Characteristic> findByValueUriGroupedByNormalizedValue( String valueUri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * Find characteristics by value matching the provided LIKE pattern.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Characteristic> findByValueLikeGroupedByNormalizedValue( String valueLike, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * Count characteristics matching the provided value URIs.
     * <p>
     * The mapping key is the normalized value of the characteristics as per {@link CharacteristicUtils#getNormalizedValue(Characteristic)}.
     */
    Map<String, Long> countByValueUriGroupedByNormalizedValue( Collection<String> uris, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    Collection<Characteristic> findByValue( String search );

    /**
     * Finds all Characteristics whose value match the given search term
     *
     * @param category constraint the category of the characteristic, or null to ignore
     */
    Collection<Characteristic> findByValueLike( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Obtain the classes of entities can can own a {@link Characteristic}.
     */
    Collection<Class<? extends Identifiable>> getParentClasses();

    /**
     * Obtain the parents (i.e. owners) of the given characteristics.
     *
     * @param characteristics  characteristics to find parents for
     * @param parentClasses    restrict the parents to these classes, all parents are returned if null. If supplied, at
     *                         least one parent must be provided unless includeNoParents is true.
     * @param includeNoParents include characteristics that have no parents, those will be mapped explicitly to
     *                         {@code null}.
     * @return the supplied characteristics mapped to their parents, or {@code null} if the characteristic has no parent
     * and includeNoParents is true. A characteristic may not have multiple parents.
     */
    Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );
}
