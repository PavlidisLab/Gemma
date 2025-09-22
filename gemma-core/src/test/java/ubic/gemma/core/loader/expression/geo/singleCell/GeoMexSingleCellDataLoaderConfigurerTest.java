package ubic.gemma.core.loader.expression.geo.singleCell;

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
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration
public class GeoMexSingleCellDataLoaderConfigurerTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class })
    static class Config {

    }

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Value("${cellranger.dir}")
    private Path cellRangerPrefix;

    @Value("${gemma.download.path}")
    private Path downloadDir;

    @Test
    public void testDetect10xUnfiltered10XData() throws IOException {
        GeoSample sample = readSeriesFromGeo( "GSE269482" ).getSamples().stream()
                .findFirst()
                .get();
        assertTrue( TenXCellRangerUtils.detect10x( sample ) );
        assertTrue( TenXCellRangerUtils.detect10xUnfiltered( sample ) );
    }

    @Test
    @Category(SlowTest.class)
    public void testDetect10xUnfiltered10XDataDownload() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE269482" );
        GeoSample sample = series.getSamples().stream()
                .filter( s -> s.getGeoAccession() != null && s.getGeoAccession().equals( "GSM8316309" ) )
                .findFirst()
                .get();
        Path dataDir;
        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector() ) {
            detector.setFTPClientFactory( ftpClientFactory );
            detector.setDownloadDirectory( downloadDir );
            dataDir = detector.downloadSingleCellData( sample );
        }
        GeoMexSingleCellDataLoaderConfigurer configurer = new GeoMexSingleCellDataLoaderConfigurer( dataDir, series, cellRangerPrefix );
        assertTrue( configurer.detect10x( "GSM8316309", dataDir ) );
        assertTrue( configurer.detectUnfiltered( "GSM8316309", dataDir ) );
        assertEquals( "Mus musculus", configurer.detect10xGenome( "GSM8316309", dataDir ) );
        assertEquals( "SC3Pv3-polyA", configurer.detect10xChemistry( "GSM8316309", dataDir ) );
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