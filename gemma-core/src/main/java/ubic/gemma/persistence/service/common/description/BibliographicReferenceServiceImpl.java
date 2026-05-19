/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.common.description;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentIdAndShortName;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link BibliographicReferenceService}.
 * <p>
 * As of the Phase 3 strangler-fig decomposition, the read cluster (browse,
 * findByExternalId variants, findVOByExternalId, count*, getRelatedExperiments,
 * listAll, search, thaw) lives on {@link BibliographicReferenceReadService}; this
 * facade delegates to that read service. Write-side methods (refresh + the inherited
 * BaseService mutators) stay here.
 * <p>
 * Note: This is only in Core because it uses SearchService (via the read service),
 * but it could be refactored.
 *
 * @author keshav
 * @see BibliographicReferenceService
 * @see BibliographicReferenceReadService
 */
@Service
@ParametersAreNonnullByDefault
public class BibliographicReferenceServiceImpl
        extends AbstractVoEnabledService<BibliographicReference, BibliographicReferenceValueObject>
        implements BibliographicReferenceService, InitializingBean {

    private final BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private BibliographicReferenceReadService bibliographicReferenceReadService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private PubMedSearch pubMedXmlFetcher;

    @Autowired
    public BibliographicReferenceServiceImpl( BibliographicReferenceDao bibliographicReferenceDao ) {
        super( bibliographicReferenceDao );
        this.bibliographicReferenceDao = bibliographicReferenceDao;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.pubMedXmlFetcher = new PubMedSearch( ncbiApiKey );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject loadValueObject( BibliographicReference entity ) {
        return this.loadMultipleValueObjectsFromObjects( Collections.singleton( entity ) ).iterator().next();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> loadAllValueObjects() {
        return this.loadMultipleValueObjectsFromObjects( this.loadAll() );
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit ) {
        return bibliographicReferenceReadService.browse( start, limit );
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending ) {
        return bibliographicReferenceReadService.browse( start, limit, orderField, descending );
    }

    @Override
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return bibliographicReferenceReadService.findByExternalId( accession );
    }

    @Override
    public BibliographicReference findByExternalId( String id ) {
        return bibliographicReferenceReadService.findByExternalId( id );
    }

    @Override
    public BibliographicReference findByExternalId( String id, String databaseName ) {
        return bibliographicReferenceReadService.findByExternalId( id, databaseName );
    }

    @Override
    public BibliographicReferenceValueObject findVOByExternalId( final String id ) {
        return bibliographicReferenceReadService.findVOByExternalId( id );
    }

    @Override
    public long countDistinctWithRelatedExperiments() {
        return bibliographicReferenceReadService.countDistinctWithRelatedExperiments();
    }

    @Override
    public long countWithRelatedExperiments() {
        return bibliographicReferenceReadService.countWithRelatedExperiments();
    }

    @Override
    public Map<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> getRelatedExperiments( int offset, int limit ) {
        return bibliographicReferenceReadService.getRelatedExperiments( offset, limit );
    }

    @Override
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return bibliographicReferenceReadService.getRelatedExperiments( records );
    }

    @Override
    public Collection<Long> listAll() {
        return bibliographicReferenceReadService.listAll();
    }

    @Override
    @Transactional
    public BibliographicReference refresh( String pubMedId ) {
        if ( StringUtils.isBlank( pubMedId ) ) {
            throw new IllegalArgumentException( "Must provide a pubmed ID" );
        }

        BibliographicReference existingBibRef = this
                .findByExternalId( pubMedId, ExternalDatabases.PUBMED );

        if ( existingBibRef == null ) {
            return null;
        }

        existingBibRef = this.thaw( existingBibRef );

        String oldAccession = existingBibRef.getPubAccession().getAccession();

        if ( StringUtils.isNotBlank( oldAccession ) && !oldAccession.equals( pubMedId ) ) {
            throw new IllegalArgumentException(
                    "The pubmed accession is already set and doesn't match the one provided" );
        }

        existingBibRef.getPubAccession().setAccession( pubMedId );
        BibliographicReference fresh;
        try {
            fresh = this.pubMedXmlFetcher.retrieve( pubMedId );
        } catch ( IOException e ) {
            throw new IllegalStateException( "Unable to retrieve record from pubmed for id=" + pubMedId, e );
        }

        if ( fresh == null || fresh.getPublicationDate() == null ) {
            throw new IllegalStateException( "Unable to retrieve record from pubmed for id=" + pubMedId );
        }

        assert fresh.getPubAccession().getAccession().equals( pubMedId );

        existingBibRef.setPublicationDate( fresh.getPublicationDate() );
        existingBibRef.setAuthorList( fresh.getAuthorList() );
        existingBibRef.setAbstractText( fresh.getAbstractText() );
        existingBibRef.setIssue( fresh.getIssue() );
        existingBibRef.setTitle( fresh.getTitle() );
        existingBibRef.setFullTextUri( fresh.getFullTextUri() );
        existingBibRef.setEditor( fresh.getEditor() );
        existingBibRef.setPublisher( fresh.getPublisher() );
        existingBibRef.setCitation( fresh.getCitation() );
        existingBibRef.setPublication( fresh.getPublication() );
        existingBibRef.setMeshTerms( fresh.getMeshTerms() );
        existingBibRef.setChemicals( fresh.getChemicals() );
        existingBibRef.setKeywords( fresh.getKeywords() );
        existingBibRef.setPages( fresh.getPages() );
        existingBibRef.setVolume( fresh.getVolume() );

        this.update( existingBibRef );

        return existingBibRef;
    }

    @Override
    public List<BibliographicReferenceValueObject> search( String query, boolean searchExperiments, boolean searchBibrefs ) throws SearchException {
        return bibliographicReferenceReadService.search( query, searchExperiments, searchBibrefs );
    }

    @Override
    public List<BibliographicReferenceValueObject> search( String query ) throws SearchException {
        return bibliographicReferenceReadService.search( query );
    }

    @Override
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return bibliographicReferenceReadService.thaw( bibliographicReference );
    }

    @Override
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return bibliographicReferenceReadService.thaw( bibliographicReferences );
    }

    private List<BibliographicReferenceValueObject> loadMultipleValueObjectsFromObjects(
            Collection<BibliographicReference> bibRefs ) {
        if ( bibRefs.isEmpty() ) {
            return Collections.emptyList();
        }
        Map<Long, BibliographicReferenceValueObject> idToBibRefVO = new HashMap<>();

        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( bibref );
            idToBibRefVO.put( bibref.getId(), vo );
        }

        this.populateRelatedExperiments( bibRefs, idToBibRefVO );

        return new ArrayList<>( idToBibRefVO.values() );
    }

    private void populateRelatedExperiments( Collection<BibliographicReference> bibRefs,
            Map<Long, BibliographicReferenceValueObject> idToBibRefVO ) {
        Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this.bibliographicReferenceDao
                .getRelatedExperiments( bibRefs );
        for ( BibliographicReference bibref : bibRefs ) {
            BibliographicReferenceValueObject vo = idToBibRefVO.get( bibref.getId() );
            if ( relatedExperiments.containsKey( bibref ) ) {
                vo.setExperiments( expressionExperimentService.loadValueObjects( relatedExperiments.get( bibref ) ) );
            }
        }
    }

}
