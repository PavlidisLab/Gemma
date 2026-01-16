package ubic.gemma.core.loader.expression.geo.singleCell;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.TenXCellRangerUtils;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCell10xMexFilter;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellTransformationConfig;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration
public class GeoMexSingleCellDataLoaderConfigurerTest extends BaseTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class, SingleCellTransformationConfig.class })
    static class Config {

    }

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    private ApplicationContext ctx;

    @Value("${cellranger.dir}")
    private Path cellRangerPrefix;

    @Value("${gemma.download.path}/singleCellData/GEO")
    private Path downloadDir;

    @Test
    @NetworkAvailable
    public void testDetect10xUnfiltered10XData() throws IOException {
        GeoSample sample = readSeriesFromGeo( "GSE269482" ).getSamples().stream()
                .findFirst()
                .get();
        assertTrue( TenXCellRangerUtils.detect10x( sample.getDataProcessing() ) );
        assertTrue( TenXCellRangerUtils.detect10xUnfiltered( sample.getDataProcessing() ) );
    }

    @Test
    @NetworkAvailable
    public void testGSE269482() throws IOException, NoSingleCellDataFoundException {
        testUnfiltered10xDataset( "GSE269482", "GSM8316309", "Mus musculus", "SC3Pv3-polyA-OCM" );
    }

    /**
     * This is an older single-cell dataset with many typos in the GEO record.
     */
    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE217511() throws IOException, NoSingleCellDataFoundException {
        testUnfiltered10xDataset( "GSE217511", "GSM6720852", "Homo sapiens", "SC3Pv3-polyA" );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE178226() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE178226", "GSM5384778", "Mus musculus", "SC3Pv3-polyA" );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE280175() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE280175", "GSM8591175", "Homo sapiens", null );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE221042() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE221042", "GSM6841143", "Homo sapiens", "SC3Pv3-polyA" );
    }

    @Test
    @NetworkAvailable
    public void testGSE223423() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE223423", "GSM6948202", "Mus musculus", "SC3Pv3-polyA" );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE143355() throws NoSingleCellDataFoundException, IOException {
        // the extraction protocol does not specify if it's 3' or 5' v3
        testUnfiltered10xDataset( "GSE143355", "GSM4257550", "Mus musculus", null );
    }

    @Test
    @NetworkAvailable
    public void testGSE198033() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE198033", "GSM5936167", "Homo sapiens", "SC3Pv3-polyA" );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable
    public void testGSE132355() throws NoSingleCellDataFoundException, IOException {
        testUnfiltered10xDataset( "GSE132355", "GSM3860733", "Mus musculus", null );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testGSE295078() throws NoSingleCellDataFoundException, IOException {
        // TODO: this is a dataset with a 5' chemistry
        testUnfiltered10xDataset( "GSE295078", "GSM8941791", "Mus musculus", null );
    }

    @Test
    @NetworkAvailable
    public void testGSE255369() throws IOException {
        // data is not in GEO, so just use an empty placeholder
        Path mexDir = Files.createTempDirectory( "GSE255369" );
        Path sampleDir = mexDir.resolve( "GSM8070652" );
        GeoSeries series = readSeriesFromGeo( "GSE255369" );
        GeoSample sample = series.getSamples().stream()
                .filter( s -> s.getGeoAccession() != null && s.getGeoAccession().equals( "GSM8070652" ) )
                .findFirst()
                .get();
        GeoMexSingleCellDataLoaderConfigurer configurer = new GeoMexSingleCellDataLoaderConfigurer( mexDir, series, cellRangerPrefix );
        assertTrue( configurer.detect10x( "GSM8070652", sampleDir ) );
        assertTrue( configurer.detectUnfiltered( "GSM8070652", sampleDir ) );
        assertEquals( "Mus musculus", configurer.detect10xGenome( "GSM8070652", sampleDir ) );
        assertEquals( "SC3Pv3-polyA-OCM", configurer.detect10xChemistry( "GSM8070652", sampleDir ) );
    }

    private void testUnfiltered10xDataset( String seriesName, String sampleName, String expectedTaxa, String expectedChemistry ) throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( seriesName );
        GeoSample sample = series.getSamples().stream()
                .filter( s -> s.getGeoAccession() != null && s.getGeoAccession().equals( sampleName ) )
                .findFirst()
                .get();
        Path dataDir;
        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector() ) {
            detector.setFTPClientFactory( ftpClientFactory );
            detector.setDownloadDirectory( downloadDir );
            dataDir = detector.downloadSingleCellData( sample );
        }
        GeoMexSingleCellDataLoaderConfigurer configurer = new GeoMexSingleCellDataLoaderConfigurer( dataDir, series, cellRangerPrefix );
        assertTrue( configurer.detect10x( sampleName, dataDir ) );
        assertTrue( configurer.detectUnfiltered( sampleName, dataDir ) );
        assertEquals( expectedTaxa, configurer.detect10xGenome( sampleName, dataDir ) );
        assertEquals( expectedChemistry, configurer.detect10xChemistry( sampleName, dataDir ) );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/geo/series/")
    public void testParallelFiltering() throws IOException, NoSingleCellDataFoundException {
        SingleCell10xMexFilter filter = ctx.getBean( SingleCell10xMexFilter.class );
        Assume.assumeTrue( "The current CPU does not support AVX instructions.", filter.isCpuSupported() );
        GeoSeries series = readSeriesFromGeo( "GSE269482" );
        Path dataDir;
        ExecutorService executor = Executors.newFixedThreadPool( 4 );
        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector() ) {
            detector.setCellRangerPrefix( cellRangerPrefix );
            detector.setFTPClientFactory( ftpClientFactory );
            detector.setDownloadDirectory( downloadDir );
            dataDir = detector.downloadSingleCellData( series );
            MexSingleCellDataLoaderConfig config = MexSingleCellDataLoaderConfig.builder()
                    .dataPath( dataDir )
                    .apply10xFilter( true )
                    .transformExecutor( Executors.newFixedThreadPool( 4 ) )
                    .build();
            // the filter is applied by the configurer, so this is enough to "test" it
            try ( SingleCellDataLoader loader = detector.getSingleCellDataLoader( series, config ) ) {
                // pass
            }
        } finally {
            executor.shutdown();
        }
    }

    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        URL url = GeoUtils.getUrl( accession, GeoSource.FTP, GeoFormat.SOFT, GeoScope.FAMILY, GeoAmount.FULL );
        try ( InputStream is = new GZIPInputStream( ftpClientFactory.openStream( url ) ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
        }
    }
}