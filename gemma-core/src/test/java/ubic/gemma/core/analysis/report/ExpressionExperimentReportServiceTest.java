package ubic.gemma.core.analysis.report;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.junit.Assert.*;

public class ExpressionExperimentReportServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    private ExpressionExperiment ee;

    @After
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public void testRecalculateBatchInfo() {
        ee = getTestPersistentBasicExpressionExperiment();
        assertNull( ee.getBatchEffect() );
        assertNull( ee.getBatchConfound() );
        assertNull( ee.getCurationDetails().getLastUpdated() );
        expressionExperimentService.update( ee );
        ee = expressionExperimentService.thawLite( ee );
        assertNotNull( ee.getCurationDetails().getLastUpdated() );
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            runAsAgent();
            expressionExperimentReportService.recalculateBatchInfo();
        } finally {
            SecurityContextHolder.setContext( previousContext );
        }
        ee = expressionExperimentService.thawLite( ee );
        assertEquals( BatchEffectType.NO_BATCH_INFO, ee.getBatchEffect() );
        assertNull( ee.getBatchConfound() );
    }

    @Test
    public void testRecalculateExperimentBatchInfo() {
        ee = getTestPersistentBasicExpressionExperiment();
        assertNotNull( ee.getExperimentalDesign() );
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            runAsAgent();
            expressionExperimentReportService.recalculateExperimentBatchInfo( ee );
        } finally {
            SecurityContextHolder.setContext( previousContext );
        }
        ee = expressionExperimentService.thawLite( ee );
        assertEquals( BatchEffectType.NO_BATCH_INFO, ee.getBatchEffect() );
        assertNull( ee.getBatchConfound() );
    }

    @Test
    public void testRecalculateBatchInfoWithMissingDesign() {
        ee = getTestPersistentExpressionExperiment();
        assertNull( ee.getExperimentalDesign() );
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            runAsAgent();
            expressionExperimentReportService.recalculateExperimentBatchInfo( ee );
        } finally {
            SecurityContextHolder.setContext( previousContext );
        }
        ee = expressionExperimentService.thawLite( ee );
        assertEquals( BatchEffectType.NO_BATCH_INFO, ee.getBatchEffect() );
        assertNull( ee.getBatchConfound() );
    }
}