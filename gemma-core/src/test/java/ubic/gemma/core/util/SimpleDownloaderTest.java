package ubic.gemma.core.util;

import org.apache.commons.io.file.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static java.nio.file.Files.createTempDirectory;

public class SimpleDownloaderTest {

    private SimpleDownloader downloader;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        downloader = new SimpleDownloader();
        tempDir = createTempDirectory( "test" );
    }

    @After
    public void tearDown() throws IOException {
        PathUtils.deleteDirectory( tempDir );
    }

    @Test
    public void testHttp() throws IOException, InterruptedException {
        downloader.download( new URL( "" ), tempDir.resolve( "a" ), false );
    }

    @Test
    public void testFtp() throws IOException, InterruptedException {
        downloader.download( new URL( "" ), tempDir.resolve( "a" ), false );
    }
}