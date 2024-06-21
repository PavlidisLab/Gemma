package ubic.gemma.core.analysis.preprocess.batcheffects;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

@Service
@CommonsLog
public class ExpressionExperimentBatchInformationServiceImpl implements ExpressionExperimentBatchInformationService {

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;
    private static final double BATCH_EFFECT_THRESHOLD = 0.01;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private AuditEventService auditEventService;

    @Override
    @Transactional(readOnly = true)
    public boolean checkHasBatchInfo( ExpressionExperiment ee ) {
        if ( hasBatchFactor( ee ) ) {
            return true;
        }

        AuditEvent lastBatchInfoEvent = this.auditEventService.getLastEvent( ee, BatchInformationEvent.class );

        if ( lastBatchInfoEvent == null )
            return false;

        // prior to 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b, cases of missing batch information was incorrectly typed
        // see https://github.com/PavlidisLab/Gemma/issues/1155 for details
        if ( lastBatchInfoEvent.getEventType() instanceof FailedBatchInformationFetchingEvent
                && lastBatchInfoEvent.getNote() != null && lastBatchInfoEvent.getNote().contains( "No header file for" ) ) {
            return false;
        }

        return lastBatchInfoEvent.getEventType() instanceof BatchInformationFetchingEvent;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkHasUsableBatchInfo( ExpressionExperiment ee ) {
        if ( hasBatchFactor( ee ) ) {
            return true;
        }

        AuditEvent lastBatchInfoEvent = this.auditEventService.getLastEvent( ee, BatchInformationEvent.class );

        if ( lastBatchInfoEvent == null )
            return false;

        return lastBatchInfoEvent.getEventType() instanceof BatchInformationFetchingEvent
                && !( lastBatchInfoEvent.getEventType() instanceof FailedBatchInformationFetchingEvent );
    }

    @Override
    @Transactional(readOnly = true)
    public String getBatchConfound( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawBioAssays( ee );

        if ( !this.checkHasUsableBatchInfo( ee ) ) {
            log.info( "Experiment has no usable batch information, cannot check for confound: " + ee );
            return null;
        }

        Collection<BatchConfound> confounds;
        try {
            confounds = BatchConfoundUtils.test( ee );
        } catch ( NotStrictlyPositiveException e ) {
            log.error( String.format( "Batch confound test for %s threw a NonStrictlyPositiveException! Returning null.", ee ), e );
            return null;
        }

        StringBuilder result = new StringBuilder();
        // Confounds have to be sorted in order to always get the same string
        List<BatchConfound> listConfounds = new ArrayList<>( confounds );
        listConfounds.sort( Comparator.comparing( BatchConfound::toString ) );

        for ( BatchConfound c : listConfounds ) {
            if ( c.getP() < BATCH_CONFOUND_THRESHOLD ) {
                String factorName = c.getEf().getName();
                if ( result.toString().isEmpty() ) {
                    result.append(
                            "One or more factors were confounded with batches in the full design; batch correction was not performed. "
                                    + "Analyses may not be affected if performed on non-confounded subsets. Factor(s) confounded were: " );
                } else {
                    result.append( ", " );
                }
                result.append( factorName );
            }
        }

        // Now check subsets, if relevant.
        if ( !listConfounds.isEmpty() && gemma.gsec.util.SecurityUtil.isUserAdmin() ) {
            Collection<ExpressionExperimentSubSet> subSets = expressionExperimentService.getSubSets( ee );
            if ( !subSets.isEmpty() ) {
                for ( ExpressionExperimentSubSet subset : subSets ) {
                    try {
                        confounds = BatchConfoundUtils.test( subset );
                        for ( BatchConfound c : confounds ) {
                            if ( c.getP() < BATCH_CONFOUND_THRESHOLD ) {
                                result.append( "<br/><br/>Confound still exists for " + c.getEf().getName() + " in " + subset );
                            }
                        }
                    } catch ( NotStrictlyPositiveException e ) {

                    }
                }
            }
        }

        return StringUtils.stripToNull( result.toString() );
    }

    @Override
    @Transactional(readOnly = true)
    public BatchEffectDetails getBatchEffectDetails( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLiter( ee );

        BatchEffectDetails details = new BatchEffectDetails( this.checkBatchFetchStatus( ee ),
                this.hasBeenBatchCorrected( ee ), this.checkIfSingleBatch( ee ) );

        // if missing or failed, we can't compute a P-value
        if ( !details.hasBatchInformation() || details.hasProblematicBatchInformation() ) {
            return details;
        }

        // we can't compute a P-value for a single batch
        if ( details.isSingleBatch() ) {
            return details;
        }

        if ( ee.getExperimentalDesign() == null ) {
            log.warn( ee + " have batch information, but it does not have an experimental design to determine the batch effect." );
            return details;
        }

        ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors()
                .stream()
                .filter( BatchInfoPopulationServiceImpl::isBatchFactor )
                .findFirst()
                .orElse( null );

        if ( ef == null ) {
            log.warn( String.format( "No suitable batch factor was found for %s to obtain batch effect statistics.", ee ) );
            return details;
        }

        SVDValueObject svd = svdService.getSvdFactorAnalysis( ee.getId() );
        if ( svd == null ) {
            log.warn( "SVD was null for " + ef + ", can't compute batch effect statistics." );
            return details;
        }

        // Use the "date run" information as a first pass to decide if there is a batch association.
        // This won't always be present.
        double minP = 1.0;
        if ( svd.getDatePvals() != null ) {
            for ( Integer component : svd.getDatePvals().keySet() ) {
                Double pVal = svd.getDatePvals().get( component );
                if ( pVal != null && pVal < minP ) {
                    details.setBatchEffectStatistics( pVal, component + 1, svd.getVariances()[component] );
                    minP = pVal;
                }
            }
        }

        // we can override the date-based p-value with the factor-based p-value if it is lower.
        // The reason to do this is it can be underpowered. The date-based one is more sensitive.
        for ( Integer component : svd.getFactorPvals().keySet() ) {
            Map<Long, Double> cmpEffects = svd.getFactorPvals().get( component );

            // could use the effect size instead of the p-values (or in addition)
            //Map<Long, Double> cmpEffectSizes = svd.getFactorCorrelations().get( component );

            Double pVal = cmpEffects.get( ef.getId() );
            if ( pVal != null && pVal < minP ) {
                details.setBatchEffectStatistics( pVal, component + 1, svd.getVariances()[component] );
                minP = pVal;
            }

        }
        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchEffectType getBatchEffect( ExpressionExperiment ee ) {
        BatchEffectDetails beDetails = this.getBatchEffectDetails( ee );
        BatchEffectDetails.BatchEffectStatistics batchEffectStatistics = beDetails.getBatchEffectStatistics();

        if ( beDetails.hasSingletonBatches() ) {
            return BatchEffectType.SINGLETON_BATCHES_FAILURE;
        } else if ( beDetails.hasUninformativeBatchInformation() ) {
            return BatchEffectType.UNINFORMATIVE_HEADERS_FAILURE;
        } else if ( !beDetails.hasBatchInformation() ) {
            return BatchEffectType.NO_BATCH_INFO;
        } else if ( beDetails.hasProblematicBatchInformation() ) {
            return BatchEffectType.PROBLEMATIC_BATCH_INFO_FAILURE;
        } else if ( beDetails.isSingleBatch() ) {
            return BatchEffectType.SINGLE_BATCH_SUCCESS;
        } else if ( beDetails.dataWasBatchCorrected() ) {
            // Checked for in ExpressionExperimentDetails.js::renderStatus()
            return BatchEffectType.BATCH_CORRECTED_SUCCESS;
        } else {
            if ( batchEffectStatistics == null ) {
                return BatchEffectType.BATCH_EFFECT_UNDETERMINED_FAILURE;
            } else if ( batchEffectStatistics.getPvalue() < BATCH_EFFECT_THRESHOLD ) {
                // this means there was a batch effect but we couldn't correct it
                return BatchEffectType.BATCH_EFFECT_FAILURE;
            } else {
                return BatchEffectType.NO_BATCH_EFFECT_SUCCESS;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getBatchEffectStatistics( ExpressionExperiment ee ) {
        BatchEffectDetails beDetails = this.getBatchEffectDetails( ee );
        if ( beDetails.getBatchEffectStatistics() != null ) {
            return String.format( "This data set may have a batch artifact (PC %d), p=%.5g",
                    beDetails.getBatchEffectStatistics().getComponent(),
                    beDetails.getBatchEffectStatistics().getPvalue() );
        }
        return null;
    }

    private boolean checkIfSingleBatch( ExpressionExperiment ee ) {
        AuditEvent ev = this.auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( ev == null ) return false;

        if ( ev.getEventType() instanceof SingleBatchDeterminationEvent ) {
            return true;
        }

        // address cases that were run prior to having the SingleBatchDeterminationEvent type.
        if ( ev.getNote() != null && ( ev.getNote().startsWith( "1 batch" ) || ev.getNote().startsWith( "AffyScanDateExtractor; 0 batches" ) ) ) {
            return true;
        }

        return false;
    }

    /**
     * Retrieve a batch information event that summarizes the state of batch information.
     */
    private BatchInformationEvent checkBatchFetchStatus( ExpressionExperiment ee ) {
        if ( hasBatchFactor( ee ) ) {
            return new BatchInformationFetchingEvent();
        }

        AuditEvent ev = auditEventService.getLastEvent( ee, BatchInformationEvent.class );

        if ( ev == null )
            return null;

        // prior to 23f7dcdbcbbf7b137c74abf2b6df96134bddc88b, cases of missing batch information was incorrectly typed
        // see https://github.com/PavlidisLab/Gemma/issues/1155 for details
        if ( ev.getEventType() instanceof FailedBatchInformationFetchingEvent
                && ev.getNote() != null && ev.getNote().contains( "No header file for" ) ) {
            return new BatchInformationMissingEvent();
        }

        return ( BatchInformationEvent ) ev.getEventType();
    }

    private boolean hasBatchFactor( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLiter( ee );
        if ( ee.getExperimentalDesign() != null ) {
            for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
                if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasBeenBatchCorrected( ExpressionExperiment ee ) {
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getIsBatchCorrected() ) {
                return true;
            }
        }
        return false;
    }
}
