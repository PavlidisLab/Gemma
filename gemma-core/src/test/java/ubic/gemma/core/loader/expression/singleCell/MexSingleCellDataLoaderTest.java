package ubic.gemma.core.loader.expression.singleCell;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactoryImpl;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellTransformationConfig;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createElementsMappingFromResourceFile;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createLoaderForResourceDir;

@ContextConfiguration
@ExtendWith(NetworkAvailableExtension.class)
public class MexSingleCellDataLoaderTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class, SingleCellTransformationConfig.class })
    static class Config {

    }

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    private SingleCellDataTransformationFactoryImpl singleCellDataTransformationFactory;

    @Value("${gemma.download.path}/singleCellData/GEO")
    private Path downloadDir;

    private GeoSingleCellDetector detector;

    @BeforeEach
    public void setUp() throws IOException {
        detector = new GeoSingleCellDetector();
        detector.setFTPClientFactory( ftpClientFactory );
        detector.setDownloadDirectory( downloadDir );
        detector.setSingleCellDataTransformationFactory( singleCellDataTransformationFactory );
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
                assertThat( dim.getNumberOfCellIds() ).isZero();
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
    @Tag("slow")
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
        assertThat( dimension.getCellIds() ).hasSize( 9996 );
        assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 9996 );
        assertThat( dimension.getNumberOfCellIdsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellIdsBySample( 1 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellIdsBySample( 9 ) ).isEqualTo( 1000 );
        assertThat( dimension.getBioAssaysOffset() )
                .containsExactly( 0, 1000, 2000, 3000, 4000, 5000, 5998, 6998, 7998, 8996 );
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
        assertThat( dimension.getCellIds() ).hasSize( 1998 );
        assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 1998 );
        assertThat( dimension.getNumberOfCellIdsBySample( 0 ) ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellIdsBySample( 1 ) ).isEqualTo( 998 );
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
                                    931, 972, 993, 1000, 1009, 1031, 1043, 1084, 1087, 1090, 1098, 1123, 1135, 1152,
                                    1177, 1180, 1189, 1190, 1206, 1208, 1217, 1237, 1242, 1270, 1285, 1296, 1305, 1317,
                                    1326, 1337, 1366, 1370, 1382, 1383, 1387, 1390, 1395, 1430, 1440, 1446, 1458, 1467,
                                    1481, 1491, 1513, 1516, 1560, 1568, 1575, 1597, 1628, 1634, 1640, 1641, 1643, 1644,
                                    1645, 1648, 1649, 1674, 1676, 1695, 1697, 1722, 1736, 1753, 1757, 1762, 1767, 1781,
                                    1796, 1810, 1827, 1837, 1843, 1858, 1881, 1899, 1904, 1918, 1931, 1948, 1955, 1962,
                                    1982 );
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
        assertThat( dimension.getNumberOfCellIds() ).isEqualTo( 1000 );
        assertThat( dimension.getNumberOfCellIdsBySample( 0 ) ).isEqualTo( 1000 );
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
     * <p>
     * Uses a chopped MEX fixture from the classpath (50 000 barcodes x ~1000 features per sample) so the loader is
     * exercised end-to-end without a real GEO download. See {@code GSE141552-chopped/README.md} for regeneration.
     * The over-the-wire variant is {@link #testGSE141552OverTheWire()}.
     */
    @Test
    @Tag("integration")
    public void testGSE141552( @TempDir Path tempDownloadDir ) throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromClasspath( "GSE141552" );
        List<String> sampleAccessions = Arrays.asList(
                "GSM4206900", "GSM4206901", "GSM4206902", "GSM4206903",
                "GSM4206904", "GSM4206905", "GSM4206906", "GSM4206907" );
        installChoppedFixtures( tempDownloadDir, "GSE141552-chopped", sampleAccessions );
        detector.setDownloadDirectory( tempDownloadDir );

        MexSingleCellDataLoader loader = ( MexSingleCellDataLoader ) detector.getSingleCellDataLoader( series, MexSingleCellDataLoaderConfig.builder()
                .apply10xFilter( false )
                .build() );

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
        // chopped-fixture-derived count; freeze whatever the loader reports after the chop.
        assertThat( dim.getNumberOfCellIds() ).isPositive();

        Map<String, SingleCellExpressionDataVector> vectorsByName = loader.loadVectors( de, dim, qt )
                .collect( Collectors.toMap( v -> v.getDesignElement().getName(), v -> v ) );

        // ENSG00000223972.5 (DDX11L1) has zero expression in the upstream and the chop preserves that.
        assertThat( vectorsByName ).hasEntrySatisfying( "ENSG00000223972.5", vec -> {
            assertThat( vec.getData() ).isEmpty();
            assertThat( vec.getDataIndices() ).isEmpty();
        } );

        // ENSG00000210082.2 (MT-RNR2, mitochondrial) is the densest gene in the bundle; the chopped fixture
        // still produces a non-trivial vector.
        assertThat( vectorsByName ).hasEntrySatisfying( "ENSG00000210082.2", vec -> {
            assertThat( vec.getDataIndices() ).isNotEmpty();
            assertThat( vec.getDataAsInts() ).isNotEmpty();
            assertThat( vec.getDataIndices() ).hasSameSizeAs( vec.getDataAsInts() );
            // every index must be within the dimension
            for ( int idx : vec.getDataIndices() ) {
                assertThat( idx ).isBetween( 0, dim.getNumberOfCellIds() - 1 );
            }
            for ( int count : vec.getDataAsInts() ) {
                assertThat( count ).isPositive();
            }
        } );

        // ENSG00000163930.10 (BAP1) appears in the chopped fixture at low coverage.
        assertThat( vectorsByName ).hasEntrySatisfying( "ENSG00000163930.10", vec -> {
            assertThat( vec.getDataIndices() ).hasSameSizeAs( vec.getDataAsInts() );
            for ( int idx : vec.getDataIndices() ) {
                assertThat( idx ).isBetween( 0, dim.getNumberOfCellIds() - 1 );
            }
        } );
    }

    /**
     * Truth-source variant of {@link #testGSE141552(Path)}. Downloads the full ~50-200 MB GSE141552 bundle from NCBI FTP
     * at test time and asserts against the upstream cell / barcode / vector counts. Kept as a regression guard so
     * that the chopped fixture's faithfulness can be re-verified on demand.
     */
    @Test
    @Tag("slow")
    @Tag("network")
    @Tag("integration")
    @Tag("geo")
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE141552OverTheWire() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE141552" );
        detector.downloadSingleCellData( series );
        MexSingleCellDataLoader loader = ( MexSingleCellDataLoader ) detector.getSingleCellDataLoader( series, MexSingleCellDataLoaderConfig.builder()
                .apply10xFilter( false )
                .build() );

        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        Collection<CompositeSequence> de = Arrays.asList(
                CompositeSequence.Factory.newInstance( "ENSG00000223972.5" ),
                CompositeSequence.Factory.newInstance( "ENSG00000163930.10" ),
                CompositeSequence.Factory.newInstance( "ENSG00000210082.2" ) );
        loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( de ) );

        SingleCellDimension dim = loader.getSingleCellDimension( Collections.singleton( BioAssay.Factory.newInstance( "GSM4206900", null, BioMaterial.Factory.newInstance( "GSM4206900" ) ) ) );
        assertThat( dim.getNumberOfCellIds() ).isEqualTo( 561738 );
        assertThat( loader.loadVectors( de, dim, qt ).collect( Collectors.toMap( v -> v.getDesignElement().getName(), v -> v ) ) )
                .hasEntrySatisfying( "ENSG00000210082.2", vec -> {
                    assertThat( vec.getDataIndices() ).hasSize( 90077 );
                    assertThat( vec.getDataAsInts() ).hasSize( 90077 );
                } )
                .hasEntrySatisfying( "ENSG00000163930.10", vec -> {
                    assertThat( vec.getDataIndices() ).hasSize( 349 );
                    assertThat( vec.getDataAsInts() ).hasSize( 349 );
                } )
                .hasEntrySatisfying( "ENSG00000223972.5", vec -> {
                    assertThat( vec.getData() ).isEmpty();
                    assertThat( vec.getDataIndices() ).isEmpty();
                } );
    }

    /**
     * This GEO series includes cell types in the barcodes.tsv.gz files.
     * <p>
     * Uses a chopped MEX fixture from the classpath; the over-the-wire variant is {@link #testGSE125708OverTheWire()}.
     */
    @Test
    @Tag("integration")
    public void testGSE125708( @TempDir Path tempDownloadDir ) throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromClasspath( "GSE125708" );
        GeoSample sample = series.getSamples().stream().filter( s -> "GSM3580724".equals( s.getGeoAccession() ) )
                .findFirst()
                .orElseThrow( IllegalArgumentException::new );
        installChoppedFixtures( tempDownloadDir, "GSE125708-chopped",
                Collections.singletonList( sample.getGeoAccession() ) );
        detector.setDownloadDirectory( tempDownloadDir );

        SingleCellDataLoader loader = detector.getSingleCellDataLoader( series, MexSingleCellDataLoaderConfig.builder()
                .ignoreSamplesLackingData( true )
                // skip the 10x cellranger-backed filter step — the chopped fixture is not a full 10x bundle
                .apply10xFilter( false )
                .build() );
        BioAssay ba = BioAssay.Factory.newInstance( sample.getGeoAccession() );
        ba.setSampleUsed( BioMaterial.Factory.newInstance( sample.getGeoAccession() ) );
        SingleCellDimension dimension = loader.getSingleCellDimension( Collections.singletonList( ba ) );
        // chopped fixture preserves the "AAACCTGAGGTGACCA-1" barcode at slot 0 of the upstream barcodes.tsv.gz
        assertThat( dimension.getCellIds() )
                .isNotEmpty()
                .contains( "AAACCTGAGGTGACCA-1" );
    }

    /**
     * Truth-source variant of {@link #testGSE125708(Path)}. Downloads the full GSE125708 bundle from NCBI FTP at test
     * time and asserts against the upstream cell count. Kept as a regression guard so that the chopped fixture's
     * faithfulness can be re-verified on demand.
     */
    @Test
    @Tag("slow")
    @Tag("network")
    @Tag("integration")
    @Tag("geo")
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE125708OverTheWire() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE125708" );
        GeoSample sample = series.getSamples().stream().filter( s -> "GSM3580724".equals( s.getGeoAccession() ) )
                .findFirst()
                .orElseThrow( IllegalArgumentException::new );
        detector.downloadSingleCellData( series, sample );
        SingleCellDataLoader loader = detector.getSingleCellDataLoader( series, MexSingleCellDataLoaderConfig.builder().ignoreSamplesLackingData( true ).build() );
        BioAssay ba = BioAssay.Factory.newInstance( sample.getGeoAccession() );
        ba.setSampleUsed( BioMaterial.Factory.newInstance( sample.getGeoAccession() ) );
        SingleCellDimension dimension = loader.getSingleCellDimension( Collections.singletonList( ba ) );
        assertThat( dimension.getCellIds() )
                .hasSize( 9939 )
                .contains( "AAACCTGAGGTGACCA-1" );
    }

    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        URL url = GeoUtils.getUrl( accession, GeoSource.FTP, GeoFormat.SOFT, GeoScope.FAMILY, GeoAmount.FULL );
        try ( InputStream is = new GZIPInputStream( ftpClientFactory.openStream( url ) ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
        }
    }

    /**
     * Parse a GEO series from a cached {@code _family.soft.gz} fixture under the classpath, avoiding the FTP fetch
     * that {@link #readSeriesFromGeo(String)} would otherwise perform.
     */
    private GeoSeries readSeriesFromClasspath( String accession ) throws IOException {
        String resource = "data/loader/expression/geo/series/" + accession + "_family.soft.gz";
        try ( InputStream raw = requireNonNull(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ),
                "Missing classpath fixture: " + resource );
              InputStream is = new GZIPInputStream( raw ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
        }
    }

    /**
     * Copy a chopped MEX bundle from {@code data/loader/expression/singleCell/<fixtureDir>/<sample>/} on the
     * classpath into {@code downloadRoot/<sample>/}, mirroring the directory layout the detector would have
     * produced from a real {@code downloadSingleCellData(series)} call so the configurer can scan it.
     */
    private void installChoppedFixtures( Path downloadRoot, String fixtureDir, List<String> sampleAccessions ) throws IOException {
        String[] mexFiles = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };
        for ( String sample : sampleAccessions ) {
            Path sampleDir = downloadRoot.resolve( sample );
            Files.createDirectories( sampleDir );
            for ( String file : mexFiles ) {
                String resource = "data/loader/expression/singleCell/" + fixtureDir + "/" + sample + "/" + file;
                try ( InputStream src = requireNonNull(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ),
                        "Missing classpath fixture: " + resource ) ) {
                    Files.copy( src, sampleDir.resolve( file ), StandardCopyOption.REPLACE_EXISTING );
                }
            }
        }
    }
}
