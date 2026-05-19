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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.association.GOEvidenceCode;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.persistence.util.Thaws;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.model.common.description.CharacteristicUtils.*;

/**
 * @author pavlidis
 * @author keshav
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentService")
public class ExpressionExperimentServiceImpl
        extends AbstractFilteringVoEnabledService<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentService {

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;
    @Autowired
    private ExpressionExperimentFilterRewriteHelperService filterRewriteService;
    @Autowired
    private ExpressionExperimentReadService readService;
    @Autowired
    private ExpressionExperimentWriteService writeService;
    @Autowired
    private ubic.gemma.persistence.service.common.description.CharacteristicService characteristicService;
    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService auditTrailService;

    @Autowired
    public ExpressionExperimentServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        super( expressionExperimentDao );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @NonNull
    @Transactional(readOnly = true)
    public ExpressionExperiment loadReference( Long id ) {
        return readService.loadReference( id );
    }

    @Override
    public Collection<ExpressionExperiment> loadReferences( Collection<Long> ids ) {
        return readService.loadReferences( ids );
    }

    @Override
    public Collection<ExpressionExperiment> loadAllReferences() {
        return readService.loadAllReferences();
    }

    @Override
    public ExpressionExperiment loadWithAuditTrail( Long id ) {
        return readService.loadWithAuditTrail( id );
    }

    @Override
    public List<Long> loadTroubledIds() {
        return readService.loadTroubledIds();
    }

    @Override
    public SortedMap<String, String> loadAllIdentifiersAndName( boolean includeNames ) {
        return readService.loadAllIdentifiersAndName( includeNames );
    }

    @Override
    public ExpressionExperiment reload( ExpressionExperiment ee ) {
        return readService.reload( ee );
    }

    @Override
    public ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        return writeService.addFactor( ee, factor );
    }

    @Override
    public FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv ) {
        return writeService.addFactorValue( ee, fv );
    }

    @Override
    public void addFactorValues( ExpressionExperiment ee, Map<BioMaterial, FactorValue> fvs ) {
        writeService.addFactorValues( ee, fvs );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType qt ) {
        return expressionExperimentDao.getRawDataVectors( ee, samples, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<RawExpressionDataVector> getPreferredRawDataVectors( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getPreferredRawDataVectors( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Collection<RawExpressionDataVector>> getMissingValuesVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.getMissingValuesVectors( ee );
    }

    @Override
    @Transactional
    public int addRawDataVectors( ExpressionExperiment ee,
            QuantitationType quantitationType,
            Collection<RawExpressionDataVector> newVectors ) {
        createDimensionIfNecessary( newVectors );
        createQuantitationTypeIfNecessary( newVectors, RawExpressionDataVector.class );
        return expressionExperimentDao.addRawDataVectors( ee, quantitationType, newVectors );
    }

    @Override
    @Transactional
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        return expressionExperimentDao.replaceRawDataVectors( ee, qt, vectors );
    }

    @Override
    @Transactional
    public int replaceAllRawDataVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {
        if ( newVectors.isEmpty() ) {
            throw new UnsupportedOperationException( "Only use this method for replacing vectors, not erasing them" );
        }

        Set<QuantitationType> existingQts = ee.getRawExpressionDataVectors().stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> newQts = newVectors.stream()
                .map( RawExpressionDataVector::getQuantitationType )
                .collect( Collectors.toSet() );

        Set<QuantitationType> preferredQts = newQts.stream()
                .filter( QuantitationType::getIsPreferred )
                .collect( Collectors.toSet() );
        if ( preferredQts.size() > 1 ) {
            throw new IllegalArgumentException( "There must be exactly one preferred quantitation type." );
        }

        // group the vectors up by QT
        Map<QuantitationType, Set<RawExpressionDataVector>> vectorsByQt = newVectors.stream()
                .collect( Collectors.groupingBy( RawExpressionDataVector::getQuantitationType, Collectors.toSet() ) );

        int replaced = 0;
        for ( Map.Entry<QuantitationType, Set<RawExpressionDataVector>> e : vectorsByQt.entrySet() ) {
            if ( existingQts.contains( e.getKey() ) ) {
                replaced += replaceRawDataVectors( ee, e.getKey(), e.getValue() );
            } else {
                replaced += addRawDataVectors( ee, e.getKey(), e.getValue() );
            }
        }

        for ( QuantitationType qt : existingQts ) {
            if ( !newQts.contains( qt ) ) {
                removeRawDataVectors( ee, qt );
            }
        }

        return replaced;
    }

    @Override
    @Transactional
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeAllRawDataVectors( ee );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return removeRawDataVectors( ee, qt, false );
    }

    @Override
    @Transactional
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        return expressionExperimentDao.removeRawDataVectors( ee, qt, keepDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays ) {
        return Optional.ofNullable( expressionExperimentDao.getProcessedDataVectors( ee, assays ) );
    }

    @Override
    @Transactional
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.createProcessedDataVectors( ee, vectors );
    }

    @Override
    @Transactional
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return expressionExperimentDao.removeProcessedDataVectors( ee );
    }

    @Override
    @Transactional
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        createDimensionIfNecessary( vectors );
        // unlike raw vectors, the "new" processed vectors might use a different QT
        createQuantitationTypeIfNecessary( vectors, ProcessedExpressionDataVector.class );
        return expressionExperimentDao.replaceProcessedDataVectors( ee, vectors );
    }

    private void createDimensionIfNecessary( Collection<? extends BulkExpressionDataVector> vectors ) {
        Collection<BioAssayDimension> dimension = vectors.stream()
                .map( BulkExpressionDataVector::getBioAssayDimension )
                .collect( Collectors.toSet() );
        if ( dimension.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common bioassay dimension" );
        }
        BioAssayDimension bad = dimension.iterator().next();
        if ( bad.getId() == null ) {
            log.info( "Creating " + bad + "..." );
            bad = this.bioAssayDimensionService.findOrCreate( bad );
            for ( BulkExpressionDataVector vector : vectors ) {
                vector.setBioAssayDimension( bad );
            }
        }
    }

    private <T extends DataVector> void createQuantitationTypeIfNecessary( Collection<T> vectors, Class<? extends DataVector> vectorType ) {
        Set<QuantitationType> quantitationType = vectors.stream()
                .map( DataVector::getQuantitationType )
                .collect( Collectors.toSet() );
        if ( quantitationType.size() != 1 ) {
            throw new IllegalArgumentException( "Vectors must share a common quantitation type." );
        }
        QuantitationType qt = quantitationType.iterator().next();
        if ( qt.getId() == null ) {
            log.info( "Creating " + qt + "..." );
            qt = quantitationTypeService.create( qt, vectorType );
            for ( DataVector vector : vectors ) {
                vector.setQuantitationType( qt );
            }
        }
    }

    @Override
    public List<ExpressionExperiment> browse( int start, int limit ) {
        return readService.browse( start, limit );
    }

    @Override
    public Collection<Long> filter( String searchString ) throws SearchException {
        return readService.filter( searchString );
    }

    @Override
    public Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon ) {
        return readService.filterByTaxon( ids, taxon );
    }

    @Override
    public ExpressionExperiment loadWithPrimaryPublication( Long id ) {
        return readService.loadWithPrimaryPublication( id );
    }

    @Override
    public ExpressionExperiment loadWithPrimaryPublicationAndOtherRelevantPublications( Long id ) {
        return readService.loadWithPrimaryPublicationAndOtherRelevantPublications( id );
    }

    @Override
    public ExpressionExperiment loadWithMeanVarianceRelation( Long id ) {
        return readService.loadWithMeanVarianceRelation( id );
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( final DatabaseEntry accession ) {
        return readService.findByAccession( accession );
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return readService.findByAccession( accession );
    }

    @Override
    public ExpressionExperiment findOneByAccession( String accession ) {
        return readService.findOneByAccession( accession );
    }

    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return readService.findByBibliographicReference( bibRef );
    }

    @Override
    public ExpressionExperiment findByBioAssay( final BioAssay ba ) {
        return readService.findByBioAssay( ba );
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return readService.findByBioAssay( ba, includeSubSets );
    }

    @Override
    public Long findIdByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return readService.findIdByBioAssay( ba, includeSubSets );
    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterial( final BioMaterial bm ) {
        return readService.findByBioMaterial( bm );
    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return readService.findByBioMaterial( bm, includeSubSets );
    }

    @Override
    public Collection<Long> findIdsByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return readService.findIdsByBioMaterial( bm, includeSubSets );
    }

    @Override
    public Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> biomaterials ) {
        return readService.findByBioMaterials( biomaterials );
    }

    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( final Gene gene, final double rank ) {
        return readService.findByExpressedGene( gene, rank );
    }

    @Override
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return readService.findByDesign( ed );
    }

    @Override
    public Long findIdByDesign( ExperimentalDesign design ) {
        return readService.findIdByDesign( design );
    }

    @Override
    public ExpressionExperiment findByDesignId( Long designId ) {
        return readService.findByDesignId( designId );
    }

    @Override
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        return readService.findByFactor( factor );
    }

    @Override
    public Long findIdByFactor( ExperimentalFactor factor ) {
        return readService.findIdByFactor( factor );
    }

    @Override
    public Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors ) {
        return readService.findByFactors( factors );
    }

    @Override
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return readService.findByFactorValue( factorValue );
    }

    @Override
    public Long findIdByFactorValue( FactorValue factorValue ) {
        return readService.findIdByFactorValue( factorValue );
    }

    @Override
    public ExpressionExperiment findByFactorValueId( final Long factorValueId ) {
        return readService.findByFactorValueId( factorValueId );
    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return readService.findByFactorValues( factorValues );
    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds ) {
        return readService.findByFactorValueIds( factorValueIds );
    }

    @Override
    public Collection<ExpressionExperiment> findByGene( final Gene gene ) {
        return readService.findByGene( gene );
    }

    @Override
    public Collection<ExpressionExperiment> findByName( final String name ) {
        return readService.findByName( name );
    }

    @Override
    public ExpressionExperiment findOneByName( String name ) {
        return readService.findOneByName( name );
    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return readService.findByQuantitationType( type );
    }

    @Override
    public ExpressionExperiment findByShortName( final String shortName ) {
        return readService.findByShortName( shortName );
    }

    @Override
    public ExpressionExperiment findByShortNameWithPrimaryPublication( String shortName ) {
        return readService.findByShortNameWithPrimaryPublication( shortName );
    }

    @Override
    public ExpressionExperiment findByShortNameAndThawLite( String shortName ) {
        return readService.findByShortNameAndThawLite( shortName );
    }

    @Override
    public Collection<ExpressionExperiment> findByTaxon( final Taxon taxon ) {
        return readService.findByTaxon( taxon );
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return readService.findByUpdatedLimit( limit );
    }

    @Override
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        return readService.findUpdatedAfter( date );
    }

    @Override
    public ExpressionExperiment findByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return readService.findByMeanVarianceRelation( mvr );
    }

    @Override
    public Long findIdByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return readService.findIdByMeanVarianceRelation( mvr );
    }

    @Override
    public boolean existsByShortName( String shortName ) {
        return readService.existsByShortName( shortName );
    }

    @Override
    public Map<Long, Long> getAnnotationCountsByIds( final Collection<Long> ids ) {
        return readService.getAnnotationCountsByIds( ids );
    }

    @Override
    public ExperimentalDesignValueObject getExperimentalDesignValueObject( ExpressionExperiment ee ) {
        return readService.getExperimentalDesignValueObject( ee );
    }

    @Override
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperiment expressionExperiment ) {
        return readService.getAnnotations( expressionExperiment );
    }

    @Override
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee ) {
        return readService.getAnnotations( ee );
    }

    @Override
    public Filters getEnhancedFilters( Filters f, @Nullable Collection<OntologyTerm> mentionedTerms, @Nullable Collection<OntologyTerm> inferredTerms, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return readService.getEnhancedFilters( f, mentionedTerms, inferredTerms, timeout, timeUnit );
    }

    /**
     * Augments the base service description with a note about ontology inference. Remains
     * on the facade because it overrides the {@code AbstractFilteringVoEnabledService}
     * hierarchy's contract; pure delegation here would break the inheritance chain.
     */
    @Override
    public String getFilterablePropertyDescription( String property ) {
        String desc = super.getFilterablePropertyDescription( property );
        if ( filterRewriteService.supportsInferredAnnotations( property ) ) {
            return "will be expanded with ontology inference" + ( desc != null ? "; " + desc : "" );
        }
        return desc;
    }

    @Override
    public Map<BioAssay, Long> getNumberOfDesignElementsPerSample( ExpressionExperiment expressionExperiment ) {
        return readService.getNumberOfDesignElementsPerSample( expressionExperiment );
    }

    @Override
    public ExpressionExperiment loadWithCharacteristics( Long id ) {
        return readService.loadWithCharacteristics( id );
    }

    @Override
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        return readService.loadAndThawLiteOrFail( id, exceptionSupplier, message );
    }

    @Override
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        return readService.loadAndThawLiteOrFail( id, exceptionSupplier );
    }

    @Override
    public <T extends Exception> ExpressionExperiment loadAndThawLiterOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        return readService.loadAndThawLiterOrFail( id, exceptionSupplier );
    }

    @Override
    public ExpressionExperiment loadAndThaw( Long id ) {
        return readService.loadAndThaw( id );
    }

    @Override
    public ExpressionExperiment loadAndThawLite( Long id ) {
        return readService.loadAndThawLite( id );
    }

    @Override
    public ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id ) {
        return readService.loadAndThawLiteWithRefreshCacheMode( id );
    }

    @Override
    public <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        return readService.loadAndThawOrFail( id, exceptionSupplier );
    }

    @Override
    public List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return readService.loadIdsWithCache( filters, sort );
    }

    @Override
    public long countWithCache( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        return readService.countWithCache( filters, extraIds );
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return readService.loadValueObjectsWithCache( filters, sort, offset, limit );
    }

    @Override
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris, int maxResults ) {
        return readService.getCategoriesUsageFrequency( filters, extraIds, excludedCategoryUris, excludedTermUris, retainedTermUris, maxResults );
    }

    @Override
    public List<CharacteristicWithUsageStatisticsAndOntologyTerm> getAnnotationsUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, int minFrequency, @Nullable Collection<String> retainedTermUris, int maxResults, boolean includePredicates, boolean includeObjects, long timeout, TimeUnit timeUnit ) throws TimeoutException {
        return readService.getAnnotationsUsageFrequency( filters, extraIds, category, excludedCategoryUris, excludedTermUris, minFrequency, retainedTermUris, maxResults, includePredicates, includeObjects, timeout, timeUnit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt ) {
        Class<? extends DataVector> dvt = quantitationTypeService.getDataVectorType( qt );
        if ( dvt == null ) {
            log.warn( "There are no vectors associated to " + qt + " in " + ee + ", will return no platforms." );
            return Collections.emptySet();
        }
        return this.expressionExperimentDao.getArrayDesignsUsed( ee, qt, dvt );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, QuantitationType qt, Class<? extends DataVector> vectorType ) {
        return this.expressionExperimentDao.getArrayDesignsUsed( ee, qt, vectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesUsedByPreferredVectors( ExpressionExperiment experimentConstraint ) {
        return this.expressionExperimentDao.getGenesUsedByPreferredVectors( experimentConstraint );
    }

    @Override
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        return readService.getTechnologyTypeUsageFrequency( filters, extraIds );
    }

    @Override
    public Map<ArrayDesign, Long> getArrayDesignUsedOrOriginalPlatformUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds, int maxResults ) {
        return readService.getArrayDesignUsedOrOriginalPlatformUsageFrequency( filters, extraIds, maxResults );
    }

    @Override
    public Map<Taxon, Long> getTaxaUsageFrequency( @Nullable Filters filters, @Nullable Set<Long> extraIds ) {
        return readService.getTaxaUsageFrequency( filters, extraIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensionsWithAssays( ExpressionExperiment expressionExperiment ) {
        Collection<BioAssayDimension> bioAssayDimensions = this.expressionExperimentDao
                .getBioAssayDimensions( expressionExperiment );
        bioAssayDimensions.forEach( Thaws::thawBioAssayDimension );
        return bioAssayDimensions;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return expressionExperimentDao.getBioAssayDimension( ee, qt, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimension( ExpressionExperiment ee, QuantitationType qt ) {
        return expressionExperimentDao.getBioAssayDimension( ee, qt );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getProcessedBioAssayDimension( ExpressionExperiment ee ) {
        return getProcessedQuantitationType( ee )
                .map( qt -> expressionExperimentDao.getBioAssayDimension( ee, qt ) )
                .orElse( null );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getProcessedBioAssayDimensionsWithAssays( ExpressionExperiment ee ) {
        Collection<BioAssayDimension> bad = expressionExperimentDao.getProcessedBioAssayDimensions( ee );
        bad.forEach( Thaws::thawBioAssayDimension );
        return bad;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssayDimension> getBioAssayDimensionsWithAssays( ExpressionExperiment ee, QuantitationType qt ) {
        Collection<BioAssayDimension> bad = expressionExperimentDao.getBioAssayDimensions( ee, qt );
        bad.forEach( Thaws::thawBioAssayDimension );
        return bad;
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return expressionExperimentDao.getBioAssayDimensionById( ee, dimensionId, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public BioAssayDimension getBioAssayDimensionById( ExpressionExperiment ee, Long dimensionId ) {
        for ( Class<? extends BulkExpressionDataVector> vectorType : quantitationTypeService.getMappedDataVectorType( BulkExpressionDataVector.class ) ) {
            BioAssayDimension bad = expressionExperimentDao.getBioAssayDimensionById( ee, dimensionId, vectorType );
            if ( bad != null ) {
                return bad;
            }
        }
        return null;
    }

    @Override
    public long getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return readService.getBioMaterialCount( expressionExperiment );
    }

    @Override
    public long getRawDataVectorCount( final ExpressionExperiment ee ) {
        return readService.getRawDataVectorCount( ee );
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        return readService.getExperimentsWithOutliers();
    }

    @Override
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return readService.getLastArrayDesignUpdate( expressionExperiments );
    }

    @Override
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return readService.getLastArrayDesignUpdate( ee );
    }

    @Override
    public Map<Long, AuditEvent> getLastLinkAnalysis( final Collection<Long> ids ) {
        return readService.getLastLinkAnalysis( ids );
    }

    @Override
    public Map<Long, AuditEvent> getLastMissingValueAnalysis( final Collection<Long> ids ) {
        return readService.getLastMissingValueAnalysis( ids );
    }

    @Override
    public Map<Long, AuditEvent> getLastProcessedDataUpdate( final Collection<Long> ids ) {
        return readService.getLastProcessedDataUpdate( ids );
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        return readService.getPerTaxonCount();
    }

    @Override
    public Map<Long, Long> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return readService.getPopulatedFactorCounts( ids );
    }

    @Override
    public Map<Long, Long> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return readService.getPopulatedFactorCountsExcludeBatch( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuantitationType> getPreferredQuantitationType( final ExpressionExperiment ee ) {
        return Optional.ofNullable( this.expressionExperimentDao.getPreferredQuantitationType( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuantitationType> getProcessedQuantitationType( final ExpressionExperiment ee ) {
        return Optional.ofNullable( this.expressionExperimentDao.getProcessedQuantitationType( ee ) );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProcessedExpressionData( ExpressionExperiment ee ) {
        return expressionExperimentDao.hasProcessedExpressionData( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee ) {
        return this.expressionExperimentDao.getQuantitationTypeCount( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        return this.quantitationTypeService.findByExpressionExperiment( expressionExperiment ).values().stream()
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }


    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends DataVector>, Set<QuantitationType>> getQuantitationTypesByVectorType( ExpressionExperiment ee ) {
        return this.quantitationTypeService.findByExpressionExperiment( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return this.quantitationTypeService.findByExpressionExperimentAndDimension( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Class<? extends BulkExpressionDataVector> dataVectorType ) {
        return quantitationTypeService.findByExpressionExperimentAndDimension( expressionExperiment, dimension, Collections.singleton( dataVectorType ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationTypeValueObject> getQuantitationTypeValueObjects( ExpressionExperiment expressionExperiment ) {
        expressionExperiment = ensureInSession( expressionExperiment );
        return quantitationTypeService.loadValueObjectsWithExpressionExperiment( expressionExperiment.getQuantitationTypes(), expressionExperiment );
    }

    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {
        return readService.getSampleRemovalEvents( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( final ExpressionExperiment expressionExperiment ) {
        return this.expressionExperimentDao.getSubSets( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> getSubSetsWithBioAssays( Collection<ExpressionExperiment> expressionExperiments ) {
        return this.expressionExperimentDao.getSubSetsByExpressionExperiments( expressionExperiments );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithCharacteristics( ExpressionExperiment ee ) {
        Collection<ExpressionExperimentSubSet> result = this.expressionExperimentDao.getSubSets( ee );
        for ( ExpressionExperimentSubSet subSet : result ) {
            Hibernate.initialize( subSet.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment ) {
        return expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimensionWithBioAssays( ExpressionExperiment expressionExperiment ) {
        Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> result = expressionExperimentDao.getSubSetsByDimension( expressionExperiment );
        for ( Set<ExpressionExperimentSubSet> subSets : result.values() ) {
            for ( ExpressionExperimentSubSet s : subSets ) {
                for ( BioAssay ba : s.getBioAssays() ) {
                    Hibernate.initialize( ba.getSampleUsed() );
                    Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return expressionExperimentDao.getSubSets( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentDao.getSubSets( expressionExperiment, dimension );
        for ( ExpressionExperimentSubSet s : subSets ) {
            for ( BioAssay ba : s.getSourceExperiment().getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
            for ( BioAssay ba : s.getBioAssays() ) {
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getSampleUsed().getSourceBioMaterial() );
            }
        }
        return subSets;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        // TODO: could this be made more efficient for a single factor?
        return getSubSetsByFactorValueInternal( getSubSetsWithBioAssays( expressionExperiment, dimension ) )
                .get( experimentalFactor );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValueWithCharacteristicsAndBioAssays( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        Map<FactorValue, ExpressionExperimentSubSet> result;
        result = getSubSetsByFactorValue( expressionExperiment, experimentalFactor, dimension );
        if ( result != null ) {
            for ( ExpressionExperimentSubSet subSet : result.values() ) {
                Hibernate.initialize( subSet.getCharacteristics() );
                for ( BioAssay ba : subSet.getBioAssays() ) {
                    Thaws.thawBioAssay( ba );
                }
            }
        }
        return result;
    }

    private Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValueInternal( Collection<ExpressionExperimentSubSet> subSets ) {
        Map<ExperimentalFactor, Map<FactorValue, Set<ExpressionExperimentSubSet>>> result = new HashMap<>();
        for ( ExpressionExperimentSubSet subSet : subSets ) {
            for ( BioAssay ba : subSet.getBioAssays() ) {
                for ( FactorValue fv : ba.getSampleUsed().getAllFactorValues() ) {
                    result.computeIfAbsent( fv.getExperimentalFactor(), k -> new HashMap<>() )
                            .computeIfAbsent( fv, k -> new HashSet<>() )
                            .add( subSet );
                }
            }
        }
        return result.entrySet().stream()
                // only retain FVs that fully separates subsets
                // if there are as many FVs than subsets, we know there is exactly one subset per FV
                .filter( e -> e.getValue().size() == subSets.size() )
                .collect( Collectors.toMap( Map.Entry::getKey, e -> e.getValue().entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, e2 -> e2.getValue().iterator().next() ) ) ) );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristics( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            Hibernate.initialize( result.getCharacteristics() );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristicsAndBioAssays( ExpressionExperiment ee, Long subSetId ) {
        ExpressionExperimentSubSet result = expressionExperimentDao.getSubSetById( ee, subSetId );
        if ( result != null ) {
            result.getSourceExperiment().getBioAssays().forEach( Thaws::thawBioAssay );
            result.getBioAssays().forEach( Thaws::thawBioAssay );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Taxon> getTaxa( Collection<ExpressionExperiment> ees ) {
        return this.expressionExperimentDao.getTaxa( ees );
    }

    @Override
    public Taxon getTaxon( final ExpressionExperiment ee ) {
        return readService.getTaxon( ee );
    }

    @Override
    public boolean isSingleCell( ExpressionExperiment ee ) {
        return ( ee.getCharacteristics().stream()
                .anyMatch( c -> hasCategory( c, Categories.ASSAY ) && hasAnyValue( c,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING_ASSAY,
                        Values.SINGLE_CELL_RNA_SEQUENCING_ASSAY,
                        Values.RNASEQ_OF_CODING_RNA_FROM_SINGLE_CELLS,
                        Values.SINGLE_NUCLEUS_RNA_SEQUENCING,
                        Values.SINGLE_CELL_RNA_SEQUENCING
                ) )
                // exclude FAC-sorted single-cell datasets
                && ee.getCharacteristics().stream()
                .noneMatch( c -> hasCategory( c, Categories.ASSAY )
                        && hasValue( c, Values.FLUORESCENCE_ACTIVATED_CELL_SORTING ) ) )
                || expressionExperimentDao.hasSingleCellQuantitationTypes( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRNASeq( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> ads = this.expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
        /*
         * This isn't completely bulletproof. We are simply assuming that if any of the platforms isn't a microarray (or
         * 'OTHER'), it's RNA-seq.
         */
        for ( ArrayDesign ad : ads ) {
            TechnologyType techtype = ad.getTechnologyType();
            if ( techtype.equals( TechnologyType.SEQUENCING ) || techtype.equals( TechnologyType.GENELIST ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentDao.getArrayDesignsUsed( expressionExperiment );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();
            if ( technologyType.equals( TechnologyType.TWOCOLOR ) || technologyType.equals( TechnologyType.DUALMODE ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param ee the expression experiment to be checked for trouble. This method will usually be preferred over
     *           checking
     *           the curation details of the object directly, as this method also checks all the array designs the
     *           given
     *           experiment belongs to.
     * @return true, if the given experiment, or any of its parenting array designs is troubled. False otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTroubled( ExpressionExperiment ee ) {
        if ( ee.getCurationDetails().getTroubled() )
            return true;
        Collection<ArrayDesign> ads = this.getArrayDesignsUsed( ee );
        for ( ArrayDesign ad : ads ) {
            if ( ad.getCurationDetails().getTroubled() )
                return true;
        }
        return false;
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return readService.loadDetailsValueObjects( ids, taxon, sort, offset, limit );
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsWithCache( Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        return readService.loadDetailsValueObjectsWithCache( ids, taxon, sort, offset, limit );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids ) {
        return readService.loadDetailsValueObjectsByIds( ids );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids ) {
        return readService.loadDetailsValueObjectsByIdsWithCache( ids );
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return readService.loadBlacklistedValueObjects( filters, sort, offset, limit );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return readService.loadLackingFactors();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        return readService.loadLackingTags();
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjectsByIdsWithRelationsAndCache( List<Long> ids ) {
        return readService.loadValueObjectsByIdsWithRelationsAndCache( ids );
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjectsByIds( final List<Long> ids,
            boolean maintainOrder ) {
        return readService.loadValueObjectsByIds( ids, maintainOrder );
    }

    @Override
    public void addCharacteristic( ExpressionExperiment ee, Characteristic vc ) {
        writeService.addCharacteristic( ee, vc );
    }

    @Override
    public void removeCharacteristics( ExpressionExperiment ee, Collection<Characteristic> characteristicsToRemove ) {
        writeService.removeCharacteristics( ee, characteristicsToRemove );
    }

    /**
     * Idempotent set-replace for an EE's direct characteristic set. See the interface javadoc.
     * <p>
     * Implementation: diff current vs desired by (category, categoryUri, value, valueUri) using
     * {@link ubic.gemma.model.common.description.CharacteristicUtils#equals(String, String, String, String)};
     * preserved characteristics retain their identity (no churn for unchanged tags), drops go through
     * {@code characteristicService.remove}, adds get an {@code IC} evidence code by default. Emits a
     * single {@link ManualAnnotationEvent} when the desired set differs from the current set.
     */
    @Override
    @Transactional
    public void updateAnnotations( ExpressionExperiment ee, Collection<Characteristic> desired ) {
        Assert.notNull( desired, "Desired characteristic set must not be null (use an empty collection to clear)." );
        for ( Characteristic vc : desired ) {
            Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Each desired characteristic must have a non-blank category." );
            Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Each desired characteristic must have a non-blank value." );
        }

        ee = ensureInSession( ee );

        Collection<Characteristic> current = ee.getCharacteristics();
        List<Characteristic> toRemove = new ArrayList<>();
        List<Characteristic> toAdd = new ArrayList<>();

        // anything in current not represented in desired -> remove
        for ( Characteristic c : current ) {
            boolean keep = false;
            for ( Characteristic d : desired ) {
                if ( sameTag( c, d ) ) {
                    keep = true;
                    break;
                }
            }
            if ( !keep ) {
                toRemove.add( c );
            }
        }
        // anything in desired not already present -> add
        for ( Characteristic d : desired ) {
            boolean already = false;
            for ( Characteristic c : current ) {
                if ( sameTag( c, d ) ) {
                    already = true;
                    break;
                }
            }
            if ( !already ) {
                Characteristic fresh = Characteristic.Factory.newInstance();
                fresh.setCategory( d.getCategory() );
                fresh.setCategoryUri( d.getCategoryUri() );
                fresh.setValue( d.getValue() );
                fresh.setValueUri( d.getValueUri() );
                fresh.setEvidenceCode( d.getEvidenceCode() != null ? d.getEvidenceCode() : GOEvidenceCode.IC );
                toAdd.add( fresh );
            }
        }

        if ( toRemove.isEmpty() && toAdd.isEmpty() ) {
            log.debug( "updateAnnotations: no change for " + ee.getShortName() + " (ID=" + ee.getId() + ")" );
            return;
        }

        if ( !toRemove.isEmpty() ) {
            Assert.isTrue( toRemove.stream().allMatch( c -> c.getId() != null ), "All characteristics to remove must be persistent." );
            current.removeAll( toRemove );
        }
        if ( !toAdd.isEmpty() ) {
            current.addAll( toAdd );
        }
        update( ee );
        if ( !toRemove.isEmpty() ) {
            characteristicService.remove( toRemove );
        }

        log.info( "updateAnnotations: " + ee.getShortName() + " (ID=" + ee.getId() + ") added=" + toAdd.size()
                + " removed=" + toRemove.size() );
        auditTrailService.addUpdateEvent( ee, ManualAnnotationEvent.class,
                "Replaced annotations via API (added=" + toAdd.size() + ", removed=" + toRemove.size() + ")" );
    }

    private static boolean sameTag( Characteristic a, Characteristic b ) {
        return CharacteristicUtils.equals( a.getCategory(), a.getCategoryUri(), b.getCategory(), b.getCategoryUri() )
                && CharacteristicUtils.equals( a.getValue(), a.getValueUri(), b.getValue(), b.getValueUri() );
    }

    @Override
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return readService.thaw( expressionExperiment );
    }

    @Override
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        return readService.thawLite( expressionExperiment );
    }

    @Override
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        return readService.thawLiter( expressionExperiment );
    }

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some types of associated
     * objects may need to be deleted before this can be run (example: analyses involving multiple experiments; these
     * will not be deleted automatically).
     */
    @Override
    public void remove( ExpressionExperiment ee ) {
        writeService.remove( ee );
    }

    @Override
    public void remove( Collection<ExpressionExperiment> entities ) {
        writeService.remove( entities );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#isBlackListed(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isBlackListed( String geoAccession ) {
        return this.blacklistedEntityService.isBlacklisted( geoAccession );
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean isSuitableForDEA( ExpressionExperiment ee ) {
        AuditEvent ev = auditEventService.getLastEvent( ee, DifferentialExpressionSuitabilityEvent.class );
        return ev == null || !( ev.getEventType() instanceof UnsuitableForDifferentialExpressionAnalysisEvent );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsLackingPublications() {
        return this.expressionExperimentDao.getExperimentsLackingPublications();
    }

    @Override
    public void updateQuantitationType( ExpressionExperiment ee, QuantitationType qt, @Nullable QuantitationType previousPreferredQt ) {
        writeService.updateQuantitationType( ee, qt, previousPreferredQt );
    }

    @Override
    public MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        return writeService.updateMeanVarianceRelation( ee, mvr );
    }

    @Override
    public long countBioMaterials( @Nullable Filters filters ) {
        return readService.countBioMaterials( filters );
    }

    /**
     * Checks for special properties that are allowed to be referenced on certain objects. E.g. characteristics on EEs.
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String property ) {
        if ( property.equals( "geeq.publicSuitabilityScore" ) ) {
            return SecurityConfig.createList( "GROUP_ADMIN" );
        } else {
            return null;
        }
    }
}