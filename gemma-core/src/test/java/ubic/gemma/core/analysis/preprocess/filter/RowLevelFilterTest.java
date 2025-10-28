package ubic.gemma.core.analysis.preprocess.filter;

import org.junit.Before;
import org.junit.Test;
import ubic.basecode.math.Constants;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RowLevelFilterTest {

    private ExpressionDataDoubleMatrix matrix;

    @Before
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ExpressionExperiment ee = new ExpressionExperiment();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        for ( int i = 0; i < 8; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );
    }

    @Test
    public void testFilterWithLowCut() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );

        filter.setLowCut( 0.0 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 100, filteredMatrix.rows() );

        filter.setLowCut( 2.0 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 39, filteredMatrix.rows() );
    }

    @Test
    public void testFilterWithLowCutForDroppingZeros() {
        for ( int i = 0; i < 8; i++ ) {
            matrix.set( 0, i, 0.0 );
        }
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        filter.setLowCut( Constants.SMALLISH );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 99, filteredMatrix.rows() );
    }

    @Test
    public void testFilterWithHighCut() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        filter.setHighCut( 2.0 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 61, filteredMatrix.rows() );
    }


    @Test
    public void testFilterWithLowCutAndHighCut() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );

        filter.setLowCut( 2.0 );
        filter.setHighCut( 3.0 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 28, filteredMatrix.rows() );

        // if equal, we're basically looking for a single value
        filter.setLowCut( 2.0 );
        filter.setHighCut( 2.0 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );

        filter.setLowCut( 3.0 );
        filter.setHighCut( 2.0 );
        assertThrows( IllegalStateException.class, () -> filter.filter( matrix ) );

        filter.setLowCut( 3.0 );
        filter.setHighCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 1, filteredMatrix.rows() );

        // in this scenario, the high cut ends-up below the low cut, a warning is produced
        filter.setLowCut( 3.0 );
        filter.setHighCutAsFraction( 0.9 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );
    }

    @Test
    public void testFilterWithLowCutAsFraction() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );

        filter.setLowCutAsFraction( 0.0 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        // the low cut is exclusive, so the first row is always filtered out
        assertEquals( 100, filteredMatrix.rows() );

        filter.setLowCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 90, filteredMatrix.rows() );

        filter.setLowCutAsFraction( 1.0 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );
    }

    @Test
    public void testFilterWithHighCutAsFraction() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );

        filter.setHighCutAsFraction( 1.0 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );

        filter.setHighCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 90, filteredMatrix.rows() );
    }

    @Test
    public void testFilterWithLowCutAndHighCutAsFraction() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );

        filter.setLowCutAsFraction( 0.1 );
        filter.setHighCutAsFraction( 0.1 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 80, filteredMatrix.rows() );

        filter.setLowCutAsFraction( 0.5 );
        filter.setHighCutAsFraction( 0.5 );
        assertThrows( IllegalStateException.class, () -> filter.filter( matrix ) );

        filter.setLowCutAsFraction( 0.8 );
        filter.setHighCutAsFraction( 0.8 );
        assertThrows( IllegalStateException.class, () -> filter.filter( matrix ) );
    }

    @Test
    public void testFilterWithCustomValues() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.CUSTOM_METHOD );
        Map<CompositeSequence, Double> cv = new HashMap<>();
        for ( CompositeSequence cs : matrix.getDesignElements() ) {
            cv.put( cs, matrix.get( cs, matrix.getBioAssayForColumn( 0 ) ) );
        }
        filter.setCustomMethod( cv::get );
        filter.filter( matrix );
    }

    @Test
    public void testFilterWithMissingValues() {
        for ( int j = 0; j < 8; j++ ) {
            matrix.set( 0, j, Double.NaN );
        }

        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 99, filteredMatrix.rows() );

        // make sure that NaN is not accounted in the total number of values
        filter.setLowCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 89, filteredMatrix.rows() );

        filter.setLowCutAsFraction( 0.0 );
        filter.setHighCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 89, filteredMatrix.rows() );
    }

    @Test
    public void testFilterEmptyMatrix() {
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        filter.filter( matrix.sliceRows( Collections.emptyList() ) );
        filter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        filter.setLowCutAsFraction( 0.0 );
        filter.setHighCutAsFraction( 0.1 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix.sliceRows( Collections.emptyList() ) );
        assertEquals( 0, filteredMatrix.rows() );
    }

    @Test
    public void testFilterMatrixFilledWithNaNs() {
        for ( int i = 0; i < 100; i++ ) {
            for ( int j = 0; j < 8; j++ ) {
                matrix.set( i, j, Double.NaN );
            }
        }
        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.MEAN );
        filter.filter( matrix.sliceRows( Collections.emptyList() ) );
        filter = new RowLevelFilter( RowLevelFilter.Method.MEAN );
        filter.setLowCutAsFraction( 0.1 );
        filter.setHighCutAsFraction( 0.1 );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );
    }

    @Test
    public void testFilterMatrixFilledWithZeroes() {
        for ( int i = 0; i < 100; i++ ) {
            for ( int j = 0; j < 8; j++ ) {
                matrix.set( i, j, 0.0 );
            }
        }

        RowLevelFilter filter = new RowLevelFilter( RowLevelFilter.Method.MEAN );
        filter.setLowCut( Constants.SMALLISH );
        ExpressionDataDoubleMatrix filteredMatrix = filter.filter( matrix );
        assertEquals( 0, filteredMatrix.rows() );

        filter = new RowLevelFilter( RowLevelFilter.Method.MEAN );
        filter.setLowCut( 0.0 );
        filter.setHighCut( 0.0 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 100, filteredMatrix.rows() );

        // all the values are the same, so we retain everything
        filter = new RowLevelFilter( RowLevelFilter.Method.MEAN );
        filter.setLowCutAsFraction( 0.1 );
        filter.setHighCutAsFraction( 0.1 );
        filteredMatrix = filter.filter( matrix );
        assertEquals( 100, filteredMatrix.rows() );
    }
}