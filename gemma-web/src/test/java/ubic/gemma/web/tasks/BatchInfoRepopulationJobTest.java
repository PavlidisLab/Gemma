package ubic.gemma.web.tasks;

import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseWebIntegrationTest;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchInfoRepopulationJobTest extends BaseWebIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private AuditEventService auditEventService;

    @Test
    public void test() throws JobExecutionException {
        JobExecutionContext context = mock();
        JobDataMap jdm = new JobDataMap();
        jdm.put( "securityContext", SecurityContextHolder.getContext() );
        jdm.put( "expressionExperimentService", expressionExperimentService );
        jdm.put( "expressionExperimentReportService", expressionExperimentReportService );
        jdm.put( "auditEventService", auditEventService );
        when( context.getMergedJobDataMap() ).thenReturn( jdm );
        when( context.getScheduler() ).thenReturn( mock() );
        BatchInfoRepopulationJob job = new BatchInfoRepopulationJob();
        job.execute( context );
    }

    @Test
    public void testWithPreviousFireTime() throws JobExecutionException {
        JobExecutionContext context = mock();
        JobDataMap jdm = new JobDataMap();
        jdm.put( "securityContext", SecurityContextHolder.getContext() );
        jdm.put( "expressionExperimentService", expressionExperimentService );
        jdm.put( "expressionExperimentReportService", expressionExperimentReportService );
        jdm.put( "auditEventService", auditEventService );
        when( context.getMergedJobDataMap() ).thenReturn( jdm );
        when( context.getScheduler() ).thenReturn( mock() );
        when( context.getPreviousFireTime() ).thenReturn( new Date() );
        BatchInfoRepopulationJob job = new BatchInfoRepopulationJob();
        job.execute( context );
    }
}