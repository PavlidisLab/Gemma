package ubic.gemma.core.loader.expression.geo.fetcher2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.loader.util.ftp.FTPClientFactoryImpl;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(GeoTest.class)
public class GeoFetcherTest {

    @Test
    @Category(SlowTest.class)
    public void testGSE246121() throws IOException {
        Path tmpdir = java.nio.file.Files.createTempDirectory( "test" );
        GeoFetcher fetcher = new GeoFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), tmpdir );
        fetcher.setFtpClientFactory( new FTPClientFactoryImpl() );
        fetcher.fetchSeriesFamilySoftFile( "GSE246121" );
    }

    /**
     * This is a fallback if we encounter issues with the FTP server. It is slow, but it works.
     */
    @Test
    public void testGSE246121ViaGeoQuery() throws IOException {
        Path tmpdir = java.nio.file.Files.createTempDirectory( "test" );
        GeoFetcher fetcher = new GeoFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), tmpdir );
        fetcher.fetchSeriesFamilySoftFileFromGeoQuery( "GSE246121" );
    }

    @Test
    @Category( SlowTest.class )
    public void testNotFound() throws IOException {
        Path tmpdir = java.nio.file.Files.createTempDirectory( "test" );
        GeoFetcher fetcher = new GeoFetcher( new SimpleRetryPolicy( 0, 1000, 1.5 ), tmpdir );
        fetcher.setFtpClientFactory( new FTPClientFactoryImpl() );
        assertThatThrownBy( () -> fetcher.fetchSeriesFamilySoftFile( "GSE09120372" ) )
                .isInstanceOf( FileNotFoundException.class );
    }
}