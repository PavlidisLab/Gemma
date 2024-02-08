package ubic.gemma.model.expression.bioAssayData;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SingleCellDimensionTest {

    @Test
    public void testGetBioAssay() {
        SingleCellDimension scd = new SingleCellDimension();
        List<String> cellIds = IntStream.range( 0, 100 ).mapToObj( i -> RandomStringUtils.randomAlphanumeric( 10 ) )
                .collect( Collectors.toList() );
        scd.setCellIds( cellIds );
        scd.setNumberOfCells( cellIds.size() );
        BioAssay ba1, ba2, ba3, ba4;
        ba1 = new BioAssay();
        ba1.setName( "a" );
        ba2 = new BioAssay();
        ba2.setName( "b" );
        ba3 = new BioAssay();
        ba3.setName( "c" );
        ba4 = new BioAssay();
        ba4.setName( "d" );
        scd.setBioAssays( Arrays.asList( ba1, ba2, ba3, ba4 ) );
        scd.setBioAssaysOffset( new int[] { 0, 25, 50, 75 } );
        assertEquals( ba1, scd.getBioAssay( 0 ) );
        assertEquals( ba1, scd.getBioAssay( 5 ) );
        assertEquals( ba1, scd.getBioAssay( 24 ) );
        assertEquals( ba2, scd.getBioAssay( 25 ) );
        assertEquals( ba2, scd.getBioAssay( 49 ) );
        assertEquals( ba3, scd.getBioAssay( 50 ) );
        assertEquals( ba3, scd.getBioAssay( 51 ) );
        assertEquals( ba4, scd.getBioAssay( 75 ) );
        assertEquals( ba4, scd.getBioAssay( 99 ) );
        assertThrows( IndexOutOfBoundsException.class, () -> scd.getBioAssay( -1 ) );
        assertThrows( IndexOutOfBoundsException.class, () -> scd.getBioAssay( 100 ) );
        assertEquals( ba1, scd.getBioAssayByCellId( cellIds.get( 0 ) ) );
        assertThrows( IllegalArgumentException.class, () -> scd.getBioAssayByCellId( "unknown cell ID" ) );
    }

}