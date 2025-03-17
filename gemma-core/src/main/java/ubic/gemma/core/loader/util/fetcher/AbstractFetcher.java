/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.description.LocalFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * @author pavlidis
 */
public abstract class AbstractFetcher implements Fetcher {

    protected static final int INFO_UPDATE_INTERVAL = 10000;
    protected static final Log log = LogFactory.getLog( AbstractFetcher.class.getName() );
    private static final int NUMBER_OF_TIMES_TO_LOG_WAITING_BEFORE_REDUCING_VERBOSITY = 5;
    /**
     * how long we wait in ms for a download that has stalled.
     */
    private static final long STALLED_BAIL_TIME_LIMIT = 60 * 1000L;
    /**
     * Whether we are allowed to use an existing file rather than downloading again, in the case where we can't connect
     * to the remote host to check the size of the file. Setting force=true overrides this. Default is FALSE.
     */
    protected boolean allowUseExisting = false;

    /**
     * Whether download is required even if the sizes match.
     */
    protected boolean force = false;

    protected String localBasePath = null;

    protected String remoteBaseDir = null;

    public AbstractFetcher() {
        super();
        this.initConfig();
    }

    /**
     * @return Returns the localBasePath.
     */
    public String getLocalBasePath() {
        return this.localBasePath;
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return this.force;
    }

    /**
     * Set to true if downloads should proceed even if the file already exists.
     *
     * @param force new force
     */
    @Override
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * @param allowUseExisting the allowUseExisting to set
     */
    public void setAllowUseExisting( boolean allowUseExisting ) {
        this.allowUseExisting = allowUseExisting;
    }

    protected LocalFile fetchedFile( String seekFile ) {
        return this.fetchedFile( seekFile, seekFile );
    }

    /**
     * @param seekFilePath   Absolute path to the file for download
     * @param outputFilePath Absolute path to the download location.
     * @return local file
     */
    protected LocalFile fetchedFile( String seekFilePath, String outputFilePath ) {
        LocalFile file = LocalFile.Factory.newInstance();
        file.setVersion( new SimpleDateFormat().format( new Date() ) );
        file.setRemoteURL( ( new File( seekFilePath ) ).toURI() );
        file.setLocalURL( ( new File( outputFilePath ).toURI() ) );
        return file;
    }

    protected abstract String formLocalFilePath( String identifier, File newDir );

    protected abstract String formRemoteFilePath( String identifier );

    /**
     * Wrap the existing file in the required Collection&lt;LocalFile&gt;
     *
     * @param existingFile existing file
     * @param seekFile     seek file
     * @return collection of local files
     */
    protected Collection<LocalFile> getExistingFile( File existingFile, String seekFile ) {
        Collection<LocalFile> fallback = new HashSet<>();
        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setLocalURL( existingFile.toURI() );
        lf.setRemoteURL( ( new File( seekFile ) ).toURI() );
        lf.setSize( existingFile.length() );
        fallback.add( lf );
        return fallback;
    }

    protected abstract void initConfig();

    /**
     * Like mkdir(accession) but for cases where there is no accession.
     *
     * @return file
     * @throws IOException when there are IO problems.
     */
    protected File mkdir() throws IOException {
        return this.mkdir( null );
    }

    /**
     * Create a directory according to the current accession number and set path information, including any non-existing
     * parent directories. If the path cannot be used, we use a temporary directory.
     *
     * @param accession accession
     * @return new directory
     * @throws IOException if there is a problem while manipulating the file
     */
    protected File mkdir( String accession ) throws IOException {
        File newDir = null;
        File targetPath = null;

        if ( localBasePath != null ) {

            targetPath = new File( localBasePath );

            if ( !( targetPath.exists() && targetPath.canRead() ) ) {
                AbstractFetcher.log.warn( "Attempting to create directory '" + localBasePath + "'" );
                FileUtils.forceMkdir( targetPath );
            }

            if ( accession == null ) {
                newDir = targetPath;
            } else {
                newDir = new File( targetPath + File.separator + accession );
            }

        }

        if ( targetPath == null || !targetPath.canRead() ) {
            File tmpDir;
            String systemTempDir = System.getProperty( "java.io.tmpdir" );
            if ( accession == null ) {
                tmpDir = new File( systemTempDir );
            } else {
                tmpDir = new File( systemTempDir + File.separator + accession );
            }
            AbstractFetcher.log.warn( "Will use local temporary directory for output: " + tmpDir.getAbsolutePath() );
            newDir = tmpDir;
        }

        //noinspection ConstantConditions // Defensiveness for future changes
        if ( newDir == null ) {
            throw new IOException( "Could not create target directory, was null" );
        }

        FileUtils.forceMkdir( newDir );

        if ( !newDir.canWrite() ) {
            throw new IOException( "Cannot write to target directory " + newDir.getAbsolutePath() );
        }

        return newDir;
    }

    /**
     * @param future       future task
     * @param expectedSize expected size
     * @param outputFile   output file
     * @return true if it finished normally, false if it was cancelled.
     */
    protected boolean waitForDownload( Future<Boolean> future, long expectedSize, File outputFile ) {
        int i = 0;
        long previousSize = 0;
        StopWatch idleTimer = new StopWatch();
        while ( true ) {
            try {
                if ( future.get( AbstractFetcher.INFO_UPDATE_INTERVAL, TimeUnit.MILLISECONDS ) ) {
                    return true;
                } else {
                    throw new RuntimeException( "Downloaded returned false." );
                }
            } catch ( TimeoutException e ) {
                if ( previousSize == outputFile.length() ) {
                    /*
                     * Possibly consider bailing after a while.
                     */
                    if ( idleTimer.getTime() > AbstractFetcher.STALLED_BAIL_TIME_LIMIT ) {
                        AbstractFetcher.log.warn( "Download does not seem to be happening, bailing" );
                        future.cancel( true );
                        return false;
                    }
                    if ( idleTimer.getTime() == 0 )
                        idleTimer.start();
                } else {
                    idleTimer.reset();
                    idleTimer.start();
                }
                //            if ( outputFile.length() >= expectedSize ) {
                //                // no special action, it will finish soon enough.
                //            }
                reportProgress( outputFile, expectedSize, i );
                previousSize = outputFile.length();
                i++;
            } catch ( CancellationException e ) {
                return false;
            } catch ( InterruptedException ie ) {
                AbstractFetcher.log.warn( "Current thread was interrupted, cancelling download...", ie );
                future.cancel( true );
                return false;
            } catch ( ExecutionException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    private void reportProgress( File outputFile, long expectedSize, int i ) {
        /*
         * Avoid logging too much. If we're waiting for a long download, reduce frequency of updates.
         */
        if ( outputFile.length() < expectedSize && (
                i < AbstractFetcher.NUMBER_OF_TIMES_TO_LOG_WAITING_BEFORE_REDUCING_VERBOSITY
                        || i % AbstractFetcher.NUMBER_OF_TIMES_TO_LOG_WAITING_BEFORE_REDUCING_VERBOSITY == 0 ) ) {

            double percent = 100.00 * outputFile.length() / expectedSize;

            // can cause npe error, breaking hot deploy
            if ( AbstractFetcher.log.isInfoEnabled() ) {
                AbstractFetcher.log.info( ( outputFile.length() + ( expectedSize > 0 ? "/" + expectedSize : "" )
                        + " bytes read (" + String.format( "%.1f", percent ) + "%)" ) );
            }
        }
    }
}
