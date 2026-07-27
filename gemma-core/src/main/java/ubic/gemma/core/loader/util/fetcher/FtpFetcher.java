/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.loader.util.fetcher;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import ubic.gemma.core.util.NetDatasourceUtil;
import ubic.gemma.core.util.NetUtils;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.concurrent.Executors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * Download files by FTP.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class FtpFetcher extends AbstractFetcher {

    /**
     * Retry transient FTP failures (connection reset / socket closed, which NCBI throws freely when many anonymous
     * connections arrive at once) with exponential backoff: up to 3 retries, 1s base delay, 1.5x factor. The staggered
     * backoff also breaks up the connection burst from concurrent imports so the retries do not all collide again.
     */
    private static final SimpleRetry<IOException> ftpRetry =
            new SimpleRetry<>( new SimpleRetryPolicy( 3, 1000, 1.5 ), IOException.class, FtpFetcher.class.getName() );

    protected FTPClient ftpClient;

    protected NetDatasourceUtil netDataSourceUtil;

    protected boolean avoidDownload = false;

    public FtpFetcher() {
        super();
        setNetDataSourceUtil();
    }

    @Override
    public Collection<File> fetch( String identifier ) {
        String seekFile = formRemoteFilePath( identifier );
        return fetch( identifier, seekFile );
    }

    /**
     * @return the netDataSourceUtil
     */
    public NetDatasourceUtil getNetDataSourceUtil() {
        return this.netDataSourceUtil;
    }

    /**
     * @param avoidDownload Set to true to avoid download if possible and simply use existing files if they are available. This skips the
     *                      usual checks for the correct file size compared to the remote one. Not all fetchers support setting this to
     *                      'true'.
     */
    public void setAvoidDownload( boolean avoidDownload ) {
        this.avoidDownload = avoidDownload;
    }

    public abstract void setNetDataSourceUtil();

    protected Callable<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        return () -> {
            File existing = new File( outputFileName );
            if ( existing.exists() && avoidDownload ) {
                log.info( "A local file exists, skipping download." );
                ftpClient.disconnect();
                return Boolean.TRUE;
            } else if ( existing.exists() && allowUseExisting ) {
                log.info( "Checking validity of existing local file: " + outputFileName );
            } else {
                log.info( "Fetching " + seekFile + " to " + outputFileName );
            }
            boolean status = NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFileName, force );
            ftpClient.disconnect();
            return status;
        };
    }

    protected Collection<File> doTask( Callable<Boolean> callable, long expectedSize, String seekFileName,
            String outputFileName ) throws IOException {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutorIfAvailable();
        Future<Boolean> future = executor.submit( callable );
        executor.shutdown();

        try {

            File outputFile = new File( outputFileName );
            boolean ok = waitForDownload( future, expectedSize, outputFile );

            if ( !ok ) {
                // cancelled, probably.
                log.info( "Download failed, was it cancelled?" );
                return null;
            } else if ( future.get() ) {
                if ( log.isInfoEnabled() )
                    log.info( "Done: local file is " + outputFile );
                File file = fetchedFile( seekFileName, outputFile.getAbsolutePath() );
                Collection<File> result = new HashSet<>();
                result.add( file );
                return result;
            }
        } catch ( ExecutionException e ) {
            // Surface a transient network failure (connection reset, socket closed, ...) as an IOException so the
            // retry wrapper in fetch() can re-attempt it, rather than burying it in a non-retryable RuntimeException.
            if ( e.getCause() instanceof IOException ) {
                throw ( IOException ) e.getCause();
            }
            throw new RuntimeException( "Couldn't fetch " + seekFileName, e );
        } catch ( InterruptedException e ) {
            log.warn( "Interrupted: Couldn't fetch " + seekFileName, e );
            return null;
        } catch ( CancellationException e ) {
            log.info( "Cancelled" );
            return null;
        }
        throw new RuntimeException( "Couldn't fetch file for " + seekFileName );
    }

    protected Collection<File> fetch( String identifier, String seekFile ) {
        File existingFile = null;
        try {
            File newDir = mkdir( identifier );
            final String outputFileName = formLocalFilePath( identifier, newDir );

            existingFile = new File( outputFileName );
//            if ( this.avoidDownload || ( existingFile.canRead() && allowUseExisting ) ) {
//                // log.info( outputFileName + " already exists." );
//            }

            // A transient IOException (connection reset, socket closed) is retried with backoff; each attempt starts
            // from a fresh connection since the reset control channel is unusable. A non-transient failure or exhausted
            // retries fall through to the fallback handling below.
            return fetchWithRetry( seekFile, outputFileName );
        } catch ( UnknownHostException e ) {
            if ( force || !allowUseExisting || existingFile == null )
                throw new RuntimeException( e );

            if ( !avoidDownload )
                throw new RuntimeException( e );

            log.warn( "Could not connect to " + this.getNetDataSourceUtil().getHost() + " to check size of " + seekFile
                    + ", using existing file" );
            return getExistingFile( existingFile, seekFile );
        } catch ( IOException e ) {

            /*
             * Note: this block can trigger if you cancel.
             */

            if ( force || !allowUseExisting || existingFile == null ) {
                /*
                 * Printing to log here because runtime error does not deliver message when passed through
                 * java.util.concurrent.Future (only throws InterruptedException and ExecutionException)
                 */
                log.error( "Runtime exception thrown: " + e.getMessage() + ". \n Stack trace follows:", e );
                throw new RuntimeException( "Cancelled, or couldn't fetch " + seekFile
                        + ", make sure the file exists on the remote server and permissions are granted.", e );

            }

            if ( Thread.currentThread().isInterrupted() ) {
                throw new CancellationException();
            }

            log.warn( "Cancelled, or couldn't fetch " + seekFile + ", make sure the file exists on the remote server.,"
                    + e + ", using existing file" );
            return getExistingFile( existingFile, seekFile );

        } finally {
            try {
                if ( ftpClient != null && ftpClient.isConnected() )
                    ftpClient.disconnect();
            } catch ( IOException e ) {
                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException( "Could not disconnect: " + e.getMessage() );
            }
        }
    }

    /**
     * Run {@link #attemptFetch} under the retry wrapper, unwrapping a {@link NonRetryableIOException} back to the
     * underlying {@link IOException} so the caller's fallback handling still sees the real (e.g. file-not-found) cause.
     */
    private Collection<File> fetchWithRetry( String seekFile, String outputFileName ) throws IOException {
        try {
            return ftpRetry.execute( ctx -> attemptFetch( seekFile, outputFileName ),
                    seekFile + " from " + this.getNetDataSourceUtil().getHost() );
        } catch ( NonRetryableIOException e ) {
            throw e.getCause();
        }
    }

    /**
     * A single connect + size-check + download attempt. Establishes a fresh FTP connection (dropping any stale one
     * from a prior try) so it is safe to invoke repeatedly under the retry wrapper. Throws {@link IOException} on a
     * transient network failure so the caller's retry can re-attempt it; a {@link FileNotFoundException} (the remote
     * file genuinely does not exist) is re-wrapped as {@link NonRetryableIOException} so it fails fast without retries.
     */
    private Collection<File> attemptFetch( String seekFile, String outputFileName ) throws IOException {
        if ( ftpClient != null && ftpClient.isConnected() ) {
            try {
                ftpClient.disconnect();
            } catch ( IOException e ) {
                log.warn( "Could not disconnect a stale FTP connection before retrying: " + e.getMessage() );
            }
        }
        ftpClient = this.getNetDataSourceUtil().connect( FTP.BINARY_FILE_TYPE );
        assert ftpClient != null; // otherwise should have gotten an exception from connect()

        try {
            long expectedSize = getExpectedSize( seekFile );
            Callable<Boolean> task = this.defineTask( outputFileName, seekFile );
            return this.doTask( task, expectedSize, seekFile, outputFileName );
        } catch ( FileNotFoundException e ) {
            throw new NonRetryableIOException( e );
        }
    }

    /**
     * Marks an {@link IOException} that should NOT be retried by {@link #ftpRetry} (it is not an {@link IOException}
     * itself, so the retry template lets it propagate). Mirrors the escape hatch used by {@code GeoBrowserImpl}.
     */
    private static class NonRetryableIOException extends RuntimeException {
        private NonRetryableIOException( IOException cause ) {
            super( cause );
        }

        @Override
        public IOException getCause() {
            return ( IOException ) super.getCause();
        }
    }

    protected long getExpectedSize( final String seekFile ) throws IOException {
        return NetUtils.ftpFileSize( ftpClient, seekFile );
    }
}
