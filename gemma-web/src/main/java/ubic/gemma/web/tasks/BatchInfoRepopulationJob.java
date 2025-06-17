package ubic.gemma.web.tasks;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchProblemsUpdateEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.scheduler.SecureQuartzJobBean;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schedule job that populates batch information for all experiments that have been updated since the last run.
 * @author paul
 * @author poirigui
 */
@Setter
@CommonsLog
public class BatchInfoRepopulationJob extends SecureQuartzJobBean implements StatefulJob {

    private ExpressionExperimentService expressionExperimentService;
    private ExpressionExperimentReportService expressionExperimentReportService;
    private AuditEventService auditEventService;

    @Override
    protected void executeAs( JobExecutionContext context ) {
        Date since = context.getPreviousFireTime();

        log.info( String.format( "Started batch info recalculation task for %s.",
                since != null ? "datasets updated after " + since : "all datasets" ) );

        Collection<ExpressionExperiment> ees;
        if ( since != null ) {
            ees = this.expressionExperimentService.findUpdatedAfter( since );
        } else {
            ees = expressionExperimentService.loadAllReferences();
        }

        if ( ees.isEmpty() ) {
            log.info( "No batch info to recalculate, next execution is scheduled on " + context.getNextFireTime() + "." );
            return;
        }

        Exception lastException = null;
        Set<Long> failedEeIds = new HashSet<>();
        log.info( "Will be checking batch information of " + ees.size() + " experiments..." );
        for ( ExpressionExperiment ee : ees ) {
            Long id = ee.getId();
            // Don't update if the only recent event was another BatchProblemsUpdateEvent
            ee = expressionExperimentService.loadWithAuditTrail( id );
            if ( ee == null ) {
                log.warn( "ExpressionExperiment Id=" + id + " was not found in the database." );
                failedEeIds.add( id );
                continue;
            }
            AuditEvent lastEvent = auditEventService.getLastEvent( ee );
            if ( lastEvent != null && lastEvent.getEventType() instanceof BatchProblemsUpdateEvent ) {
                log.debug( "Ignoring " + ee + ", it already has a BatchProblemsUpdateEvent." );
                continue;
            }
            try {
                expressionExperimentReportService.recalculateExperimentBatchInfo( ee );
            } catch ( Exception e ) {
                log.warn( "Batch effect recalculation failed for " + ee, e );
                failedEeIds.add( ee.getId() );
                lastException = e;
            }
        }
        if ( !failedEeIds.isEmpty() ) {
            String eeIds = failedEeIds.stream().sorted()
                    .map( String::valueOf )
                    .collect( Collectors.joining( ", " ) );
            String message = String.format( "There were %d failures out of %d during the batch info recalculation: %s. Only the last exception stacktrace is appended.",
                    failedEeIds.size(), ees.size(), eeIds );
            // report as an error of at least 5% fails
            if ( ( double ) failedEeIds.size() / ( double ) ees.size() > 0.05 ) {
                log.error( message, lastException );
            } else {
                log.warn( message, lastException );
            }
        }
        log.info( "Finished batch info recalculation task, next execution is scheduled on " + context.getNextFireTime() + "." );
    }
}
