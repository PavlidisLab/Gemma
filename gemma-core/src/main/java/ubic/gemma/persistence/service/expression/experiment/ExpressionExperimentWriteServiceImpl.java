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

import ubic.gemma.core.security.SecurityService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreferredDataChangedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreferredRawDataChangedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreferredSingleCellDataChangedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ExpressionExperimentWriteService}.
 * <p>
 * All public methods are {@code @Transactional} (write). ACL enforcement is the responsibility
 * of the facade {@link ExpressionExperimentService} interface -- this class is unsecured at the
 * AOP boundary on purpose, so that intra-{@code gemma-core} callers that hold an authenticated
 * session can bypass duplicate ACL checks.
 * <p>
 * For lifecycle removal, the heavy orchestration is preserved verbatim from the original
 * monolith: subset removal, DEA / sample-coex / PCA cleanup, then removal from EE sets, then
 * delegation to the DAO. The subset lookup goes directly through the DAO
 * ({@code expressionExperimentDao.getSubSets(ee)}) so this service has no back-edge into
 * {@link ExpressionExperimentReadService} and therefore introduces no new construction cycle.
 *
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentWriteService")
public class ExpressionExperimentWriteServiceImpl implements ExpressionExperimentWriteService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentWriteServiceImpl.class );

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;
    @Autowired
    private FactorValueService factorValueService;
    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private SecurityService securityService;

    @Autowired
    public ExpressionExperimentWriteServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    // ---------------------------------------------------------------------
    // Bucket E -- design mutation
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public ExperimentalFactor addFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExpressionExperiment experiment = expressionExperimentDao.load( ee.getId() );
        if ( experiment == null ) {
            throw new IllegalArgumentException( "The passed EE does not exist anymore." );
        }
        factor.setExperimentalDesign( experiment.getExperimentalDesign() );
        factor.setSecurityOwner( experiment );
        factor = experimentalFactorService.create( factor ); // to make sure we get acls.
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
        experiment.getExperimentalDesign().getExperimentalFactors().add( factor );
        expressionExperimentDao.update( experiment );
        return factor;
    }

    @Override
    @Transactional
    public FactorValue addFactorValue( ExpressionExperiment ee, FactorValue fv ) {
        assert fv.getExperimentalFactor() != null;
        ExpressionExperiment experiment = requireNonNull( expressionExperimentDao.load( ee.getId() ) );
        fv.setSecurityOwner( experiment );
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
        Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
        fv = this.factorValueService.create( fv );
        for ( ExperimentalFactor ef : efs ) {
            if ( fv.getExperimentalFactor().equals( ef ) ) {
                ef.getFactorValues().add( fv );
                break;
            }
        }
        expressionExperimentDao.update( experiment );
        return fv;
    }

    @Override
    @Transactional
    public void addFactorValues( ExpressionExperiment ee, Map<BioMaterial, FactorValue> fvs ) {
        ExpressionExperiment experiment = requireNonNull( expressionExperimentDao.load( ee.getId() ) );
        if ( experiment.getExperimentalDesign() == null ) {
            log.info( "Creating missing experimental design for " + experiment );
            experiment.setExperimentalDesign( new ExperimentalDesign() );
        }
        Collection<ExperimentalFactor> efs = experiment.getExperimentalDesign().getExperimentalFactors();
        int count = 0;
        for ( Map.Entry<BioMaterial, FactorValue> fvEntry : fvs.entrySet() ) {
            BioMaterial bm = fvEntry.getKey();
            FactorValue fv = fvEntry.getValue();
            fv.setSecurityOwner( experiment );
            fv = this.factorValueService.create( fv );

            for ( ExperimentalFactor ef : efs ) {
                if ( fv.getExperimentalFactor().equals( ef ) ) {
                    ef.getFactorValues().add( fv );
                    break;
                }
            }
            bm.getFactorValues().add( fv );
            ++count;
            if ( count % 50 == 0 ) {
                log.info( "Processed: " + count + " biomaterials for new factor values" );
            }
        }
        log.info( "Processed: " + count + " biomaterials for new factor values, updating ..." );
        //  expressionExperimentDao.update( experiment );
        bioMaterialService.update( fvs.keySet() );
    }

    /**
     * Will add the characteristic to the expression experiment and persist the changes.
     *
     * @param ee the experiment to add the characteristics to.
     * @param vc If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     */
    @Override
    @Transactional
    public void addCharacteristic( ExpressionExperiment ee, Characteristic vc ) {
        Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Must provide a category" );
        Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Must provide a value" );

        ee = ensureInSession( ee );

        if ( vc.getEvidenceCode() == null ) {
            log.debug( String.format( "No evidence code set for %s, defaulting to %s.", vc, GOEvidenceCode.IC ) );
            vc.setEvidenceCode( GOEvidenceCode.IC ); // assume: manually added characteristic
        }

        log.info( "Adding characteristic '" + vc.getValue() + "' to " + ee.getShortName() + " (ID=" + ee.getId()
                + ") : " + vc );

        ee.getCharacteristics().add( vc );
        expressionExperimentDao.update( ee );
    }

    @Override
    @Transactional
    public void removeCharacteristics( ExpressionExperiment ee, Collection<Characteristic> characteristicsToRemove ) {
        Assert.isTrue( characteristicsToRemove.stream().allMatch( c -> c.getId() != null ), "All characteristics must be persistent." );
        Assert.isTrue( ee.getCharacteristics().containsAll( characteristicsToRemove ), "expected true" );
        ee.getCharacteristics().removeAll( characteristicsToRemove );
        expressionExperimentDao.update( ee );
        characteristicService.remove( characteristicsToRemove );
    }

    @Override
    @Transactional
    public void updateQuantitationType( ExpressionExperiment ee, QuantitationType qt, @Nullable QuantitationType previousPreferredQt ) {
        Assert.notNull( ee.getId(), "The experiment must be persistent." );
        Assert.notNull( qt.getId(), "The quantitation type must be persistent." );
        // FIXME: hashing depends on properties that might have been altered that would in turn affect hashCode(), so we
        //        cannot use contains
        Assert.isTrue( ee.getQuantitationTypes().stream().anyMatch( qt::equals ),
                "The quantitation type does not belong to " + ee + "." );

        Class<? extends DataVector> vectorType = quantitationTypeService.getDataVectorType( qt );

        if ( vectorType != null ) {
            if ( qt.isPreferred( vectorType ) ) {
                // set all other QTs to non-preferred (regardless of their type)
                for ( QuantitationType otherQt : ee.getQuantitationTypes() ) {
                    if ( otherQt.isPreferred( vectorType ) && !otherQt.equals( qt ) ) {
                        log.info( "Marking " + otherQt + " as non-preferred for " + vectorType + "." );
                        otherQt.setIsPreferred( false, vectorType );
                        quantitationTypeService.update( otherQt );
                    }
                }
                if ( !qt.equals( previousPreferredQt ) ) {
                    Class<? extends PreferredDataChangedEvent> eventType = getPreferredDataChangedEventForVectorType( vectorType );
                    String message = String.format( "The preferred quantitation type for %s changed%s to %s.",
                            vectorType.getSimpleName(), previousPreferredQt != null ? " from " + previousPreferredQt : "", qt );
                    if ( eventType != null ) {
                        auditTrailService.addUpdateEvent( ee, eventType, message );
                    } else {
                        log.warn( message + " There is no audit event type for this change." );
                    }
                }
            } else if ( previousPreferredQt != null && previousPreferredQt.isPreferred( vectorType ) && qt.equals( previousPreferredQt ) ) {
                Class<? extends PreferredDataChangedEvent> eventType = getPreferredDataChangedEventForVectorType( vectorType );
                String message = String.format( "The preferred quantitation type for %s was cleared (previously %s).",
                        vectorType.getSimpleName(), previousPreferredQt );
                if ( eventType != null ) {
                    auditTrailService.addUpdateEvent( ee, eventType, message );
                } else {
                    log.warn( message + " There is no audit event type for this change." );
                }
            }
        } else {
            log.warn( qt + " does not have a vector type, likely cause is the absence of data vectors." );
        }

        quantitationTypeService.update( qt );
    }

    @Nullable
    private Class<? extends PreferredDataChangedEvent> getPreferredDataChangedEventForVectorType( Class<? extends DataVector> vectorType ) {
        if ( SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            return PreferredSingleCellDataChangedEvent.class;
        } else if ( RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            return PreferredRawDataChangedEvent.class;
        } else {
            // there is no event for a change of processed data because we don't allow more than one set of processed
            return null;
        }
    }

    @Override
    @Transactional
    public MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        return expressionExperimentDao.updateMeanVarianceRelation( ee, mvr );
    }

    // ---------------------------------------------------------------------
    // Bucket F -- lifecycle (remove)
    // ---------------------------------------------------------------------

    /**
     * Deletes an experiment and all of its associated objects, including coexpression links. Some
     * types of associated objects may need to be deleted before this can be run (example: analyses
     * involving multiple experiments; these will not be deleted automatically).
     */
    @Override
    @Transactional
    public void remove( ExpressionExperiment ee ) {
        ee = ensureInSession( ee );

        if ( !securityService.isEditableByCurrentUser( ee ) ) {
            throw new SecurityException(
                    "Error performing 'ExpressionExperimentService.remove(ExpressionExperiment expressionExperiment)' --> "
                            + " You do not have permission to edit this experiment." );
        }

        // Remove subsets
        Collection<ExpressionExperimentSubSet> subsets = expressionExperimentDao.getSubSets( ee );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            expressionExperimentSubSetService.remove( subset );
        }

        // Remove differential expression analyses
        this.differentialExpressionAnalysisService.removeForExperiment( ee, true );

        // Remove any sample coexpression matrices
        this.sampleCoexpressionAnalysisService.removeForExperiment( ee );

        // Remove PCA
        this.principalComponentAnalysisService.removeForExperiment( ee );

        /*
         * Delete any expression experiment sets that only have this one ee in it. If possible remove this experiment
         * from other sets, and update them. IMPORTANT, this section assumes that we already checked for gene2gene
         * analyses!
         */
        this.expressionExperimentSetService.removeFromSets( ee );

        expressionExperimentDao.remove( ee );
    }

    @Override
    @Transactional
    public void remove( Collection<ExpressionExperiment> entities ) {
        entities.forEach( this::remove );
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
}
