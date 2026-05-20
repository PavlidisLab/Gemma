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

import org.springframework.beans.factory.annotation.Autowired;
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
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.search.SearchException;
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
        if ( ed == null ) {
            report.getBlockers().add( new DesignPreflightReport.Blocker( "NO_EXISTING_DESIGN",
                    "Experiment has no experimental design; preflight cannot diff against current state." ) );
            return report;
        }

        // ---- thaw what we need to walk ----
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
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
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
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
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
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
    public ExperimentalDesignValueObject applyDesignChange( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        Assert.notNull( proposed, "A proposed design must be supplied." );
        ee = expressionExperimentDao.reload( ee );

        // Re-run preflight as the authoritative gate. The REST layer also runs it for client feedback, but we
        // re-check here so direct service callers can't bypass validation.
        DesignPreflightReport report = previewDesignChange( ee, proposed );
        if ( !report.getBlockers().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot apply proposed design: "
                    + report.getBlockers().get( 0 ).getMessage() );
        }

        ExperimentalDesign ed = ee.getExperimentalDesign();
        Assert.notNull( ed, "Experiment has no experimental design after reload; preflight should have caught this." );

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
        DesignPreflightReport.Summary s = report.getSummary();
        String note = String.format(
                "Design replaced via REST: factors +%d / -%d, factor values +%d / -%d, biomaterial assignments changed: %d, analyses removed: %d.",
                s.getFactorsToCreate(), s.getFactorsToDelete(),
                s.getFactorValuesToCreate(), s.getFactorValuesToDelete(),
                s.getBiomaterialsWithChangedAssignments(),
                s.getDifferentialExpressionAnalysesToDelete() );
        auditTrailService.addUpdateEvent( ee, ExperimentalDesignUpdatedEvent.class, note );

        return getExperimentalDesignValueObject( ee );
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
        for ( FactorValueBasicValueObject pv : pf.getValues() ) {
            if ( pv.getId() == null ) {
                FactorValue created = createFactorValue( ef, pv );
                ef.getFactorValues().add( created );
                currentFvsById.put( created.getId(), created );
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
                + " removed=" + toRemove.size() );
        // Audit event written by @AuditedConditional via AuditedAspect; the
        // SpEL guard `#result > 0` keeps the no-change early-return branch
        // (return 0) from emitting a spurious row. Return value carries the
        // total change count so the note text matches the prior behaviour as
        // closely as possible (the added/removed breakdown is now in the log
        // line above; AUDIT_EVENT.NOTE records the aggregate).
        return toAdd.size() + toRemove.size();
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