package ubic.gemma.core.analysis.preprocess.filter;

import cern.colt.matrix.DoubleMatrix2D;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;
import ubic.basecode.math.Constants;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.randomBulkVectors;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.setSeed;

public class ZeroVarianceFilterTest {

    @Test
    public void test() {
        setSeed( 124 );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        ExpressionDataDoubleMatrix filteredMatrix = new ZeroVarianceFilter().filter( matrix );
        assertThat( filteredMatrix.rows() )
                .isEqualTo( 10 );

        // fill a row with zeroes
        DoubleMatrix2D dmatrix = matrix.asDoubleMatrix2D();
        for ( int j = 0; j < 10; j++ ) {
            dmatrix.set( 5, j, 10.0 );
        }
        matrix = matrix.withMatrix( dmatrix );

        filteredMatrix = new ZeroVarianceFilter().filter( matrix );
        assertThat( filteredMatrix.rows() )
                .isEqualTo( 9 );

        // fill low-variance noise
        NormalDistribution dist = new NormalDistribution( 0, Math.sqrt( 0.1 * Constants.SMALLISH ) );
        dmatrix = matrix.asDoubleMatrix2D();
        for ( int j = 0; j < 10; j++ ) {
            dmatrix.set( 5, j, dist.sample() );
        }
        matrix = matrix.withMatrix( dmatrix );

        filteredMatrix = new ZeroVarianceFilter().filter( matrix );
        assertThat( filteredMatrix.rows() )
                .isEqualTo( 9 );

        // fill noise above the detection threshold
        dist = new NormalDistribution( 0, Math.sqrt( 10 * Constants.SMALLISH ) );
        dmatrix = matrix.asDoubleMatrix2D();
        for ( int j = 0; j < 10; j++ ) {
            dmatrix.set( 5, j, dist.sample() );
        }
        matrix = matrix.withMatrix( dmatrix );

        filteredMatrix = new ZeroVarianceFilter().filter( matrix );
        assertThat( filteredMatrix.rows() )
                .isEqualTo( 10 );
    }
}