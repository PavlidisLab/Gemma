package ubic.gemma.core.analysis.preprocess.convert;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

public class RepresentationConversionUtilsTest {

    @Before
    public void setUp() {
        RandomSingleCellDataUtils.setSeed( 123L );
    }

    @Test
    public void test() {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.LONG );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors( qt );
        Collection<SingleCellExpressionDataVector> converted = RepresentationConversionUtils.convertVectors( vectors, PrimitiveType.DOUBLE, SingleCellExpressionDataVector.class );
        assertThat( converted ).first()
                .satisfies( vec -> {
                    assertThat( vec.getQuantitationType().getRepresentation() )
                            .isEqualTo( PrimitiveType.DOUBLE );
                    assertThat( vec ).asInstanceOf( type( DesignElementDataVector.class ) )
                            .satisfies( dedv -> {
                                assertThat( dedv.getDesignElement() )
                                        .isEqualTo( vectors.iterator().next().getDesignElement() );
                            } );
                    assertThat( vec.getDataAsDoubles() )
                            .containsExactly( 12.0, 5.0, 3.0, 7.0, 8.0, 6.0, 4.0, 5.0, 6.0, 3.0, 8.0, 3.0, 3.0,
                                    4.0, 1.0, 4.0, 4.0, 8.0, 6.0, 4.0, 4.0, 2.0, 12.0, 8.0, 11.0, 3.0, 9.0, 8.0, 6.0,
                                    1.0, 7.0, 7.0, 10.0, 7.0, 5.0, 3.0, 9.0, 10.0, 4.0, 5.0, 7.0, 5.0, 3.0, 2.0, 2.0,
                                    4.0, 7.0, 3.0, 10.0, 6.0, 8.0, 3.0, 3.0, 7.0, 11.0, 4.0, 7.0, 5.0, 1.0, 5.0, 11.0,
                                    2.0, 10.0, 9.0, 8.0, 9.0, 2.0, 10.0, 6.0, 10.0, 3.0, 5.0, 1.0, 2.0, 6.0, 10.0, 3.0,
                                    3.0, 8.0, 6.0, 9.0, 1.0, 2.0, 6.0, 8.0, 9.0, 5.0, 1.0, 1.0, 1.0, 1.0, 6.0, 9.0, 7.0,
                                    3.0, 10.0, 6.0, 3.0, 5.0, 4.0, 8.0, 2.0, 8.0, 5.0, 7.0, 5.0, 1.0, 8.0, 3.0, 4.0,
                                    5.0, 3.0, 5.0, 8.0, 7.0, 4.0, 7.0, 4.0, 9.0, 6.0, 6.0, 8.0, 9.0, 10.0, 2.0, 9.0,
                                    2.0, 2.0, 6.0, 3.0, 4.0, 1.0, 9.0, 5.0, 7.0, 4.0, 3.0, 8.0, 8.0, 5.0, 5.0, 5.0, 8.0,
                                    10.0, 7.0, 7.0, 4.0, 10.0, 7.0, 4.0, 4.0, 10.0, 10.0, 9.0, 9.0, 2.0, 16.0, 3.0, 4.0,
                                    5.0, 4.0, 7.0, 3.0, 2.0, 13.0, 4.0, 4.0, 10.0, 11.0, 4.0, 7.0, 7.0, 13.0, 4.0, 13.0,
                                    6.0, 5.0, 5.0, 1.0, 5.0, 2.0, 1.0, 5.0, 5.0, 7.0, 0.0, 9.0, 4.0, 11.0, 9.0, 7.0,
                                    8.0, 7.0, 2.0, 2.0, 7.0, 9.0, 5.0, 2.0, 9.0, 14.0, 2.0, 5.0, 4.0, 2.0, 7.0, 4.0,
                                    2.0, 5.0, 8.0, 6.0, 8.0, 9.0, 2.0, 12.0, 5.0, 5.0, 4.0, 3.0, 12.0, 9.0, 7.0, 1.0,
                                    8.0, 8.0, 10.0, 3.0, 6.0, 6.0, 3.0, 2.0, 9.0, 6.0, 1.0, 4.0, 11.0, 5.0, 5.0, 9.0,
                                    4.0, 2.0, 2.0, 11.0, 3.0, 2.0, 2.0, 3.0, 8.0, 3.0, 5.0, 16.0, 13.0, 8.0, 4.0, 10.0,
                                    8.0, 2.0, 3.0, 3.0, 9.0, 2.0, 1.0, 8.0, 2.0, 5.0, 9.0, 4.0, 4.0, 4.0, 1.0, 6.0, 4.0,
                                    1.0, 6.0, 13.0, 7.0, 5.0, 5.0, 2.0, 3.0, 2.0, 4.0, 5.0, 1.0, 4.0, 10.0, 10.0, 6.0,
                                    5.0, 7.0, 8.0, 8.0, 16.0, 9.0, 4.0, 3.0, 2.0, 6.0, 5.0, 5.0, 1.0, 8.0, 3.0, 3.0,
                                    11.0, 7.0, 2.0, 13.0, 8.0, 5.0, 11.0, 4.0, 3.0, 4.0, 7.0, 3.0, 7.0, 3.0, 6.0, 4.0,
                                    7.0, 3.0, 15.0, 8.0, 7.0, 13.0, 6.0, 5.0, 10.0, 4.0, 6.0, 5.0, 1.0, 6.0, 6.0, 15.0,
                                    18.0, 2.0, 6.0, 3.0, 8.0, 5.0, 0.0, 11.0, 6.0, 0.0, 10.0, 13.0, 6.0, 4.0, 7.0, 1.0,
                                    9.0, 11.0, 9.0, 11.0, 8.0, 5.0, 7.0, 8.0, 6.0, 2.0, 6.0, 4.0, 4.0, 1.0, 9.0, 5.0,
                                    8.0, 12.0, 6.0, 11.0, 1.0, 8.0, 4.0, 14.0, 14.0, 2.0, 4.0, 11.0, 4.0, 9.0, 2.0, 2.0,
                                    3.0, 2.0, 7.0, 4.0, 3.0, 11.0, 4.0, 11.0, 3.0, 2.0, 3.0, 2.0, 1.0, 5.0, 3.0, 4.0 );
                } );
    }
}