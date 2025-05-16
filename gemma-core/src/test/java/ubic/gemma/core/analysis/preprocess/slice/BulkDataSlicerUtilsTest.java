package ubic.gemma.core.analysis.preprocess.slice;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.analysis.preprocess.slice.BulkDataSlicerUtils.createSlicer;

public class BulkDataSlicerUtilsTest {

    @Test
    public void test() {
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 10; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i, ad, null ) );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<RawExpressionDataVector> vecs = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );
        assertThat( vecs.stream().map( createSlicer( Collections.singletonList( ee.getBioAssays().iterator().next() ), RawExpressionDataVector.class ) ) )
                .hasSize( 100 )
                .allSatisfy( v -> {
                    assertThat( v.getBioAssayDimension().getBioAssays() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).hasSize( 1 );
                } );
    }
}