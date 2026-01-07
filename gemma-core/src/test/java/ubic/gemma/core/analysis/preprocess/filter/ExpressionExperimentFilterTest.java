package ubic.gemma.core.analysis.preprocess.filter;

import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author poirigui
 */
public class ExpressionExperimentFilterTest {

    @Test
    public void testFilterSingleCellMatrix() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        Integer[] numberOfCells = { 110, 100, 10, 5, 33, 99, 200, 22, 110, 100, 10, 5, 33, 99, 200, 22, 1, 3, 5, 1 };
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 20; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            ba.setNumberOfCells( numberOfCells[i] );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        ExpressionExperimentFilterConfig config = new ExpressionExperimentFilterConfig();

        // default is too stringent
        config.setLowVarianceCut( 0.5 );

        ExpressionExperimentFilter filter = new ExpressionExperimentFilter( config );
        assertThat( filter ).hasToString( "ExpressionExperimentFilter Config=%s", config );
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2cpmMatrix( ee );
        ExpressionExperimentFilterResult filterResult = new ExpressionExperimentFilterResult();
        filter.filter( dataMatrix, Collections.singleton( ad ), filterResult );

        assertThat( filterResult.getStartingRows() ).isEqualTo( 100 );

        assertThat( filterResult.isAffyControlsFilterApplied() ).isFalse();
        assertThat( filterResult.getAfterAffyControlsFilter() ).isEqualTo( 100 );

        assertThat( filterResult.isNoSequencesFilterApplied() ).isFalse();
        assertThat( filterResult.getAfterNoSequencesFilter() ).isEqualTo( 100 );

        assertThat( filterResult.isMinPresentFilterApplied() ).isTrue();
        assertThat( filterResult.getAfterMinPresentFilter() ).isEqualTo( 100 );

        assertThat( filterResult.isZeroVarianceFilterApplied() ).isTrue();
        assertThat( filterResult.getAfterZeroVarianceFilter() ).isEqualTo( 100 );

        assertThat( filterResult.isLowExpressionFilterApplied() ).isTrue();
        assertThat( filterResult.getAfterLowExpressionFilter() ).isEqualTo( 81 );

        assertThat( filterResult.isLowVarianceFilterApplied() ).isTrue();
        assertThat( filterResult.getAfterLowVarianceFilter() ).isEqualTo( 61 );

        assertThat( filterResult.getFinalRows() ).isEqualTo( 61 );
    }

    @Test
    public void testFilterMatrixWithOutliers() throws FilteringException {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 25; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            if ( i == 5 ) {
                ba.setIsOutlier( true );
            }
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        ExpressionDataDoubleMatrix dataMatrix = RandomExpressionDataMatrixUtils.randomLog2cpmMatrix( ee );
        ExpressionExperimentFilterConfig config = new ExpressionExperimentFilterConfig();
        config.setMaskOutliers( true );
        config.setLowVarianceCut( 0.5 );
        ExpressionExperimentFilterResult result = new ExpressionExperimentFilterResult();
        ExpressionDataDoubleMatrix filteredMatrix = new ExpressionExperimentFilter( config ).filter( dataMatrix, result );
        assertThat( filteredMatrix.columns() ).isEqualTo( 25 );
        assertThat( filteredMatrix.rows() ).isEqualTo( 65 );
        assertThat( ExpressionDataFilterUtils.countSamplesWithData( filteredMatrix ) ).isEqualTo( 24 );
        assertThat( result.getColumnsAfterOutliersFilter() ).isEqualTo( 24 );
        assertThat( result.getFinalColumns() ).isEqualTo( 24 );
    }
}