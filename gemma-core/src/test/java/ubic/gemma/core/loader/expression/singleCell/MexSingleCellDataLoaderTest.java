package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createElementsMappingFromResourceFile;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createLoaderForResourceDir;

public class MexSingleCellDataLoaderTest {

    @Test
    public void test() throws IOException {
        // consider the first file for mapping to elements
        Map<String, CompositeSequence> elementsMapping = createElementsMappingFromResourceFile( "data/loader/expression/singleCell/GSE224438/GSM7022367_1_features.tsv.gz" );

        MexSingleCellDataLoader loader = createLoaderForResourceDir( "data/loader/expression/singleCell/GSE224438" );
        loader.setIgnoreUnmatchedSamples( false );
        loader.setBioAssayToSampleNameMatcher( ( bm, n ) -> n.equals( bm.getName() ) );
        ArrayList<BioAssay> bas = new ArrayList<>();
        for ( String sampleName : loader.getSampleNames() ) {
            bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
        }
        assertThat( loader.getCellTypeAssignment( mock() ) ).isEmpty();
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        assertThat( qt ).isNotNull();
        assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
        assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
        assertThat( qt.getScale() ).isEqualTo( ScaleType.COUNT );
        assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 10000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 10000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 1 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 9 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000 );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping, dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000074782" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .containsExactly( 38, 256, 382, 431, 788, 814, 942 );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000038206" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    int lastSampleOffset = dimension.getBioAssaysOffset()[3];
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .hasSize( 594 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 594 )
                            // from the first sample, offset is zero
                            .containsSequence( 12, 24, 59, 67, 92, 95, 103, 107 )
                            // from the last sample
                            .containsSequence( lastSampleOffset + 3, lastSampleOffset + 8, lastSampleOffset + 24, lastSampleOffset + 30, lastSampleOffset + 31, lastSampleOffset + 39, lastSampleOffset + 45, lastSampleOffset + 59 );
                } );
    }

    @Test
    public void testLoadSpecificSamples() throws IOException {
        // consider the first file for mapping to elements
        Map<String, CompositeSequence> elementsMapping = createElementsMappingFromResourceFile( "data/loader/expression/singleCell/GSE224438/GSM7022370_2-3_features.tsv.gz" );

        MexSingleCellDataLoader loader = createLoaderForResourceDir( "data/loader/expression/singleCell/GSE224438" );
        loader.setBioAssayToSampleNameMatcher( ( bm, n ) -> n.equals( bm.getName() ) );
        ArrayList<BioAssay> bas = new ArrayList<>();
        bas.add( BioAssay.Factory.newInstance( "GSM7022370", null, BioMaterial.Factory.newInstance( "GSM7022370" ) ) );
        bas.add( BioAssay.Factory.newInstance( "GSM7022375", null, BioMaterial.Factory.newInstance( "GSM7022375" ) ) );
        assertThat( loader.getCellTypeAssignment( mock() ) ).isEmpty();
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 2000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 2000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 1 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssays() ).extracting( BioAssay::getName )
                .containsExactly( "GSM7022370", "GSM7022375" );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0, 1000 );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping, dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000039108" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .hasSize( 155 )
                            .startsWith( 2, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 155 )
                            .startsWith( 0, 6, 8, 13 );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000027291" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .containsExactly( 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 3.0, 1.0, 2.0, 1.0, 1.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0,
                                    3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
                                    1.0, 1.0 );

                    assertThat( v.getDataIndices() )
                            .containsExactly( 24, 30, 40, 59, 69, 94, 96, 105, 106, 113, 115, 126, 128, 132, 136,
                                    138, 143, 161, 164, 178, 189, 200, 201, 202, 203, 207, 212, 216, 226, 230, 233, 263,
                                    274, 276, 282, 295, 307, 323, 337, 344, 345, 398, 436, 444, 461, 462, 483, 487, 509,
                                    529, 534, 537, 540, 541, 573, 582, 595, 603, 604, 611, 631, 639, 650, 694, 704, 709,
                                    730, 736, 758, 768, 792, 799, 821, 851, 855, 856, 878, 883, 885, 902, 915, 917, 928,
                                    931, 972, 993, 1000, 1009, 1031, 1043, 1084, 1087, 1090, 1098, 1123, 1135, 1152, 1177,
                                    1180, 1189, 1190, 1206, 1208, 1217, 1237, 1242, 1270, 1285, 1296, 1305, 1317, 1326,
                                    1337, 1366, 1370, 1382, 1383, 1387, 1390, 1395, 1430, 1440, 1446, 1458, 1467, 1481,
                                    1491, 1513, 1516, 1560, 1568, 1575, 1597, 1628, 1634, 1640, 1641, 1643, 1644, 1645,
                                    1648, 1649, 1674, 1676, 1695, 1697, 1722, 1736, 1753, 1757, 1762, 1767, 1781, 1796,
                                    1810, 1827, 1837, 1843, 1858, 1882, 1901, 1906, 1920, 1933, 1950, 1957, 1964, 1984 );
                } );
    }

    @Test
    public void testUnmatchedSample() throws IOException {
        // consider the first file for mapping to elements
        Map<String, CompositeSequence> elementsMapping = createElementsMappingFromResourceFile( "data/loader/expression/singleCell/GSE224438/GSM7022370_2-3_features.tsv.gz" );

        MexSingleCellDataLoader loader = createLoaderForResourceDir( "data/loader/expression/singleCell/GSE224438" );
        loader.setBioAssayToSampleNameMatcher( ( bm, n ) -> n.equals( bm.getName() ) );
        ArrayList<BioAssay> bas = new ArrayList<>();
        // this sample does note exist
        bas.add( BioAssay.Factory.newInstance( "GSM7022354", null, BioMaterial.Factory.newInstance( "GSM7022354" ) ) );
        bas.add( BioAssay.Factory.newInstance( "GSM7022370", null, BioMaterial.Factory.newInstance( "GSM7022370" ) ) );
        assertThat( loader.getCellTypeAssignment( mock() ) ).isEmpty();
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 1000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssays() ).extracting( BioAssay::getName )
                .containsExactly( "GSM7022370" );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0 );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping, dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000039108" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( ByteArrayUtils.byteArrayToDoubles( v.getData() ) )
                            .hasSize( 155 )
                            .startsWith( 2, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 155 )
                            .startsWith( 0, 6, 8, 13 );
                } );
    }
}