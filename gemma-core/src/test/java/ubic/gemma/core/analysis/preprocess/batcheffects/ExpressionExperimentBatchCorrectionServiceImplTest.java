package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.experiment.RandomExpressionExperimentUtils.randomExpressionExperiment;

/**
 * Unit tests for {@link ExpressionExperimentBatchCorrectionServiceImpl} that need no Spring context: the
 * {@code comBat(ee, matrix)} path only reads the experiment's in-memory design, so the impl can be constructed
 * directly.
 *
 * @author paul
 */
public class ExpressionExperimentBatchCorrectionServiceImplTest {

    private static final int NUM_ASSAYS = 8;
    private static final int NUM_PROBES = 100;

    private final ExpressionExperimentBatchCorrectionServiceImpl service = new ExpressionExperimentBatchCorrectionServiceImpl();

    @BeforeEach
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
    }

    @Test
    public void testComBat() {
        ExpressionExperiment ee = experimentWithTwoBatches();
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );

        ExpressionDataDoubleMatrix corrected = service.comBat( ee, matrix );

        assertThat( corrected ).isNotNull();
        assertThat( corrected.columns() ).isEqualTo( NUM_ASSAYS );
        assertThat( corrected.getQuantitationType().getIsBatchCorrected() ).isTrue();
    }

    /**
     * Outlier samples are sliced out before ComBat runs and their original values put back afterwards. The restored
     * matrix still has to carry ComBat's quantitation type: {@code PreprocessorHelperServiceImpl.getCorrectedData}
     * checks {@code isBatchCorrected} on the way out and throws if it is unset, so returning the original matrix
     * as-is fails every experiment that has an outlier.
     */
    @Test
    public void testComBatWithAnOutlier() {
        ExpressionExperiment ee = experimentWithTwoBatches();
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );
        BioAssay outlier = new ArrayList<>( ee.getBioAssays() ).get( 0 );
        outlier.setIsOutlier( true );

        ExpressionDataDoubleMatrix corrected = service.comBat( ee, matrix );

        assertThat( corrected ).isNotNull();
        // the outlier is restored, so no column is lost
        assertThat( corrected.columns() ).isEqualTo( NUM_ASSAYS );
        assertThat( corrected.getQuantitationType().getIsBatchCorrected() ).isTrue();
    }

    /**
     * An experiment with a two-value batch factor, samples split evenly between the batches.
     */
    private ExpressionExperiment experimentWithTwoBatches() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad ) );
        }

        ExpressionExperiment ee = randomExpressionExperiment( taxon, NUM_ASSAYS, ad );

        ExperimentalFactor batchFactor = ExperimentalFactor.Factory
                .newInstance( "batch", FactorType.CATEGORICAL, Categories.BLOCK );
        batchFactor.setId( 1L );
        // DiffExAnalyzerUtils.nameForR() requires IDs to name the design matrix columns
        FactorValue batch1 = FactorValue.Factory.newInstance( batchFactor );
        batch1.setId( 1L );
        batch1.setValue( "batch 1" );
        FactorValue batch2 = FactorValue.Factory.newInstance( batchFactor );
        batch2.setId( 2L );
        batch2.setValue( "batch 2" );
        batchFactor.getFactorValues().add( batch1 );
        batchFactor.getFactorValues().add( batch2 );
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.getExperimentalDesign().getExperimentalFactors().add( batchFactor );

        List<BioAssay> assays = new ArrayList<>( ee.getBioAssays() );
        for ( int i = 0; i < assays.size(); i++ ) {
            BioMaterial bm = assays.get( i ).getSampleUsed();
            bm.getFactorValues().add( i < assays.size() / 2 ? batch1 : batch2 );
        }

        return ee;
    }
}
