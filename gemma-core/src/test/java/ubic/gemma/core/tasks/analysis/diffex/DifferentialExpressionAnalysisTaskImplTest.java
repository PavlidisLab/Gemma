package ubic.gemma.core.tasks.analysis.diffex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Removing an analysis has to take its on-disk artifacts with it, whichever route asks for the removal.
 *
 * <p>{@code DifferentialExpressionAnalysisService.remove} drops the rows only; {@code deleteAnalysis} drops them and
 * then the diffex archive and the per-result-set TSV caches. This task backs
 * {@code DELETE /datasets/{id}/tasks/differential/{analysisId}}, the second route into the same removal.</p>
 *
 * @author paul
 */
public class DifferentialExpressionAnalysisTaskImplTest {

    private DifferentialExpressionAnalysisTaskImpl task;
    private DifferentialExpressionAnalyzerService analyzerService;
    private DifferentialExpressionAnalysisService analysisService;
    private ExpressionExperiment ee;
    private DifferentialExpressionAnalysis toRemove;

    @BeforeEach
    public void setUp() {
        task = new DifferentialExpressionAnalysisTaskImpl();
        analyzerService = mock( DifferentialExpressionAnalyzerService.class );
        analysisService = mock( DifferentialExpressionAnalysisService.class );
        ReflectionTestUtils.setField( task, "differentialExpressionAnalyzerService", analyzerService );
        ReflectionTestUtils.setField( task, "differentialExpressionAnalysisService", analysisService );

        ee = new ExpressionExperiment();
        ee.setId( 42L );
        toRemove = DifferentialExpressionAnalysis.Factory.newInstance();
        toRemove.setId( 7L );
    }

    /**
     * The rule: the removal goes through deleteAnalysis, with this experiment and this analysis, and the row-only
     * remove is not called at all.
     */
    @Test
    public void testRemovingAnAnalysisTakesItsFilesWithIt() {
        task.setTaskCommand( new DifferentialExpressionAnalysisRemoveTaskCommand( ee, toRemove ) );

        TaskResult result = task.call();

        assertThat( result ).isNotNull();
        // the exact experiment and the exact analysis, so a call that merely happened cannot pass
        verify( analyzerService ).deleteAnalysis( ee, toRemove );
        verify( analysisService, never() ).remove( any( DifferentialExpressionAnalysis.class ) );
        verifyNoMoreInteractions( analyzerService );
    }

    /**
     * Positive control off the same wiring: the guard above still rejects a command with nothing to remove, so a
     * task that quietly did nothing at all cannot pass the test above.
     */
    @Test
    public void testACommandWithNoAnalysisIsRefused() {
        task.setTaskCommand( new DifferentialExpressionAnalysisRemoveTaskCommand( ee, null ) );

        assertThat( catchIllegalArgument() ).isNotNull();
        verifyNoInteractions( analyzerService );
        verifyNoInteractions( analysisService );
    }

    private IllegalArgumentException catchIllegalArgument() {
        try {
            task.call();
            return null;
        } catch ( IllegalArgumentException e ) {
            return e;
        }
    }
}
