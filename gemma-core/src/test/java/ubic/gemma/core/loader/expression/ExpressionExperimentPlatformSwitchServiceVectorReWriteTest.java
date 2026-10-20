package ubic.gemma.core.loader.expression;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rewriting a vector onto a dimension that orders or covers its samples differently.
 * <p>
 * The loop that maps each sample to its new position incremented its index twice per sample (reported by frinkbro,
 * 2026-09-15), so only every other sample was mapped and the missing-value fill landed one slot off.
 *
 * @author gembro
 */
public class ExpressionExperimentPlatformSwitchServiceVectorReWriteTest {

    private final ExpressionExperimentPlatformSwitchService service = new ExpressionExperimentPlatformSwitchService();

    @Test
    public void testASampleOrderThatDiffersIsRewrittenInTheNewOrder() {
        List<BioAssay> assays = assays( 3 );
        RawExpressionDataVector vector = vector( assays, 1.0, 2.0, 3.0 );
        BioAssayDimension reversed = BioAssayDimension.Factory.newInstance(
                Arrays.asList( assays.get( 2 ), assays.get( 1 ), assays.get( 0 ) ) );

        service.vectorReWrite( vector, reversed );

        assertThat( vector.getDataAsDoubles() ).containsExactly( 3.0, 2.0, 1.0 );
        assertThat( vector.getBioAssayDimension() ).isSameAs( reversed );
    }

    @Test
    public void testASampleTheVectorDoesNotHoldIsMissing() {
        List<BioAssay> assays = assays( 4 );
        RawExpressionDataVector vector = vector( assays.subList( 0, 3 ), 1.0, 2.0, 3.0 );
        BioAssayDimension wider = BioAssayDimension.Factory.newInstance(
                Arrays.asList( assays.get( 3 ), assays.get( 0 ), assays.get( 1 ), assays.get( 2 ) ) );

        service.vectorReWrite( vector, wider );

        assertThat( vector.getDataAsDoubles() ).containsExactly( Double.NaN, 1.0, 2.0, 3.0 );
    }

    private static List<BioAssay> assays( int n ) {
        BioAssay[] result = new BioAssay[n];
        for ( int i = 0; i < n; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( "sample" + i );
            bm.setId( i + 1L );
            BioAssay ba = BioAssay.Factory.newInstance( "assay" + i );
            ba.setId( i + 1L );
            ba.setSampleUsed( bm );
            result[i] = ba;
        }
        return Arrays.asList( result );
    }

    private static RawExpressionDataVector vector( List<BioAssay> assays, double... data ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.DOUBLE );
        RawExpressionDataVector v = new RawExpressionDataVector();
        v.setQuantitationType( qt );
        v.setBioAssayDimension( BioAssayDimension.Factory.newInstance( assays ) );
        v.setDataAsDoubles( data );
        return v;
    }
}
