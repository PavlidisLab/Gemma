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

    public void setFtpClientFactory( FTPClientFactory ftpClientFactory ) {
        simpleDownloader.setFtpClientFactory( ftpClientFactory );
    }

    public void setFileLockManager( FileLockManager fileLockManager ) {
        simpleDownloader.setFileLockManager( fileLockManager );
    }

    public void setProgressReporterFactory( ProgressReporterFactory progressReporterFactory ) {
        this.simpleDownloader.setProgressReporterFactory( progressReporterFactory );
    }

    public void setTaskExecutor( ExecutorService taskExecutor ) {
        this.simpleDownloader.setTaskExecutor( taskExecutor );
    }

}
