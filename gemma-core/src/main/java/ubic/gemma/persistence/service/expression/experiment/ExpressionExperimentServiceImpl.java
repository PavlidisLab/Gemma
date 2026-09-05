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
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
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
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.MeasurementValueObject;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
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
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.persistence.service.common.description.PublicationAssociationService;
import ubic.gemma.persistence.service.common.measurement.UnitDao;
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
    private AnnotationSetService annotationSetService;
    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService ticketService;
    @Autowired
    private ubic.gemma.core.security.authentication.UserManager userManager;
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
    /**
     * Resolves a curated measurement's unit to a persistent {@link Unit}. {@code Measurement.unit} does not cascade
     * on a factor-value persist, so a transient one would be silently dropped.
     */
    @Autowired
    private UnitDao unitDao;
    @Autowired
    private OntologyService ontologyService;
    /**
     * Keeps the publication assertions in step with the publication links. The two are one record
     * split across two tables (Gemma 1.32.x shares the database and reads only the links), so every
     * write to either goes through {@link #updatePublications} and reaches both.
     */
    @Autowired
    private PublicationAssociationService publicationAssociationService;
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
     * Used by {@link #applyDesignChange} step 2 so a cascaded analysis takes its on-disk artifacts with it.
     * {@code @Lazy} because {@code DifferentialExpressionAnalyzerServiceImpl} autowires this service back.
     */
    @Autowired
    @Lazy
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;
    /**
     * Used inside {@link #commitCuration} to flush after tag/sample-characteristic adds (so the cascaded inserts
     * assign their ids in time to echo {@code clientRef → newId}) and to bump the curation {@code lastUpdated}
     * concurrency token on any change.
     */
    @Autowired
    private SessionFactory sessionFactory;
    /**
     * Used by {@link #commitCuration} to bring {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} up to date for the
     * one experiment the commit touched, inside the commit's own transaction.
     */
    @Autowired
    private ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil tableMaintenanceUtil;

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
    public List<ExpressionExperimentDao.Identifiers> loadIdentifiers( Collection<Long> ids ) {
        return readService.loadIdentifiers( ids );
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
                                Map<Long, Integer> proposedPerStatement = new HashMap<>();
                                for ( StatementValueObject ps : pv.getStatements() ) {
                                    if ( ps.getId() == null ) {
                                        continue;
                                    }
                                    if ( !existingStmtIds.contains( ps.getId() ) ) {
                                        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                                "UNKNOWN_STATEMENT_ID",
                                                "Statement id " + ps.getId() + " does not belong to factor value " + pv.getId() + "." );
                                        b.setFactorValueId( pv.getId() );
                                        b.setStatementId( ps.getId() );
                                        report.getBlockers().add( b );
                                    }
                                    proposedPerStatement.merge( ps.getId(), 1, Integer::sum );
                                }
                                // Two entries for one id is the normal shape of a compound statement on the wire
                                // (see unflattenStatements). A statement holds two object slots and no more, so a
                                // third entry has nowhere to go — refuse it here rather than let the extra clause
                                // be silently dropped on the way in.
                                for ( Map.Entry<Long, Integer> e : proposedPerStatement.entrySet() ) {
                                    if ( e.getValue() > 2 ) {
                                        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                                                "STATEMENT_ID_REPEATED",
                                                "Statement id " + e.getKey() + " appears " + e.getValue()
                                                        + " times on factor value " + pv.getId()
                                                        + "; a statement carries at most two objects." );
                                        b.setFactorValueId( pv.getId() );
                                        b.setStatementId( e.getKey() );
                                        report.getBlockers().add( b );
                                    }
                                }
                            }
                        } else {
                            summary.setFactorValuesToCreate( summary.getFactorValuesToCreate() + 1 );
                        }
                    }
                    // NOTE: more than one baseline per factor is allowed and is NOT blocked here. A dataset that
                    // holds two experiments legitimately has a reference level per experiment, and a curator has to
                    // be able to record that. The constraint belongs where it actually bites: LinearModelAnalyzer
                    // refuses to run such a factor as a single contrast unless a subset factor is configured
                    // (MultipleBaselinesRequireSubsetException). Blocking it at curation time instead made the
                    // legitimate design unrecordable while leaving the analysis free to pick one arbitrarily.
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

        // Counted once, here, because the invalidation rule below needs it and the summary needs it too.
        // It used to be recomputed in a second pass over the same two maps further down.
        int changedBmCount = 0;
        for ( Map.Entry<Long, BioMaterial> e : currentBmsById.entrySet() ) {
            Set<Long> currentFvIds = e.getValue().getAllFactorValues().stream()
                    .map( FactorValue::getId ).collect( Collectors.toSet() );
            Set<Long> proposedFvIdsForBm = proposedAssignByBmId.getOrDefault( e.getKey(), Collections.emptySet() );
            if ( !currentFvIds.equals( proposedFvIdsForBm ) ) {
                changedBmCount++;
            }
        }

        // ---- impact: differential expression analyses ----
        // ONE EXCLUSION, not a list of inclusions: a design commit invalidates this dataset's analyses
        // UNLESS the only thing that changed was labels on kept factor values. Paul, 2026-08-26 — the
        // previous four inclusion rules (factor deleted / FV deleted / FV added / assignment changed)
        // were four places to be wrong, and they missed two real cases: adding a WHOLE factor marked
        // nothing, because a new factor has no id and no analyses of its own; and a measurement change on
        // a continuous factor changes the regression while moving no structural counter.
        //
        // The failure mode is inverted on purpose. Before, a case nobody enumerated rode through silently
        // and falsified a live analysis; now it triggers a re-run, and re-running a DEA is cheap.
        //
        // Invalidation is DATASET-WIDE rather than per-factor: adding a factor invalidates the analyses on
        // the other factors too, because they were fitted without a variable the design now declares.
        //
        // What is excluded, and only this: statement / characteristic / free-text-value edits on a kept
        // factor value. Those relabel; they do not move a sample, a level, or a baseline.
        boolean structuralChange = summary.getFactorsToCreate() > 0
                || summary.getFactorsToDelete() > 0
                || summary.getFactorValuesToCreate() > 0
                || summary.getFactorValuesToDelete() > 0
                || changedBmCount > 0;
        boolean invalidatingEdit = hasKeptFactorValueEditsThatChangeTheMath( ee, proposed );

        if ( structuralChange || invalidatingEdit ) {
            for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByExperiment( ee, true ) ) {
                Long subsetFvId = a.getSubsetFactorValue() != null ? a.getSubsetFactorValue().getId() : null;
                report.getDifferentialExpressionAnalysesToDelete().add(
                        new DesignPreflightReport.AnalysisRef( a.getId(), a.getName(), subsetFvId ) );
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
        summary.setBiomaterialsWithChangedAssignments( changedBmCount );

        // ---- impact: in-place edits on kept factors and factor values ----
        // Reported, never counted as structural. These are the edits the invalidation rule above deliberately
        // excludes -- they relabel, they move no sample and no level -- so they must stay out of
        // `structuralChange`. What they must NOT stay out of is the report: before this, re-terming a factor
        // value preflighted as all-zero, which the caller reads as "nothing to do" for an edit a PUT applies.
        for ( ExperimentalFactor ef : keptFactorMetadataEdits( ee, proposed ) ) {
            report.getFactorsToUpdate().add( new DesignPreflightReport.EntityRef( ef.getId(), ef.getName() ) );
        }
        summary.setFactorsToUpdate( report.getFactorsToUpdate().size() );
        for ( FactorValue fv : keptFactorValueEdits( ee, proposed ) ) {
            report.getFactorValuesToUpdate().add(
                    new DesignPreflightReport.EntityRef( fv.getId(), FactorValueUtils.getSummaryString( fv ) ) );
        }
        summary.setFactorValuesToUpdate( report.getFactorValuesToUpdate().size() );

        return report;
    }

    @Override
    @Transactional
    @AuditedConditional(value = DesignChangeEvent.class,
            when = "#result.applied",
            // 🛑 The `~` counts are the point. This note used to report creates, deletes, assignment
            // moves and analyses only, so a commit that re-termed a statement, attached evidence,
            // flipped a baseline or renamed a factor wrote "+0 / -0 ... changed: 0" -- a description
            // of a real write that reads as a no-op. uib was led to report the commit path as
            // non-idempotent on the strength of it (2026-09-04); the write was real and the note
            // denied it. factorsToUpdate / factorValuesToUpdate were already on the summary and
            // simply were not being said.
            messageSpel = "'Design replaced via REST: factors +' + #result.preflightAtApply.summary.factorsToCreate + ' / -' + #result.preflightAtApply.summary.factorsToDelete + ' / ~' + #result.preflightAtApply.summary.factorsToUpdate + ', factor values +' + #result.preflightAtApply.summary.factorValuesToCreate + ' / -' + #result.preflightAtApply.summary.factorValuesToDelete + ' / ~' + #result.preflightAtApply.summary.factorValuesToUpdate + ', biomaterial assignments changed: ' + #result.preflightAtApply.summary.biomaterialsWithChangedAssignments + ', analyses removed: ' + #result.preflightAtApply.summary.differentialExpressionAnalysesToDelete + '.'")
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

        // ---- step 2: remove the diff-ex analyses the preflight enumerated ----
        // The invalidation rule lives in previewDesignChange and nowhere else; this side executes the list the
        // report already carries instead of deriving it a second time. The two derivations DID drift: when the
        // report was widened to dataset-wide invalidation (2026-08-26) this side still asked per-factor, so a
        // baseline flip the curator was warned would delete an analysis left that analysis in place, falsified.
        // Subset-anchored analyses need no separate pass -- the report is built from findByExperiment(ee, true),
        // so it already names them. Step 4 (factor removal) cascades through ExperimentalFactorService#remove;
        // whatever it would have reached is already gone by then.
        Set<Long> analysisIdsToRemove = report.getDifferentialExpressionAnalysesToDelete().stream()
                .map( DesignPreflightReport.AnalysisRef::getId )
                .collect( Collectors.toCollection( HashSet::new ) );
        // Routed through deleteAnalysis, not differentialExpressionAnalysisService.remove: remove drops the rows
        // only, leaving each analysis's diffex archive and its per-result-set TSV caches on the deployment
        // volume. deleteAnalysis does the same removal and then those files. It runs at SUPPORTS so it joins
        // this transaction -- which means the files go before the commit, and a rollback here leaves a
        // surviving analysis without its caches; both are rebuilt from the database on next request.
        if ( !analysisIdsToRemove.isEmpty() ) {
            for ( DifferentialExpressionAnalysis a : differentialExpressionAnalysisService.findByExperiment( ee, true ) ) {
                if ( analysisIdsToRemove.contains( a.getId() ) ) {
                    differentialExpressionAnalyzerService.deleteAnalysis( ee, a );
                }
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
        // Same blind spot one level up: renaming a kept factor, rewriting its description, or re-terming its
        // category leaves every structural counter at zero and touches no factor value at all. Without this the
        // PUT is swallowed and readers keep seeing the old category with nothing to signal otherwise.
        if ( hasKeptFactorMetadataEdits( ee, proposed ) ) {
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
     * Whether {@code proposed} carries an in-place edit to an existing (kept) <em>factor</em>: its name, its
     * description, or its category. None of these move a structural counter, and none of them live on a factor
     * value, so {@link #hasKeptFactorValueEdits} cannot see them either. Mirrors the fields
     * {@link #updateFactorMetadata} writes and its {@code null = "no change"} convention, including the rule that
     * a category is only applied when the factor already has one.
     */
    private boolean hasKeptFactorMetadataEdits( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        return !keptFactorMetadataEdits( ee, proposed ).isEmpty();
    }

    /**
     * The kept factors {@link #hasKeptFactorMetadataEdits} finds an in-place edit on, in proposal order.
     *
     * @see #keptFactorValueEdits
     */
    private List<ExperimentalFactor> keptFactorMetadataEdits( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null || proposed.getExperimentalFactors() == null ) {
            return Collections.emptyList();
        }
        Map<Long, ExperimentalFactor> currentFactorsById = new HashMap<>();
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            currentFactorsById.put( ef.getId(), ef );
        }
        List<ExperimentalFactor> edited = new ArrayList<>();
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
            if ( pf.getId() == null ) continue; // creations are already counted in the summary
            ExperimentalFactor cur = currentFactorsById.get( pf.getId() );
            if ( cur == null ) continue; // unknown id — a blocker, surfaced by previewDesignChange
            if ( pf.getName() != null && !Objects.equals( pf.getName(), cur.getName() ) ) {
                edited.add( cur );
                continue;
            }
            if ( pf.getDescription() != null && !Objects.equals( pf.getDescription(), cur.getDescription() ) ) {
                edited.add( cur );
                continue;
            }
            if ( pf.getCategory() != null && cur.getCategory() != null
                    && ( !Objects.equals( pf.getCategory().getCategory(), cur.getCategory().getCategory() )
                    || !Objects.equals( pf.getCategory().getCategoryUri(), cur.getCategory().getCategoryUri() )
                    || !Objects.equals( pf.getCategory().getValue(), cur.getCategory().getValue() )
                    || !Objects.equals( pf.getCategory().getValueUri(), cur.getCategory().getValueUri() ) ) ) {
                edited.add( cur );
            }
        }
        return edited;
    }

    /**
     * The subset of {@link #hasKeptFactorValueEdits} that changes the analysis MATH rather than its labels:
     * a baseline flip, or a measurement change on a continuous factor value.
     * <p>
     * A baseline flip reverses the direction of every contrast in an existing DEA —
     * {@link ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet} records the factor value
     * that WAS the reference, and each contrast's fold change is relative to it. For a two-level factor that
     * is a pure negation; for three or more the contrast SET changes (baseline A gives B-vs-A and C-vs-A;
     * baseline B needs A-vs-B and C-vs-B), so there is no in-place correction and the analysis must be re-run.
     * A measurement change moves the regression the same way.
     * <p>
     * 🛑 Statement, characteristic and free-text {@code value} edits are deliberately NOT here. They relabel a
     * factor value; they do not move a sample, a level or a reference. That is the ONE exclusion the
     * invalidation rule in {@link #previewDesignChange} is built around.
     * <p>
     * 🛑 Baseline-hood is read from the explicit {@code baseline} flag only. It is deliberately NOT computed
     * through {@code BaselineSelection}, which falls back to control-group characteristics when the flag is
     * absent: that would make a statement edit flip a baseline, and statement edits are the exclusion.
     */
    private boolean hasKeptFactorValueEditsThatChangeTheMath( ExpressionExperiment ee,
            ExperimentalDesignValueObject proposed ) {
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
                if ( pv.getId() == null ) continue; // a creation; already structural
                FactorValue cur = currentFvsById.get( pv.getId() );
                if ( cur == null ) continue; // unknown id — a blocker, surfaced by previewDesignChange
                if ( pv.getBaseline() != null && !Objects.equals( pv.getBaseline(), cur.getIsBaseline() ) ) {
                    return true;
                }
                if ( pv.getMeasurementObject() != null && measurementChanged( cur, pv.getMeasurementObject() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether {@code proposed} carries an in-place edit to an existing (kept) factor value that the structural
     * preflight summary does not count: a baseline-flag change, a deprecated-{@code value} change, a statement
     * edit, or a measurement edit on a continuous factor value. All honour the {@code null = "no change"}
     * convention used by {@link #applyFactorValueChanges}. Used by {@link #isNoOpDesignApply} so such edits are
     * not short-circuited away.
     */
    private boolean hasKeptFactorValueEdits( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        return !keptFactorValueEdits( ee, proposed ).isEmpty();
    }

    /**
     * The kept factor values {@link #hasKeptFactorValueEdits} finds an in-place edit on, in proposal order.
     * <p>
     * Returning them rather than a bare boolean is what lets {@link #previewDesignChange} report the edit
     * instead of counting it as {@code unchanged}: one comparison, read by the no-op gate and by the report,
     * so the two cannot disagree about what an edit is.
     */
    private List<FactorValue> keptFactorValueEdits( ExpressionExperiment ee, ExperimentalDesignValueObject proposed ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        if ( ed == null || proposed.getExperimentalFactors() == null ) {
            return Collections.emptyList();
        }
        Map<Long, FactorValue> currentFvsById = new HashMap<>();
        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {
            for ( FactorValue fv : ef.getFactorValues() ) {
                currentFvsById.put( fv.getId(), fv );
            }
        }
        List<FactorValue> edited = new ArrayList<>();
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry pf : proposed.getExperimentalFactors() ) {
            if ( pf.getValues() == null ) continue;
            for ( FactorValueBasicValueObject pv : pf.getValues() ) {
                if ( pv.getId() == null ) continue; // creations are already counted in the summary
                FactorValue cur = currentFvsById.get( pv.getId() );
                if ( cur == null ) continue; // unknown id — a blocker, surfaced by previewDesignChange
                if ( pv.getBaseline() != null && !Objects.equals( pv.getBaseline(), cur.getIsBaseline() ) ) {
                    edited.add( cur );
                    continue;
                }
                //noinspection deprecation
                if ( pv.getValue() != null && !Objects.equals( pv.getValue(), cur.getValue() ) ) {
                    edited.add( cur );
                    continue;
                }
                // Statements are replaced wholesale by updateFactorValueStatements when the payload provides them
                // (null = "no change"). Compare by content so an add / remove / edit registers, while a pure
                // round-trip that echoes the same statements stays a no-op.
                if ( pv.getStatements() != null && statementsChanged( cur, pv ) ) {
                    edited.add( cur );
                    continue;
                }
                // Attaching provenance to an otherwise-unchanged statement leaves the content keys identical, so
                // statementsChanged cannot see it. Without this an evidence-only write is swallowed exactly the
                // way a factor description-only write used to be.
                if ( pv.getStatements() != null && statementEvidenceChanged( cur, proposedStatements( pv ) ) ) {
                    edited.add( cur );
                    continue;
                }
                // A continuous factor value's measurement is the field its whole meaning rests on; retiming a
                // timepoint from 7 to 37 days moves no structural counter.
                if ( pv.getMeasurementObject() != null && measurementChanged( cur, pv.getMeasurementObject() ) ) {
                    edited.add( cur );
                }
            }
        }
        return edited;
    }

    /**
     * Whether any proposed statement carries provenance — supporting evidence or an evidence code — that differs
     * from what the statement it refers to already holds. Resolution mirrors
     * {@link #updateFactorValueStatements}: by id when the payload supplies one, otherwise by content key. A
     * proposed statement matching nothing is a creation, which {@link #statementsChanged} already counts, so it
     * is not considered here.
     * <p>
     * Only non-null proposed values are compared, honouring the {@code null = "no change"} convention that
     * {@link #applyStatementFields} writes under. Both slots go through this one comparison: an evidence code is
     * as invisible to {@link #statementsChanged} as supporting evidence is, and a second check beside this one
     * would be a second place for the no-op gate to disagree with the apply.
     */
    private static boolean statementEvidenceChanged( FactorValue cur, List<StatementValueObject> proposed ) {
        Map<Long, Statement> byId = new HashMap<>();
        Map<String, Statement> byContent = new HashMap<>();
        for ( Statement s : cur.getCharacteristics() ) {
            if ( s.getId() != null ) {
                byId.put( s.getId(), s );
            }
            byContent.putIfAbsent( statementContentKey( s ), s );
        }
        for ( StatementValueObject ps : proposed ) {
            if ( !CharacteristicUtils.hasRecordedEvidence( ps.getSupportingEvidence() ) && ps.getEvidenceCode() == null ) {
                continue;
            }
            Statement match = ps.getId() != null ? byId.get( ps.getId() ) : byContent.get( statementContentKey( ps ) );
            if ( match == null ) {
                continue; // a creation; statementsChanged covers it
            }
            if ( CharacteristicUtils.hasRecordedEvidence( ps.getSupportingEvidence() ) ) {
                String proposedEvidence = CharacteristicUtils.serializeSupportingEvidence( ps.getSupportingEvidence() );
                if ( !Objects.equals( proposedEvidence, match.getSupportingEvidence() ) ) {
                    return true;
                }
            }
            if ( ps.getEvidenceCode() != null
                    && !Objects.equals( parseEvidenceCode( ps.getEvidenceCode() ), match.getEvidenceCode() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the proposed measurement differs from the one the factor value currently carries. A factor value
     * that has no measurement yet and is given one counts as changed. Compares the four fields
     * {@link #applyMeasurementFields} writes, so a verbatim round-trip stays a no-op.
     */
    private static boolean measurementChanged( FactorValue cur, MeasurementValueObject proposed ) {
        Measurement m = cur.getMeasurement();
        if ( m == null ) {
            return true;
        }
        String currentUnit = m.getUnit() != null ? m.getUnit().getUnitNameCV() : null;
        String currentType = m.getType() != null ? m.getType().name() : null;
        String currentRepresentation = m.getRepresentation() != null ? m.getRepresentation().name() : null;
        return !Objects.equals( m.getValue(), proposed.getValue() )
                || !Objects.equals( currentUnit, proposed.getUnit() )
                || !Objects.equals( currentType, proposed.getType() )
                || !Objects.equals( currentRepresentation, proposed.getRepresentation() );
    }

    /**
     * Whether the proposed statement set differs in content from what the factor value currently carries. Compares
     * a multiset of {@link #statementContentKey content keys} so ordering and database ids are irrelevant — only
     * add / remove / field edits count. Echoing the current statements verbatim (the common baseline-edit
     * round-trip) yields equal multisets and is therefore not a change.
     */
    /**
     * Whether the statements {@code pv} proposes differ in content from the ones the factor value holds.
     * <p>
     * Compares the multiset of content keys the apply would end up with against the one it starts from, so an
     * add, a removal and a re-term all register while a payload that echoes what it read stays a no-op.
     * <p>
     * 🛑 <b>The proposed side is BOTH projections, not just {@code statements}.</b> A statement with no object
     * is rendered under {@code characteristics} and left out of {@code statements} entirely
     * ({@code AbstractFactorValueValueObjectSerializer} writes a statement only when it has an object), and
     * that is the commonest shape there is — a plain {@code organism part: chorionic villus}. Reading the
     * statements list alone, a client PUTting back exactly what {@code GET /design} gave it looks like it is
     * deleting every such row, which cost a spurious design-change event on every full-design round trip and
     * would have counted the whole design as edited in the preflight report. {@link #updateFactorValueStatements}
     * has always resolved the two projections together; this is the same resolution, read-only.
     */
    private static boolean statementsChanged( FactorValue cur, FactorValueBasicValueObject pv ) {
        List<StatementValueObject> proposed = proposedStatements( pv );
        Map<String, Integer> currentKeys = new HashMap<>();
        Map<Long, Statement> existingById = new HashMap<>();
        for ( Statement s : cur.getCharacteristics() ) {
            currentKeys.merge( statementContentKey( s ), 1, Integer::sum );
            if ( s.getId() != null ) {
                existingById.put( s.getId(), s );
            }
        }
        Map<String, Integer> proposedKeys = new HashMap<>();
        Set<Long> claimedByStatement = new HashSet<>();
        for ( StatementValueObject ps : proposed ) {
            proposedKeys.merge( statementContentKey( ps ), 1, Integer::sum );
            if ( ps.getId() != null ) {
                claimedByStatement.add( ps.getId() );
            }
        }
        if ( pv.getCharacteristics() != null ) {
            for ( CharacteristicValueObject pc : pv.getCharacteristics() ) {
                if ( pc.getId() == null || claimedByStatement.contains( pc.getId() ) ) continue;
                Statement target = existingById.get( pc.getId() );
                if ( target == null ) continue;
                // Mirrors applyCharacteristicSubjectFields: the characteristic projection rewrites the subject
                // side and cannot express a predicate or an object, so the ones on the row survive.
                proposedKeys.merge( statementContentKey( pc.getCategory(), pc.getCategoryUri(),
                        pc.getValue(), pc.getValueUri(),
                        target.getPredicate(), target.getPredicateUri(),
                        target.getObject(), target.getObjectUri(),
                        target.getSecondPredicate(), target.getSecondPredicateUri(),
                        target.getSecondObject(), target.getSecondObjectUri() ), 1, Integer::sum );
            }
        }
        return !currentKeys.equals( proposedKeys );
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
        // Provenance: null = "no change", the same round-trip-safe convention `value` and `isBaseline` use on a
        // factor value. A client that does not carry evidence cannot wipe evidence somebody else recorded.
        if ( CharacteristicUtils.hasRecordedEvidence( pf.getSupportingEvidence() ) ) {
            ef.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( pf.getSupportingEvidence() ) );
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
                // Baseline flag: null = "no change" (same round-trip-safe convention as `value`).
                if ( pv.getBaseline() != null ) {
                    existing.setIsBaseline( pv.getBaseline() );
                }
                // Measurement on a continuous factor value: same null = "no change" convention.
                if ( pv.getMeasurementObject() != null ) {
                    applyMeasurementFields( existing, pv.getMeasurementObject() );
                }
                // Provenance on the VALUE itself, distinct from the evidence on its statements: same
                // null = "no change" convention again.
                if ( CharacteristicUtils.hasRecordedEvidence( pv.getSupportingEvidence() ) ) {
                    existing.setSupportingEvidence(
                            CharacteristicUtils.serializeSupportingEvidence( pv.getSupportingEvidence() ) );
                }
            }
        }
        // Siblings are deliberately left alone. Clearing them made a second baseline impossible to record at all:
        // marking B would silently unmark A, so a two-experiment dataset could never carry its two reference
        // levels. Each factor value's flag now means exactly what the payload said about that value, and nothing
        // about its neighbours -- `null` still means "no change", so a client that omits the field is unaffected.
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
        ef.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( pf.getSupportingEvidence() ) );
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
        for ( StatementValueObject ps : proposedStatements( pv ) ) {
            fv.getCharacteristics().add( buildStatement( ps ) );
        }
        if ( pv.getMeasurementObject() != null ) {
            applyMeasurementFields( fv, pv.getMeasurementObject() );
        }
        fv.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( pv.getSupportingEvidence() ) );
        return factorValueService.create( fv );
    }

    /**
     * Write a proposed measurement onto a factor value, creating the {@link Measurement} if the factor value does
     * not have one yet. Shared by the create and update halves of the design apply so a continuous factor value
     * carries the same fields however it was reached.
     * <p>
     * The unit is resolved through {@link UnitDao} rather than attached transiently: {@code FactorValue.measurement}
     * cascades on persist but {@code Measurement.unit} does not, so a fresh {@link Unit} would be dropped and the
     * measurement would land as a bare number. Mirrors {@code EeWriteServiceImpl#findOrCreateUnit}.
     */
    private void applyMeasurementFields( FactorValue fv, MeasurementValueObject pm ) {
        Measurement m = fv.getMeasurement();
        if ( m == null ) {
            m = Measurement.Factory.newInstance();
            fv.setMeasurement( m );
        }
        m.setValue( pm.getValue() );
        if ( pm.getRepresentation() != null ) {
            m.setRepresentation( PrimitiveType.valueOf( pm.getRepresentation() ) );
        }
        if ( pm.getType() != null ) {
            m.setType( MeasurementType.valueOf( pm.getType() ) );
        }
        if ( StringUtils.isNotBlank( pm.getUnit() ) ) {
            Unit unit = Unit.Factory.newInstance( pm.getUnit() );
            Unit existing = unitDao.find( unit );
            m.setUnit( existing != null ? existing : unitDao.create( unit ) );
        }
    }

    private void updateFactorValueStatements( FactorValue existing, FactorValueBasicValueObject pv ) {
        List<StatementValueObject> proposedStatements = proposedStatements( pv );
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

    /**
     * The entity half of the comparison, canonicalized to match the VO half.
     * <p>
     * {@link StatementValueObject} canonicalizes its term URIs and labels on construction and this
     * side did not, so every stored statement holding a retired URI compared unequal to the very
     * document that was rendered from it: a preflight that asserted nothing reported updates, on 9
     * of 9 datasets probed on 2026-08-29. Category and predicate are raw on both sides — the shim
     * deliberately leaves them alone — so they stay raw here.
     */
    private static String statementContentKey( Statement s ) {
        return statementContentKey( s.getCategory(), s.getCategoryUri(),
                CharacteristicUtils.canonicalLabel( s.getSubjectUri(), s.getSubject() ),
                CharacteristicUtils.canonicalUri( s.getSubjectUri() ),
                s.getPredicate(), s.getPredicateUri(),
                CharacteristicUtils.canonicalLabel( s.getObjectUri(), s.getObject() ),
                CharacteristicUtils.canonicalUri( s.getObjectUri() ),
                s.getSecondPredicate(), s.getSecondPredicateUri(),
                CharacteristicUtils.canonicalLabel( s.getSecondObjectUri(), s.getSecondObject() ),
                CharacteristicUtils.canonicalUri( s.getSecondObjectUri() ) );
    }

    private static String statementContentKey( String... fields ) {
        return Stream.of( fields ).map( f -> f == null ? "\0" : f ).collect( Collectors.joining( "\u001F" ) );
    }

    /**
     * Whether a proposed term differs from the stored one ONLY by the read-time canonicalisation.
     * <p>
     * A client edits what the API served it, and the API serves canonical URIs, so a document that
     * changes nothing still proposes the canonical form of every retired URI it was shown. Writing
     * that back performs the parked database migration
     * ({@code scripts/sql/term_uri_migration.sql}) on whichever rows happen to ride along with an
     * unrelated edit — a migration nobody ran, one row at a time, and no way to tell afterwards
     * which rows moved. The stored value stays until someone runs the migration deliberately.
     */
    private static boolean isOnlyCanonicalization( @Nullable String storedLabel, @Nullable String storedUri,
            @Nullable String proposedLabel, @Nullable String proposedUri ) {
        if ( storedUri == null || proposedUri == null || storedUri.equals( proposedUri ) ) {
            return false;
        }
        return proposedUri.equals( CharacteristicUtils.canonicalUri( storedUri ) )
                && Objects.equals( proposedLabel, CharacteristicUtils.canonicalLabel( storedUri, storedLabel ) );
    }

    /**
     * Re-join the halves of a compound statement that the wire format splits apart.
     * <p>
     * A statement carrying two objects reaches clients as <em>two</em> entries in {@code statements[]} sharing
     * one id, the second putting the second clause under the generic {@code predicate} / {@code object} keys.
     * That flattening is the settled contract (#814, {@code dff752727c}) and is why
     * {@link StatementValueObject}'s {@code second*} slots are withheld from the API. The write path never
     * learned the inverse: both entries claimed the same row, {@link #applyStatementFields} ran twice, and each
     * run wrote the {@code second*} slots as null because neither entry carries them. A GET followed by an
     * unedited PUT therefore dropped the second clause of every compound statement it touched.
     * <p>
     * Rows with no id cannot be two halves of one statement and pass through untouched, in order. A third row
     * claiming one id is refused by the preflight ({@code STATEMENT_ID_REPEATED}) before anything reaches here,
     * so the extras dropped below are unreachable from an accepted payload.
     *
     * @param proposed statements exactly as the payload carried them
     * @return one entry per statement, with both object slots filled; never the caller's own instances, since
     * the same payload is walked again by the preflight
     */
    private static List<StatementValueObject> unflattenStatements( List<StatementValueObject> proposed ) {
        List<StatementValueObject> out = new ArrayList<>( proposed.size() );
        Map<Long, StatementValueObject> firstById = new HashMap<>();
        for ( StatementValueObject ps : proposed ) {
            if ( ps.getId() == null ) {
                out.add( ps );
                continue;
            }
            StatementValueObject first = firstById.get( ps.getId() );
            if ( first == null ) {
                StatementValueObject copy = copyStatementValueObject( ps );
                firstById.put( ps.getId(), copy );
                out.add( copy );
                continue;
            }
            if ( first.getSecondPredicate() != null || first.getSecondObject() != null ) {
                continue;
            }
            first.setSecondPredicate( ps.getPredicate() );
            first.setSecondPredicateUri( ps.getPredicateUri() );
            first.setSecondObject( ps.getObject() );
            first.setSecondObjectUri( ps.getObjectUri() );
            // Provenance rides on whichever half recorded it; the serializer emits it on neither, so this only
            // matters for a hand-built payload that puts it on the second row.
            if ( first.getSupportingEvidence() == null ) {
                first.setSupportingEvidence( ps.getSupportingEvidence() );
            }
            if ( first.getEvidenceCode() == null ) {
                first.setEvidenceCode( ps.getEvidenceCode() );
            }
        }
        return out;
    }

    /**
     * The statements a factor-value payload proposes, with split compound statements re-joined.
     *
     * @see #unflattenStatements(List)
     */
    private static List<StatementValueObject> proposedStatements( FactorValueBasicValueObject pv ) {
        return pv.getStatements() != null ? unflattenStatements( pv.getStatements() ) : Collections.emptyList();
    }

    private static StatementValueObject copyStatementValueObject( StatementValueObject ps ) {
        StatementValueObject copy = new StatementValueObject();
        copy.setId( ps.getId() );
        copy.setCategory( ps.getCategory() );
        copy.setCategoryUri( ps.getCategoryUri() );
        copy.setSubject( ps.getSubject() );
        copy.setSubjectUri( ps.getSubjectUri() );
        copy.setPredicate( ps.getPredicate() );
        copy.setPredicateUri( ps.getPredicateUri() );
        copy.setObject( ps.getObject() );
        copy.setObjectUri( ps.getObjectUri() );
        copy.setSecondPredicate( ps.getSecondPredicate() );
        copy.setSecondPredicateUri( ps.getSecondPredicateUri() );
        copy.setSecondObject( ps.getSecondObject() );
        copy.setSecondObjectUri( ps.getSecondObjectUri() );
        copy.setSupportingEvidence( ps.getSupportingEvidence() );
        copy.setEvidenceCode( ps.getEvidenceCode() );
        return copy;
    }

    private Statement buildStatement( StatementValueObject ps ) {
        Statement s = Statement.Factory.newInstance();
        applyStatementFields( s, ps );
        return s;
    }

    private void applyStatementFields( Statement s, StatementValueObject ps ) {
        s.setCategory( ps.getCategory() );
        s.setCategoryUri( ps.getCategoryUri() );
        if ( !isOnlyCanonicalization( s.getSubject(), s.getSubjectUri(), ps.getSubject(), ps.getSubjectUri() ) ) {
            s.setSubject( ps.getSubject() );
            s.setSubjectUri( ps.getSubjectUri() );
        }
        s.setPredicate( ps.getPredicate() );
        s.setPredicateUri( ps.getPredicateUri() );
        if ( !isOnlyCanonicalization( s.getObject(), s.getObjectUri(), ps.getObject(), ps.getObjectUri() ) ) {
            s.setObject( ps.getObject() );
            s.setObjectUri( ps.getObjectUri() );
        }
        s.setSecondPredicate( ps.getSecondPredicate() );
        s.setSecondPredicateUri( ps.getSecondPredicateUri() );
        if ( !isOnlyCanonicalization( s.getSecondObject(), s.getSecondObjectUri(), ps.getSecondObject(), ps.getSecondObjectUri() ) ) {
            s.setSecondObject( ps.getSecondObject() );
            s.setSecondObjectUri( ps.getSecondObjectUri() );
        }
        // Supporting evidence follows the same null = "no change" convention as the rest of the payload, so a
        // client that doesn't carry provenance cannot wipe provenance somebody else recorded.
        if ( CharacteristicUtils.hasRecordedEvidence( ps.getSupportingEvidence() ) ) {
            s.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( ps.getSupportingEvidence() ) );
        }
        // Same null = "no change" convention for the evidence code. A statement the payload says nothing about
        // keeps whatever code it has, which for a new statement is none — the design path has never assigned one
        // and that stays true for a caller that does not ask.
        if ( ps.getEvidenceCode() != null ) {
            s.setEvidenceCode( parseEvidenceCode( ps.getEvidenceCode() ) );
        }
    }

    /**
     * Resolve a {@link GOEvidenceCode} name, case-insensitively. The REST layer validates first and answers a
     * 400; this is the guard for a direct service caller, and it names the offending value rather than letting
     * {@code valueOf}'s bare message surface.
     */
    private static GOEvidenceCode parseEvidenceCode( String name ) {
        try {
            return GOEvidenceCode.valueOf( name.trim().toUpperCase( Locale.ROOT ) );
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Unknown evidence code '" + name
                    + "'; expected a GOEvidenceCode name (IC, IEA, IIA, TAS, …).", e );
        }
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
    public boolean hasSourceMetadata( ExpressionExperiment ee ) {
        return expressionExperimentDao.hasSourceMetadata( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public String getSourceMetadata( ExpressionExperiment ee ) {
        return expressionExperimentDao.getSourceMetadata( ee );
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
    @Transactional(readOnly = true)
    public boolean isSingleCell( ExpressionExperiment ee ) {
        // Reads the lazy characteristics collection and then hits the DAO, so it needs a session of its
        // own when called from outside one (the REST layer does). isRNASeq below is already annotated.
        // 🛑 @Transactional alone is NOT enough: a caller outside a transaction hands us a DETACHED
        // instance from a transaction that has already closed, and opening a new one does not re-attach
        // it — ee.getCharacteristics() still throws LazyInitializationException. Re-attach explicitly.
        // Caught live on GET /datasets/3937/sample-correlation, 2026-08-31.
        ee = ensureInSession( ee );
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

        // Same normalization as addAnnotation: the desired set is what gets written, so it lands as
        // statements. Existing rows are left as they are — sameTag matches on content, so a plain
        // stored tag and a bare desired statement still pair up and neither is churned.
        desired = desired.stream().map( CharacteristicUtils::asStatement ).collect( Collectors.toList() );

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
     * Replace an EE's primary + other-relevant publications, recording every one as a bare curator
     * assertion. See the interface javadoc.
     */
    @Override
    @Transactional
    public void updatePublications( ExpressionExperiment ee, BibliographicReference primaryPublication,
            Collection<BibliographicReference> otherRelevantPublications ) {
        Assert.notNull( otherRelevantPublications, "The other-relevant-publication set must not be null (use an empty collection to clear)." );
        List<PublicationAssertion> other = new ArrayList<>( otherRelevantPublications.size() );
        for ( BibliographicReference ref : otherRelevantPublications ) {
            other.add( new PublicationAssertion( ref, PublicationAssociationSource.CURATOR ) );
        }
        // null, not emptyList: this form has no rejection argument, so it is not in a position to say
        // anything about them and must not clear the ones on record.
        updatePublications( ee,
                primaryPublication != null ? new PublicationAssertion( primaryPublication, PublicationAssociationSource.CURATOR ) : null,
                other, null );
    }

    /**
     * Replace an EE's publications and the evidence behind them. See the interface javadoc.
     * <p>
     * Set-replace: the other-relevant set is cleared and repopulated from {@code otherRelevantPublications}
     * (skipping any entry that equals the incoming primary, so the primary never doubles as an other-relevant
     * row), and the primary is set to {@code primaryPublication} (or cleared when null). Persisted through the
     * inherited {@code update(ee)}, which carries the audit event — matching the legacy
     * {@code setPrimaryPublication(...) + update(ee)} flow the gemma-web controller and the CLI used.
     * <p>
     * The assertions are reconciled first, on purpose. It is the step that can refuse — a publication
     * standing rejected by an authority the caller does not outrank throws — and doing it before the
     * links are touched means the refusal happens with the experiment unmodified rather than relying
     * on the transaction to undo a half-applied change.
     */
    @Override
    @Transactional
    public void updatePublications( ExpressionExperiment ee, @Nullable PublicationAssertion primaryPublication,
            Collection<PublicationAssertion> otherRelevantPublications,
            @Nullable Collection<PublicationAssertion> rejectedPublications ) {
        Assert.notNull( otherRelevantPublications, "The other-relevant-publication set must not be null (use an empty collection to clear)." );

        ee = ensureInSession( ee );

        BibliographicReference primaryRef = primaryPublication != null ? primaryPublication.getPublication() : null;

        Set<BibliographicReference> desiredOther = new HashSet<>();
        List<PublicationAssertion> otherAssertions = new ArrayList<>();
        for ( PublicationAssertion a : otherRelevantPublications ) {
            if ( primaryRef != null && Objects.equals( a.getPublication().getId(), primaryRef.getId() ) ) {
                continue;
            }
            if ( desiredOther.add( a.getPublication() ) ) {
                otherAssertions.add( a );
            }
        }

        publicationAssociationService.reconcile( ee, primaryPublication, otherAssertions, rejectedPublications );

        ee.setPrimaryPublication( primaryRef );
        ee.getOtherRelevantPublications().clear();
        ee.getOtherRelevantPublications().addAll( desiredOther );

        update( ee );
        log.info( "updatePublications: " + ee.getShortName() + " (ID=" + ee.getId() + ") primary="
                + ( primaryRef != null ? primaryRef.getId() : "none" )
                + " otherRelevant=" + desiredOther.size()
                + " rejected=" + ( rejectedPublications != null ? String.valueOf( rejectedPublications.size() ) : "untouched" ) );
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

    /**
     * The text this commit appends to each annotation audit note: the disposition key and the prose, or
     * whichever of the two was supplied. Composed once here so every annotation the commit touches
     * carries the same sentence, and so the key leads — that is the part a later query can group on.
     */
    @Nullable
    private static String auditReason( CurationCommitRequest request ) {
        String code = StringUtils.trimToNull( request.getReasonCode() );
        String prose = StringUtils.trimToNull( request.getReason() );
        if ( code == null ) {
            return prose;
        }
        return prose == null ? code : code + ": " + prose;
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
            PublicationAssertion primary = request.getPrimaryPublication();
            Long primaryId = primary != null ? primary.getPublication().getId() : null;
            List<PublicationAssertion> desiredOther = new ArrayList<>();
            for ( PublicationAssertion a : request.getOtherRelevantPublications() ) {
                if ( Objects.equals( a.getPublication().getId(), primaryId ) ) {
                    continue;
                }
                desiredOther.add( a );
            }
            Set<Long> currentIds = new HashSet<>();
            if ( ee.getPrimaryPublication() != null ) {
                currentIds.add( ee.getPrimaryPublication().getId() );
            }
            for ( BibliographicReference r : ee.getOtherRelevantPublications() ) {
                currentIds.add( r.getId() );
            }
            Set<Long> desiredIds = new HashSet<>();
            if ( primaryId != null ) {
                desiredIds.add( primaryId );
            }
            for ( PublicationAssertion a : desiredOther ) {
                desiredIds.add( a.getPublication().getId() );
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
                // Through the same reconcile the standalone write path uses, so a commit cannot leave
                // an ACCEPTED assertion pointing at a link it has just removed. The caller's basis rides
                // with each assertion (defaulting to a bare curator claim when none was given), so a
                // publication this commit adds records why -- and a snapshot replayed as a restore puts a
                // paper back with the basis it had, instead of as an unexplained curator claim. A kept
                // publication is untouched unless the incoming source outranks the recorded one.
                //
                // Rejections are passed as null -- untouched, not cleared. CurationPublications has no
                // rejection field, so this section cannot express one, and a section that cannot say a
                // thing must not be read as denying it. Committing an unrelated edit to a dataset is
                // not a curator withdrawing a ruling about which paper is not theirs.
                publicationAssociationService.reconcile( ee, primary, desiredOther, null );
                ee.setPrimaryPublication( primary != null ? primary.getPublication() : null );
                ee.getOtherRelevantPublications().clear();
                for ( PublicationAssertion a : desiredOther ) {
                    ee.getOtherRelevantPublications().add( a.getPublication() );
                }
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
                // `updated` is every kind of in-place change: samples moved between factor values, and the
                // relabels — a statement re-termed, evidence attached, a baseline flipped, a measurement
                // retimed, a factor renamed or re-categorized. Assignments used to be the only one counted,
                // so a term-only edit reported `unchanged: 1` and read as "nothing to do" for an edit the
                // commit would in fact apply (cab, GSE49354.1, 2026-08-27).
                result.setDesignUpdated( s.getBiomaterialsWithChangedAssignments()
                        + s.getFactorsToUpdate() + s.getFactorValuesToUpdate() );
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
                int updated = s1.getBiomaterialsWithChangedAssignments()
                        + s1.getFactorsToUpdate() + s1.getFactorValuesToUpdate();

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
                            // Pass 2 exists only to attach samples to factor values pass 1 created, so only the
                            // assignment count can move; the in-place counters would restate pass 1's edits.
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
                    if ( self.removeAnnotation( ee, id, auditReason( request ) ) != null ) {
                        deleted++;
                    }
                }
                List<CurationCommitRequest.TagAdd> adds = request.getTagsToAdd();
                for ( CurationCommitRequest.TagAdd add : adds ) {
                    self.addAnnotation( ee, add.getCharacteristic(), auditReason( request ) );
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
                    if ( bm != null && bioMaterialService.removeAnnotation( ee, bm, id, auditReason( request ) ) != null ) {
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
                    bioMaterialService.addAnnotation( ee, bm, add.getCharacteristic(), auditReason( request ) );
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
        // ── auto-snapshot: keep the curation this commit displaced ──
        // The payload was read before anything applied, so the row holds the pre-commit state and the ordinary
        // restore path puts it back. Minted inside the commit's transaction: a rollback must not leave a snapshot
        // claiming a state that was never displaced. Only when something actually changed — a no-op commit
        // displaces nothing, and a row per retry buries the restore points that matter. SNAPSHOT emits no audit
        // event (AnnotationSetServiceImpl#ATTACH_AUDIT_WHEN excludes it), so this does not touch lastUpdated.
        if ( !dryRun && anyChange && StringUtils.isNotBlank( request.getSnapshotPayloadJson() ) ) {
            AnnotationSetService.AttachedAnnotationSet snapshot = annotationSetService.attach( ee,
                    AnnotationSetRole.SNAPSHOT,
                    // Gemma read this payload out of itself, so source can only say which kind of actor's commit
                    // displaced it: a named run means an agent applied it, anything else a curator.
                    StringUtils.isNotBlank( request.getRunId() ) ? AnnotationSetSource.AGENT : AnnotationSetSource.CURATOR,
                    null, AnnotationSetService.PRE_COMMIT_SNAPSHOT_RUN_ID_PREFIX + UUID.randomUUID(),
                    request.getSnapshotCreatedBy(), null, request.getSnapshotPayloadJson(), null );
            result.setSnapshotAnnotationSetId( snapshot.getAnnotationSet().getId() );
            log.info( "commitCuration: " + ee.getShortName() + " (ID=" + ee.getId() + ") displaced curation kept as"
                    + " AnnotationSet#" + snapshot.getAnnotationSet().getId() );
        }

        // ── run provenance: record WHICH agent run applied this, if the caller named one ──
        // Minted here rather than in the web layer so it shares the commit's transaction: if the commit rolls back
        // there must be no row claiming the run applied anything. Sparse by design — a curator commit names no run
        // and mints nothing. A no-op commit that DID name a run still mints, so that an absent row means "no run
        // was named" and never "the run did nothing"; those are different facts and identical bytes otherwise.
        // The event is deliberately suppressed for COMMIT (see AnnotationSetServiceImpl#ATTACH_AUDIT_WHEN) — the
        // sections above already emitted the trail entries and already moved lastUpdated.
        if ( !dryRun && StringUtils.isNotBlank( request.getRunId() ) ) {
            AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach( ee,
                    AnnotationSetRole.COMMIT, AnnotationSetSource.AGENT, null,
                    request.getRunId(), null, request.getRunProvenance(), null, request.getRunParentProposal() );
            result.setCommitAnnotationSetId( attached.getAnnotationSet().getId() );
            log.info( "commitCuration: " + ee.getShortName() + " (ID=" + ee.getId() + ") stamped with run "
                    + request.getRunId() + " as AnnotationSet#" + attached.getAnnotationSet().getId()
                    + ( attached.isCreated() ? "" : " (already recorded — this run has committed here before)" ) );
        }

        // ── close the curation ticket this commit fulfilled ──
        // In the commit's own transaction (Paul, 2026-08-29): if the commit rolls back, the ticket does
        // not advance. Restore and preflight leave the flag false, so a revert never closes the ticket.
        if ( !dryRun && request.isAdvanceLinkedTickets() ) {
            advanceLinkedCurationTickets( ee );
        }

        // ── denormalized annotation table ──
        if ( !dryRun ) {
            refreshEe2c( ee, result );
        }

        result.setNewLastUpdated( ee.getCurationDetails() != null ? ee.getCurationDetails().getLastUpdated() : null );
        return result;
    }

    /**
     * Bring {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} up to date for the one experiment this commit
     * touched, in the commit's own transaction.
     * <p>
     * The table is denormalized and was refreshed only by the nightly Quartz job, which made a correct
     * write read as a lost one: {@code EE2C_CHARACTERISTIC_FKC} is {@code ON DELETE CASCADE}, so a deleted
     * annotation left EE2C immediately while an added one waited for the night. cab's first write-back
     * (GSE197199, 2026-08-30) saw exactly that — one tag added and one removed in a single all-or-none
     * commit, and only the removal visible in EE2C, which reads as a partial write of the one endpoint that
     * guarantees it cannot happen. The nightly job stays; it remains the backstop for every other writer
     * (GEO import, the CLI, direct SQL, single-cell assignments) and the repair path if this leg fails.
     * <p>
     * Three constraints shape what is called here.
     * <p>
     * <b>A narrowed level, never {@code null}.</b> The all-levels refresh unions five queries, two of which
     * ({@code CellTypeAssignment}, {@code CellLevelCharacteristics}) no curation section can affect. On
     * production's largest single-cell experiment (eid 42860, 1,681,672 EE2C rows) that union measured
     * 159 s, of which 150 s was the cell-level branch; the three levels a commit can actually move measured
     * 13-28 ms on that same experiment. Passing {@code null} would put a two-and-a-half-minute write
     * transaction on a curator's save.
     * <p>
     * <b>Only when something changed.</b> A no-op or preflight commit pays nothing.
     * {@code designUpdated} lumps sample-assignment moves (which do not reach EE2C) in with statement
     * re-terms (which do), so an assignment-only design change does buy one upsert that changes no rows —
     * ~13-30 ms, and separating the two would mean splitting a tally the wire format already publishes.
     * <p>
     * <b>Flush first.</b> The refresh is a native query that declares only the EE2C query space, so
     * Hibernate's auto-flush will not notice pending {@code CHARACTERISTIC} inserts and the rebuild would
     * read the pre-commit state — the very staleness this closes. The tag and sample-characteristic
     * sections already flush after their adds; the design section does not, and neither flushes after a
     * delete.
     */
    private void refreshEe2c( ExpressionExperiment ee, CurationCommitResult result ) {
        List<Class<?>> levels = new ArrayList<>( 3 );
        if ( result.getTagsCreated() > 0 || result.getTagsDeleted() > 0 ) {
            levels.add( ExpressionExperiment.class );
        }
        if ( result.getSampleCharsCreated() > 0 || result.getSampleCharsDeleted() > 0 ) {
            levels.add( BioMaterial.class );
        }
        if ( result.getDesignCreated() > 0 || result.getDesignDeleted() > 0 || result.getDesignUpdated() > 0 ) {
            levels.add( ExperimentalDesign.class );
        }
        if ( levels.isEmpty() ) {
            return;
        }
        sessionFactory.getCurrentSession().flush();
        for ( Class<?> level : levels ) {
            tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ee, level );
        }
    }

    /**
     * After a successful curation commit, walk the open CURATION / SCREENING tickets that target this
     * dataset: mark each not-yet-DONE target for this EE as DONE, and RESOLVE a ticket whose last open
     * target this closes. A multi-dataset ticket keeps its other datasets' targets. Called inside
     * {@link #commitCuration}'s transaction so the advance shares the commit's fate.
     */
    private void advanceLinkedCurationTickets( ExpressionExperiment ee ) {
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            // No principal to attribute the advance to; leave the ticket for a manual touch rather than
            // writing an unattributed event.
            return;
        }
        for ( Ticket ticket : ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) {
            if ( ticket.getType() != TicketType.CURATION && ticket.getType() != TicketType.SCREENING ) {
                continue;
            }
            Ticket current = ticket;
            List<Long> toAdvance = new ArrayList<>();
            for ( TicketTarget t : current.getTargets() ) {
                if ( t.getTargetType() == TicketTargetType.EXPRESSION_EXPERIMENT
                        && ee.getId().equals( t.getTargetId() )
                        && t.getStatus() != TicketTargetStatus.DONE ) {
                    toAdvance.add( t.getId() );
                }
            }
            for ( Long rowId : toAdvance ) {
                current = ticketService.updateTargetStatus( current, rowId, TicketTargetStatus.DONE, actor );
            }
            boolean allDone = current.getTargets().stream()
                    .allMatch( t -> t.getStatus() == TicketTargetStatus.DONE );
            if ( allDone && current.getState() != TicketState.RESOLVED ) {
                ticketService.transition( current, TicketState.RESOLVED, actor, "curation committed" );
            }
        }
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
        return doAddAnnotation( ee, vc );
    }

    /**
     * Reason-carrying overload. Separate method rather than a parameter on the one above so that every
     * existing caller and its tests keep the signature they have; the two differ only in the audit note
     * the aspect writes.
     * <p>
     * 🛑 Both are audited and both delegate to the same private body. The delegation is a plain
     * {@code this} call, so the inner method is NOT re-advised and one call still writes one event.
     */
    @Override
    @Transactional
    @Audited(value = TagAddedEvent.class,
            messageSpel = "'Added tag ' + #vc.category + ' = ' + #vc.value + (#reason != null ? ' \u2014 ' + #reason : '')")
    public Characteristic addAnnotation( ExpressionExperiment ee, Characteristic vc, @Nullable String reason ) {
        return doAddAnnotation( ee, vc );
    }

    private Characteristic doAddAnnotation( ExpressionExperiment ee, Characteristic vc ) {
        Assert.notNull( vc, "Characteristic must not be null." );
        Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Must provide a category" );
        Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Must provide a value" );
        // Experiment tags are statements; a bare one is a statement with no predicate or object. Doing
        // this here rather than at each caller means every write path lands the same shape, including
        // ones added later. sameTag compares content, so this changes no comparison.
        vc = CharacteristicUtils.asStatement( vc );
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
        return doRemoveAnnotation( ee, annotationId );
    }

    /** Reason-carrying overload; see {@link #addAnnotation(ExpressionExperiment, Characteristic, String)}. */
    @Override
    @Transactional
    @AuditedConditional(value = TagRemovedEvent.class,
            when = "#result != null",
            messageSpel = "'Removed tag ' + #result.category + ' = ' + #result.value + (#reason != null ? ' \u2014 ' + #reason : '')")
    @Nullable
    public Characteristic removeAnnotation( ExpressionExperiment ee, Long annotationId, @Nullable String reason ) {
        return doRemoveAnnotation( ee, annotationId );
    }

    @Nullable
    private Characteristic doRemoveAnnotation( ExpressionExperiment ee, Long annotationId ) {
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
}