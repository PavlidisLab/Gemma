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
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author paul
 */
public interface CharacteristicService extends BaseService<Characteristic>, BaseVoEnabledService<Characteristic, CharacteristicValueObject> {

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start How far into the list to start
     * @param  limit Maximum records to retrieve
     * @return characteristics
     */
    List<Characteristic> browse( int start, int limit );

    /**
     * Browse through the characteristics, excluding GO annotations.
     *
     * @param  start      How far into the list to start
     * @param  limit      Maximum records to retrieve
     * @param  sortField  sort field
     * @param  descending sor order
     * @return characteristics
     */
    List<Characteristic> browse( int start, int limit, String sortField, boolean descending );

    /**
     * @see CharacteristicDao#findExperimentsByUris(Collection, Taxon, int, boolean)
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean loadEEs, boolean rankByLevel );

    /**
     * given a collection of strings that represent URI's will find all the characteristics that are used in the system
     * with URI's matching anyone in the given collection
     *
     * @param  uris uris
     * @return characteristics
     */
    Collection<Characteristic> findByUri( Collection<String> uris );

    /**
     * Looks for an exact match of the give string to a valueUri in the characteristic database
     *
     * @param  searchString search string
     * @return characteristics
     */
    Collection<Characteristic> findByUri( String searchString );

    /**
     * Find the best possible characteristic for a given URI.
     */
    @Nullable
    Characteristic findBestByUri( String uri );

    /**
     * Returns a collection of characteristics that have a value starting with the given string.
     * <p>
     * The value is usually a human-readable form of the termURI. SQL {@code LIKE} patterns are escaped. Use
     * {@link #findByValueLike(String)} to do wildcard searches instead.
     */
    Collection<Characteristic> findByValueStartingWith( String search );

    /**
     * Returns a collection of characteristics that have a value matching the given SQL {@code LIKE} pattern.
     */
    Collection<Characteristic> findByValueLike( String search );

    /**
     * @see CharacteristicDao#findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue(String)
     */
    Map<String, Characteristic> findCharacteristicsByValueUriOrValueLike( String search );

    Map<String, Long> countCharacteristicsByValueUri( Collection<String> uris );

    /**
     * @param characteristics characteristics
     * @return a map of the specified characteristics to their annotated objects.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_VALUES_READ" })
    Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> parentClasses, int maxResults );

    //    /**
    //     * @param classes constraint
    //     * @param string  string value
    //     * @return characteristics
    //     */
    //    Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string );

    Collection<Characteristic> findByCategoryStartingWith( String queryPrefix );

    Collection<Characteristic> findByCategoryUri( String query );

    /**
     * Find a characteristic by any value it contains including its category, value, predicates and objects.
     */
    Collection<? extends Characteristic> findByAnyValue( String value );

    /**
     * Find a characteristic by any value it contains including its category, value, predicates and objects that starts
     * with the given query.
     */
    Collection<? extends Characteristic> findByAnyValueStartingWith( String value );

    /**
     * Find a characteristic or statement by any URI it contains including its category, value, predicates and objects.
     */
    Collection<? extends Characteristic> findByAnyUri( String uri );

    Collection<Statement> findByPredicate( String value );

    Collection<Statement> findByPredicateUri( String uri );

    Collection<Statement> findByObject( String value );

    Collection<Statement> findByObjectUri( String uri );

    @Override
    @Secured({ "GROUP_USER" })
    Characteristic create( Characteristic c );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Characteristic c );

    /**
     * @see FilteringVoEnabledDao#getFilterableProperties()
     */
    Set<String> getFilterableProperties();

    /**
     * @see FilteringVoEnabledDao#getFilterablePropertyType(String)
     */
    Class<?> getFilterablePropertyType( String property );

    /**
     * @see FilteringVoEnabledDao#getFilterablePropertyDescription(String)
     */
    @Nullable
    String getFilterablePropertyDescription( String property );

    /**
     * @see FilteringVoEnabledDao#getFilterableProperties()
     */
    Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException;

    /**
     * @see FilteringVoEnabledDao#getFilter(String, Filter.Operator, Collection)
     */
    Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException;

    /**
     * @see FilteringVoEnabledDao#getSort(String, Sort.Direction)
     */
    Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException;
}
