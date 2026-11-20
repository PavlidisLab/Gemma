package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.Collections;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A redo reads the design of the analysed experiment to recover blocking factors from the protocol, and the analyzer
 * service runs without a transaction ({@code Propagation.NEVER}), so whatever it reads must already be loaded by
 * {@link DifferentialExpressionAnalysisService#thaw(DifferentialExpressionAnalysis)}.
 * <p>
 * The design was not, and every redo of an analysis with a protocol description (all of them) failed with a
 * {@code LazyInitializationException}. This is not {@code @Transactional}, so the analysis is read back detached, as
 * it is in a real redo.
 */
public class DifferentialExpressionAnalysisRedoSessionTest extends BaseSpringContextTest5 {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    private ExpressionExperiment ee;
    private ExperimentalFactor genotype;

    @BeforeEach
    public void setUp() {
        ee = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        Iterator<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors().iterator();
        genotype = factors.next();
    }

    @Test
    public void testExtendConfigReadsTheDesignOfADetachedAnalysis() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( Collections.singletonList( genotype ) );
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        analysis.setProtocol( DiffExAnalyzerUtils.createProtocolForConfig( config, Collections.emptyMap() ) );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( analysis );
        resultSet.getExperimentalFactors().add( genotype );
        analysis.getResultSets().add( resultSet );
        analysis.setExperimentAnalyzed( ee );
        Long id = differentialExpressionAnalyzerService.persistAnalysis( ee, analysis, config ).getId();

        DifferentialExpressionAnalysis detached = differentialExpressionAnalysisService.thaw( differentialExpressionAnalysisService.load( id ) );
        Object target = AopTestUtils.getUltimateTargetObject( differentialExpressionAnalyzerService );
        assertThatCode( () -> ReflectionTestUtils.invokeMethod( target, "extendConfig", new DifferentialExpressionAnalysisConfig(), detached ) )
                .doesNotThrowAnyException();
    }
}
