/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentIdAndShortName;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link BibliographicReferenceReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link BibliographicReferenceService} interface -- this class
 * is unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see BibliographicReferenceService
 */
@Service("bibliographicReferenceReadService")
@ParametersAreNonnullByDefault
public class BibliographicReferenceReadServiceImpl implements BibliographicReferenceReadService {

    private final BibliographicReferenceDao bibliographicReferenceDao;
    private final SearchService searchService;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public BibliographicReferenceReadServiceImpl( BibliographicReferenceDao bibliographicReferenceDao,
            SearchService searchService,
            ExpressionExperimentService expressionExperimentService ) {
        this.bibliographicReferenceDao = bibliographicReferenceDao;
        this.searchService = searchService;
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( int start, int limit ) {
        return this.bibliographicReferenceDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending ) {
        return this.bibliographicReferenceDao.browse( start, limit, orderField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.bibliographicReferenceDao.findByExternalId( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( String id ) {
        return this.bibliographicReferenceDao.findByExternalId( id, ExternalDatabases.PUBMED );
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference findByExternalId( String id, String databaseName ) {
        return this.bibliographicReferenceDao.findByExternalId( id, databaseName );
    }

    @Override
    @Nullable
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findVOByExternalId( final String id ) {
        try {
            BibliographicReference bibref = this.findByExternalId( id );
            if ( bibref == null ) {
                return null;
            }
            BibliographicReferenceValueObject bibrefVO = new BibliographicReferenceValueObject( bibref );
            this.populateRelatedExperiments( bibref, bibrefVO );
            return bibrefVO;
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BibliographicReferenceService.findByExternalId(String id)' --> " + th, th );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctWithRelatedExperiments() {
        return this.bibliographicReferenceDao.countDistinctWithRelatedExperiments();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithRelatedExperiments() {
        return this.bibliographicReferenceDao.countWithRelatedExperiments();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> getRelatedExperiments( int offset, int limit ) {
        return this.bibliographicReferenceDao.getRelatedExperiments( offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return this.bibliographicReferenceDao.getRelatedExperiments( records );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> listAll() {
        return bibliographicReferenceDao.listAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( String query, boolean searchExperiments, boolean searchBibrefs ) throws SearchException {
        SearchSettings ss = SearchSettings.bibliographicReferenceSearch( query );

        List<SearchResult<BibliographicReference>> resultEntities = searchService.search( ss )
                .getByResultObjectType( BibliographicReference.class );

        List<BibliographicReferenceValueObject> results = new ArrayList<>();

        // only return associations with the selected entity types.
        for ( SearchResult<BibliographicReference> sr : resultEntities ) {
            BibliographicReference entity = sr.getResultObject();
            if ( entity == null )
                continue; // might be a compass hit that is no longer valid
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );


            if ( searchExperiments || searchBibrefs ) {
                this.populateRelatedExperiments( entity, vo );
                if ( !vo.getExperiments().isEmpty() || searchBibrefs ) {
                    results.add( vo );
                }
            }

            if ( searchBibrefs && !searchExperiments ) {
                results.add( vo );
            }

        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BibliographicReferenceValueObject> search( String query ) throws SearchException {
        List<SearchResult<BibliographicReference>> resultEntities = searchService
                .search( SearchSettings.bibliographicReferenceSearch( query ) )
                .getByResultObjectType( BibliographicReference.class );
        List<BibliographicReferenceValueObject> results = new ArrayList<>();
        for ( SearchResult<BibliographicReference> sr : resultEntities ) {
            BibliographicReference entity = sr.getResultObject();
            if ( entity == null ) {
                continue;
            }
            BibliographicReferenceValueObject vo = new BibliographicReferenceValueObject( entity );
            this.populateRelatedExperiments( entity, vo );
            results.add( vo );
        }

        return results;

    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return this.bibliographicReferenceDao.thaw( bibliographicReference );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return this.bibliographicReferenceDao.thaw( bibliographicReferences );
    }

    private void populateRelatedExperiments( BibliographicReference bibRef,
            BibliographicReferenceValueObject bibRefVO ) {
        Collection<ExpressionExperiment> relatedExperiments = this.bibliographicReferenceDao
                .getRelatedExperiments( Collections.singleton( ( bibRef ) ) )
                .getOrDefault( ( bibRef ), Collections.emptyList() );
        if ( relatedExperiments.isEmpty() ) {
            bibRefVO.setExperiments( new ArrayList<>() );
        } else {
            bibRefVO.setExperiments( expressionExperimentService.loadValueObjects( relatedExperiments ) );
        }
    }
}
