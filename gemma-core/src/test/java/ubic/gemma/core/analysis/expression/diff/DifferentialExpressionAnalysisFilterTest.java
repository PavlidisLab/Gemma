package ubic.gemma.core.analysis.expression.diff;

import org.junit.Test;
import ubic.gemma.core.analysis.preprocess.filter.*;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author poirigui
 */
public class DifferentialExpressionAnalysisFilterTest {

    @Test
    public void test() {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysisFilter filter = new DifferentialExpressionAnalysisFilter( config );
        assertThat( filter ).hasToString( "DifferentialExpressionAnalysisFilter [%s] -> [%s] -> [%s] -> [%s]",
                new OutliersFilter(),
                new MinimumCellsFilter( 100 ),
                new RepetitiveValuesFilter(),
                new LowVarianceFilter( 1e-2 ) );
    }

    @Test
    public void testFilterSingleCellMatrix() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        Integer[] numberOfCells = { 110, 100, 10, 5, 33, 99, 200, 22 };
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            ba.setNumberOfCells( numberOfCells[i] );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMinimumVariance( 0.1 );
        DifferentialExpressionAnalysisFilter filter = new DifferentialExpressionAnalysisFilter( config );
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2cpmMatrix( ee );
        DifferentialExpressionAnalysisFilterResult filterResult = new DifferentialExpressionAnalysisFilterResult();
        filter.filter( dataMatrix, filterResult );
        assertThat( filterResult.isMinimumCellsFilterApplied() ).isTrue();
        assertThat( filterResult.getStartingSamples() )
                .hasSize( 8 );
        assertThat( filterResult.getSamplesAfterMinimumCells() ).hasSize( 3 )
                .extracting( BioMaterial::getName )
                .containsExactlyInAnyOrder( "bm0", "bm1", "bm6" );
        assertThat( filterResult.getFinalSamples() ).hasSize( 3 )
                .extracting( BioMaterial::getName )
                .containsExactlyInAnyOrder( "bm0", "bm1", "bm6" );

        assertThat( filterResult.isRepetitiveValuesFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterRepetitiveValues() ).isEqualTo( 100 );

        assertThat( filterResult.isLowVarianceFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterLowVariance() ).isEqualTo( 77 );

        assertThat( filterResult.getFinalSamples() ).hasSize( 3 );
        assertThat( filterResult.getFinalDesignElements() ).isEqualTo( 77 );
    }

    @Test
    public void testFilterBulkData() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMinimumVariance( 0.1 );

        DifferentialExpressionAnalysisFilter filter = new DifferentialExpressionAnalysisFilter( config );
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2cpmMatrix( ee );
        DifferentialExpressionAnalysisFilterResult filterResult = new DifferentialExpressionAnalysisFilterResult();
        filter.filter( dataMatrix, filterResult );
        assertThat( filterResult.getStartingSamples() )
                .hasSize( 8 );

        assertThat( filterResult.isMinimumCellsFilterApplied() ).isFalse();
        assertThat( filterResult.getSamplesAfterMinimumCells() ).hasSize( 8 );

        assertThat( filterResult.isRepetitiveValuesFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterRepetitiveValues() ).isEqualTo( 100 );

        assertThat( filterResult.isLowVarianceFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterLowVariance() ).isEqualTo( 99 );

        assertThat( filterResult.getFinalSamples() ).hasSize( 8 );
        assertThat( filterResult.getFinalDesignElements() ).isEqualTo( 99 );
    }

    @Test
    public void testFilterMicroarrayData() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.TWOCOLOR );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMinimumVariance( 0.1 );

        DifferentialExpressionAnalysisFilter filter = new DifferentialExpressionAnalysisFilter( config );
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2RatiometricMatrix( ee );
        DifferentialExpressionAnalysisFilterResult filterResult = new DifferentialExpressionAnalysisFilterResult();
        filter.filter( dataMatrix, filterResult );
        assertThat( filterResult.getStartingSamples() )
                .hasSize( 8 );

        assertThat( filterResult.isMinimumCellsFilterApplied() ).isFalse();
        assertThat( filterResult.getSamplesAfterMinimumCells() ).hasSize( 8 );

        assertThat( filterResult.isRepetitiveValuesFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterRepetitiveValues() ).isEqualTo( 100 );

        assertThat( filterResult.isLowVarianceFilterApplied() ).isFalse();
        assertThat( filterResult.getDesignElementsAfterLowVariance() ).isEqualTo( 100 );

        assertThat( filterResult.getFinalSamples() ).hasSize( 8 );
        assertThat( filterResult.getFinalDesignElements() ).isEqualTo( 100 );
    }

    @Test
    public void testFilterDataWithOutliers() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            if ( i == 5 ) {
                ba.setIsOutlier( true );
            }
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMinimumVariance( 0.1 );

        DifferentialExpressionAnalysisFilter filter = new DifferentialExpressionAnalysisFilter( config );
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2cpmMatrix( ee );
        DifferentialExpressionAnalysisFilterResult filterResult = new DifferentialExpressionAnalysisFilterResult();
        filter.filter( dataMatrix, filterResult );
        assertThat( filterResult.getStartingSamples() )
                .hasSize( 8 );

        assertThat( filterResult.isOutliersFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterOutliers() ).isEqualTo( 100 );
        assertThat( filterResult.getSamplesAfterOutliers() ).hasSize( 7 );

        assertThat( filterResult.isMinimumCellsFilterApplied() ).isFalse();
        assertThat( filterResult.getSamplesAfterMinimumCells() ).hasSize( 7 );

        assertThat( filterResult.isRepetitiveValuesFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterRepetitiveValues() ).isEqualTo( 100 );

        assertThat( filterResult.isLowVarianceFilterApplied() ).isTrue();
        assertThat( filterResult.getDesignElementsAfterLowVariance() ).isEqualTo( 98 );

        assertThat( filterResult.getFinalSamples() ).hasSize( 7 );
        assertThat( filterResult.getFinalDesignElements() ).isEqualTo( 98 );
    }
}