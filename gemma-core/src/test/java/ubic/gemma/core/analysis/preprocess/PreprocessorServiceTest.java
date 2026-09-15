package ubic.gemma.core.analysis.preprocess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PreprocessorServiceImpl} that do not need a Spring context.
 *
 * @author gemma
 */
@ExtendWith(MockitoExtension.class)
class PreprocessorServiceTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @InjectMocks
    private PreprocessorServiceImpl preprocessorService;

    /**
     * A dataset with no raw expression data vectors (e.g. an RNA-seq GEO series whose data is not in
     * the SOFT/series matrix) must skip post-processing gracefully rather than throwing
     * "No preferred data vectors".
     */
    @Test
    void processSkipsPostProcessingWhenNoRawData() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE100009" );
        when( expressionExperimentService.getRawDataVectorCount( any() ) ).thenReturn( 0L );

        preprocessorService.process( ee );

        verify( processedExpressionDataVectorService, never() )
                .createProcessedDataVectors( any(), anyBoolean(), anyBoolean() );
    }
}
