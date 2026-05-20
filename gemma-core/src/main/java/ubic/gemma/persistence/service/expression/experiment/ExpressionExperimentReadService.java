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

import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Thin read-only retrieval service for {@link ExpressionExperiment}.
 * <p>
 * Phase 1 of the {@link ExpressionExperimentService} decomposition (strangler fig). This service
 * houses the pure-retrieval "bucket A" methods: load/find/exists/thaw helpers that delegate
 * directly to {@link ExpressionExperimentDao} with no orchestration of write-side collaborators.
 * <p>
 * Callers should generally keep using {@link ExpressionExperimentService} as the facade — the
 * facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring dependency cycle through the heavier facade
 * (e.g. {@code ExpressionExperimentSetService}, {@code SampleCoexpressionAnalysisService}).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExpressionExperimentService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary.
 *
 * @see ExpressionExperimentService
 */
public interface ExpressionExperimentReadService {

    @Nonnull
    ExpressionExperiment loadReference( Long id );

    Collection<ExpressionExperiment> loadReferences( Collection<Long> ids );

    Collection<ExpressionExperiment> loadAllReferences();

    @Nullable
    ExpressionExperiment loadWithAuditTrail( Long id );

    List<Long> loadTroubledIds();

    SortedMap<String, String> loadAllIdentifiersAndName( boolean includeNames );

    ExpressionExperiment reload( ExpressionExperiment ee );

    @Nullable
    ExpressionExperiment loadWithPrimaryPublication( Long id );

    @Nullable
    ExpressionExperiment loadWithPrimaryPublicationAndOtherRelevantPublications( Long id );

    @Nullable
    ExpressionExperiment loadWithMeanVarianceRelation( Long id );

    @Nullable
    ExpressionExperiment loadWithCharacteristics( Long id );

    @Nullable
    ExpressionExperiment loadAndThaw( Long id );

    @Nullable
    ExpressionExperiment loadAndThawLite( Long id );

    @Nullable
    ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id );

    <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    <T extends Exception> ExpressionExperiment loadAndThawLiterOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession );

    Collection<ExpressionExperiment> findByAccession( String accession );

    @Nullable
    ExpressionExperiment findOneByAccession( String accession );

    Collection<ExpressionExperiment> findByBibliographicReference( BibliographicReference bibRef );

    ExpressionExperiment findByBioAssay( BioAssay ba );

    ExpressionExperiment findByBioAssay( BioAssay ba, boolean includeSubSets );

    @Nullable
    Long findIdByBioAssay( BioAssay ba, boolean includeSubSets );

    Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm );

    Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm, boolean includeSubSets );

    Collection<Long> findIdsByBioMaterial( BioMaterial bm, boolean includeSubSets );

    Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> biomaterials );

    Collection<ExpressionExperiment> findByExpressedGene( Gene gene, double rank );

    @Nullable
    ExpressionExperiment findByDesign( ExperimentalDesign ed );

    @Nullable
    Long findIdByDesign( ExperimentalDesign design );

    @Nullable
    ExpressionExperiment findByDesignId( Long designId );

    @Nullable
    ExpressionExperiment findByFactor( ExperimentalFactor factor );

    @Nullable
    Long findIdByFactor( ExperimentalFactor factor );

    Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors );

    @Nullable
    ExpressionExperiment findByFactorValue( FactorValue factorValue );

    @Nullable
    Long findIdByFactorValue( FactorValue factorValue );

    @Nullable
    ExpressionExperiment findByFactorValueId( Long factorValueId );

    Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> factorValues );

    Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds );

    Collection<ExpressionExperiment> findByGene( Gene gene );

    Collection<ExpressionExperiment> findByName( String name );

    @Nullable
    ExpressionExperiment findOneByName( String name );

    ExpressionExperiment findByQuantitationType( QuantitationType type );

    @Nullable
    ExpressionExperiment findByShortName( String shortName );

    @Nullable
    ExpressionExperiment findByShortNameWithPrimaryPublication( String shortName );

    @Nullable
    ExpressionExperiment findByShortNameAndThawLite( String shortName );

    Collection<ExpressionExperiment> findByTaxon( Taxon taxon );

    List<ExpressionExperiment> findByUpdatedLimit( int limit );

    Collection<ExpressionExperiment> findUpdatedAfter( Date date );

    @Nullable
    ExpressionExperiment findByMeanVarianceRelation( MeanVarianceRelation mvr );

    @Nullable
    Long findIdByMeanVarianceRelation( MeanVarianceRelation mvr );

    boolean existsByShortName( String shortName );

    Collection<ExpressionExperiment> loadLackingFactors();

    Collection<ExpressionExperiment> loadLackingTags();

    ExpressionExperiment thaw( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawLite( ExpressionExperiment expressionExperiment );

    ExpressionExperiment thawLiter( ExpressionExperiment expressionExperiment );

    /**
     * Included for cycle-break parity with the facade: the only read-method back-edge from
     * {@code ExpressionExperimentSetServiceImpl} is {@code getTaxon}. Living on the read
     * service breaks that dependency cycle.
     */
    @Nullable
    Taxon getTaxon( ExpressionExperiment expressionExperiment );

    // ---------------------------------------------------------------------
    // Phase 1.5 -- bucket B (counts / reporting / VOs) + bucket G
    // (filter/search infra). These methods were moved bodily from
    // ExpressionExperimentServiceImpl in the Phase 3 decomposition;
    // the facade delegates to this service.
    // ---------------------------------------------------------------------

    List<ExpressionExperiment> browse( int start, int limit );

    Collection<Long> filter( String searchString ) throws SearchException;

    Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon );

    Map<Long, Long> getAnnotationCountsByIds( Collection<Long> ids );

    @Nullable
    ExperimentalDesignValueObject getExperimentalDesignValueObject( ExpressionExperiment ee );

    Set<AnnotationValueObject> getAnnotations( ExpressionExperiment expressionExperiment );

    Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee );

    Filters getEnhancedFilters( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException;

    Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment );

    List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort );

    long countWithCache( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    Slice<ExpressionExperimentValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults );

    List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults, boolean includePredicates, boolean includeObjects, long timeout, TimeUnit timeUnit ) throws TimeoutException;

    Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, int maxResults );

    Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds );

    long getBioMaterialCount( ExpressionExperiment expressionExperiment );

    long getRawDataVectorCount( ExpressionExperiment ee );

    Collection<ExpressionExperiment> getExperimentsWithOutliers();

    Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments );

    Date getLastArrayDesignUpdate( ExpressionExperiment ee );

    Map<Long, AuditEvent> getLastLinkAnalysis( Collection<Long> ids );

    Map<Long, AuditEvent> getLastMissingValueAnalysis( Collection<Long> ids );

    Map<Long, AuditEvent> getLastProcessedDataUpdate( Collection<Long> ids );

    Map<Taxon, Long> getPerTaxonCount();

    Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids );

    Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents( Collection<ExpressionExperiment> expressionExperiments );

    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsWithCache( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit );

    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids );

    List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids );

    Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    /**
     * Cursor-mode counterpart to {@link #loadBlacklistedValueObjects(Filters, Sort, int, int)}.
     * @see ExpressionExperimentDao#loadBlacklistedValueObjectsByCursor(Filters, Sort, Cursor, int)
     */
    CursorPage<ExpressionExperimentValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit );

    List<ExpressionExperimentValueObject> loadValueObjectsByIdsWithRelationsAndCache( List<Long> ids );

    List<ExpressionExperimentValueObject> loadValueObjectsByIds( List<Long> ids, boolean maintainOrder );

    long countBioMaterials( @Nullable Filters filters );
}
