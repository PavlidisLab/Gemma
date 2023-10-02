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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.expression.experiment.StatementDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Luke
 * @see    CharacteristicService
 */
@Service
public class CharacteristicServiceImpl extends AbstractFilteringVoEnabledService<Characteristic, CharacteristicValueObject>
        implements CharacteristicService {

    private final CharacteristicDao characteristicDao;

    @Autowired
    public CharacteristicServiceImpl( CharacteristicDao characteristicDao, StatementDao statementDao ) {
        super( characteristicDao );
        this.characteristicDao = characteristicDao;
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
    public Collection<Characteristic> findByValue( java.lang.String search ) {
        return this.characteristicDao.findByValue( search + '%' );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Characteristic> findCharacteristicsByValueUriOrValueLike( String search ) {
        return this.characteristicDao.findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue( search + '%' );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countCharacteristicsByValueUri( Collection<String> uris ) {
        return this.characteristicDao.countCharacteristicsByValueUriGroupedByNormalizedValue( uris );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> parentClasses, int maxResults ) {
        return characteristicDao.getParents( characteristics, parentClasses, maxResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends Characteristic> findByCategory( String query ) {
        return this.characteristicDao.findByCategory( query );
    }
}