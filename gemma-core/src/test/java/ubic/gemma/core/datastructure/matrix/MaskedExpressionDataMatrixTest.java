package ubic.gemma.core.datastructure.matrix;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MaskedExpressionDataMatrixTest {

    @Test
    public void testMaskedMatrix() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 10; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomLog2Matrix( ee );
        boolean[][] mask = new boolean[10][10];
        for ( boolean[] booleans : mask ) {
            Arrays.fill( booleans, false );
        }
        mask[4][3] = true;
        MaskedExpressionDataMatrix<Double> maskedMatrix = MaskedExpressionDataMatrix.maskElements( matrix, mask, null );
        assertNull( maskedMatrix.get( 4, 3 ) );
        assertNotNull( maskedMatrix.inverted().get( 4, 3 ) );
        assertNotNull( maskedMatrix.get( 3, 4 ) );
        assertNull( maskedMatrix.inverted().get( 3, 4 ) );
        Assertions.assertThat( maskedMatrix.getRow( 4 ) ).containsExactly(
                5.827920750003934,
                7.018724365373276,
                3.7058230209304104,
                null,
                3.3555218043168904,
                5.832033681037108,
                7.739087549610023,
                3.6781386524468185,
                7.507147494414346,
                7.190541963175411
        );
        Assertions.assertThat( maskedMatrix.inverted().getRow( 4 ) ).containsExactly(
                null, null, null, 4.559351403676336, null, null, null, null, null, null );

        Assertions.assertThat( maskedMatrix.getColumn( 3 ) ).containsExactly(
                5.692345118027769,
                7.2323629648134915,
                5.262887208571561,
                5.466947273513192,
                null,
                6.909793447108138,
                4.690425856173462,
                5.286909553325072,
                4.938108710992379,
                10.375618791030696
        );
        Assertions.assertThat( maskedMatrix.inverted().getColumn( 3 ) ).containsExactly(
                null, null, null, null, 4.559351403676336, null, null, null, null, null );

        // whole rows
        maskedMatrix = MaskedExpressionDataMatrix.maskRows( matrix, new boolean[] { false, false, false, false, true, false, false, false, false, false }, null );
        assertNull( maskedMatrix.get( 4, 3 ) );
        assertNotNull( maskedMatrix.inverted().get( 4, 3 ) );
        assertNotNull( maskedMatrix.get( 3, 4 ) );
        assertNull( maskedMatrix.inverted().get( 3, 4 ) );
        Assertions.assertThat( maskedMatrix.getRow( 4 ) ).containsExactly(
                null, null, null, null, null, null, null, null, null, null
        );
        Assertions.assertThat( maskedMatrix.inverted().getRow( 4 ) ).containsExactly(
                5.827920750003934,
                7.018724365373276,
                3.7058230209304104,
                4.559351403676336,
                3.3555218043168904,
                5.832033681037108,
                7.739087549610023,
                3.6781386524468185,
                7.507147494414346,
                7.190541963175411 );

        // whole columns
        maskedMatrix = MaskedExpressionDataMatrix.maskColumns( matrix, new boolean[] { false, false, false, false, true, false, false, false, false, false }, null );
        assertNotNull( maskedMatrix.get( 4, 3 ) );
        assertNull( maskedMatrix.inverted().get( 4, 3 ) );
        assertNull( maskedMatrix.get( 3, 4 ) );
        assertNotNull( maskedMatrix.inverted().get( 3, 4 ) );
        Assertions.assertThat( maskedMatrix.getColumn( 4 ) )
                .containsExactly( null, null, null, null, null, null, null, null, null, null );
        Assertions.assertThat( maskedMatrix.getColumn( 5 ) )
                .containsExactly(
                        8.061197118619896,
                        3.7844715947661713,
                        4.975987136005411,
                        3.8392204834312214,
                        5.832033681037108,
                        3.96447857210895,
                        5.829592866104185,
                        8.60779237520048,
                        7.167811812224883,
                        5.021299747557997 );
        Assertions.assertThat( maskedMatrix.getRow( 4 ) ).containsExactly(
                5.827920750003934,
                7.018724365373276,
                3.7058230209304104,
                4.559351403676336,
                null,
                5.832033681037108,
                7.739087549610023,
                3.6781386524468185,
                7.507147494414346,
                7.190541963175411
        );
        Assertions.assertThat( maskedMatrix.inverted().getRow( 4 ) ).containsExactly(
                null, null, null, null, 3.3555218043168904, null, null, null, null, null );

        // sparse representation
        maskedMatrix = MaskedExpressionDataMatrix.maskElements( matrix, new int[] { 4 }, new int[] { 3 }, null );
        assertNull( maskedMatrix.get( 4, 3 ) );
        assertNotNull( maskedMatrix.get( 3, 4 ) );
        Assertions.assertThat( maskedMatrix.getRow( 4 ) ).containsExactly(
                5.827920750003934,
                7.018724365373276,
                3.7058230209304104,
                null,
                3.3555218043168904,
                5.832033681037108,
                7.739087549610023,
                3.6781386524468185,
                7.507147494414346,
                7.190541963175411 );
        Assertions.assertThat( maskedMatrix.inverted().getRow( 4 ) ).containsExactly(
                null, null, null, 4.559351403676336, null, null, null, null, null, null );
        Assertions.assertThat( maskedMatrix.getColumn( 3 ) )
                .containsExactly(
                        5.692345118027769,
                        7.2323629648134915,
                        5.262887208571561,
                        5.466947273513192,
                        null,
                        6.909793447108138,
                        4.690425856173462,
                        5.286909553325072,
                        4.938108710992379,
                        10.375618791030696 );
        Assertions.assertThat( maskedMatrix.inverted().getColumn( 3 ) )
                .containsExactly( null, null, null, null, 4.559351403676336, null, null, null, null, null );
    }

    @Test
    public void testEmptyMatrix() {
        ExpressionExperiment ee = new ExpressionExperiment();
        BioAssayDimension bad = new BioAssayDimension();
        for ( int i = 0; i < 4; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "bm" + i );
            BioAssay ba = BioAssay.Factory.newInstance( "ba" + i, null, bm );
            bm.getBioAssaysUsedIn().add( ba );
            bad.getBioAssays().add( ba );
        }
        QuantitationType qt = new QuantitationType();
        MaskedExpressionDataMatrix<Object> maskedMatrix = MaskedExpressionDataMatrix.maskColumns( new EmptyBulkExpressionDataMatrix( ee, bad, qt ), new boolean[] { false, false, true, false }, null );
        assertEquals( 0, maskedMatrix.rows() );
        assertEquals( 4, maskedMatrix.columns() );
        Assertions.assertThat( maskedMatrix.getColumn( 2 ) ).isEmpty();
        maskedMatrix.inverted();
    }
}