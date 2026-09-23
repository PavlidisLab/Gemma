package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The DEA archive writer hands {@link ExpressionExperimentBatchInformationService#hasSignificantBatchConfound} the
 * analysed subset as loaded in an earlier transaction. Every subset analysis's archive failed on gemma2 with
 * "failed to lazily initialize a collection of role: ExpressionExperimentSubSet.bioAssays".
 * <p>
 * No ambient transaction here, on purpose: a transactional test would keep the subset attached and hide the failure.
 */
class ExpressionExperimentBatchInformationServiceSubsetTest extends BaseSpringContextTest5 {

    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Test
    void confoundTestAcceptsADetachedSubset() {
        ExpressionExperiment ee = testHelper.getTestPersistentBasicExpressionExperiment();
        // usable batch information, so the test reaches the subset's assays
        auditTrailService.addUpdateEvent( ee, BatchInformationFetchingEvent.class, "test batch information" );
        ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance( "subset", ee );
        subset.getBioAssays().addAll( ee.getBioAssays() );
        subset = expressionExperimentSubSetService.create( subset );

        ExpressionExperimentSubSet detached = expressionExperimentSubSetService.load( subset.getId() );
        assertFalse( Hibernate.isInitialized( detached.getBioAssays() ) );

        // the fixture has no batch factor, so there is no confound; the point is that this does not throw
        assertFalse( expressionExperimentBatchInformationService.hasSignificantBatchConfound( detached ) );
    }
}
