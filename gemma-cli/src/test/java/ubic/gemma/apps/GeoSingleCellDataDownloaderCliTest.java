package ubic.gemma.apps;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.FileLockManagerImpl;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@Category({ GeoTest.class, SlowTest.class })
public class GeoSingleCellDataDownloaderCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, FTPConfig.class })
    static class CC {

        @Bean
        public GeoSingleCellDataDownloaderCli singleCellDataDownloaderCli() {
            return new GeoSingleCellDataDownloaderCli();
        }

        @Bean
        public FileLockManager fileLockManager() {
            return new FileLockManagerImpl();
        }
    }

    @Autowired
    private GeoSingleCellDataDownloaderCli geoSingleCellDataDownloaderCli;

    @Value("${geo.local.datafile.basepath}")
    private File geoSeriesDownloadPath;

    @Value("${geo.local.singleCellData.basepath}")
    private File singleCellDataBasePath;

    @Test
    @Category(SlowTest.class)
    public void testDownloadSingleSampleAccession() {
        assertThat( geoSingleCellDataDownloaderCli )
                .withArguments( "-e", "GSE224438", "--sample-accessions", "GSM7022367" )
                .succeeds();
        assertThat( geoSeriesDownloadPath )
                .isDirectoryRecursivelyContaining( "glob:**/GSE224438.soft.gz" );
        assertThat( singleCellDataBasePath )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7022367/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7022367/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7022367/matrix.mtx.gz" );
    }
}