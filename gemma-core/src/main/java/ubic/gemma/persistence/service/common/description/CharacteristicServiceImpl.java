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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

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
    private CharacteristicReadService readService;

    @Autowired
    public CharacteristicServiceImpl( CharacteristicDao characteristicDao ) {
        super( characteristicDao );
        this.characteristicDao = characteristicDao;
    }

    // =====================================================================
    // Read methods -- delegate to CharacteristicReadService.
    // ACL @Secured annotations live on the CharacteristicService interface
    // and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public List<Characteristic> browse( int start, int limit ) {
        return readService.browse( start, limit );
    }

    @Override
    public List<Characteristic> browse( int start, int limit, String sortField, boolean descending ) {
        return readService.browse( start, limit, sortField, descending );
    }

    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean loadEEs, boolean rankByLevel ) {
        return readService.findExperimentsByUris( uris, includeSubjects, includePredicates, includeObjects, taxon, limit, loadEEs, rankByLevel );
    }

    @Override
    public Collection<Characteristic> findByParentClasses( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, @Nullable String category, int maxResults ) {
        return readService.findByParentClasses( parentClasses, includeNoParents, category, maxResults );
    }

    @Override
    public Collection<Characteristic> findByUri( String uri, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        return readService.findByUri( uri, category, parentClasses, includeNoParents, maxResults );
    }

    @Nullable
    @Override
    public Characteristic findBestByUri( String uri ) {
        return readService.findBestByUri( uri );
    }

    @Override
    public Collection<Characteristic> findByValueStartingWith( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        return readService.findByValueStartingWith( search, category, parentClasses, includeNoParents, maxResults );
    }

    @Override
    public Collection<Characteristic> findByValueLike( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        return readService.findByValueLike( search, category, parentClasses, includeNoParents, maxResults );
    }

    @Override
    public Map<String, Characteristic> findByValueUriOrValueStartingWith( String search, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        return readService.findByValueUriOrValueStartingWith( search, parentClasses, includeNoParents );
    }

    @Override
    public Map<String, Long> countByValueUri( Collection<String> uris, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        return readService.countByValueUri( uris, parentClasses, includeNoParents );
    }

    @Override
    public Map<String, String> findValueGroupedByValueUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean includePredicates, boolean includeObjects, int maxResults ) {
        return readService.findValueGroupedByValueUri( parentClasses, includeNoParents, includePredicates, includeObjects, maxResults );
    }

    @Override
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean thawParents ) {
        return readService.getParents( characteristics, parentClasses, includeNoParents, thawParents );
    }

    @Override
    public Collection<Characteristic> findByCategoryStartingWith( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        return readService.findByCategoryStartingWith( query, parentClasses, includeNoParents, maxResults );
    }

    @Override
    public Collection<Characteristic> findByCategoryUri( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        return readService.findByCategoryUri( query, parentClasses, includeNoParents, maxResults );
    }

    @Override
    public Collection<? extends Characteristic> findByAnyValue( String value ) {
        return readService.findByAnyValue( value );
    }

    @Override
    public Collection<? extends Characteristic> findByAnyValueStartingWith( String value ) {
        return readService.findByAnyValueStartingWith( value );
    }

    @Override
    public Collection<? extends Characteristic> findByAnyUri( String uri ) {
        return readService.findByAnyUri( uri );
    }

    @Override
    public Collection<Statement> findByPredicate( String value ) {
        return readService.findByPredicate( value );
    }

    @Override
    public Collection<Statement> findByPredicateUri( String uri ) {
        return readService.findByPredicateUri( uri );
    }

    @Override
    public Collection<Statement> findByObject( String value ) {
        return readService.findByObject( value );
    }

    @Override
    public Collection<Statement> findByObjectUri( String uri ) {
        return readService.findByObjectUri( uri );
    }

    // =====================================================================
    // Write methods stay on the facade -- inherited from AbstractFilteringVoEnabledService /
    // BaseService (create, save, update, remove). No write-specific methods on this facade.
    // =====================================================================
}
