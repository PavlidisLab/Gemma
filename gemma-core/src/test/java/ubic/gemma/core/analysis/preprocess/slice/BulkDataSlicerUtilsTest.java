package ubic.gemma.core.analysis.preprocess.slice;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        RandomBulkDataUtils.setSeed( 123L );
        Collection<RawExpressionDataVector> vecs = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );
        Map<CompositeSequence, RawExpressionDataVector> vecMap = vecs.stream().collect( Collectors.toMap( DesignElementDataVector::getDesignElement, Function.identity() ) );
        BioAssay s = ee.getBioAssays().iterator().next();
        int sampleIndex = vecs.iterator().next().getBioAssayDimension().getBioAssays().indexOf( s );
        assertThat( vecs.stream().map( createSlicer( Collections.singletonList( s ), RawExpressionDataVector.class, false ) ) )
                .hasSize( 100 )
                .allSatisfy( v -> {
                    assertThat( v.getBioAssayDimension().getBioAssays() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).containsExactly( vecMap.get( v.getDesignElement() ).getDataAsDoubles()[sampleIndex] );
                } );

        assertThat( vecs.stream().map( createSlicer( Collections.singletonList( s ), RawExpressionDataVector.class, true ) ) )
                .hasSize( 100 )
                .allSatisfy( v -> {
                    assertThat( v.getBioAssayDimension().getBioAssays() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).containsExactly( vecMap.get( v.getDesignElement() ).getDataAsDoubles()[sampleIndex] );
                } );
    }

    @Test
    public void testAllowMissing() {
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
        RandomBulkDataUtils.setSeed( 123L );
        Collection<RawExpressionDataVector> vecs = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );

        BioAssay s = BioAssay.Factory.newInstance( "foo" );
        assertThat( vecs.stream().map( createSlicer( Collections.singletonList( s ), RawExpressionDataVector.class, true ) ) )
                .hasSize( 100 )
                .allSatisfy( v -> {
                    assertThat( v.getBioAssayDimension().getBioAssays() ).hasSize( 1 ).containsExactly( s );
                    assertThat( v.getDataAsDoubles() ).hasSize( 1 );
                    assertThat( v.getDataAsDoubles() ).containsExactly( Double.NaN );
                } );
    }
}