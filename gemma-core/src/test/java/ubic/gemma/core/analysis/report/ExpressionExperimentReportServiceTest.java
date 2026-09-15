package ubic.gemma.core.analysis.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionExperimentReportServiceTest extends BaseSpringContextTest5 {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    private ExpressionExperiment ee;

    @AfterEach
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public void testRecalculateExperimentBatchInfoAsAdmin() {
        ee = getTestPersistentBasicExpressionExperiment();
        assertNotNull( ee.getExperimentalDesign() );
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            runAsAgent();
            expressionExperimentReportService.recalculateExperimentBatchInfoAsAdmin( ee );
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
            expressionExperimentReportService.recalculateExperimentBatchInfoAsAdmin( ee );
        } finally {
            SecurityContextHolder.setContext( previousContext );
        }
        ee = expressionExperimentService.thawLite( ee );
        assertEquals( BatchEffectType.NO_BATCH_INFO, ee.getBatchEffect() );
        assertNull( ee.getBatchConfound() );
    }
}