package ubic.gemma.core.analysis.preprocess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.preprocess.detect.InferredQuantitationMismatchException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private GeeqService geeqService;
    @Mock
    private PreprocessorHelperService preprocessorHelperService;
    @Mock
    private ExpressionDataFileService dataFileService;

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

    /**
     * A GEEQ scoring failure reaches the caller as a {@link PreprocessingException}, so that
     * {@code process(..., ignoreDiagnosticsFailure)} handles it like the other diagnostics.
     */
    @Test
    void processDiagnosticsReportsAGeeqFailureAsAPreprocessingException() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE100010" );
        when( geeqService.calculateScore( ee, GeeqService.ScoreMode.all ) )
                .thenThrow( new RuntimeException( "GEEQ scoring (mode: all) did not finish for GSE100010; no score was saved." ) );

        assertThatThrownBy( () -> preprocessorService.processDiagnostics( ee ) )
                .isInstanceOf( PreprocessingException.class )
                .hasMessageContaining( "GEEQ scoring failed" );
    }

    /**
     * The default {@code process(ee)} -- what loading and raw-data replacement call -- must refuse a quantitation type
     * the data contradicts. It used to ignore the mismatch, so GSE38485 (already-log2 Lumi vst values typed LINEAR)
     * was logged a second time at load.
     */
    @Test
    void processByDefaultRefusesAQuantitationTypeTheDataContradicts() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "GSE38485" );
        when( expressionExperimentService.getRawDataVectorCount( any() ) ).thenReturn( 1L );
        doThrow( new InferredQuantitationMismatchException( new QuantitationType(), new QuantitationType(),
                "The scale LINEAR differs from the one inferred from data: LOG2." ) )
                .when( processedExpressionDataVectorService ).createProcessedDataVectors( ee, true, false );

        assertThatThrownBy( () -> preprocessorService.process( ee ) )
                .isInstanceOf( QuantitationTypeDetectionRelatedPreprocessingException.class );
        verify( processedExpressionDataVectorService ).createProcessedDataVectors( eq( ee ), eq( true ), eq( false ) );
    }
}
