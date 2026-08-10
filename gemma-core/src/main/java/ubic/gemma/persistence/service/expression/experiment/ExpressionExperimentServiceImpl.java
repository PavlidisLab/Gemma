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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.hibernate.Hibernate;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.association.GOEvidenceCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.AuditedConditional;
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
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
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
    private BioMaterialService bioMaterialService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private ExperimentalDesignService experimentalDesignService;
    @Autowired
    private FactorValueService factorValueService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;
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
    private ExpressionExperimentSubSetReadService subSetReadService;
    @Autowired
    private ExpressionExperimentDataVectorService dataVectorService;
    @Autowired
    private ubic.gemma.persistence.service.common.description.CharacteristicService characteristicService;
    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService auditTrailService;
    /**
     * Self-reference through the Spring proxy. Used by {@link #commitCuration} to invoke
     * {@link #applyDesignChange} so its {@code @AuditedConditional} aspect still fires — a same-class
     * {@code this.applyDesignChange(...)} would join the transaction but bypass the proxy (and thus the audit).
     * {@code @Lazy} avoids a circular-init failure on the self-injection.
     */
    @Autowired
    @Lazy
    private ExpressionExperimentService self;
    /**
     * Used inside {@link #commitCuration} to flush after tag/sample-characteristic adds (so the cascaded inserts
     * assign their ids in time to echo {@code clientRef → newId}) and to bump the curation {@code lastUpdated}
     * concurrency token on any change.
     */
    @Autowired
    private SessionFactory sessionFactory;

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
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return dataVectorService.getRawDataVectors( ee, qt );
    }

    @Override
    public Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType qt ) {
        return dataVectorService.getRawDataVectors( ee, samples, qt );
    }

    @Override
    public Collection<RawExpressionDataVector> getPreferredRawDataVectors( ExpressionExperiment expressionExperiment ) {
        return dataVectorService.getPreferredRawDataVectors( expressionExperiment );
    }

    @Override
    public Map<QuantitationType, Collection<RawExpressionDataVector>> getMissingValuesVectors( ExpressionExperiment ee ) {
        return dataVectorService.getMissingValuesVectors( ee );
    }

    @Override
    public int addRawDataVectors( ExpressionExperiment ee,
            QuantitationType quantitationType,
            Collection<RawExpressionDataVector> newVectors ) {
        return dataVectorService.addRawDataVectors( ee, quantitationType, newVectors );
    }

    @Override
    public int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType qt, Collection<RawExpressionDataVector> vectors ) {
        return dataVectorService.replaceRawDataVectors( ee, qt, vectors );
    }

    @Override
    public int replaceAllRawDataVectors( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {
        return dataVectorService.replaceAllRawDataVectors( ee, newVectors );
    }

    @Override
    public int removeAllRawDataVectors( ExpressionExperiment ee ) {
        return dataVectorService.removeAllRawDataVectors( ee );
    }

    @Override
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return dataVectorService.removeRawDataVectors( ee, qt );
    }

    @Override
    public int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        return dataVectorService.removeRawDataVectors( ee, qt, keepDimension );
    }

    @Override
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee ) {
        return dataVectorService.getProcessedDataVectors( ee );
    }

    @Override
    public Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays ) {
        return dataVectorService.getProcessedDataVectors( ee, assays );
    }

    @Override
    public int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        return dataVectorService.createProcessedDataVectors( ee, vectors );
    }

    @Override
    public int removeProcessedDataVectors( ExpressionExperiment ee ) {
        return dataVectorService.removeProcessedDataVectors( ee );
    }

    @Override
    public int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors ) {
        return dataVectorService.replaceProcessedDataVectors( ee, vectors );
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
    @Transactional(readOnly = true)
    public DesignPreflightReport previewDesignChange( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        ee = expressionExperimentDao.reload( ee );
        DesignPreflightReport report = new DesignPreflightReport();
        DesignPreflightReport.Summary summary = report.getSummary();

        ExperimentalDesign ed = ee.getExperimentalDesign();
        // If the current ee is lacking a design, set currentFactors to empty
        Collection<ExperimentalFactor> currentFactors = ed != null
                ? ed.getExperimentalFactors() : Collections.emptyList();

        // ---- thaw what we need to walk ----
        for ( ExperimentalFactor ef : currentFactors ) {
            Hibernate.initialize( ef.getFactorValues() );
        }
        Hibernate.initialize( ee.getBioAssays() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null ) {
                Thaws.thawBioMaterial( bm );
            }
        }

        // ---- index current state ----
        Map<Long, ExperimentalFactor> currentFactorsById = new HashMap<>();
        Map<Long, FactorValue> currentFvsById = new HashMap<>();
        Map<Long, ExperimentalFactor> currentFvParentByFvId = new HashMap<>();
        for ( ExperimentalFactor ef : currentFactors ) {
            currentFactorsById.put( ef.getId(), ef );
            for ( FactorValue fv : ef.getFactorValues() ) {
                currentFvsById.put( fv.getId(), fv );
                currentFvParentByFvId.put( fv.getId(), ef );
            }
        }
        Map<Long, BioMaterial> currentBmsById = new HashMap<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null && bm.getId() != null ) {
                currentBmsById.put( bm.getId(), bm );
            }
        }

        // ---- index proposed state and validate ----
        Set<Long> proposedFactorIds = new HashSet<>();
        Set<Long> proposedFvIds = new HashSet<>();
        Map<Long, Long> proposedFvIdToProposedFactorId = new HashMap<>();
        if ( proposed.getExperimentalFactors() != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
                if ( pf.getId() != null ) {
                    proposedFactorIds.add( pf.getId() );
                    ExperimentalFactor existing = currentFactorsById.get( pf.getId() );
                    if ( existing == null ) {
                        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                "UNKNOWN_FACTOR_ID",
                                "Proposed factor id " + pf.getId() + " is not part of this experiment's design." );
                        b.setFactorId( pf.getId() );
                        report.getBlockers().add( b );
                    } else if ( pf.getType() != null ) {
                        String existingType = existing.getType() != null && existing.getType().equals( FactorType.CONTINUOUS ) ? "continuous" : "categorical";
                        if ( !pf.getType().equalsIgnoreCase( existingType ) && !existing.getFactorValues().isEmpty() ) {
                            DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                    "FACTOR_TYPE_CHANGE_WITH_VALUES",
                                    "Cannot change the type of factor " + pf.getId() + " from " + existingType + " to " + pf.getType()
                                            + " while it has " + existing.getFactorValues().size() + " factor value(s)." );
                            b.setFactorId( pf.getId() );
                            report.getBlockers().add( b );
                        }
                    }
                } else {
                    summary.setFactorsToCreate( summary.getFactorsToCreate() + 1 );
                }
                if ( pf.getValues() != null ) {
                    for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                        if ( pv.getId() != null ) {
                            proposedFvIds.add( pv.getId() );
                            if ( pf.getId() != null ) {
                                proposedFvIdToProposedFactorId.put( pv.getId(), pf.getId() );
                            }
                            FactorValue existingFv = currentFvsById.get( pv.getId() );
                            if ( existingFv == null ) {
                                DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                        "UNKNOWN_FACTOR_VALUE_ID",
                                        "Proposed factor value id " + pv.getId() + " is not part of this experiment's design." );
                                b.setFactorValueId( pv.getId() );
                                report.getBlockers().add( b );
                            } else if ( pf.getId() != null && existingFv.getExperimentalFactor() != null
                                    && !pf.getId().equals( existingFv.getExperimentalFactor().getId() ) ) {
                                DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                        "FACTOR_VALUE_PARENT_MISMATCH",
                                        "Factor value " + pv.getId() + " currently belongs to factor "
                                                + existingFv.getExperimentalFactor().getId()
                                                + " but is being moved under factor " + pf.getId() + "." );
                                b.setFactorValueId( pv.getId() );
                                b.setFactorId( pf.getId() );
                                report.getBlockers().add( b );
                            } else if ( existingFv != null && pv.getStatements() != null ) {
                                Set<Long> existingStmtIds = existingFv.getCharacteristics().stream()
                                        .map( Statement::getId ).collect( Collectors.toSet() );
                                for ( StatementValueObject ps : pv.getStatements() ) {
                                    if ( ps.getId() != null && !existingStmtIds.contains( ps.getId() ) ) {
                                        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                                "UNKNOWN_STATEMENT_ID",
                                                "Statement id " + ps.getId() + " does not belong to factor value " + pv.getId() + "." );
                                        b.setFactorValueId( pv.getId() );
                                        b.setStatementId( ps.getId() );
                                        report.getBlockers().add( b );
                                    }
                                }
                            }
                        } else {
                            summary.setFactorValuesToCreate( summary.getFactorValuesToCreate() + 1 );
                        }
                    }
                    // At most one baseline per factor: reject a payload that marks more than one FV as baseline.
                    long baselineCount = pf.getValues().stream()
                            .filter( bpv -> Boolean.TRUE.equals( bpv.getBaseline() ) )
                            .count();
                    if ( baselineCount > 1 ) {
                        DesignPreflightReport.Blocker bb = new DesignPreflightReport.Blocker(
                                "MULTIPLE_BASELINES",
                                "Factor " + ( pf.getId() != null ? pf.getId() : "\"" + pf.getName() + "\"" )
                                        + " designates " + baselineCount + " factor values as baseline; at most one is allowed." );
                        bb.setFactorId( pf.getId() );
                        report.getBlockers().add( bb );
                    }
                }
            }
        }

        // ---- validate biomaterial assignments ----
        if ( proposed.getBioMaterialAssignments() != null ) {
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : proposed.getBioMaterialAssignments() ) {
                if ( a.getBioMaterialId() == null || !currentBmsById.containsKey( a.getBioMaterialId() ) ) {
                    DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                            "UNKNOWN_BIOMATERIAL_ID",
                            "Biomaterial " + a.getBioMaterialId() + " is not part of this experiment." );
                    b.setBioMaterialId( a.getBioMaterialId() );
                    report.getBlockers().add( b );
                    continue;
                }
                if ( a.getFactorValueIds() != null ) {
                    for ( Long fvId : a.getFactorValueIds() ) {
                        if ( !proposedFvIds.contains( fvId ) ) {
                            DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                    "ASSIGNMENT_REFERENCES_UNKNOWN_FV",
                                    "Biomaterial " + a.getBioMaterialId() + " is assigned to factor value " + fvId
                                            + " which is not present in the proposed design." );
                            b.setBioMaterialId( a.getBioMaterialId() );
                            b.setFactorValueId( fvId );
                            report.getBlockers().add( b );
                        }
                    }
                }
            }
        }

        // ---- impact: deletions ----
        List<ExperimentalFactor> factorsBeingDeleted = new ArrayList<>();
        for ( ExperimentalFactor ef : currentFactors ) {
            if ( !proposedFactorIds.contains( ef.getId() ) ) {
                factorsBeingDeleted.add( ef );
                report.getFactorsToDelete().add( new DesignPreflightReport.EntityRef( ef.getId(), ef.getName() ) );
            }
        }
        summary.setFactorsToDelete( factorsBeingDeleted.size() );

        Set<Long> fvIdsBeingDeleted = new HashSet<>();
        List<FactorValue> fvsBeingDeleted = new ArrayList<>();
        for ( FactorValue fv : currentFvsById.values() ) {
            // a FV is deleted if its id is not in the proposed set, OR its parent factor is being deleted
            boolean parentBeingDeleted = factorsBeingDeleted.stream()
                    .anyMatch( ef -> ef.getId().equals( currentFvParentByFvId.get( fv.getId() ).getId() ) );
            if ( !proposedFvIds.contains( fv.getId() ) || parentBeingDeleted ) {
                fvsBeingDeleted.add( fv );
                fvIdsBeingDeleted.add( fv.getId() );
                report.getFactorValuesToDelete().add(
                        new DesignPreflightReport.EntityRef( fv.getId(), FactorValueUtils.getSummaryString( fv ) ) );
            }
        }
        summary.setFactorValuesToDelete( fvsBeingDeleted.size() );

        // ---- precompute the proposed BM->FV map (used by both DE-analysis and BM-change detection) ----
        Map<Long, Set<Long>> proposedAssignByBmId = new HashMap<>();
        if ( proposed.getBioMaterialAssignments() != null ) {
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : proposed.getBioMaterialAssignments() ) {
                if ( a.getBioMaterialId() != null && a.getFactorValueIds() != null ) {
                    proposedAssignByBmId.put( a.getBioMaterialId(), new HashSet<>( a.getFactorValueIds() ) );
                }
            }
        }

        // ---- impact: differential expression analyses ----
        // A factor is "affected" (and therefore its DE analyses must be deleted) when:
        //   (a) the factor itself is being deleted;
        //   (b) any of its FactorValues is being deleted (Gemma's existing cascade rule, see FactorValueDeletionImpl);
        //   (c) a new FactorValue is being added under it (changes the design space);
        //   (d) any biomaterial assignment to one of its FactorValues changes (different sample groupings -> different
        //       analysis result, even if every row still exists).
        // Statement edits on a kept FV do NOT count: the analysis math is unchanged, only labels would be stale.
        Set<Long> factorIdsAffected = new HashSet<>();
        for ( ExperimentalFactor ef : factorsBeingDeleted ) {
            factorIdsAffected.add( ef.getId() );
        }
        for ( FactorValue fv : fvsBeingDeleted ) {
            ExperimentalFactor parent = currentFvParentByFvId.get( fv.getId() );
            if ( parent != null ) {
                factorIdsAffected.add( parent.getId() );
            }
        }
        // (c) new FV being added under an existing factor
        if ( proposed.getExperimentalFactors() != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
                if ( pf.getId() == null || pf.getValues() == null ) continue;
                for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                    if ( pv.getId() == null ) {
                        factorIdsAffected.add( pf.getId() );
                        break;
                    }
                }
            }
        }
        // (d) biomaterial assignment changes — for each FV whose membership set changed, mark its parent factor
        for ( Map.Entry<Long, BioMaterial> e : currentBmsById.entrySet() ) {
            Set<Long> currentFvIds = e.getValue().getAllFactorValues().stream()
                    .map( FactorValue::getId ).collect( Collectors.toSet() );
            Set<Long> proposedFvIdsForBm = proposedAssignByBmId.getOrDefault( e.getKey(), Collections.emptySet() );
            if ( currentFvIds.equals( proposedFvIdsForBm ) ) continue;
            // factor membership changed for this biomaterial — flag every involved factor
            Set<Long> changedFvIds = new HashSet<>( currentFvIds );
            changedFvIds.addAll( proposedFvIdsForBm );
            Set<Long> commonFvIds = new HashSet<>( currentFvIds );
            commonFvIds.retainAll( proposedFvIdsForBm );
            changedFvIds.removeAll( commonFvIds );
            for ( Long fvId : changedFvIds ) {
                ExperimentalFactor parent = currentFvParentByFvId.get( fvId );
                if ( parent != null ) {
                    factorIdsAffected.add( parent.getId() );
                }
                // Proposed-new FVs (id != null but not in currentFvParentByFvId) are unreachable here because
                // they have id == null in the proposal; their parent factor is already flagged by rule (c).
            }
        }

        Set<Long> seenAnalysisIds = new HashSet<>();
        for ( Long efId : factorIdsAffected ) {
            ExperimentalFactor ef = currentFactorsById.get( efId );
            if ( ef == null ) continue;
            for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByFactor( ef ) ) {
                if ( seenAnalysisIds.add( a.getId() ) ) {
                    Long subsetFvId = a.getSubsetFactorValue() != null ? a.getSubsetFactorValue().getId() : null;
                    report.getDifferentialExpressionAnalysesToDelete().add(
                            new DesignPreflightReport.AnalysisRef( a.getId(), a.getName(), subsetFvId ) );
                }
            }
        }
        // Also: subset-level analyses anchored on a deleted FV (subsetFactorValue FK becomes dangling)
        for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByExperiment( ee, true ) ) {
            FactorValue subsetFv = a.getSubsetFactorValue();
            if ( subsetFv != null && fvIdsBeingDeleted.contains( subsetFv.getId() ) && seenAnalysisIds.add( a.getId() ) ) {
                report.getDifferentialExpressionAnalysesToDelete().add(
                        new DesignPreflightReport.AnalysisRef( a.getId(), a.getName(), subsetFv.getId() ) );
            }
        }
        summary.setDifferentialExpressionAnalysesToDelete( report.getDifferentialExpressionAnalysesToDelete().size() );

        // ---- impact: subsets with stale anchor (informational) ----
        // Heuristic: a subset is "anchored" on a factor value when every biomaterial in the subset carries
        // that FV. If any anchor FV is being deleted, flag the subset.
        Collection<ExpressionExperimentSubSet> subsets = this.getSubSetsWithBioAssays( ee );
        for ( ExpressionExperimentSubSet ss : subsets ) {
            Set<Long> sharedFvIds = null;
            for ( BioAssay ba : ss.getBioAssays() ) {
                BioMaterial bm = ba.getSampleUsed();
                if ( bm == null ) continue;
                Set<Long> bmFvIds = bm.getAllFactorValues().stream().map( FactorValue::getId ).collect( Collectors.toSet() );
                if ( sharedFvIds == null ) {
                    sharedFvIds = new HashSet<>( bmFvIds );
                } else {
                    sharedFvIds.retainAll( bmFvIds );
                }
            }
            if ( sharedFvIds == null ) continue;
            List<Long> lost = sharedFvIds.stream().filter( fvIdsBeingDeleted::contains ).collect( Collectors.toList() );
            if ( !lost.isEmpty() ) {
                report.getSubsetsWithStaleAnchor().add(
                        new DesignPreflightReport.SubsetRef( ss.getId(), ss.getName(), lost ) );
            }
        }
        summary.setSubsetsWithStaleAnchor( report.getSubsetsWithStaleAnchor().size() );

        // ---- impact: biomaterials with changed assignments ----
        int changedBmCount = 0;
        for ( Map.Entry<Long, BioMaterial> e : currentBmsById.entrySet() ) {
            Set<Long> currentFvIds = e.getValue().getAllFactorValues().stream()
                    .map( FactorValue::getId ).collect( Collectors.toSet() );
            Set<Long> proposedFvIdsForBm = proposedAssignByBmId.getOrDefault( e.getKey(), Collections.emptySet() );
            if ( !currentFvIds.equals( proposedFvIdsForBm ) ) {
                changedBmCount++;
            }
        }
        summary.setBiomaterialsWithChangedAssignments( changedBmCount );

        return report;
    }

    @Override
    @Transactional
    @AuditedConditional(value = DesignChangeEvent.class,
            when = "#result.applied",
            messageSpel = "'Design replaced via REST: factors +' + #result.preflightAtApply.summary.factorsToCreate + ' / -' + #result.preflightAtApply.summary.factorsToDelete + ', factor values +' + #result.preflightAtApply.summary.factorValuesToCreate + ' / -' + #result.preflightAtApply.summary.factorValuesToDelete + ', biomaterial assignments changed: ' + #result.preflightAtApply.summary.biomaterialsWithChangedAssignments + ', analyses removed: ' + #result.preflightAtApply.summary.differentialExpressionAnalysesToDelete + '.'")
    public DesignApplyOutcome applyDesignChange( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        Assert.notNull( proposed, "A proposed design must be supplied." );
        ee = expressionExperimentDao.reload( ee );

        // Re-run preflight as the authoritative gate. The REST layer also runs it for client feedback, but we
        // re-check here so direct service callers can't bypass validation.
        DesignPreflightReport report = previewDesignChange( ee, proposed );
        if ( !report.getBlockers().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot apply proposed design: "
                    + report.getBlockers().get( 0 ).getMessage() );
        }

        // Idempotent no-op short-circuit. If the apply-time preflight reports zero changes across every dimension
        // (factors, factor values, biomaterial assignments, design-level metadata), return applied=false without
        // mutating anything. @AuditedConditional's `when` predicate then suppresses the audit row.
        if ( isNoOpDesignApply( ee, report, proposed ) ) {
            return new DesignApplyOutcome( false, getExperimentalDesignValueObject( ee ), report );
        }

        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null ) {
            // if experiment doesn't have a design, create one.
            ed = ExperimentalDesign.Factory.newInstance();
            ed = experimentalDesignService.save( ed );
            ee.setExperimentalDesign( ed );
        }

        // Thaw what we'll mutate. Mirrors previewDesignChange.
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

        Map<Long, ExperimentalFactor> currentFactorsById = new HashMap<>();
        Map<Long, FactorValue> currentFvsById = new HashMap<>();
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            currentFactorsById.put( ef.getId(), ef );
            for ( FactorValue fv : ef.getFactorValues() ) {
                currentFvsById.put( fv.getId(), fv );
            }
        }
        Map<Long, BioMaterial> currentBmsById = new HashMap<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null && bm.getId() != null ) {
                currentBmsById.put( bm.getId(), bm );
            }
        }

        Set<Long> proposedFactorIds = new HashSet<>();
        Set<Long> proposedFvIds = new HashSet<>();
        if ( proposed.getExperimentalFactors() != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
                if ( pf.getId() != null ) {
                    proposedFactorIds.add( pf.getId() );
                }
                if ( pf.getValues() != null ) {
                    for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                        if ( pv.getId() != null ) {
                            proposedFvIds.add( pv.getId() );
                        }
                    }
                }
            }
        }

        // ---- step 1: drop biomaterial -> deleted-FV links eagerly so subsequent FV removes don't trip FKs ----
        Set<Long> fvIdsBeingDeleted = currentFvsById.keySet().stream()
                .filter( id -> !proposedFvIds.contains( id ) )
                .collect( Collectors.toCollection( HashSet::new ) );
        // Also fold in FVs whose parent factor is being deleted.
        for ( Map.Entry<Long, ExperimentalFactor> e : currentFactorsById.entrySet() ) {
            if ( !proposedFactorIds.contains( e.getKey() ) ) {
                for ( FactorValue fv : e.getValue().getFactorValues() ) {
                    if ( fv.getId() != null ) {
                        fvIdsBeingDeleted.add( fv.getId() );
                    }
                }
            }
        }
        Set<BioMaterial> dirtyBms = new HashSet<>();
        for ( BioMaterial bm : currentBmsById.values() ) {
            if ( bm.getFactorValues().removeIf( fv -> fvIdsBeingDeleted.contains( fv.getId() ) ) ) {
                dirtyBms.add( bm );
            }
        }

        // ---- step 2: remove diff-ex analyses dependent on factors being deleted (or that become invalid) ----
        // Step 3 (factor removal) will also cascade DE analyses through ExperimentalFactorService#remove, so we
        // only explicitly remove analyses tied to surviving factors whose membership / structure changed in a way
        // that the FV/factor cascade won't reach. The preflight already enumerated these (see factorIdsAffected).
        Set<Long> factorIdsAffected = computeAffectedFactorIds( currentFactorsById, currentFvsById,
                proposedFactorIds, fvIdsBeingDeleted, proposed, currentBmsById );
        Set<Long> removedAnalysisIds = new HashSet<>();
        for ( Long efId : factorIdsAffected ) {
            ExperimentalFactor ef = currentFactorsById.get( efId );
            if ( ef == null ) continue;
            for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByFactor( ef ) ) {
                if ( removedAnalysisIds.add( a.getId() ) ) {
                    differentialExpressionAnalysisService.remove( a );
                }
            }
        }
        // Subset-anchored analyses pointing at deleted FVs would dangle.
        for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByExperiment( ee, true ) ) {
            FactorValue subsetFv = a.getSubsetFactorValue();
            if ( subsetFv != null && fvIdsBeingDeleted.contains( subsetFv.getId() )
                    && removedAnalysisIds.add( a.getId() ) ) {
                differentialExpressionAnalysisService.remove( a );
            }
        }

        // ---- step 3: delete factor values whose parent factor survives but the FV itself was dropped ----
        Set<Long> standaloneFvDeletes = new HashSet<>();
        for ( Long fvId : fvIdsBeingDeleted ) {
            FactorValue fv = currentFvsById.get( fvId );
            if ( fv == null ) continue;
            ExperimentalFactor parent = fv.getExperimentalFactor();
            if ( parent != null && proposedFactorIds.contains( parent.getId() ) ) {
                standaloneFvDeletes.add( fvId );
            }
        }
        for ( Long fvId : standaloneFvDeletes ) {
            FactorValue fv = currentFvsById.get( fvId );
            ExperimentalFactor parent = fv.getExperimentalFactor();
            parent.getFactorValues().remove( fv );
            factorValueService.remove( fv );
        }

        // ---- step 4: delete factors that were dropped (cascades remaining FVs and any tied analyses) ----
        List<ExperimentalFactor> factorsToRemove = new ArrayList<>();
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            if ( !proposedFactorIds.contains( ef.getId() ) ) {
                factorsToRemove.add( ef );
            }
        }
        for ( ExperimentalFactor ef : factorsToRemove ) {
            ed.getExperimentalFactors().remove( ef );
            experimentalFactorService.remove( ef );
        }

        // ---- step 5: update kept factors and create new ones ----
        if ( proposed.getExperimentalFactors() != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
                if ( pf.getId() != null ) {
                    ExperimentalFactor ef = currentFactorsById.get( pf.getId() );
                    updateFactorMetadata( ef, pf );
                    applyFactorValueChanges( ef, pf, currentFvsById );
                } else {
                    ExperimentalFactor created = createFactor( ed, ee, pf );
                    ed.getExperimentalFactors().add( created );
                    currentFactorsById.put( created.getId(), created );
                    for ( FactorValue fv : created.getFactorValues() ) {
                        currentFvsById.put( fv.getId(), fv );
                    }
                }
            }
        }

        // ---- step 6: apply biomaterial -> FV assignments ----
        if ( proposed.getBioMaterialAssignments() != null ) {
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : proposed.getBioMaterialAssignments() ) {
                BioMaterial bm = currentBmsById.get( a.getBioMaterialId() );
                if ( bm == null ) continue; // preflight should have caught this
                Set<Long> targetFvIds = a.getFactorValueIds() != null
                        ? new HashSet<>( a.getFactorValueIds() )
                        : Collections.emptySet();
                // Inherited (sourceBioMaterial) FVs are not part of this BM's directly-owned set and cannot be
                // mutated here; treat them as immutable and reconcile only what bm.factorValues directly holds.
                Set<Long> inheritedFvIds = bm.getAllFactorValues().stream()
                        .map( FactorValue::getId )
                        .filter( java.util.Objects::nonNull )
                        .collect( Collectors.toSet() );
                inheritedFvIds.removeAll( bm.getFactorValues().stream().map( FactorValue::getId )
                        .filter( java.util.Objects::nonNull ).collect( Collectors.toSet() ) );

                Set<Long> desiredOwn = new HashSet<>( targetFvIds );
                desiredOwn.removeAll( inheritedFvIds );
                Set<Long> currentOwn = bm.getFactorValues().stream().map( FactorValue::getId )
                        .filter( java.util.Objects::nonNull ).collect( Collectors.toSet() );
                if ( !desiredOwn.equals( currentOwn ) ) {
                    bm.getFactorValues().removeIf( fv -> !desiredOwn.contains( fv.getId() ) );
                    Set<Long> toAdd = new HashSet<>( desiredOwn );
                    toAdd.removeAll( currentOwn );
                    for ( Long fvId : toAdd ) {
                        FactorValue fv = currentFvsById.get( fvId );
                        if ( fv != null ) {
                            bm.getFactorValues().add( fv );
                        }
                    }
                    dirtyBms.add( bm );
                }
            }
        }
        if ( !dirtyBms.isEmpty() ) {
            bioMaterialService.update( dirtyBms );
        }

        // ---- step 7: update design-level metadata ----
        boolean edMetadataChanged = false;
        if ( !Objects.equals( ed.getName(), proposed.getName() ) ) {
            ed.setName( proposed.getName() );
            edMetadataChanged = true;
        }
        if ( !Objects.equals( ed.getDescription(), proposed.getDescription() ) ) {
            ed.setDescription( proposed.getDescription() );
            edMetadataChanged = true;
        }
        if ( !Objects.equals( ed.getReplicateDescription(), proposed.getReplicateDescription() ) ) {
            ed.setReplicateDescription( proposed.getReplicateDescription() );
            edMetadataChanged = true;
        }
        if ( !Objects.equals( ed.getQualityControlDescription(), proposed.getQualityControlDescription() ) ) {
            ed.setQualityControlDescription( proposed.getQualityControlDescription() );
            edMetadataChanged = true;
        }
        if ( !Objects.equals( ed.getNormalizationDescription(), proposed.getNormalizationDescription() ) ) {
            ed.setNormalizationDescription( proposed.getNormalizationDescription() );
            edMetadataChanged = true;
        }
        if ( edMetadataChanged ) {
            experimentalDesignService.update( ed );
        }

        // ---- step 8: audit event ----
        // Emitted declaratively via @AuditedConditional on the public method (DesignChangeEvent). The aspect
        // fires only when #result.applied is true, which suppresses the no-op early-return branch above.
        return new DesignApplyOutcome( true, getExperimentalDesignValueObject( ee ), report );
    }

    /**
     * Decide whether the proposed design change is a no-op against the current state.
     * <p>
     * Returns true iff the preflight summary shows zero changes AND the design-level metadata fields would not
     * change either. The metadata check mirrors step 7 of the apply path; the summary check covers steps 3-6
     * (factor / factor value / biomaterial assignment deltas).
     */
    private boolean isNoOpDesignApply( ExpressionExperiment ee, DesignPreflightReport report,
            ExperimentalDesignValueObject proposed ) {
        DesignPreflightReport.Summary s = report.getSummary();
        if ( s.getFactorsToCreate() > 0 || s.getFactorsToDelete() > 0
                || s.getFactorValuesToCreate() > 0 || s.getFactorValuesToDelete() > 0
                || s.getBiomaterialsWithChangedAssignments() > 0
                || s.getDifferentialExpressionAnalysesToDelete() > 0 ) {
            return false;
        }
        // The summary counters above track only structural add/delete of factors, factor values, and biomaterial
        // assignments. An in-place edit to a KEPT factor value (its baseline flag, deprecated value, or statement
        // set) leaves every counter at zero, so without this check such a PUT would be mistaken for a no-op and
        // silently dropped — the mutation in applyFactorValueChanges / updateFactorValueStatements would never be
        // reached.
        if ( hasKeptFactorValueEdits( ee, proposed ) ) {
            return false;
        }
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null ) {
            // current design is null; proposal that introduces any non-null metadata is not a no-op
            return proposed.getName() == null && proposed.getDescription() == null
                    && proposed.getReplicateDescription() == null
                    && proposed.getQualityControlDescription() == null
                    && proposed.getNormalizationDescription() == null;
        }
        return Objects.equals( ed.getName(), proposed.getName() )
                && Objects.equals( ed.getDescription(), proposed.getDescription() )
                && Objects.equals( ed.getReplicateDescription(), proposed.getReplicateDescription() )
                && Objects.equals( ed.getQualityControlDescription(), proposed.getQualityControlDescription() )
                && Objects.equals( ed.getNormalizationDescription(), proposed.getNormalizationDescription() );
    }

    /**
     * Whether {@code proposed} carries an in-place edit to an existing (kept) factor value that the structural
     * preflight summary does not count: a baseline-flag change or a deprecated-{@code value} change. Both honour the
     * {@code null = "no change"} convention used by {@link #applyFactorValueChanges}. Used by
     * {@link #isNoOpDesignApply} so such edits are not short-circuited away.
     */
    private boolean hasKeptFactorValueEdits( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null || proposed.getExperimentalFactors() == null ) {
            return false;
        }
        Map<Long, FactorValue> currentFvsById = new HashMap<>();
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            for ( FactorValue fv : ef.getFactorValues() ) {
                currentFvsById.put( fv.getId(), fv );
            }
        }
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
            if ( pf.getValues() == null ) continue;
            for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                if ( pv.getId() == null ) continue; // creations are already counted in the summary
                FactorValue cur = currentFvsById.get( pv.getId() );
                if ( cur == null ) continue; // unknown id — a blocker, surfaced by previewDesignChange
                if ( pv.getBaseline() != null && !Objects.equals( pv.getBaseline(), cur.getIsBaseline() ) ) {
                    return true;
                }
                //noinspection deprecation
                if ( pv.getValue() != null && !Objects.equals( pv.getValue(), cur.getValue() ) ) {
                    return true;
                }
                // Statements are replaced wholesale by updateFactorValueStatements when the payload provides them
                // (null = "no change"). Compare by content so an add / remove / edit registers, while a pure
                // round-trip that echoes the same statements stays a no-op.
                if ( pv.getStatements() != null && statementsChanged( cur, pv.getStatements() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the proposed statement set differs in content from what the factor value currently carries. Compares
     * a multiset of {@link #statementContentKey content keys} so ordering and database ids are irrelevant — only
     * add / remove / field edits count. Echoing the current statements verbatim (the common baseline-edit
     * round-trip) yields equal multisets and is therefore not a change.
     */
    private static boolean statementsChanged( FactorValue cur, List<StatementValueObject> proposed ) {
        Map<String, Integer> currentKeys = new HashMap<>();
        for ( Statement s : cur.getCharacteristics() ) {
            currentKeys.merge( statementContentKey( s ), 1, Integer::sum );
        }
        Map<String, Integer> proposedKeys = new HashMap<>();
        for ( StatementValueObject ps : proposed ) {
            proposedKeys.merge( statementContentKey( ps ), 1, Integer::sum );
        }
        return !currentKeys.equals( proposedKeys );
    }

    private Set<Long> computeAffectedFactorIds( Map<Long, ExperimentalFactor> currentFactorsById,
            Map<Long, FactorValue> currentFvsById, Set<Long> proposedFactorIds, Set<Long> fvIdsBeingDeleted,
            ExperimentalDesignValueObject proposed, Map<Long, BioMaterial> currentBmsById ) {
        Set<Long> affected = new HashSet<>();
        // factor being deleted -> handled by experimentalFactorService.remove later; not in this set
        // factor losing a FV but staying -> affected
        for ( Long fvId : fvIdsBeingDeleted ) {
            FactorValue fv = currentFvsById.get( fvId );
            if ( fv != null && fv.getExperimentalFactor() != null
                    && proposedFactorIds.contains( fv.getExperimentalFactor().getId() ) ) {
                affected.add( fv.getExperimentalFactor().getId() );
            }
        }
        // new FV under existing factor -> affected
        if ( proposed.getExperimentalFactors() != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
                if ( pf.getId() == null || pf.getValues() == null ) continue;
                for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                    if ( pv.getId() == null ) {
                        affected.add( pf.getId() );
                        break;
                    }
                }
            }
        }
        // biomaterial assignment changes -> affected (only via parent factor of changed FVs)
        Map<Long, Set<Long>> proposedAssignByBmId = new HashMap<>();
        if ( proposed.getBioMaterialAssignments() != null ) {
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : proposed.getBioMaterialAssignments() ) {
                if ( a.getBioMaterialId() != null && a.getFactorValueIds() != null ) {
                    proposedAssignByBmId.put( a.getBioMaterialId(), new HashSet<>( a.getFactorValueIds() ) );
                }
            }
        }
        for ( Map.Entry<Long, BioMaterial> e : currentBmsById.entrySet() ) {
            Set<Long> currentFvIds = e.getValue().getAllFactorValues().stream()
                    .map( FactorValue::getId ).collect( Collectors.toSet() );
            Set<Long> proposedFvIdsForBm = proposedAssignByBmId.getOrDefault( e.getKey(), Collections.emptySet() );
            if ( currentFvIds.equals( proposedFvIdsForBm ) ) continue;
            Set<Long> changed = new HashSet<>( currentFvIds );
            changed.addAll( proposedFvIdsForBm );
            Set<Long> common = new HashSet<>( currentFvIds );
            common.retainAll( proposedFvIdsForBm );
            changed.removeAll( common );
            for ( Long fvId : changed ) {
                FactorValue fv = currentFvsById.get( fvId );
                if ( fv != null && fv.getExperimentalFactor() != null
                        && proposedFactorIds.contains( fv.getExperimentalFactor().getId() ) ) {
                    affected.add( fv.getExperimentalFactor().getId() );
                }
            }
        }
        return affected;
    }

    private void updateFactorMetadata( ExperimentalFactor ef, ExperimentalDesignValueObject.ExperimentalFactorEntry pf ) {
        if ( pf.getName() != null ) {
            ef.setName( pf.getName() );
        }
        if ( pf.getDescription() != null ) {
            ef.setDescription( pf.getDescription() );
        }
        if ( pf.getType() != null ) {
            FactorType proposedType = "continuous".equalsIgnoreCase( pf.getType() )
                    ? FactorType.CONTINUOUS : FactorType.CATEGORICAL;
            // Switching type with values is blocked at preflight; only takes effect when the factor has no FVs.
            if ( ef.getFactorValues().isEmpty() ) {
                ef.setType( proposedType );
            }
        }
        if ( pf.getCategory() != null && ef.getCategory() != null ) {
            ef.getCategory().setCategory( pf.getCategory().getCategory() );
            ef.getCategory().setCategoryUri( pf.getCategory().getCategoryUri() );
            ef.getCategory().setValue( pf.getCategory().getValue() );
            ef.getCategory().setValueUri( pf.getCategory().getValueUri() );
        }
        experimentalFactorService.update( ef );
    }

    private void applyFactorValueChanges( ExperimentalFactor ef,
            ExperimentalDesignValueObject.ExperimentalFactorEntry pf,
            Map<Long, FactorValue> currentFvsById ) {
        if ( pf.getValues() == null ) return;
        FactorValue designatedBaseline = null;
        boolean baselineDesignated = false;
        for ( FactorValueBasicValueObject pv : pf.getValues() ) {
            FactorValue target;
            if ( pv.getId() == null ) {
                FactorValue created = createFactorValue( ef, pv );
                ef.getFactorValues().add( created );
                currentFvsById.put( created.getId(), created );
                target = created;
            } else {
                FactorValue existing = currentFvsById.get( pv.getId() );
                if ( existing == null ) continue; // preflight should have caught this
                updateFactorValueStatements( existing, pv );
                // Honour the deprecated `value` field on the FactorValue payload. Null is treated as "no change"
                // so that a client which omits the field round-trips safely.
                if ( pv.getValue() != null ) {
                    //noinspection deprecation
                    existing.setValue( pv.getValue() );
                }
                // Baseline flag: null = "no change" (same round-trip-safe convention as `value`).
                if ( pv.getBaseline() != null ) {
                    existing.setIsBaseline( pv.getBaseline() );
                }
                target = existing;
            }
            if ( Boolean.TRUE.equals( pv.getBaseline() ) ) {
                baselineDesignated = true;
                designatedBaseline = target;
            }
        }
        // At most one baseline per factor. When the payload designates one, clear the flag on every sibling so a
        // stale baseline the client left untouched cannot coexist. previewDesignChange blocks a payload that
        // designates more than one, so designatedBaseline is unambiguous here.
        if ( baselineDesignated ) {
            for ( FactorValue fv : ef.getFactorValues() ) {
                if ( fv != designatedBaseline && Boolean.TRUE.equals( fv.getIsBaseline() ) ) {
                    fv.setIsBaseline( false );
                }
            }
        }
    }

    private ExperimentalFactor createFactor( ExperimentalDesign ed, ExpressionExperiment ee,
            ExperimentalDesignValueObject.ExperimentalFactorEntry pf ) {
        FactorType type = "continuous".equalsIgnoreCase( pf.getType() )
                ? FactorType.CONTINUOUS : FactorType.CATEGORICAL;
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setName( pf.getName() );
        ef.setDescription( pf.getDescription() );
        ef.setType( type );
        ef.setExperimentalDesign( ed );
        ef.setSecurityOwner( ee );
        if ( pf.getCategory() != null ) {
            Characteristic cat = Characteristic.Factory.newInstance();
            cat.setCategory( pf.getCategory().getCategory() );
            cat.setCategoryUri( pf.getCategory().getCategoryUri() );
            cat.setValue( pf.getCategory().getValue() );
            cat.setValueUri( pf.getCategory().getValueUri() );
            ef.setCategory( cat );
        }
        ef = experimentalFactorService.create( ef );
        if ( pf.getValues() != null ) {
            for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                if ( pv.getId() != null ) {
                    // a new factor cannot reference pre-existing FVs by id; preflight permits the payload but we ignore here
                    continue;
                }
                FactorValue fv = createFactorValue( ef, pv );
                ef.getFactorValues().add( fv );
            }
        }
        return ef;
    }

    private FactorValue createFactorValue( ExperimentalFactor ef, FactorValueBasicValueObject pv ) {
        FactorValue fv = new FactorValue();
        fv.setExperimentalFactor( ef );
        fv.setSecurityOwner( ef.getSecurityOwner() );
        if ( pv.getValue() != null ) {
            //noinspection deprecation
            fv.setValue( pv.getValue() );
        }
        if ( pv.getBaseline() != null ) {
            fv.setIsBaseline( pv.getBaseline() );
        }
        if ( pv.getStatements() != null ) {
            for ( StatementValueObject ps : pv.getStatements() ) {
                fv.getCharacteristics().add( buildStatement( ps ) );
            }
        }
        if ( pv.getMeasurementObject() != null ) {
            ubic.gemma.model.common.measurement.Measurement m = ubic.gemma.model.common.measurement.Measurement.Factory.newInstance();
            m.setValue( pv.getMeasurementObject().getValue() );
            if ( pv.getMeasurementObject().getRepresentation() != null ) {
                m.setRepresentation( ubic.gemma.model.common.quantitationtype.PrimitiveType.valueOf( pv.getMeasurementObject().getRepresentation() ) );
            }
            if ( pv.getMeasurementObject().getType() != null ) {
                m.setType( ubic.gemma.model.common.measurement.MeasurementType.valueOf( pv.getMeasurementObject().getType() ) );
            }
            fv.setMeasurement( m );
        }
        return factorValueService.create( fv );
    }

    private void updateFactorValueStatements( FactorValue existing, FactorValueBasicValueObject pv ) {
        List<StatementValueObject> proposedStatements = pv.getStatements() != null ? pv.getStatements() : Collections.emptyList();
        List<CharacteristicValueObject> proposedCharacteristics = pv.getCharacteristics() != null ? pv.getCharacteristics() : Collections.emptyList();

        Map<Long, Statement> existingById = existing.getCharacteristics().stream()
                .filter( s -> s.getId() != null )
                .collect( Collectors.toMap( Statement::getId, s -> s, ( a, b ) -> a ) );

        // Resolve claims over existing entities in priority order. Each existing entity is claimed at most
        // once. Statements outrank characteristics, so an id-less statement that content-matches an entity
        // already referenced by a proposed characteristic id (deployed serializers exposing characteristic
        // ids but hiding statement ids) doesn't double-write the entity.
        Map<StatementValueObject, Long> stmtClaims = new IdentityHashMap<>();
        Map<CharacteristicValueObject, Long> charClaims = new IdentityHashMap<>();
        Set<Long> claimedIds = new HashSet<>();

        // Tier 1: proposed statements with id -> claim that entity by id.
        for ( StatementValueObject ps : proposedStatements ) {
            if ( ps.getId() == null || !existingById.containsKey( ps.getId() ) ) continue;
            claimedIds.add( ps.getId() );
            stmtClaims.put( ps, ps.getId() );
        }

        // Tier 2: id-less proposed statements -> content-match against any remaining existing entity, even one
        // that a characteristic also references by id (statements win over characteristics for the same
        // underlying entity). When a content bucket has multiple candidates, prefer the one whose id is also
        // mentioned in the payload's characteristics, so the two projections align on the same DB row.
        Set<Long> characteristicIds = proposedCharacteristics.stream()
                .map( CharacteristicValueObject::getId )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        Map<String, Deque<Long>> remainingByContent = new HashMap<>();
        for ( Statement s : existing.getCharacteristics() ) {
            if ( s.getId() == null || claimedIds.contains( s.getId() ) ) continue;
            remainingByContent
                    .computeIfAbsent( statementContentKey( s ), k -> new ArrayDeque<>() )
                    .add( s.getId() );
        }
        for ( StatementValueObject ps : proposedStatements ) {
            if ( ps.getId() != null ) continue;
            Deque<Long> bucket = remainingByContent.get( statementContentKey( ps ) );
            if ( bucket == null || bucket.isEmpty() ) continue;
            Long claimed = null;
            for ( Iterator<Long> it = bucket.iterator(); it.hasNext(); ) {
                Long candidate = it.next();
                if ( characteristicIds.contains( candidate ) ) {
                    claimed = candidate;
                    it.remove();
                    break;
                }
            }
            if ( claimed == null ) {
                claimed = bucket.poll();
            }
            claimedIds.add( claimed );
            stmtClaims.put( ps, claimed );
        }

        // Tier 3: proposed characteristics with id -> claim leftover entities (deletion protection +
        // subject-side writes). Skip ids already covered by a statement claim above.
        for ( CharacteristicValueObject pc : proposedCharacteristics ) {
            if ( pc.getId() == null || !existingById.containsKey( pc.getId() ) ) continue;
            if ( claimedIds.contains( pc.getId() ) ) continue;
            claimedIds.add( pc.getId() );
            charClaims.put( pc, pc.getId() );
        }

        // Delete entities not claimed.
        List<Statement> toDelete = existing.getCharacteristics().stream()
                .filter( s -> s.getId() != null && !claimedIds.contains( s.getId() ) )
                .collect( Collectors.toList() );
        for ( Statement s : toDelete ) {
            factorValueService.removeStatement( existing, s );
        }

        // Statement claims (id-matched or content-matched): full field application.
        for ( Map.Entry<StatementValueObject, Long> e : stmtClaims.entrySet() ) {
            Statement target = existingById.get( e.getValue() );
            if ( target != null ) {
                applyStatementFields( target, e.getKey() );
            }
        }
        // Characteristic claims: subject-side only. Predicate/object on the existing entity are preserved
        // because the characteristic projection doesn't carry them.
        for ( Map.Entry<CharacteristicValueObject, Long> e : charClaims.entrySet() ) {
            Statement target = existingById.get( e.getValue() );
            if ( target != null ) {
                applyCharacteristicSubjectFields( target, e.getKey() );
            }
        }

        // Create new statements: id-less statements that didn't find a content match.
        for ( StatementValueObject ps : proposedStatements ) {
            if ( ps.getId() != null || stmtClaims.containsKey( ps ) ) continue;
            Statement s = buildStatement( ps );
            factorValueService.saveStatement( existing, s );
        }
    }

    private void applyCharacteristicSubjectFields( Statement s, CharacteristicValueObject pc ) {
        s.setCategory( pc.getCategory() );
        s.setCategoryUri( pc.getCategoryUri() );
        s.setSubject( pc.getValue() );
        s.setSubjectUri( pc.getValueUri() );
    }

    private static String statementContentKey( StatementValueObject s ) {
        return statementContentKey( s.getCategory(), s.getCategoryUri(),
                s.getSubject(), s.getSubjectUri(),
                s.getPredicate(), s.getPredicateUri(),
                s.getObject(), s.getObjectUri(),
                s.getSecondPredicate(), s.getSecondPredicateUri(),
                s.getSecondObject(), s.getSecondObjectUri() );
    }

    private static String statementContentKey( Statement s ) {
        return statementContentKey( s.getCategory(), s.getCategoryUri(),
                s.getSubject(), s.getSubjectUri(),
                s.getPredicate(), s.getPredicateUri(),
                s.getObject(), s.getObjectUri(),
                s.getSecondPredicate(), s.getSecondPredicateUri(),
                s.getSecondObject(), s.getSecondObjectUri() );
    }

    private static String statementContentKey( String... fields ) {
        return Stream.of( fields ).map( f -> f == null ? " " : f ).collect( Collectors.joining( "" ) );
    }

    private Statement buildStatement( StatementValueObject ps ) {
        Statement s = Statement.Factory.newInstance();
        applyStatementFields( s, ps );
        return s;
    }

    private void applyStatementFields( Statement s, StatementValueObject ps ) {
        s.setCategory( ps.getCategory() );
        s.setCategoryUri( ps.getCategoryUri() );
        s.setSubject( ps.getSubject() );
        s.setSubjectUri( ps.getSubjectUri() );
        s.setPredicate( ps.getPredicate() );
        s.setPredicateUri( ps.getPredicateUri() );
        s.setObject( ps.getObject() );
        s.setObjectUri( ps.getObjectUri() );
        s.setSecondPredicate( ps.getSecondPredicate() );
        s.setSecondPredicateUri( ps.getSecondPredicateUri() );
        s.setSecondObject( ps.getSecondObject() );
        s.setSecondObjectUri( ps.getSecondObjectUri() );
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
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperiment expressionExperiment, boolean includeFreeText ) {
        return readService.getAnnotations( expressionExperiment, includeFreeText );
    }

    @Override
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee ) {
        return readService.getAnnotations( ee );
    }

    @Override
    public Set<AnnotationValueObject> getAnnotations( ExpressionExperimentSubSet ee, boolean includeFreeText ) {
        return readService.getAnnotations( ee, includeFreeText );
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
    public Map<ExpressionExperiment, Collection<ArrayDesign>> getArrayDesignsUsedByExperiment( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( expressionExperiments == null || expressionExperiments.isEmpty() ) {
            return Collections.emptyMap();
        }
        return this.expressionExperimentDao.getArrayDesignsUsedByExperiment( expressionExperiments );
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
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( final ExpressionExperiment expressionExperiment ) {
        return subSetReadService.getSubSetsWithBioAssays( expressionExperiment );
    }

    @Override
    public Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> getSubSetsWithBioAssays( Collection<ExpressionExperiment> expressionExperiments ) {
        return subSetReadService.getSubSetsWithBioAssays( expressionExperiments );
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSetsWithCharacteristics( ExpressionExperiment ee ) {
        return subSetReadService.getSubSetsWithCharacteristics( ee );
    }

    @Override
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment ) {
        return subSetReadService.getSubSetsByDimension( expressionExperiment );
    }

    @Override
    public Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimensionWithBioAssays( ExpressionExperiment expressionExperiment ) {
        return subSetReadService.getSubSetsByDimensionWithBioAssays( expressionExperiment );
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return subSetReadService.getSubSets( expressionExperiment, dimension );
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return subSetReadService.getSubSetsWithBioAssays( expressionExperiment, dimension );
    }

    @Override
    public Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return subSetReadService.getSubSetsByFactorValue( expressionExperiment, dimension );
    }

    @Override
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValue( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        return subSetReadService.getSubSetsByFactorValue( expressionExperiment, experimentalFactor, dimension );
    }

    @Override
    public Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValueWithCharacteristicsAndBioAssays( ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension ) {
        return subSetReadService.getSubSetsByFactorValueWithCharacteristicsAndBioAssays( expressionExperiment, experimentalFactor, dimension );
    }

    @Override
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristics( ExpressionExperiment ee, Long subSetId ) {
        return subSetReadService.getSubSetByIdWithCharacteristics( ee, subSetId );
    }

    @Override
    public ExpressionExperimentSubSet getSubSetByIdWithCharacteristicsAndBioAssays( ExpressionExperiment ee, Long subSetId ) {
        return subSetReadService.getSubSetByIdWithCharacteristicsAndBioAssays( ee, subSetId );
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
    public CursorPage<ExpressionExperimentValueObject> loadBlacklistedValueObjectsByCursor( @Nullable Filters filters, Sort sort, @Nullable Cursor cursor, int limit ) {
        return readService.loadBlacklistedValueObjectsByCursor( filters, sort, cursor, limit );
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
    @AuditedConditional( value = ManualAnnotationEvent.class,
            when = "#result > 0",
            messageSpel = "'Replaced annotations via API (' + #result + ' change(s))'" )
    public int updateAnnotations( ExpressionExperiment ee, Collection<Characteristic> desired ) {
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
        // anything in desired not already present -> add; a matched-but-present tag that arrives with
        // new supporting evidence -> refresh the evidence in place (identity by sameTag is unchanged).
        int evidenceUpdates = 0;
        for ( Characteristic d : desired ) {
            Characteristic match = null;
            for ( Characteristic c : current ) {
                if ( sameTag( c, d ) ) {
                    match = c;
                    break;
                }
            }
            if ( match == null ) {
                Characteristic fresh;
                if ( d instanceof Statement ) {
                    // Preserve the Statement discriminator + predicate / object pair on add. Plain
                    // Characteristic.Factory.newInstance() would silently downgrade the row to a
                    // non-Statement Characteristic and drop the S-P-O semantics.
                    Statement ds = ( Statement ) d;
                    Statement fs = Statement.Factory.newInstance();
                    fs.setCategory( ds.getCategory() );
                    fs.setCategoryUri( ds.getCategoryUri() );
                    fs.setSubject( ds.getSubject() );
                    if ( ds.getSubjectUri() != null ) {
                        fs.setSubjectUri( ds.getSubjectUri() );
                    }
                    fs.setPredicate( ds.getPredicate() );
                    fs.setPredicateUri( ds.getPredicateUri() );
                    fs.setObject( ds.getObject() );
                    fs.setObjectUri( ds.getObjectUri() );
                    fs.setSecondPredicate( ds.getSecondPredicate() );
                    fs.setSecondPredicateUri( ds.getSecondPredicateUri() );
                    fs.setSecondObject( ds.getSecondObject() );
                    fs.setSecondObjectUri( ds.getSecondObjectUri() );
                    fresh = fs;
                } else {
                    fresh = Characteristic.Factory.newInstance();
                    fresh.setCategory( d.getCategory() );
                    fresh.setCategoryUri( d.getCategoryUri() );
                    fresh.setValue( d.getValue() );
                    fresh.setValueUri( d.getValueUri() );
                }
                fresh.setEvidenceCode( d.getEvidenceCode() != null ? d.getEvidenceCode() : GOEvidenceCode.IC );
                fresh.setSupportingEvidence( d.getSupportingEvidence() );
                toAdd.add( fresh );
            } else if ( d.getSupportingEvidence() != null
                    && !Objects.equals( d.getSupportingEvidence(), match.getSupportingEvidence() ) ) {
                // Refresh provenance on an existing tag without disturbing its identity. A desired tag
                // arriving without evidence (null) leaves any stored evidence intact.
                match.setSupportingEvidence( d.getSupportingEvidence() );
                evidenceUpdates++;
            }
        }

        if ( toRemove.isEmpty() && toAdd.isEmpty() && evidenceUpdates == 0 ) {
            log.debug( "updateAnnotations: no change for " + ee.getShortName() + " (ID=" + ee.getId() + ")" );
            return 0;
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
                + " removed=" + toRemove.size() + " evidenceUpdates=" + evidenceUpdates );
        // Audit event written by @AuditedConditional via AuditedAspect; the
        // SpEL guard `#result > 0` keeps the no-change early-return branch
        // (return 0) from emitting a spurious row. Return value carries the
        // total change count so the note text matches the prior behaviour as
        // closely as possible (the added/removed breakdown is now in the log
        // line above; AUDIT_EVENT.NOTE records the aggregate).
        return toAdd.size() + toRemove.size() + evidenceUpdates;
    }

    /**
     * Replace an EE's primary + other-relevant publications. See the interface javadoc.
     * <p>
     * Set-replace: the other-relevant set is cleared and repopulated from {@code otherRelevantPublications}
     * (skipping any entry that equals the incoming primary, so the primary never doubles as an other-relevant
     * row), and the primary is set to {@code primaryPublication} (or cleared when null). Persisted through the
     * inherited {@code update(ee)}, which carries the audit event — matching the legacy
     * {@code setPrimaryPublication(...) + update(ee)} flow the gemma-web controller and the CLI used.
     */
    @Override
    @Transactional
    public void updatePublications( ExpressionExperiment ee, BibliographicReference primaryPublication,
            Collection<BibliographicReference> otherRelevantPublications ) {
        Assert.notNull( otherRelevantPublications, "The other-relevant-publication set must not be null (use an empty collection to clear)." );

        ee = ensureInSession( ee );

        ee.setPrimaryPublication( primaryPublication );

        Set<BibliographicReference> desiredOther = new HashSet<>();
        for ( BibliographicReference ref : otherRelevantPublications ) {
            if ( primaryPublication != null && Objects.equals( ref.getId(), primaryPublication.getId() ) ) {
                continue;
            }
            desiredOther.add( ref );
        }
        ee.getOtherRelevantPublications().clear();
        ee.getOtherRelevantPublications().addAll( desiredOther );

        update( ee );
        log.info( "updatePublications: " + ee.getShortName() + " (ID=" + ee.getId() + ") primary="
                + ( primaryPublication != null ? primaryPublication.getId() : "none" )
                + " otherRelevant=" + desiredOther.size() );
    }

    @Override
    @Transactional
    public boolean updateNameAndDescription( ExpressionExperiment ee, @Nullable String name, @Nullable String description ) {
        Assert.isTrue( name != null || description != null, "Provide a name and/or a description to update." );
        Assert.isTrue( name == null || StringUtils.isNotBlank( name ), "The name must not be blank when provided." );

        ee = ensureInSession( ee );

        boolean changed = false;
        if ( name != null && !name.equals( ee.getName() ) ) {
            ee.setName( name );
            changed = true;
        }
        if ( description != null && !description.equals( ee.getDescription() ) ) {
            ee.setDescription( description );
            changed = true;
        }
        if ( changed ) {
            update( ee );
            log.info( "updateNameAndDescription: " + ee.getShortName() + " (ID=" + ee.getId() + ")" );
        }
        return changed;
    }

    @Override
    @Transactional
    public CurationCommitResult commitCuration( ExpressionExperiment ee, CurationCommitRequest request, boolean dryRun ) {
        ee = ensureInSession( ee );

        // Optimistic concurrency: reject if the dataset moved since the draft's baseline.
        Date expected = request.getExpectedLastUpdated();
        if ( expected != null && ee.getCurationDetails() != null && ee.getCurationDetails().getLastUpdated() != null ) {
            Date current = ee.getCurationDetails().getLastUpdated();
            if ( current.getTime() != expected.getTime() ) {
                throw new OptimisticLockingFailureException( "Dataset " + ee.getShortName()
                        + " changed since the draft baseline (expected lastUpdated " + expected + ", found " + current + ")." );
            }
        }

        CurationCommitResult result = new CurationCommitResult();
        boolean anyChange = false;

        // ── basics ──
        if ( request.isBasicsPresent() ) {
            boolean basicsChanged = false;
            if ( request.getShortName() != null ) {
                String sn = request.getShortName().trim();
                if ( !sn.equals( ee.getShortName() ) ) {
                    if ( !request.isShortNameChangeAllowed() ) {
                        throw new AccessDeniedException( "Changing the short name requires administrator rights." );
                    }
                    if ( existsByShortName( sn ) ) {
                        throw new IllegalArgumentException( "short_name '" + sn + "' is already in use." );
                    }
                    if ( !dryRun ) {
                        ee.setShortName( sn );
                    }
                    basicsChanged = true;
                }
            }
            if ( request.getName() != null && !request.getName().equals( ee.getName() ) ) {
                if ( !dryRun ) {
                    ee.setName( request.getName() );
                }
                basicsChanged = true;
            }
            if ( request.getDescription() != null && !request.getDescription().equals( ee.getDescription() ) ) {
                if ( !dryRun ) {
                    ee.setDescription( request.getDescription() );
                }
                basicsChanged = true;
            }
            result.setBasicsChanged( basicsChanged );
            anyChange = anyChange || basicsChanged;
        }

        // ── publications (set-replace, diffed by id) ──
        if ( request.isPublicationsPresent() ) {
            BibliographicReference primary = request.getPrimaryPublication();
            List<BibliographicReference> desiredOther = new ArrayList<>();
            for ( BibliographicReference ref : request.getOtherRelevantPublications() ) {
                if ( primary != null && Objects.equals( ref.getId(), primary.getId() ) ) {
                    continue;
                }
                desiredOther.add( ref );
            }
            Set<Long> currentIds = new HashSet<>();
            if ( ee.getPrimaryPublication() != null ) {
                currentIds.add( ee.getPrimaryPublication().getId() );
            }
            for ( BibliographicReference r : ee.getOtherRelevantPublications() ) {
                currentIds.add( r.getId() );
            }
            Set<Long> desiredIds = new HashSet<>();
            if ( primary != null ) {
                desiredIds.add( primary.getId() );
            }
            for ( BibliographicReference r : desiredOther ) {
                desiredIds.add( r.getId() );
            }
            int created = 0, deleted = 0, unchanged = 0;
            for ( Long id : desiredIds ) {
                if ( currentIds.contains( id ) ) {
                    unchanged++;
                } else {
                    created++;
                }
            }
            for ( Long id : currentIds ) {
                if ( !desiredIds.contains( id ) ) {
                    deleted++;
                }
            }
            result.setPublicationsCreated( created );
            result.setPublicationsDeleted( deleted );
            result.setPublicationsUnchanged( unchanged );
            if ( !dryRun && ( created > 0 || deleted > 0 ) ) {
                ee.setPrimaryPublication( primary );
                ee.getOtherRelevantPublications().clear();
                ee.getOtherRelevantPublications().addAll( desiredOther );
            }
            anyChange = anyChange || ( created > 0 || deleted > 0 );
        }

        // ── design (factors → factor-values → statements) ──
        // The web layer already mapped CAB's declared-delete DesignCommit onto a COMPLETE
        // ExperimentalDesignValueObject (carry-forward untouched + delta) and gated blockers (400) / force (409),
        // so here we just apply through the shipped replace-by-absence path. Two passes handle sample assignments
        // to brand-new factor values, whose ids don't exist until the first pass creates them.
        if ( request.isDesignPresent() && request.getProposedDesign() != null ) {
            ExperimentalDesignValueObject edvo1 = request.getProposedDesign();
            if ( dryRun ) {
                DesignPreflightReport.Summary s = previewDesignChange( ee, edvo1 ).getSummary();
                result.setDesignCreated( s.getFactorsToCreate() + s.getFactorValuesToCreate() );
                result.setDesignDeleted( s.getFactorsToDelete() + s.getFactorValuesToDelete() );
                result.setDesignUpdated( s.getBiomaterialsWithChangedAssignments() );
                // Symmetry with basics/publications: a clean no-op keep reports unchanged=1, not all-zero.
                result.setDesignUnchanged( result.getDesignCreated() + result.getDesignDeleted() + result.getDesignUpdated() == 0 ? 1 : 0 );
                anyChange = anyChange || result.getDesignCreated() > 0 || result.getDesignDeleted() > 0
                        || result.getDesignUpdated() > 0;
            } else {
                // Pass 1 — through the proxy so the DesignChangeEvent audit aspect fires.
                DesignApplyOutcome outcome1 = self.applyDesignChange( ee, edvo1 );
                DesignPreflightReport.Summary s1 = outcome1.getPreflightAtApply().getSummary();
                int created = s1.getFactorsToCreate() + s1.getFactorValuesToCreate();
                int deleted = s1.getFactorsToDelete() + s1.getFactorValuesToDelete();
                int updated = s1.getBiomaterialsWithChangedAssignments();

                List<Long> auditIds = new ArrayList<>();
                collectDesignChangeEventId( ee, outcome1, auditIds );

                Map<String, Long> idMap = new LinkedHashMap<>();
                DesignCommitPlan plan = request.getDesignPlan();
                if ( plan != null ) {
                    correlateNewDesignIds( outcome1.getDesign(), plan, idMap );
                    if ( !plan.getPendingAssignments().isEmpty() ) {
                        ExperimentalDesignValueObject edvo2 = buildAssignmentPass( outcome1.getDesign(), plan, idMap );
                        if ( edvo2 != null ) {
                            DesignApplyOutcome outcome2 = self.applyDesignChange( ee, edvo2 );
                            updated += outcome2.getPreflightAtApply().getSummary().getBiomaterialsWithChangedAssignments();
                            collectDesignChangeEventId( ee, outcome2, auditIds );
                        }
                    }
                }
                result.setDesignCreated( created );
                result.setDesignDeleted( deleted );
                result.setDesignUpdated( updated );
                result.setDesignUnchanged( created + deleted + updated == 0 ? 1 : 0 );
                result.setDesignIdMap( idMap );
                result.setDesignAuditEventIds( auditIds );
                anyChange = anyChange || outcome1.isApplied();
            }
        }

        // ── experiment-level tags (id-based: remove deletedIds, add clientRef items; gemmaId items are kept) ──
        // addAnnotation returns the persisted tag with its id, so clientRef → id is direct (no correlation pass).
        // Through the proxy (self) so the @Audited TagAdded/TagRemoved events fire.
        if ( request.isTagsPresent() ) {
            Map<String, Long> idMap = new LinkedHashMap<>();
            int created = 0, deleted = 0;
            if ( dryRun ) {
                created = request.getTagsToAdd().size();
                deleted = request.getTagsToDelete().size();
            } else {
                for ( Long id : request.getTagsToDelete() ) {
                    if ( self.removeAnnotation( ee, id ) != null ) {
                        deleted++;
                    }
                }
                List<CurationCommitRequest.TagAdd> adds = request.getTagsToAdd();
                for ( CurationCommitRequest.TagAdd add : adds ) {
                    self.addAnnotation( ee, add.getCharacteristic() );
                    created++;
                }
                if ( created > 0 ) {
                    // addAnnotation persists via merge, so the passed-in characteristic stays transient — resolve each
                    // new id by content (the same sameTag equality addAnnotation uses to reject duplicates, so the
                    // match is unambiguous) from a fresh read after the flush.
                    sessionFactory.getCurrentSession().flush();
                    Collection<Characteristic> persisted = load( ee.getId() ).getCharacteristics();
                    for ( CurationCommitRequest.TagAdd add : adds ) {
                        idMap.put( add.getClientRef(), matchCharacteristicId( persisted, add.getCharacteristic() ) );
                    }
                }
            }
            result.setTagsCreated( created );
            result.setTagsDeleted( deleted );
            result.setTagsUnchanged( request.getTagsUnchanged() );
            result.setTagsIdMap( idMap );
            anyChange = anyChange || created > 0 || deleted > 0;
        }

        // ── per-sample characteristics (id-based, resolved to a biomaterial thawed from the experiment) ──
        if ( request.isSampleCharsPresent() ) {
            Map<String, Long> idMap = new LinkedHashMap<>();
            int created = 0, deleted = 0;
            if ( dryRun ) {
                created = request.getSampleCharsToAdd().size();
                deleted = request.getSampleCharsToDelete().size();
            } else {
                ExpressionExperiment thawed = thawBioAssays( ee );
                Map<Long, BioMaterial> bmById = new HashMap<>();
                Map<Long, BioMaterial> charIdToBm = new HashMap<>();
                for ( BioAssay ba : thawed.getBioAssays() ) {
                    BioMaterial bm = ba.getSampleUsed();
                    if ( bm == null || bm.getId() == null ) {
                        continue;
                    }
                    bmById.putIfAbsent( bm.getId(), bm );
                    for ( Characteristic c : bm.getCharacteristics() ) {
                        if ( c.getId() != null ) {
                            charIdToBm.put( c.getId(), bm );
                        }
                    }
                }
                for ( Long id : request.getSampleCharsToDelete() ) {
                    BioMaterial bm = charIdToBm.get( id );
                    if ( bm != null && bioMaterialService.removeAnnotation( ee, bm, id ) != null ) {
                        deleted++;
                    }
                }
                List<CurationCommitRequest.SampleCharacteristicAdd> adds = request.getSampleCharsToAdd();
                for ( CurationCommitRequest.SampleCharacteristicAdd add : adds ) {
                    BioMaterial bm = bmById.get( add.getBioMaterialId() );
                    if ( bm == null ) {
                        throw new IllegalArgumentException( "sampleCharacteristics references biomaterial "
                                + add.getBioMaterialId() + " which is not part of " + ee.getShortName() + "." );
                    }
                    bioMaterialService.addAnnotation( ee, bm, add.getCharacteristic() );
                    created++;
                }
                if ( created > 0 ) {
                    // Same merge-persist caveat as tags — resolve the new id by content from a fresh read of the sample.
                    sessionFactory.getCurrentSession().flush();
                    Map<Long, Collection<Characteristic>> freshByBm = new HashMap<>();
                    for ( CurationCommitRequest.SampleCharacteristicAdd add : adds ) {
                        Collection<Characteristic> persisted = freshByBm.computeIfAbsent( add.getBioMaterialId(),
                                bmId -> bioMaterialService.thaw( bioMaterialService.load( bmId ) ).getCharacteristics() );
                        idMap.put( add.getClientRef(), matchCharacteristicId( persisted, add.getCharacteristic() ) );
                    }
                }
            }
            result.setSampleCharsCreated( created );
            result.setSampleCharsDeleted( deleted );
            result.setSampleCharsUnchanged( request.getSampleCharsUnchanged() );
            result.setSampleCharsIdMap( idMap );
            anyChange = anyChange || created > 0 || deleted > 0;
        }

        // ── curationDetails: only the free-text note commits here (troubled/needsAttention go through tickets) ──
        if ( request.isCurationDetailsPresent() && request.getCurationDetailsNote() != null ) {
            String desired = request.getCurationDetailsNote();
            String current = ee.getCurationDetails() != null ? ee.getCurationDetails().getCurationNote() : null;
            if ( !desired.equals( current ) ) {
                if ( !dryRun ) {
                    // The CurationNoteUpdateEvent hook copies the note onto CurationDetails.
                    auditTrailService.addUpdateEvent( ee, CurationNoteUpdateEvent.class, desired );
                }
                result.setCurationNoteChanged( true );
                anyChange = true;
            }
        }

        // ── split advice (stopgap: recorded in the free-text curation note; no structured home yet) ──
        if ( request.getSplitOnFactorId() != null || request.getSplitRationale() != null ) {
            if ( !dryRun ) {
                applySplitAdviceNote( ee, request.getSplitOnFactorId(), request.getSplitRationale() );
            }
            anyChange = true;
        }

        if ( !dryRun && anyChange ) {
            update( ee );
            // Advance the curation lastUpdated concurrency token on every change. Sections that emit an audit event
            // (design/tags/sampleCharacteristics/curationNote) already bump it via the curatable audit hook, but a
            // basics- or publications-only change emits none — so bump it here (set + merge, same as that hook) so a
            // stale baseline is always detectable on the next commit.
            if ( ee.getCurationDetails() != null ) {
                ee.getCurationDetails().setLastUpdated( new Date() );
                ee.setCurationDetails( ( CurationDetails ) sessionFactory.getCurrentSession().merge( ee.getCurationDetails() ) );
            }
            log.info( "commitCuration: " + ee.getShortName() + " (ID=" + ee.getId() + ") applied" );
        }
        result.setNewLastUpdated( ee.getCurationDetails() != null ? ee.getCurationDetails().getLastUpdated() : null );
        return result;
    }

    /**
     * Resolve each new design entity's {@code clientRef} to the id it was assigned, reading the rebuilt design
     * after apply. Correlation is order-based: the rebuilt design sorts factors/values by ascending id and ids are
     * monotonic in creation order, so the k-th newly-created entity (id absent from the pre-commit id sets) matches
     * the k-th recorded clientRef — deterministic even when two new values share a label.
     * <p>
     * Package-private so the order-based correlation can be unit-tested directly.
     */
    void correlateNewDesignIds( ExperimentalDesignValueObject rebuilt, DesignCommitPlan plan, Map<String, Long> idMap ) {
        if ( rebuilt == null || rebuilt.getExperimentalFactors() == null ) {
            return;
        }
        Set<Long> preFactorIds = plan.getPreExistingFactorIds() != null ? plan.getPreExistingFactorIds() : Collections.emptySet();
        Set<Long> preFvIds = plan.getPreExistingFactorValueIds() != null ? plan.getPreExistingFactorValueIds() : Collections.emptySet();

        List<ExperimentalDesignValueObject.ExperimentalFactorEntry> factors = new ArrayList<>( rebuilt.getExperimentalFactors() );
        factors.sort( Comparator.comparingLong( f -> f.getId() == null ? Long.MAX_VALUE : f.getId() ) );

        // New factors, in id order, ↔ recorded new-factor clientRefs, in emission order.
        List<String> factorRefs = plan.getNewFactorClientRefs();
        Map<Long, String> newFactorIdToRef = new HashMap<>();
        int fi = 0;
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : factors ) {
            if ( f.getId() != null && !preFactorIds.contains( f.getId() ) && fi < factorRefs.size() ) {
                String ref = factorRefs.get( fi++ );
                idMap.put( ref, f.getId() );
                newFactorIdToRef.put( f.getId(), ref );
            }
        }

        // New factor values, per parent factor, in id order ↔ recorded clientRefs in emission order.
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : factors ) {
            if ( f.getId() == null ) {
                continue;
            }
            String parentKey;
            if ( preFactorIds.contains( f.getId() ) ) {
                parentKey = DesignCommitPlan.existingFactorKey( f.getId() );
            } else if ( newFactorIdToRef.containsKey( f.getId() ) ) {
                parentKey = DesignCommitPlan.newFactorKey( newFactorIdToRef.get( f.getId() ) );
            } else {
                continue;
            }
            List<String> fvRefs = plan.getNewFactorValueClientRefsByParentKey().get( parentKey );
            if ( fvRefs == null || fvRefs.isEmpty() || f.getValues() == null ) {
                continue;
            }
            List<FactorValueBasicValueObject> values = new ArrayList<>( f.getValues() );
            values.sort( Comparator.comparingLong( v -> v.getId() == null ? Long.MAX_VALUE : v.getId() ) );
            int vi = 0;
            for ( FactorValueBasicValueObject v : values ) {
                if ( v.getId() != null && !preFvIds.contains( v.getId() ) && vi < fvRefs.size() ) {
                    idMap.put( fvRefs.get( vi++ ), v.getId() );
                }
            }
        }
    }

    /**
     * Build the second-pass design: the rebuilt design (all real ids, existing assignments) with each deferred
     * new-factor-value assignment added to its biomaterials. Returns {@code null} when nothing needs wiring (so the
     * caller skips a redundant apply). Because it re-submits the whole design, the replace-by-absence path keeps
     * every untouched entity.
     */
    // package-private for direct unit testing
    @Nullable
    ExperimentalDesignValueObject buildAssignmentPass( ExperimentalDesignValueObject rebuilt, DesignCommitPlan plan, Map<String, Long> idMap ) {
        Map<Long, ExperimentalDesignValueObject.BioMaterialFactorValueAssignment> byBm = new LinkedHashMap<>();
        if ( rebuilt.getBioMaterialAssignments() != null ) {
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : rebuilt.getBioMaterialAssignments() ) {
                byBm.put( a.getBioMaterialId(), a );
            }
        }
        boolean any = false;
        for ( DesignCommitPlan.PendingAssignment pa : plan.getPendingAssignments() ) {
            Long fvId = idMap.get( pa.getFactorValueClientRef() );
            if ( fvId == null ) {
                continue;
            }
            for ( Long bmId : pa.getBioMaterialIds() ) {
                ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a = byBm.get( bmId );
                if ( a == null ) {
                    continue;
                }
                if ( a.getFactorValueIds() == null ) {
                    a.setFactorValueIds( new ArrayList<>() );
                }
                if ( !a.getFactorValueIds().contains( fvId ) ) {
                    a.getFactorValueIds().add( fvId );
                    any = true;
                }
            }
        }
        return any ? rebuilt : null;
    }

    /**
     * Best-effort capture of the {@code DesignChangeEvent} id emitted by a proxied {@link #applyDesignChange}.
     * The audit row is written by the aspect only when the apply changed something; an unresolved id is skipped
     * (auditEventIds is advisory).
     */
    private void collectDesignChangeEventId( ExpressionExperiment ee, DesignApplyOutcome outcome, List<Long> auditIds ) {
        if ( !outcome.isApplied() ) {
            return;
        }
        AuditEvent ev = auditEventService.getLastEvent( ee, DesignChangeEvent.class );
        if ( ev != null && ev.getId() != null && !auditIds.contains( ev.getId() ) ) {
            auditIds.add( ev.getId() );
        }
    }

    /**
     * Stopgap home for the curator's split advice: a single delimited line in the free-text curation note, upserted
     * so a re-commit replaces rather than stacks. There is no structured persistence for split decisions yet.
     * {@code factorId == -1} is the "do not split" sentinel.
     */
    private void applySplitAdviceNote( ExpressionExperiment ee, @Nullable Long factorId, @Nullable String rationale ) {
        final String marker = "[split-advice]";
        CurationDetails cd = ee.getCurationDetails();
        String existing = cd != null ? cd.getCurationNote() : null;
        StringBuilder line = new StringBuilder( marker ).append( ' ' );
        if ( factorId != null && factorId == -1L ) {
            line.append( "do not split" );
        } else if ( factorId != null ) {
            line.append( "split on factor " ).append( factorId );
        } else {
            line.append( "(no factor specified)" );
        }
        if ( StringUtils.isNotBlank( rationale ) ) {
            line.append( " — " ).append( rationale.trim() );
        }
        String cleaned = existing == null ? "" : Arrays.stream( existing.split( "\n" ) )
                .filter( l -> !l.startsWith( marker ) )
                .collect( Collectors.joining( "\n" ) );
        String updated = cleaned.isEmpty() ? line.toString() : cleaned + "\n" + line;
        // The CurationNoteUpdateEvent hook copies the note onto CurationDetails — mirrors updateDatasetCurationDetails.
        auditTrailService.addUpdateEvent( ee, CurationNoteUpdateEvent.class, updated );
    }

    /**
     * Resolve the id of a just-added characteristic by content-matching it against a fresh (persisted) collection,
     * using the same {@code sameTag} equality {@code addAnnotation} uses to reject duplicates — so within one owner
     * the match is unambiguous. Needed because tag / sample-characteristic adds persist via merge, leaving the
     * passed-in characteristic transient (no id) so it can't be echoed directly.
     */
    @Nullable
    private static Long matchCharacteristicId( Collection<Characteristic> persisted, Characteristic target ) {
        for ( Characteristic c : persisted ) {
            if ( c.getId() != null && sameTag( c, target ) ) {
                return c.getId();
            }
        }
        return null;
    }

    /**
     * Per-tag REST-write counterpart to {@link #addCharacteristic(ExpressionExperiment, Characteristic)}.
     * <p>
     * Emits one {@link TagAddedEvent} per call via the {@code @Audited} aspect; rejects duplicates by
     * {@code (categoryUri, valueUri)} so the {@code POST /annotations/datasets/{id}/annotations}
     * handler can surface a {@code 409 Conflict}. Delegates the actual persistence to
     * {@link ExpressionExperimentWriteService#addCharacteristic(ExpressionExperiment, Characteristic)}
     * (which does the {@code IC}-evidence-code defaulting and the Hibernate session attach).
     */
    @Override
    @Transactional
    @Audited(value = TagAddedEvent.class,
            messageSpel = "'Added tag ' + #vc.category + ' = ' + #vc.value")
    public Characteristic addAnnotation( ExpressionExperiment ee, Characteristic vc ) {
        Assert.notNull( vc, "Characteristic must not be null." );
        Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Must provide a category" );
        Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Must provide a value" );
        ee = ensureInSession( ee );
        for ( Characteristic existing : ee.getCharacteristics() ) {
            if ( sameTag( existing, vc ) ) {
                throw new IllegalArgumentException( "An annotation with the same (category, value) already exists on "
                        + ee.getShortName() + " (existing id=" + existing.getId() + ")." );
            }
        }
        writeService.addCharacteristic( ee, vc );
        // writeService.addCharacteristic attaches vc via ee.getCharacteristics().add(vc) and
        // cascades the insert through ee update; vc.getId() is set after the flush.
        return vc;
    }

    /**
     * Per-tag REST-write counterpart to {@link #removeCharacteristics(ExpressionExperiment, Collection)}.
     * Returns {@code null} when the id is not in {@code ee}'s characteristic set so the REST handler
     * can surface a {@code 404}.
     */
    @Override
    @Transactional
    @AuditedConditional(value = TagRemovedEvent.class,
            when = "#result != null",
            messageSpel = "'Removed tag ' + #result.category + ' = ' + #result.value")
    @Nullable
    public Characteristic removeAnnotation( ExpressionExperiment ee, Long annotationId ) {
        Assert.notNull( annotationId, "Annotation id must not be null." );
        ee = ensureInSession( ee );
        Characteristic target = null;
        for ( Characteristic c : ee.getCharacteristics() ) {
            if ( annotationId.equals( c.getId() ) ) {
                target = c;
                break;
            }
        }
        if ( target == null ) {
            return null;
        }
        writeService.removeCharacteristics( ee, Collections.singleton( target ) );
        return target;
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

    @Override
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        return readService.thawBioAssays( expressionExperiment );
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
    public Collection<ExpressionExperiment> getExperimentsLackingPublications( int maxResults ) {
        return this.expressionExperimentDao.getExperimentsLackingPublications( maxResults );
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