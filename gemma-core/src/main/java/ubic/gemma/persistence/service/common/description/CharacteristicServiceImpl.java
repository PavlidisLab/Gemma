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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.expression.experiment.StatementDao;

import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.escapeLike;

/**
 * @author Luke
 * @see    CharacteristicService
 */
@Service
public class CharacteristicServiceImpl extends AbstractFilteringVoEnabledService<Characteristic, CharacteristicValueObject>
        implements CharacteristicService {

    private final CharacteristicDao characteristicDao;
    private final StatementDao statementDao;

    @Autowired
    public CharacteristicServiceImpl( CharacteristicDao characteristicDao, StatementDao statementDao ) {
        super( characteristicDao );
        this.characteristicDao = characteristicDao;
        this.statementDao = statementDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( int start, int limit ) {
        return this.characteristicDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( int start, int limit, String sortField, boolean descending ) {
        return this.characteristicDao.browse( start, limit, sortField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean loadEEs, boolean rankByLevel ) {
        if ( loadEEs ) {
            return this.characteristicDao.findExperimentsByUris( uris, taxon, limit, rankByLevel );
        } else {
            return this.characteristicDao.findExperimentReferencesByUris( uris, taxon, limit, rankByLevel );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByUri( Collection<String> uris ) {
        return this.characteristicDao.findByUri( uris );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByUri( String searchString ) {
        return this.characteristicDao.findByUri( searchString );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public Characteristic findBestByUri( String uri ) {
        return this.characteristicDao.findBestByUri( uri );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByValueStartingWith( String search ) {
        return this.characteristicDao.findByValueLike( escapeLike( search ) + '%' );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByValueLike( String search ) {
        return this.characteristicDao.findByValueLike( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Characteristic> findByValueUriOrValueLike( String search, @Nullable Collection<Class<?>> parentClasses ) {
        Map<String, Characteristic> results = new HashMap<>();
        results.putAll( this.characteristicDao.findByValueLikeGroupedByNormalizedValue( escapeLike( search ) + '%', parentClasses ) );
        // will override term found by like with an exact URI match if they have the same normalized value
        results.putAll( this.characteristicDao.findByValueUriGroupedByNormalizedValue( search, parentClasses ) );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByValueUri( Collection<String> uris, @Nullable Collection<Class<?>> parentClasses ) {
        return this.characteristicDao.countCharacteristicsByValueUriGroupedByNormalizedValue( uris, parentClasses );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> parentClasses, int maxResults ) {
        return characteristicDao.getParents( characteristics, parentClasses, maxResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByCategoryStartingWith( String query ) {
        return this.characteristicDao.findByCategoryLike( escapeLike( query ) + "%" );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByCategoryUri( String query ) {
        return this.characteristicDao.findByCategoryUri( query );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Characteristic> findByAnyValue( String value ) {
        Collection<Characteristic> results = new HashSet<>();
        results.addAll( this.characteristicDao.findByCategory( value ) );
        results.addAll( this.characteristicDao.findByValue( value ) );
        results.addAll( this.statementDao.findByPredicate( value ) );
        results.addAll( this.statementDao.findByObject( value ) );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Characteristic> findByAnyValueStartingWith( String value ) {
        Collection<Characteristic> results = new HashSet<>();
        String query = escapeLike( value ) + "%";
        results.addAll( this.characteristicDao.findByCategoryLike( query ) );
        results.addAll( this.characteristicDao.findByValueLike( query ) );
        results.addAll( this.statementDao.findByPredicateLike( query ) );
        results.addAll( this.statementDao.findByObjectLike( query ) );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Characteristic> findByAnyUri( String uri ) {
        Collection<Characteristic> results = new HashSet<>();
        results.addAll( this.characteristicDao.findByCategoryUri( uri ) );
        results.addAll( this.characteristicDao.findByUri( uri ) );
        results.addAll( this.statementDao.findByPredicateUri( uri ) );
        results.addAll( this.statementDao.findByObjectUri( uri ) );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Statement> findByPredicate( String value ) {
        return this.statementDao.findByPredicate( value );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Statement> findByPredicateUri( String uri ) {
        return this.statementDao.findByPredicateUri( uri );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Statement> findByObject( String value ) {
        return this.statementDao.findByObject( value );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Statement> findByObjectUri( String uri ) {
        return this.statementDao.findByObjectUri( uri );
    }
}