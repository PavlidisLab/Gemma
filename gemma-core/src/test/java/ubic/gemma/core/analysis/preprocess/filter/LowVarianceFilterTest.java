package ubic.gemma.core.analysis.preprocess.filter;

import cern.colt.matrix.DoubleMatrix1D;
import org.junit.Test;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
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

public class LowVarianceFilterTest {

    @Test
    public void testEnsureLog2ScaleAndFilterForLowVarianceForLog2cpm() {
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
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ExpressionDataDoubleMatrix countMatrix = new ExpressionDataDoubleMatrix( randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );

        // fill a row with zeroes
        CompositeSequence deToDrop = countMatrix.getDesignElementForRow( 5 );
        DoubleMatrix<CompositeSequence, BioMaterial> cm = countMatrix.getMatrix().copy();
        for ( int j = 0; j < cm.columns(); j++ ) {
            cm.set( 5, j, 0.0 );
        }

        countMatrix = new ExpressionDataDoubleMatrix( countMatrix, cm );
        // calculate library sizes
        DoubleMatrix1D librarySize = MatrixStats.colSums( new DenseDoubleMatrix<>( countMatrix.getRawMatrixAsDoubles() ) );
        for ( int i = 0; i < librarySize.size(); i++ ) {
            countMatrix.getBioAssayDimension().getBioAssays().get( i )
                    .setSequenceReadCount( Math.round( librarySize.get( i ) ) );
        }
        // normalize
        QuantitationType log2fcQt = new QuantitationType();
        log2fcQt.setName( "log2cpm" );
        log2fcQt.setGeneralType( GeneralType.QUANTITATIVE );
        log2fcQt.setType( StandardQuantitationType.AMOUNT );
        log2fcQt.setScale( ScaleType.LOG2 );
        log2fcQt.setRepresentation( PrimitiveType.DOUBLE );
        DoubleMatrix<CompositeSequence, BioMaterial> log2fcM = countMatrix.getMatrix().copy();

        for ( int i = 0; i < log2fcM.rows(); i++ ) {
            for ( int j = 0; j < log2fcM.columns(); j++ ) {
                log2fcM.set( i, j, Math.log( 1e6 * ( log2fcM.get( i, j ) + 0.5 ) / ( librarySize.get( j ) + 1.0 ) ) / Math.log( 2 ) );
            }
        }

        ExpressionDataDoubleMatrix log2fcMatrix = new ExpressionDataDoubleMatrix( ee, log2fcQt, log2fcM );
        // calculate log2fc
        ExpressionDataDoubleMatrix filteredMatrix = new LowVarianceFilter().filter( log2fcMatrix );
        assertThat( filteredMatrix.rows() ).isEqualTo( 9 );
        assertThat( filteredMatrix.getDesignElements() )
                .doesNotContain( deToDrop );
    }
}