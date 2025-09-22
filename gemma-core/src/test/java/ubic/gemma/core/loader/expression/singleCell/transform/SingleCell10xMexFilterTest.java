package ubic.gemma.core.loader.expression.singleCell.transform;

import org.apache.commons.io.file.PathUtils;
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
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@ContextConfiguration
public class SingleCell10xMexFilterTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class, SingleCellTransformationConfig.class })
    static class Config {

    }

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Value("${gemma.download.path}/singleCellData/GEO")
    private Path downloadDir;

    @Test
    @Category(SlowTest.class)
    public void testGSE269482() throws IOException, NoSingleCellDataFoundException {
        SingleCell10xMexFilter filter = ctx.getBean( SingleCell10xMexFilter.class );
        assumeThat( filter.getCellRangerExecutable() ).exists();
        assumeThat( filter.getPythonExecutable() ).exists();
        Path dataPath;
        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector() ) {
            detector.setFTPClientFactory( ftpClientFactory );
            detector.setDownloadDirectory( downloadDir );
            GeoSample sample = readSeriesFromGeo( "GSE269482" ).getSamples().stream()
                    .filter( s -> s.getGeoAccession() != null && s.getGeoAccession().equals( "GSM8316309" ) )
                    .findFirst()
                    .get();
            dataPath = detector.downloadSingleCellData( sample );
        }
        filter.setInputFile( dataPath, SingleCellDataType.MEX );
        Path outputFile = Files.createTempDirectory( "test" );
        try {
            filter.setOutputFile( outputFile, SingleCellDataType.MEX );
            filter.setGenome( "Mus musculus" );
            filter.setChemistry( null );
            filter.perform();
            assertThat( outputFile )
                    .exists()
                    .isDirectoryContaining( "glob:**/barcodes.tsv.gz" )
                    .isDirectoryContaining( "glob:**/features.tsv.gz" )
                    .isDirectoryContaining( "glob:**/matrix.mtx.gz" );
        } finally {
            PathUtils.delete( outputFile );
        }
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