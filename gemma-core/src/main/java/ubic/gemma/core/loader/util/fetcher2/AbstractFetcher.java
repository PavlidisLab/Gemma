package ubic.gemma.core.loader.util.fetcher2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.ProgressReporterFactory;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.locking.FileLockManager;

import java.util.concurrent.ExecutorService;

/**
 * Base class for fetchers that provide data downloading, file locking, progress reporting, logging and parallel task
 * execution capabilities.
 *
 * @author poirigui
 */
public abstract class AbstractFetcher {

    protected final Log log = LogFactory.getLog( this.getClass() );

    protected final SimpleDownloader simpleDownloader;

    protected AbstractFetcher( SimpleDownloader simpleDownloader ) {
        this.simpleDownloader = simpleDownloader;
    }

    /**
     * Set the FTP client factory to use for downloading files over FTP.
     *
     * @see SimpleDownloader#setFtpClientFactory(FTPClientFactory)
     */
    public void setFtpClientFactory( FTPClientFactory ftpClientFactory ) {
        simpleDownloader.setFtpClientFactory( ftpClientFactory );
    }

    /**
     * Set the file lock manager to use for managing locks on downloaded files.
     *
     * @see SimpleDownloader#setFileLockManager(FileLockManager)
     */
    public void setFileLockManager( FileLockManager fileLockManager ) {
        simpleDownloader.setFileLockManager( fileLockManager );
    }

    /**
     * Set the progress reporter factory to use for reporting download progress.
     *
     * @see SimpleDownloader#setProgressReporterFactory(ProgressReporterFactory)
     */
    public void setProgressReporterFactory( ProgressReporterFactory progressReporterFactory ) {
        simpleDownloader.setProgressReporterFactory( progressReporterFactory );
    }

    /**
     * Set the task executor for parallel downloads.
     *
     * @see SimpleDownloader#setTaskExecutor(ExecutorService)
     */
    public void setTaskExecutor( ExecutorService taskExecutor ) {
        simpleDownloader.setTaskExecutor( taskExecutor );
    }
}
