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
import ubic.basecode.util.NetUtils;
import ubic.gemma.core.util.NetDatasourceUtil;

import java.io.File;
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

    protected FutureTask<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        return new FutureTask<>( new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
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
            }
        } );
    }

    protected Collection<File> doTask( FutureTask<Boolean> future, long expectedSize, String seekFileName,
            String outputFileName ) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute( future );
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
            String outputFileName = formLocalFilePath( identifier, newDir );

            existingFile = new File( outputFileName );
//            if ( this.avoidDownload || ( existingFile.canRead() && allowUseExisting ) ) {
//                // log.info( outputFileName + " already exists." );
//            }

            if ( ftpClient == null || !ftpClient.isConnected() ) {
                ftpClient = this.getNetDataSourceUtil().connect( FTP.BINARY_FILE_TYPE );
                assert ftpClient != null; // otherwise should have gotten an exception from connect()
            }

            long expectedSize = getExpectedSize( seekFile );

            FutureTask<Boolean> future = this.defineTask( outputFileName, seekFile );
            return this.doTask( future, expectedSize, seekFile, outputFileName );
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
                 * java.util.concurrent.FutureTask (only throws InterruptedException and ExecutionException)
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

    protected long getExpectedSize( final String seekFile ) throws IOException {
        return NetUtils.ftpFileSize( ftpClient, seekFile );
    }
}
