/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.simple.OntologyTermSimple;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.common.description.CharacteristicUtils.hasCategory;
import static ubic.gemma.model.expression.experiment.StatementUtils.formatStatement;
import static ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao.FREE_TEXT;
import static ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao.UNCATEGORIZED;

/**
 * Implementation of {@link ExpressionExperimentReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link ExpressionExperimentService} interface — this class
 * is unsecured at the AOP boundary on purpose, so that intra-{@code gemma-core} callers
 * that hold an authenticated session can bypass duplicate ACL checks.
 *
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentReadService")
public class ExpressionExperimentReadServiceImpl implements ExpressionExperimentReadService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentReadServiceImpl.class );

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditEventService auditEventService;
    /**
     * {@code @Lazy} to break a Spring construction cycle. {@code ExpressionExperimentSetServiceImpl}
     * constructor-injects this read service; {@code OntologyService} -> {@code SearchService} ->
     * {@code valueObjectConversionService} -> {@code ExpressionExperimentSetService} closes the
     * cycle. Lazy proxies on these two field deps defer instantiation past the cycle.
     */
    @Autowired
    @Lazy
    private OntologyService ontologyService;
    @Autowired
    @Lazy
    private SearchService searchService;
    @Autowired
    @Lazy
    private ExpressionExperimentFilterRewriteHelperService filterRewriteService;

    @Autowired
    public ExpressionExperimentReadServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public ExpressionExperiment loadReference( Long id ) {
        return expressionExperimentDao.loadReference( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadReferences( Collection<Long> ids ) {
        return expressionExperimentDao.loadReference( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadAllReferences() {
        return expressionExperimentDao.loadReference( expressionExperimentDao.loadIds( null, null ) );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithAuditTrail( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getAuditTrail() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadTroubledIds() {
        return expressionExperimentDao.loadTroubledIds();
    }

    @Override
    @Transactional(readOnly = true)
    public SortedMap<String, String> loadAllIdentifiersAndName( boolean includeNames ) {
        List<ExpressionExperimentDao.Identifiers> allIds = expressionExperimentDao.loadAllIdentifiers();
        TreeMap<String, String> finalIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        populateIdentifierMap( allIds, identifiers -> String.valueOf( identifiers.getId() ), finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getShortName, finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getAccession, finalIds );
        if ( includeNames ) {
            populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getName, finalIds );
        }
        return finalIds;
    }

    private void populateIdentifierMap( Collection<ExpressionExperimentDao.Identifiers> identifiers,
            Function<ExpressionExperimentDao.Identifiers, String> extractor, Map<String, String> identifierMap ) {
        Map<String, String> eeIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Set<String> ambiguousIdentifiers = new HashSet<>();
        for ( ExpressionExperimentDao.Identifiers ids : identifiers ) {
            String id = extractor.apply( ids );
            if ( id == null ) {
                continue;
            }
            if ( identifierMap.containsKey( id ) ) {
                continue;
            }
            if ( eeIds.put( id, ids.getName() ) != null ) {
                ambiguousIdentifiers.add( id );
            }
        }
        ambiguousIdentifiers.forEach( eeIds::remove );
        identifierMap.putAll( eeIds );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment reload( ExpressionExperiment ee ) {
        return expressionExperimentDao.reload( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithPrimaryPublication( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
            }
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithPrimaryPublicationAndOtherRelevantPublications( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
            }
            ee.getOtherRelevantPublications().forEach( Thaws::thawBibliographicReference );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithMeanVarianceRelation( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getMeanVarianceRelation() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithCharacteristics( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getCharacteristics() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThaw( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            expressionExperimentDao.thaw( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLite( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id, CacheMode.REFRESH );
        if ( ee != null ) {
            expressionExperimentDao.evictCharacteristicsCache( ee );
            expressionExperimentDao.evictBioAssaysCache( ee );
            expressionExperimentDao.evictQuantitationTypesCache( ee );
            expressionExperimentDao.evictOtherPartsCache( ee );
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thaw( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, message );
        expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiterOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thawLiter( ee );
        return ee;
    }

    private <T extends Exception> ExpressionExperiment loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee == null ) {
            throw exceptionSupplier.apply( message );
        }
        return ee;
    }

    private String defaultMissingMessage( Long id ) {
        return String.format( "No %s with ID %d.", ExpressionExperiment.class.getName(), id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( final DatabaseEntry accession ) {
        return expressionExperimentDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return expressionExperimentDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findOneByAccession( String accession ) {
        return expressionExperimentDao.findOneByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return expressionExperimentDao.findByBibliographicReference( bibRef );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( final BioAssay ba ) {
        return expressionExperimentDao.findByBioAssay( ba );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return expressionExperimentDao.findByBioAssay( ba, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return expressionExperimentDao.findIdByBioAssay( ba, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBioMaterial( final BioMaterial bm ) {
        return expressionExperimentDao.findByBioMaterial( bm );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return expressionExperimentDao.findByBioMaterial( bm, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> findIdsByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return expressionExperimentDao.findIdsByBioMaterial( bm, includeSubSets );
    }

    @Override
    public Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> biomaterials ) {
        return expressionExperimentDao.findByBioMaterials( biomaterials );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByExpressedGene( final Gene gene, final double rank ) {
        return expressionExperimentDao.findByExpressedGene( gene, rank );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return expressionExperimentDao.findByDesign( ed );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByDesign( ExperimentalDesign design ) {
        return expressionExperimentDao.findIdByDesign( design );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesignId( Long designId ) {
        return expressionExperimentDao.findByDesignId( designId );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        return expressionExperimentDao.findByFactor( factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByFactor( ExperimentalFactor factor ) {
        return expressionExperimentDao.findIdByFactor( factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors ) {
        return expressionExperimentDao.findByFactors( factors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return expressionExperimentDao.findByFactorValue( factorValue );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByFactorValue( FactorValue factorValue ) {
        return expressionExperimentDao.findIdByFactorValue( factorValue );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValueId( final Long factorValueId ) {
        return expressionExperimentDao.findByFactorValueId( factorValueId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return expressionExperimentDao.findByFactorValues( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds ) {
        return expressionExperimentDao.findByFactorValueIds( factorValueIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByGene( final Gene gene ) {
        return expressionExperimentDao.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByName( final String name ) {
        return expressionExperimentDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findOneByName( String name ) {
        return expressionExperimentDao.findOneByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return expressionExperimentDao.findByQuantitationType( type );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortName( final String shortName ) {
        return expressionExperimentDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortNameWithPrimaryPublication( String shortName ) {
        ExpressionExperiment ee = expressionExperimentDao.findByShortName( shortName );
        if ( ee != null && ee.getPrimaryPublication() != null ) {
            Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortNameAndThawLite( String shortName ) {
        ExpressionExperiment ee = expressionExperimentDao.findByShortName( shortName );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByTaxon( final Taxon taxon ) {
        return expressionExperimentDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return expressionExperimentDao.findByUpdatedLimit( limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        return expressionExperimentDao.findUpdatedAfter( date );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return expressionExperimentDao.findByMeanVarianceRelation( mvr );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return expressionExperimentDao.findIdByMeanVarianceRelation( mvr );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByShortName( String shortName ) {
        return expressionExperimentDao.existsByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return expressionExperimentDao.loadLackingFactors();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingTags() {
        return expressionExperimentDao.loadLackingTags();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thaw( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thawLite( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thawLiter( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        result.getBioAssays().forEach( Thaws::thawBioAssay );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( final ExpressionExperiment ee ) {
        return expressionExperimentDao.getTaxon( ee );
    }

    /**
     * Re-implementation of {@code AbstractService.ensureInSession} so this service does
     * not need to extend the base service hierarchy.
     */
    private ExpressionExperiment ensureInSession( ExpressionExperiment entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( expressionExperimentDao.load( id ),
                String.format( "No %s with ID %d.", ExpressionExperiment.class.getName(), id ) );
    }

    // =====================================================================
    // Phase 1.5 -- bucket B (counts / reporting / VOs) + bucket G
    // (filter/search infra). Methods moved bodily from
    // ExpressionExperimentServiceImpl.
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> browse( int start, int limit ) {
        return this.expressionExperimentDao.browse( start, limit );
    }

    /**
     * returns ids of search results
     *
     * @return collection of ids or an empty collection
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filter( String searchString ) throws SearchException {

        SearchService.SearchResultMap searchResultsMap = searchService
                .search( SearchSettings.expressionExperimentSearch( searchString ) );

        assert searchResultsMap != null;

        List<SearchResult<ExpressionExperiment>> searchResults = searchResultsMap.getByResultObjectType( ExpressionExperiment.class );

        Collection<Long> ids = new ArrayList<>( searchResults.size() );

        for ( SearchResult<ExpressionExperiment> s : searchResults ) {
            ids.add( s.getResultId() );
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon ) {
        return this.expressionExperimentDao.filterByTaxon( ids, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getAnnotationCountsByIds( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getAnnotationCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesignValueObject getExperimentalDesignValueObject( ExpressionExperiment ee ) {
        ee = expressionExperimentDao.reload( ee );
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null ) {
            return null;
        }
        // initialize the bits the VO ctor will touch
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            Hibernate.initialize( ef.getFactorValues() );
            for ( FactorValue fv : ef.getFactorValues() ) {
                Hibernate.initialize( fv.getCharacteristics() );
                if ( fv.getMeasurement() != null ) {
                    Hibernate.initialize( fv.getMeasurement() );
                }
            }
        }
        Hibernate.initialize( ee.getBioAssays() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null ) {
                Thaws.thawBioMaterial( bm );
            }
        }
        return new ExperimentalDesignValueObject( ed, ee.getBioAssays() );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperiment expressionExperiment ) {
        Set<AnnotationValueObject> annotations = new LinkedHashSet<>();
        Set<String> seenTerms = new HashSet<>();

        expressionExperimentDao.getExperimentAnnotations( expressionExperiment, false ).stream()
                .filter( this::filterExperimentAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperiment.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getExperimentSubSetAnnotations( expressionExperiment ).stream()
                .filter( this::filterSubSetAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperimentSubSet.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        String[] ignoredPredicates = new String[] {
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", // duration
                "http://gemma.msl.ubc.ca/ont/TGEMO_00167", // dose
                "http://gemma.msl.ubc.ca/ont/TGEMO_00168"  // development stage
        };
        expressionExperimentDao.getFactorValueAnnotations( expressionExperiment ).stream()
                .filter( this::filterFactorValueAnnotation )
                .map( c -> new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getSubjectUri(), formatStatement( c, ignoredPredicates ), FactorValue.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getBioMaterialAnnotations( expressionExperiment, false ).stream()
                .filter( this::filterBioMaterialAnnotation )
                .map( c -> new AnnotationValueObject( c, BioMaterial.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        return annotations;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee ) {
        Set<AnnotationValueObject> annotations = new HashSet<>();
        Set<String> seenTerms = new HashSet<>();

        // inherited from the EE
        expressionExperimentDao.getExperimentAnnotations( ee.getSourceExperiment(), false ).stream()
                .filter( this::filterExperimentAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperiment.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        // specifically for the subset
        ee.getCharacteristics().stream()
                .filter( this::filterSubSetAnnotations )
                .map( c -> new AnnotationValueObject( c, ExpressionExperimentSubSet.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        String[] ignoredPredicates = new String[] {
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", // duration
                "http://gemma.msl.ubc.ca/ont/TGEMO_00167", // dose
                "http://gemma.msl.ubc.ca/ont/TGEMO_00168"  // development stage
        };
        expressionExperimentDao.getFactorValueAnnotations( ee ).stream()
                .filter( this::filterFactorValueAnnotation )
                .map( c -> new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getSubjectUri(), formatStatement( c, ignoredPredicates ), FactorValue.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        expressionExperimentDao.getBioMaterialAnnotations( ee ).stream()
                .filter( this::filterBioMaterialAnnotation )
                .map( c -> new AnnotationValueObject( c, BioMaterial.class ) )
                .forEach( c -> addIfNovel( annotations, c, seenTerms ) );

        return annotations;
    }

    /**
     * Check if a term is novel and add it to the set of seen terms.
     */
    private void addIfNovel( Collection<AnnotationValueObject> annotations, AnnotationValueObject term, Set<String> seenTerms ) {
        if ( seenTerms.add( StringUtils.lowerCase( StringUtils.normalizeSpace( term.getTermName() ) ) ) ) {
            annotations.add( term );
        }
    }

    private boolean filterExperimentAnnotations( Characteristic c ) {
        return filterAnnotation( c );
    }

    private boolean filterSubSetAnnotations( Characteristic c ) {
        return filterAnnotation( c );
    }

    /**
     * Filter factor value annotations to be included as experiment tags.
     */
    private boolean filterFactorValueAnnotation( Statement c ) {
        return filterAnnotation( c )
                // ignore baseline conditions
                && !BaselineSelection.isBaselineCondition( c ) && !hasCategory( c, Categories.BLOCK )
                // ignore timepoints
                && !"http://www.ebi.ac.uk/efo/EFO_0000724".equals( c.getCategoryUri() )
                // DE_include/exclude
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00013".equals( c.getSubjectUri() )
                && !"http://gemma.msl.ubc.ca/ont/TGEMO_00014".equals( c.getSubjectUri() );
    }

    /**
     * Filter sample annotations to be included as experiment tags.
     */
    private boolean filterBioMaterialAnnotation( Characteristic c ) {
        return filterAnnotation( c )
                && !"MaterialType".equalsIgnoreCase( c.getCategory() )
                && !"molecular entity".equalsIgnoreCase( c.getCategory() )
                && !"LabelCompound".equalsIgnoreCase( c.getCategory() )
                && !BaselineSelection.isBaselineCondition( c );
    }

    private boolean filterAnnotation( Characteristic characteristic ) {
        return filterAnnotation( characteristic.getCategoryUri(), characteristic.getCategory(), characteristic.getValueUri(), characteristic.getValue() );
    }

    /**
     * Minimal requirements for an annotation to be included as an experiment tag.
     */
    private boolean filterAnnotation( @Nullable String categoryUri, @Nullable String category, @Nullable String valueUri, String value ) {
        // ignore uncategorized terms
        return category != null
                // ignore free-text categories
                && categoryUri != null // free-text categories
                // ignore free-text terms
                && valueUri != null;
    }

    @Override
    public Filters getEnhancedFilters( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        // do the inference first, some of the terms that we *duplicate* for a second property are subject to inference
        f = filterRewriteService.getFiltersWithInferredAnnotations( f, "ee", mentionedTerms, inferredTerms, timeout, timeUnit );
        f = filterRewriteService.getFiltersWithAdditionalProperties( f );
        return f;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getNumberOfDesignElementsPerSample( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return expressionExperimentDao.loadIdsWithCache( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithCache( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( extraIds != null ) {
            List<Long> eeIds = loadIdsWithCache( filters, null );
            eeIds.retainAll( extraIds );
            return eeIds.size();
        }
        return expressionExperimentDao.countWithCache( filters );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return expressionExperimentDao.loadValueObjectsWithCache( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults ) {
        Collection<Long> eeIds;
        if ( filters == null || filters.isEmpty() ) {
            eeIds = extraIds;
        } else {
            eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                eeIds.retainAll( extraIds );
            }
        }
        if ( excludedTermUris != null ) {
            try {
                excludedTermUris = inferTermsUris( excludedTermUris, 30000 );
            } catch ( TimeoutException e ) {
                log.warn( "Inference for excluded terms too too much time to compute, will only use the original set of terms." );
            }
        }
        return expressionExperimentDao.getCategoriesUsageFrequency( eeIds, excludedCategoryUris, excludedTermUris, retainedTermUris, maxResults );
    }

    /**
     * If the term cannot be resolved via {@link OntologyService#getTerm(String, long, TimeUnit)}, an attempt is done to
     * resolve its category and assign it as its parent. This handles free-text terms that lack a value URI.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults, boolean includePredicates, boolean includeObjects, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
        if ( excludedTermUris != null ) {
            try {
                excludedTermUris = inferTermsUris( excludedTermUris, Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ) );
            } catch ( TimeoutException e ) {
                log.warn( "Inference for excluded terms too too much time to compute, will only use the original set of terms." );
            }
        }

        Collection<Long> eeIds;
        if ( filters == null || filters.isEmpty() ) {
            eeIds = extraIds;
        } else {
            eeIds = expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                eeIds.retainAll( extraIds );
            }
        }

        Map<Characteristic, Long> result = expressionExperimentDao.getAnnotationsUsageFrequency( eeIds, null, maxResults, minFrequency, category, excludedCategoryUris, excludedTermUris, retainedTermUris, includePredicates, includeObjects );

        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> resultWithParents = new ArrayList<>( result.size() );

        // gather all the values and categories
        Set<String> uris = result.keySet().stream()
                .flatMap( c -> Stream.of( c.getValueUri(), c.getCategoryUri() ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        Map<String, Set<OntologyTerm>> termByUri = ontologyService.getTerms( uris, Math.max( timeUnit.toMillis( timeout ) - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ).stream()
                .filter( t -> t.getUri() != null ) // should never occur, but better be safe than sorry
                .collect( Collectors.groupingBy( OntologyTerm::getUri, Collectors.toSet() ) );

        for ( Map.Entry<Characteristic, Long> entry : result.entrySet() ) {
            Characteristic c = entry.getKey();
            OntologyTerm term;
            if ( c.getValueUri() != null && termByUri.containsKey( c.getValueUri() ) ) {
                // TODO: handle more than one term per URI
                term = termByUri.get( c.getValueUri() ).iterator().next();
            } else if ( c.getCategoryUri() != null && termByUri.containsKey( c.getCategoryUri() ) ) {
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), termByUri.get( c.getCategoryUri() ).iterator().next() );
            } else {
                // create an uncategorized term
                term = new OntologyTermSimpleWithCategory( c.getValueUri(), c.getValue(), null );
            }
            resultWithParents.add( new ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm( entry.getKey(), entry.getValue(), term ) );
        }

        // sort in descending order
        resultWithParents.sort( Comparator.comparing( ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) );

        return resultWithParents;
    }

    /**
     * Infer all the implied terms from the given collection of term URIs.
     */
    private Set<String> inferTermsUris( Collection<String> termUris, long timeoutMs ) throws TimeoutException {
        StopWatch timer = StopWatch.createStarted();
        Set<String> excludedTermUris = new HashSet<>( termUris );
        // null is a special indicator for free-text terms or categories
        boolean removedFreeText = excludedTermUris.remove( FREE_TEXT );
        boolean removedUncategorized = excludedTermUris.remove( UNCATEGORIZED );
        // expand exclusions with implied terms via subclass relation
        Set<OntologyTerm> excludedTerms = ontologyService.getTerms( excludedTermUris, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        // exclude terms using the subClass relation
        Set<OntologyTerm> impliedTerms = ontologyService.getChildren( excludedTerms, false, false, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
        for ( OntologyTerm t : impliedTerms ) {
            excludedTermUris.add( t.getUri() );
        }
        if ( removedFreeText ) {
            excludedTermUris.add( FREE_TEXT );
        }
        if ( removedUncategorized ) {
            excludedTermUris.add( UNCATEGORIZED );
        }
        return excludedTermUris;
    }

    /**
     * Extension of {@link OntologyTermSimple} that adds a category term as unique parent.
     */
    private static class OntologyTermSimpleWithCategory extends OntologyTermSimple {

        @Nullable
        private final OntologyTerm categoryTerm;

        public OntologyTermSimpleWithCategory( @Nullable String uri, String term, @Nullable OntologyTerm categoryTerm ) {
            //noinspection DataFlowIssue
            super( uri, term );
            this.categoryTerm = categoryTerm;
        }

        @Override
        public Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
            if ( categoryTerm == null ) {
                return Collections.emptySet();
            }
            if ( direct ) {
                return Collections.singleton( categoryTerm );
            } else {
                // combine the direct parents + all the parents from the parents
                return Stream.concat( Stream.of( categoryTerm ), Stream.of( categoryTerm ).flatMap( t -> t.getParents( false, includeAdditionalProperties, keepObsoletes ).stream() ) )
                        .collect( Collectors.toSet() );
            }
        }

        @Override
        public boolean isRoot() {
            return categoryTerm == null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                return expressionExperimentDao.getTechnologyTypeUsageFrequency( extraIds );
            } else {
                return expressionExperimentDao.getTechnologyTypeUsageFrequency();
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
            return expressionExperimentDao.getTechnologyTypeUsageFrequency( ids );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, int maxResults ) {
        Map<ArrayDesign, Long> result;
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( extraIds, maxResults ) );
                for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( extraIds, maxResults ).entrySet() ) {
                    result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
                }
            } else {
                result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( maxResults ) );
                for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( maxResults ).entrySet() ) {
                    result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
                }
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
            result = new HashMap<>( expressionExperimentDao.getArrayDesignsUsageFrequency( ids, maxResults ) );
            for ( Map.Entry<ArrayDesign, Long> e : expressionExperimentDao.getOriginalPlatformsUsageFrequency( ids, maxResults ).entrySet() ) {
                result.compute( e.getKey(), ( k, v ) -> ( v != null ? v : 0L ) + e.getValue() );
            }
        }
        // retain top results
        // this happens when original platforms are mixed in
        if ( maxResults > 0 && result.size() > maxResults ) {
            return result.entrySet()
                    .stream()
                    .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                    .limit( maxResults )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        if ( filters == null || filters.isEmpty() ) {
            if ( extraIds != null ) {
                return expressionExperimentDao.getPerTaxonCount( extraIds );
            } else {
                return expressionExperimentDao.getPerTaxonCount();
            }
        } else {
            List<Long> ids = this.expressionExperimentDao.loadIdsWithCache( filters, null );
            if ( extraIds != null ) {
                ids.retainAll( extraIds );
            }
            return expressionExperimentDao.getPerTaxonCount( ids );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getBioMaterialCount( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public long getRawDataVectorCount( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getRawDataVectorCount( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        return this.expressionExperimentDao.getExperimentsWithOutliers();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getLastArrayDesignUpdate( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        return this.getLastEvent( this.expressionExperimentDao.load( ids ), new LinkAnalysisEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return this.getLastEvent( this.expressionExperimentDao.load( ids ), new MissingValueAnalysisEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return this.getLastEvent( this.expressionExperimentDao.load( ids ), new ProcessedVectorComputationEvent() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.expressionExperimentDao.getPerTaxonCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCounts( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return this.expressionExperimentDao.getPopulatedFactorCountsExcludeBatch( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSampleRemovalEvents( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return this.expressionExperimentDao.loadDetailsValueObjects( ids, taxon, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsWithCache( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIdsWithCache( ids, taxon, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIds( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids ) {
        return this.expressionExperimentDao.loadDetailsValueObjectsByIdsWithCache( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return expressionExperimentDao.loadBlacklistedValueObjects( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ExpressionExperimentValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        return expressionExperimentDao.loadBlacklistedValueObjectsByCursor( filters, sort, cursor, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsByIdsWithRelationsAndCache( List<Long> ids ) {
        List<ExpressionExperiment> results = expressionExperimentDao.loadWithRelationsAndCache( ids );
        Map<Long, Integer> id2position = ListUtils.indexOfElements( ids );
        return expressionExperimentDao.loadValueObjects( results ).stream()
                .sorted( Comparator.comparing( vo -> id2position.get( vo.getId() ) ) )
                .collect( Collectors.toList() );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentValueObject> loadValueObjectsByIds( final List<Long> ids,
            boolean maintainOrder ) {
        List<ExpressionExperimentValueObject> results = this.expressionExperimentDao.loadValueObjectsByIds( ids );

        // sort results according to ids
        if ( maintainOrder ) {
            Map<Long, Integer> id2position = ListUtils.indexOfElements( ids );
            return results.stream()
                    .sorted( Comparator.comparing( vo -> id2position.get( vo.getId() ) ) )
                    .collect( Collectors.toList() );
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public long countBioMaterials( @Nullable Filters filters ) {
        return expressionExperimentDao.countBioMaterials( filters );
    }

    /**
     * @param ees  experiments
     * @param type event type
     * @return a map of the expression experiment ids to the last audit event for the given audit event type the
     * map can contain nulls if the specified auditEventType isn't found for a given expression experiment id
     */
    private Map<Long, AuditEvent> getLastEvent( Collection<ExpressionExperiment> ees, AuditEventType type ) {
        Map<Long, AuditEvent> lastEventMap = new HashMap<>();
        AuditEvent last;
        for ( ExpressionExperiment experiment : ees ) {
            last = this.auditEventService.getLastEvent( experiment, type.getClass() );
            lastEventMap.put( experiment.getId(), last );
        }
        return lastEventMap;
    }
}
