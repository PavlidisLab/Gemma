package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * A scoring run that throws must not be saved and audited as if it had finished: the GeeqEvent is what
 * {@code runGeeq -auto} uses to decide a dataset is done.
 */
public class GeeqScoringFailureTest {

    @Test
    public void testAFailedScoringRunThrowsAndRecordsNoGeeqEvent() {
        ExpressionExperimentService eeService = mock();
        GeeqAuditService geeqAuditService = mock();
        GeeqServiceImpl geeqService = new GeeqServiceImpl( mock( GeeqDao.class ), eeService,
                mock( ExpressionExperimentBatchInformationService.class ), mock( OutlierDetectionService.class ),
                geeqAuditService, mock( SampleCoexpressionAnalysisService.class ) );

        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE0001" );
        when( eeService.loadOrFail( 1L ) ).thenReturn( ee );
        when( eeService.getArrayDesignsUsed( ee ) ).thenThrow( new IllegalStateException( "scoring step failed" ) );

        assertThatThrownBy( () -> geeqService.calculateScore( ee, GeeqService.ScoreMode.all ) )
                .hasMessageContaining( "GSE0001" )
                .hasMessageContaining( "no score was saved" )
                .hasRootCauseMessage( "scoring step failed" );
        verify( geeqAuditService, never() ).recordGeeqScoring( any(), anyString(), anyString() );
    }
}
