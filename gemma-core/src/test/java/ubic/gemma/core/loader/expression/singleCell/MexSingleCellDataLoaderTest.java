package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createElementsMappingFromResourceFile;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createLoaderForResourceDir;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

@ContextConfiguration
public class MexSingleCellDataLoaderTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class })
    static class Config {

    }

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Value("${gemma.download.path}/singleCellData/GEO")
    private Path downloadDir;

    private GeoSingleCellDetector detector;

    @Before
    public void setUp() throws IOException {
        detector = new GeoSingleCellDetector();
        detector.setFTPClientFactory( ftpClientFactory );
        detector.setDownloadDirectory( downloadDir );
    }

    @Test
    public void testEmpty() throws IOException {
        try ( MexSingleCellDataLoader loader = new MexSingleCellDataLoader( Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList() ) ) {
            loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
            assertThat( loader.getSampleNames() ).isEmpty();
            assertThat( loader.getQuantitationTypes() ).singleElement()
                    .satisfies( qt -> {
                        assertThat( qt.getName() ).isEqualTo( "10x MEX" );
                        assertThat( qt.getDescription() ).isEqualTo( "10x MEX data loaded from 0 sets of files (i.e. features.tsv.gz, barcodes.tsv.gz and matrix.mtx.gz)." );
                        assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
                        assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
                        assertThat( qt.getScale() ).isEqualTo( ScaleType.COUNT );
                        assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.INT );
                    } );
            BioAssay ba = BioAssay.Factory.newInstance( "test", null, BioMaterial.Factory.newInstance( "test" ) );
            assertThat( loader.getSingleCellDimension( Collections.singleton( ba ) ) ).satisfies( dim -> {
                assertThat( dim.getCellIds() ).isEmpty();
                assertThat( dim.getBioAssays() ).isEmpty();
                assertThat( dim.getNumberOfCells() ).isZero();
            } );
            SingleCellDimension dim = loader.getSingleCellDimension( Collections.singleton( ba ) );
            QuantitationType qt = loader.getQuantitationTypes().iterator().next();
            CompositeSequence de = CompositeSequence.Factory.newInstance( "test" );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( Collections.singleton( de ) ) );
            assertThat( loader.getSequencingMetadata( dim ) )
                    .isEmpty();
            assertThat( loader.loadVectors( Collections.singleton( de ), dim, qt ) )
                    .isEmpty();
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testGSE224438() throws IOException {
        // consider the first file for mapping to elements
        Map<String, CompositeSequence> elementsMapping = createElementsMappingFromResourceFile( "data/loader/expression/singleCell/GSE224438/GSM7022367_1_features.tsv.gz" );

        MexSingleCellDataLoader loader = createLoaderForResourceDir( "data/loader/expression/singleCell/GSE224438" );
        loader.setIgnoreUnmatchedSamples( false );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        ArrayList<BioAssay> bas = new ArrayList<>();
        for ( String sampleName : loader.getSampleNames() ) {
            bas.add( BioAssay.Factory.newInstance( sampleName, null, BioMaterial.Factory.newInstance( sampleName ) ) );
        }
        assertThatThrownBy( () -> loader.getCellTypeAssignments( mock() ) )
                .isInstanceOf( UnsupportedOperationException.class );
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        assertThat( qt ).isNotNull();
        assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
        assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
        assertThat( qt.getScale() ).isEqualTo( ScaleType.COUNT );
        assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.INT );
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 10000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 10000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 1 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 9 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000 );
        assertThat( loader.getSequencingMetadata( dimension ) )
                .containsOnlyKeys( bas )
                .values()
                .extracting( SequencingMetadata::getReadCount )
                .containsExactlyInAnyOrder( 197092L, 240642L, 178510L, 200020L, 128837L, 161978L, 203185L, 345699L, 267183L, 263007L );
        loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getOriginalDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000074782" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( v.getOriginalDesignElement() ).isEqualTo( "ENSMUSG00000074782" );
                    assertThat( v.getDataAsInts() )
                            .containsExactly( 1, 1, 1, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .containsExactly( 38, 256, 382, 431, 788, 814, 942 );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000038206" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    int lastSampleOffset = dimension.getBioAssaysOffset()[3];
                    assertThat( v.getDataAsInts() )
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
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        ArrayList<BioAssay> bas = new ArrayList<>();
        bas.add( BioAssay.Factory.newInstance( "GSM7022370", null, BioMaterial.Factory.newInstance( "GSM7022370" ) ) );
        bas.add( BioAssay.Factory.newInstance( "GSM7022375", null, BioMaterial.Factory.newInstance( "GSM7022375" ) ) );
        assertThatThrownBy( () -> loader.getCellTypeAssignments( mock() ) )
                .isInstanceOf( UnsupportedOperationException.class );
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
        loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "", elementsMapping ) );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000039108" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( v.getDataAsInts() )
                            .hasSize( 155 )
                            .startsWith( 2, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 155 )
                            .startsWith( 0, 6, 8, 13 );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000027291" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( v.getDataAsInts() )
                            .containsExactly( 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1,
                                    1, 1, 1, 1, 1, 3, 1, 2, 1, 1, 3, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 2,
                                    3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1,
                                    1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1 );

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
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        ArrayList<BioAssay> bas = new ArrayList<>();
        // this sample does note exist
        bas.add( BioAssay.Factory.newInstance( "GSM7022354", null, BioMaterial.Factory.newInstance( "GSM7022354" ) ) );
        bas.add( BioAssay.Factory.newInstance( "GSM7022370", null, BioMaterial.Factory.newInstance( "GSM7022370" ) ) );
        assertThatThrownBy( () -> loader.getCellTypeAssignments( mock() ) )
                .isInstanceOf( UnsupportedOperationException.class );
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        SingleCellDimension dimension = loader.getSingleCellDimension( bas );
        assertThat( dimension.getCellIds() ).hasSize( 1000 );
        assertThat( dimension.getNumberOfCells() ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssays() ).extracting( BioAssay::getName )
                .containsExactly( "GSM7022370" );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0 );
        loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );
        List<SingleCellExpressionDataVector> vectors = loader.loadVectors( elementsMapping.values(), dimension, qt ).collect( Collectors.toList() );
        assertThat( vectors )
                .hasSize( 1000 )
                .allSatisfy( v -> {
                    assertThat( v.getDesignElement() ).isNotNull();
                    assertThat( v.getSingleCellDimension() ).isEqualTo( dimension );
                    assertThat( v.getQuantitationType() ).isEqualTo( qt );
                } );

        assertThat( vectors.stream().filter( v -> v.getDesignElement().getName().equals( "ENSMUSG00000039108" ) ).findFirst() )
                .hasValueSatisfying( v -> {
                    assertThat( v.getDataAsInts() )
                            .hasSize( 155 )
                            .startsWith( 2, 1, 1, 1, 1 );
                    assertThat( v.getDataIndices() )
                            .hasSize( 155 )
                            .startsWith( 0, 6, 8, 13 );
                } );
    }

    /**
     * This dataset does not filter empty droplets and thus many barcodes are simply unused and can be discarded.
     */
    @Test
    @Category({ GeoTest.class, SlowTest.class })
    public void testGSE141552() throws IOException, NoSingleCellDataFoundException {
        assumeThatResourceIsAvailable( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/" );
        GeoSeries series = readSeriesFromGeo( "GSE141552" );
        detector.downloadSingleCellData( series );
        MexSingleCellDataLoader loader = ( MexSingleCellDataLoader ) detector.getSingleCellDataLoader( series, SingleCellDataLoaderConfig.builder().build() );

        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        Collection<CompositeSequence> de = Arrays.asList(
                // this one has no data
                CompositeSequence.Factory.newInstance( "ENSG00000223972.5" ),
                // a random one
                CompositeSequence.Factory.newInstance( "ENSG00000163930.10" ),
                // this one has the most data
                CompositeSequence.Factory.newInstance( "ENSG00000210082.2" ) );
        loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( de ) );

        SingleCellDimension dim = loader.getSingleCellDimension( Collections.singleton( BioAssay.Factory.newInstance( "GSM4206900", null, BioMaterial.Factory.newInstance( "GSM4206900" ) ) ) );
        assertThat( dim )
                .satisfies( scd -> {
                    assertThat( scd.getNumberOfCells() ).isEqualTo( 561738 );
                } );
        assertThat( loader.loadVectors( de, dim, qt ).collect( Collectors.toMap( v -> v.getDesignElement().getName(), v -> v ) ) )
                .hasEntrySatisfying( "ENSG00000210082.2", vec -> {
                    // assertThat( vec.getData() ).isEmpty();
                    assertThat( vec.getDataIndices() )
                            .hasSize( 90077 )
                            .startsWith( 4, 17, 25, 30, 40, 41, 45, 56, 72, 74, 78, 83, 86,
                                    88, 89, 93, 95, 96, 97, 105, 119, 127, 136, 140, 146, 150,
                                    152, 168, 170, 171, 177, 184, 193, 204, 207, 213, 217, 221, 222,
                                    232, 234, 236, 240, 244, 259, 260, 267, 286, 290, 294, 296, 305,
                                    309, 315, 318, 319, 321, 324, 327, 337, 343, 362, 373, 382, 392,
                                    400, 406, 417, 420, 422, 426, 427, 434, 436, 447, 454, 479, 488,
                                    489, 492, 494, 495, 499, 504, 505, 513, 522, 527, 529, 558, 566,
                                    584, 586, 596, 597, 602, 619, 621, 623, 635 );
                    assertThat( vec.getDataAsInts() )
                            .hasSize( 90077 )
                            .startsWith( 2, 2, 1, 5, 9, 1, 7, 2, 1, 3, 15, 3, 1,
                                    8, 2, 1, 1, 1, 5, 1, 1, 3, 2, 1, 7, 1,
                                    3, 1, 5, 2, 1, 2, 2, 2, 4, 1, 4, 2, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 3, 6, 1, 3, 1,
                                    1, 3, 7, 5, 3, 1, 1, 2, 1, 8, 1, 1, 1,
                                    21, 1, 9, 1, 2, 2, 2, 1, 1, 5, 2, 3, 1,
                                    1, 2, 1, 1, 4, 23, 1, 1, 1, 2, 1, 1, 1,
                                    2, 1, 10, 1, 5, 2, 2, 1, 3 );
                } )
                .hasEntrySatisfying( "ENSG00000163930.10", vec -> {
                    assertThat( vec.getDataIndices() )
                            .hasSize( 349 )
                            .containsExactly( 1869, 3252, 4246, 4827, 6406, 6970, 8431, 8673,
                                    11395, 15227, 15269, 16785, 17036, 19800, 20275, 21248,
                                    25095, 30432, 30798, 31580, 32354, 35333, 37094, 37312,
                                    39350, 45103, 46542, 47224, 49577, 50203, 50590, 57009,
                                    58077, 59998, 61991, 65081, 68093, 68721, 70727, 71591,
                                    72563, 74332, 75130, 75209, 76522, 76553, 77920, 78594,
                                    81544, 81589, 83307, 86729, 91784, 93174, 94523, 96559,
                                    96578, 97332, 99974, 101258, 105956, 106975, 109080, 112502,
                                    114411, 114482, 116686, 116688, 118001, 119559, 119672, 120765,
                                    120948, 126886, 127945, 131120, 132002, 132243, 132412, 133386,
                                    134085, 134461, 137163, 139294, 144950, 145725, 146946, 147627,
                                    148819, 148889, 152448, 152664, 153450, 154144, 154705, 155550,
                                    157816, 158139, 159255, 159986, 160099, 163080, 164346, 166230,
                                    167474, 169610, 171790, 172627, 173414, 173957, 173965, 174428,
                                    174492, 177330, 177456, 179902, 180103, 181901, 182290, 188259,
                                    188355, 194410, 195028, 195143, 196157, 199187, 199776, 201077,
                                    206014, 208250, 208379, 208460, 208540, 216059, 218608, 218905,
                                    219722, 222242, 222345, 224692, 226433, 226657, 226999, 231235,
                                    231244, 231261, 232172, 232836, 235031, 236318, 237253, 238967,
                                    239162, 242640, 242891, 243894, 245932, 253294, 258391, 259080,
                                    259445, 259486, 264107, 264333, 266548, 267500, 269343, 270565,
                                    270870, 272184, 275508, 277034, 279283, 280041, 286060, 286548,
                                    287858, 295312, 295471, 295716, 297145, 298749, 301731, 301933,
                                    302225, 302882, 303547, 303999, 305446, 306109, 308213, 312755,
                                    315793, 316918, 320619, 323600, 323899, 326143, 328613, 331160,
                                    331518, 333165, 334760, 336258, 337612, 338476, 338637, 340371,
                                    341793, 345850, 348136, 348433, 348606, 348913, 348964, 350062,
                                    351404, 351933, 352312, 352468, 352679, 353023, 354829, 354854,
                                    359262, 360053, 363589, 364161, 365604, 368904, 372013, 375027,
                                    376456, 378155, 382334, 384055, 386359, 386962, 388045, 388831,
                                    390151, 390764, 390829, 391765, 392148, 392825, 393347, 394485,
                                    397287, 397350, 397631, 398979, 401913, 403417, 407860, 408458,
                                    409234, 409421, 410015, 410111, 411838, 418149, 425026, 425068,
                                    427116, 428190, 429050, 429190, 431253, 434756, 437258, 438769,
                                    440925, 442895, 444165, 446436, 448024, 449421, 450886, 450895,
                                    451333, 451356, 452144, 452302, 452315, 453672, 454233, 454394,
                                    459130, 459630, 460016, 461206, 465948, 466062, 466691, 467699,
                                    469208, 473534, 474485, 474938, 480333, 484675, 488670, 489785,
                                    490362, 491726, 499194, 501994, 502701, 503354, 505492, 506325,
                                    507542, 508127, 509434, 511584, 512814, 513114, 513766, 516596,
                                    516774, 516875, 517712, 517747, 518830, 523238, 523994, 525676,
                                    527714, 528165, 528793, 528988, 529976, 530443, 530829, 533435,
                                    535810, 536607, 540709, 541792, 542755, 543952, 544728, 553323,
                                    553853, 554017, 554032, 559095, 559213 );
                    assertThat( vec.getDataAsInts() )
                            .hasSize( 349 )
                            .startsWith(
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 3,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 4, 1,
                                    1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1,
                                    2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1,
                                    1, 1, 1, 1, 1, 1, 1, 1, 1 );
                } )
                .hasEntrySatisfying( "ENSG00000223972.5", vec -> {
                    assertThat( vec.getData() ).isEmpty();
                    assertThat( vec.getDataIndices() ).isEmpty();
                } );
    }

    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        URL url = GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP, GeoFormat.SOFT );
        try ( InputStream is = new GZIPInputStream( ftpClientFactory.openStream( url ) ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
        }
    }
}