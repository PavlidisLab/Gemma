package ubic.gemma.core.analysis.preprocess.svd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A pseudobulk sample of a cell type with no cells has no data at all. Enough of them leave every row short of the
 * present fraction the SVD requires, so PCA failed for these datasets; now they are left out of the SVD and get NaN in
 * the stored components, which stay aligned with every sample of the dimension.
 */
public class SVDEmptySamplesTest {

    private static final int GENES = 20, SAMPLES = 6;
    private static final int[] EMPTY = { 1, 4 };

    private ExpressionDataDoubleMatrix mat;
    private List<BioMaterial> samples;

    @BeforeEach
    public void setUp() {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 1L );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        samples = new ArrayList<>();
        for ( int j = 0; j < SAMPLES; j++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "s" + j );
            bm.setId( ( long ) j + 1 );
            BioAssay ba = BioAssay.Factory.newInstance( "s" + j, ad, bm );
            ba.setId( ( long ) j + 1 );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
            samples.add( bm );
        }
        List<CompositeSequence> genes = new ArrayList<>();
        for ( int i = 0; i < GENES; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "g" + i, ad );
            cs.setId( ( long ) i + 1 );
            genes.add( cs );
        }
        DoubleMatrix<CompositeSequence, BioMaterial> data = new DenseDoubleMatrix<>( GENES, SAMPLES );
        data.setRowNames( genes );
        data.setColumnNames( samples );
        Random random = new Random( 1 );
        for ( int i = 0; i < GENES; i++ ) {
            for ( int j = 0; j < SAMPLES; j++ ) {
                data.set( i, j, isEmpty( j ) ? Double.NaN : 8 + random.nextGaussian() );
            }
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        mat = new ExpressionDataDoubleMatrix( ee, data, qt );
    }

    /**
     * With the empty samples in, every row is present in 4 of 6 samples, under the 0.75 required. The SVD used to fail
     * with a bare NoSuchElementException from the low-variance filter; it now says why.
     */
    @Test
    public void testEmptySamplesLeaveNoRowsAndTheErrorSaysSo() {
        assertThatThrownBy( () -> new ExpressionDataSVD( mat, true ) )
                .isInstanceOf( SVDException.class )
                .hasMessage( "After filtering, matrix has no rows, SVD cannot be computed" );
    }

    @Test
    public void testTheSamplesWithDataAreTheOthers() {
        assertThat( SVDServiceImpl.samplesWithData( mat ) )
                .containsExactly( samples.get( 0 ), samples.get( 2 ), samples.get( 3 ), samples.get( 5 ) );
    }

    @Test
    public void testWithoutTheEmptySamplesTheSvdIsStoredAgainstEverySample() throws SVDException {
        ExpressionDataSVD svd = new ExpressionDataSVD( mat.sliceColumns( SVDServiceImpl.samplesWithData( mat ) ), true );
        DoubleMatrix<BioMaterial, Integer> v = svd.getV();
        DoubleMatrix<BioMaterial, Integer> full = SVDServiceImpl.expandToAllSamples( v, mat );
        assertThat( full.getRowNames() ).containsExactlyElementsOf( samples );
        assertThat( full.columns() ).isEqualTo( v.columns() );
        for ( int j = 0; j < SAMPLES; j++ ) {
            for ( int k = 0; k < full.columns(); k++ ) {
                if ( isEmpty( j ) ) {
                    assertThat( full.get( j, k ) ).isNaN();
                } else {
                    assertThat( full.get( j, k ) ).isEqualTo( v.get( v.getRowIndexByName( samples.get( j ) ), k ) );
                }
            }
        }
    }

    private static boolean isEmpty( int j ) {
        for ( int e : EMPTY ) {
            if ( e == j ) {
                return true;
            }
        }
        return false;
    }
}
