package ubic.gemma.core.analysis.preprocess.batcheffects;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDResult;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExperimentFactorUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@Service
@CommonsLog
public class ExpressionExperimentBatchInformationServiceImpl implements ExpressionExperimentBatchInformationService {

    private static final double BATCH_CONFOUND_THRESHOLD = 0.01;

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
    public boolean hasSignificantBatchConfound( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );

        if ( !this.checkHasUsableBatchInfo( ee ) ) {
            log.warn( ee + " has no usable batch information, cannot check for confound: " + ee );
            return false;
        }

        Collection<BatchConfound> confounds;
        try {
            confounds = BatchConfoundUtils.test( ee );
        } catch ( NotStrictlyPositiveException e ) {
            log.error( String.format( "Batch confound test for %s threw a NonStrictlyPositiveException! Returning null.", ee ), e );
            return false;
        }

        for ( BatchConfound c : confounds ) {
            if ( c.getPValue() < BATCH_CONFOUND_THRESHOLD ) {
                return true;
            }
        }

        // no need to check for subsets since there's no confound in the experiment itself

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchConfound> getSignificantBatchConfounds( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );

        if ( !this.checkHasUsableBatchInfo( ee ) ) {
            log.warn( ee + " has no usable batch information, cannot check for confounds." );
            return Collections.emptyList();
        }

        List<BatchConfound> significantConfounds = new ArrayList<>();
        try {
            List<BatchConfound> confounds;
            confounds = new ArrayList<>( BatchConfoundUtils.test( ee ) );
            // confounds have to be sorted in order to always get the same string
            confounds.sort( Comparator.comparing( BatchConfound::toString ) );
            for ( BatchConfound c : confounds ) {
                if ( c.getPValue() < BATCH_CONFOUND_THRESHOLD ) {
                    significantConfounds.add( c );
                }
            }
        } catch ( NotStrictlyPositiveException e ) {
            log.error( String.format( "Batch confound test for %s threw a NonStrictlyPositiveException! Returning null.", ee ), e );
            return null;
        }

        return significantConfounds;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentSubSet, List<BatchConfound>> getSignificantBatchConfoundsForSubsets( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thawLite( ee );

        if ( !this.checkHasUsableBatchInfo( ee ) ) {
            log.info( ee + " has no usable batch information, cannot check for confounds for subsets." );
            return Collections.emptyMap();
        }

        Map<ExpressionExperimentSubSet, List<BatchConfound>> significantSubsetConfounds = new HashMap<>();

        Collection<ExpressionExperimentSubSet> subSets = expressionExperimentService.getSubSetsWithBioAssays( ee );
        for ( ExpressionExperimentSubSet subset : subSets ) {
            try {
                List<BatchConfound> subsetConfounds = new ArrayList<>( BatchConfoundUtils.test( subset ) );
                // confounds have to be sorted in order to always get the same string
                subsetConfounds.sort( Comparator.comparing( BatchConfound::toString ) );
                for ( BatchConfound c : subsetConfounds ) {
                    if ( c.getPValue() < BATCH_CONFOUND_THRESHOLD ) {
                        significantSubsetConfounds
                                .computeIfAbsent( subset, k -> new ArrayList<>() )
                                .add( c );
                    }
                }
            } catch ( NotStrictlyPositiveException e ) {
                log.error( String.format( "Batch confound test for %s threw a NonStrictlyPositiveException, it will not be included in the batch confound summary of %s.", subset, ee ), e );
            }
        }

        return significantSubsetConfounds;
    }

    @Override
    @Transactional(readOnly = true)
    public String getBatchConfoundAsHtmlString( ExpressionExperiment ee ) {
        List<BatchConfound> confounds = getSignificantBatchConfounds( ee );

        if ( confounds.isEmpty() )
            return null;

        StringBuilder result = new StringBuilder();

        result.append( "One or more factors were confounded with batches in the full design; batch correction was not performed. "
                + "Analyses may not be affected if performed on non-confounded subsets. Factor(s) confounded were: " );
        result.append( confounds.stream()
                .map( c -> escapeHtml4( c.getFactor().getName() ) )
                .collect( Collectors.joining( ", " ) ) );

        Map<ExpressionExperimentSubSet, List<BatchConfound>> subsetConfoundss = getSignificantBatchConfoundsForSubsets( ee );
        for ( Map.Entry<ExpressionExperimentSubSet, List<BatchConfound>> subsetConfounds : subsetConfoundss.entrySet() ) {
            ExpressionExperimentSubSet subset = subsetConfounds.getKey();
            for ( BatchConfound c : subsetConfounds.getValue() ) {
                result.append( "<br/><br/>Confound still exists for " )
                        .append( escapeHtml4( c.getFactor().getName() ) )
                        .append( " in " )
                        .append( escapeHtml4( subset.toString() ) );
            }
        }

        return result.toString();
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
                .filter( ExperimentFactorUtils::isBatchFactor )
                .findFirst()
                .orElse( null );

        if ( ef == null ) {
            log.warn( String.format( "No suitable batch factor was found for %s to obtain batch effect statistics.", ee ) );
            return details;
        }

        SVDResult svd = svdService.getSvdFactorAnalysis( ee );
        if ( svd == null ) {
            log.warn( "SVD was null for " + ef + ", can't compute batch effect statistics." );
            return details;
        }

        // Use the "date run" information as a first pass to decide if there is a batch association.
        // This won't always be present.
        double minP = 1.0;
        if ( svd.getDatePVals() != null ) {
            for ( Integer component : svd.getDatePVals().keySet() ) {
                Double pVal = svd.getDatePVals().get( component );
                if ( pVal != null && pVal < minP ) {
                    details.setBatchEffectStatistics( pVal, component + 1, svd.getVariances()[component] );
                    minP = pVal;
                }
            }
        }

        // we can override the date-based p-value with the factor-based p-value if it is lower.
        // The reason to do this is it can be underpowered. The date-based one is more sensitive.
        for ( Integer component : svd.getFactorPVals().keySet() ) {
            Map<ExperimentalFactor, Double> cmpEffects = svd.getFactorPVals().get( component );

            // could use the effect size instead of the p-values (or in addition)
            //Map<Long, Double> cmpEffectSizes = svd.getFactorCorrelations().get( component );

            Double pVal = cmpEffects.get( ef );
            if ( pVal != null && pVal < minP ) {
                details.setBatchEffectStatistics( pVal, component + 1, svd.getVariances()[component] );
                minP = pVal;
            }

        }
        return details;
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
                if ( ExperimentFactorUtils.isBatchFactor( ef ) ) {
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
