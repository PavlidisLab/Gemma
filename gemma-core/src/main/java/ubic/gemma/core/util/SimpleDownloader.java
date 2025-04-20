package ubic.gemma.core.util;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.LockedPath;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * A simple downloader for FTP and HTTP(s) URLs.
 * @author poirigui
 */
@CommonsLog
public class SimpleDownloader {

    @Nullable
    private final SimpleRetry<IOException> retry;
    /**
     * Factory to use for FTP downloads.
     */
    @Nullable
    private FTPClientFactory ftpClientFactory;
    /**
     * Task executor to use for parallel and async downloads.
     */
    @Nullable
    private ExecutorService taskExecutor;

    /**
     * Set the file lock manager to use to lock files while downloading.
     */
    @Nullable
    private FileLockManager fileLockManager;

    /**
     * If enabled, the integrity of archive (i.e. ZIP, TAR and Gzipped files) will be checked after downloading.
     */
    private boolean checkArchiveIntegrity;

    /**
     * @param retryPolicy policy for retrying failed downloads, or {@code null} to only attempt downloads once
     */
    public SimpleDownloader( @Nullable SimpleRetryPolicy retryPolicy ) {
        this.retry = retryPolicy != null ? new SimpleRetry<>( retryPolicy, IOException.class, SimpleDownloader.class.getName() ) : null;
    }

    /**
     * Set the factory for creating FTP clients.
     */
    public void setFtpClientFactory( @Nullable FTPClientFactory ftpClientFactory ) {
        this.ftpClientFactory = ftpClientFactory;
    }

    /**
     * Set the task executor for performing asynchronous downloads.
     */
    public void setTaskExecutor( @Nullable ExecutorService taskExecutor ) {
        this.taskExecutor = taskExecutor;
    }

    public void setFileLockManager( @Nullable FileLockManager fileLockManager ) {
        this.fileLockManager = fileLockManager;
    }

    /**
     * Set whether to check the integrity of gzipped files after downloading.
     */
    public void setCheckArchiveIntegrity( boolean checkArchiveIntegrity ) {
        this.checkArchiveIntegrity = checkArchiveIntegrity;
    }

    @Value
    public static class URLAndDestination {
        URL url;
        Path dest;
    }

    /**
     * Download a file from a URL to a local destination asynchronously.
     */
    public long downloadInParallel( List<URLAndDestination> urls2dest, boolean force ) throws IOException, InterruptedException {
        Assert.notNull( taskExecutor, "A task executor must be set to perform parallel downloads." );
        List<Future<Long>> futures = urls2dest.stream()
                .map( entry -> downloadAsync( entry.getUrl(), entry.getDest(), force ) )
                .collect( Collectors.toList() );
        long downloadedBytes = 0;
        List<ExecutionException> errors = new ArrayList<>();
        for ( int i = 0; i < futures.size(); i++ ) {
            URLAndDestination u2d = urls2dest.get( i );
            Future<Long> future = futures.get( i );
            try {
                downloadedBytes += future.get();
            } catch ( InterruptedException e ) {
                // cancel remaining downloads
                for ( int j = i; j < futures.size(); j++ ) {
                    futures.get( j ).cancel( true );
                }
                throw e;
            } catch ( ExecutionException e ) {
                log.warn( String.format( "An error occurred while downloading %s to %s, will wait for other downloads to finish...", u2d.url, u2d.dest ), e );
                errors.add( e );
            }
        }
        Iterator<ExecutionException> it = errors.iterator();
        if ( it.hasNext() ) {
            Throwable e = it.next().getCause();
            it.forEachRemaining( e2 -> {
                e.addSuppressed( e2.getCause() );
            } );
            if ( e instanceof IOException ) {
                throw ( IOException ) e.getCause();
            } else if ( e instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        }
        return downloadedBytes;
    }

    /**
     * Download a file from a URL to a local destination asynchronously.
     */
    public Future<Long> downloadAsync( URL url, Path dest, boolean force ) {
        Assert.notNull( taskExecutor, "A task executor must be set to perform asynchronous downloads." );
        return taskExecutor.submit( () -> download( url, dest, force ) );
    }

    /**
     * Download a file from a URL to a local destination.
     * @param url   URL to download
     * @param dest  destination for the download
     * @param force download even if the file exists, has the same size and is up to date
     * @return the number of bytes that were downloaded
     */
    public long download( URL url, Path dest, boolean force ) throws IOException {
        if ( retry != null ) {
            return retry.execute( ( ctx ) -> downloadWithLock( url, dest, force ),
                    "download " + url + " to " + dest );
        } else {
            return downloadWithLock( url, dest, force );
        }
    }

    private long downloadWithLock( URL url, Path dest, boolean force ) throws IOException {
        if ( fileLockManager != null ) {
            try ( LockedPath lock = fileLockManager.acquirePathLock( dest, true ) ) {
                return downloadMultiProtocol( url, lock.getPath(), force );
            }
        } else {
            return downloadMultiProtocol( url, dest, force );
        }
    }

    private long downloadMultiProtocol( URL url, Path dest, boolean force ) throws IOException {
        if ( url.getProtocol().equalsIgnoreCase( "ftp" ) ) {
            return downloadFtp( url, dest, force );
        } else {
            return downloadHttp( url, dest, force );
        }
    }

    private long downloadFtp( URL url, Path dest, boolean force ) throws IOException {
        Assert.notNull( ftpClientFactory, "A FTPClientFactory must be set to download files from FTP servers." );
        FTPClient client = ftpClientFactory.getFtpClient( url );
        try {
            String remoteFile = url.getFile();
            boolean download;
            // check if downloading is necessary
            // check size and last modified
            FTPFile ftpFile = client.mlistFile( remoteFile );
            long remoteFileSize = ftpFile.getSize();
            if ( force ) {
                log.info( "Force download requested, downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( !Files.exists( dest ) ) {
                log.info( "File does not exist, downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( remoteFileSize != -1 && ftpFile.getSize() != Files.size( dest ) ) {
                log.info( "File differ in size, re-downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( ftpFile.getTimestampInstant().isAfter( Files.getLastModifiedTime( dest ).toInstant() ) ) {
                log.info( "File is newer, re-downloading " + url + " to " + dest + "." );
                download = true;
            } else {
                log.info( "Skipping download of " + url + " to " + dest + ": same size and modification time." );
                download = false;
            }
            long downloadedBytes;
            if ( download ) {
                PathUtils.createParentDirectories( dest );
                try {
                    CopyStreamListener previousCSL = client.getCopyStreamListener();
                    try ( OutputStream out = Files.newOutputStream( dest ) ) {
                        ProgressReporter pr = new ProgressReporter( url.toString(), SimpleDownloader.class.getName() );
                        client.setCopyStreamListener( new CopyStreamListener() {
                            @Override
                            public void bytesTransferred( CopyStreamEvent event ) {
                            }

                            @Override
                            public void bytesTransferred( long totalBytesTransferred, int bytesTransferred, long streamSize ) {
                                pr.reportProgress( totalBytesTransferred, streamSize );
                            }
                        } );
                        if ( !client.retrieveFile( remoteFile, out ) ) {
                            throw new IOException( client.getReplyString() );
                        }
                    } finally {
                        client.setCopyStreamListener( previousCSL );
                    }
                    checkDownloadedFile( dest, remoteFileSize );
                    downloadedBytes = Files.size( dest );
                } catch ( Exception e ) {
                    if ( Files.exists( dest ) ) {
                        log.warn( "An error occurred while downloading " + url + ", deleting " + dest + ".", e );
                        Files.delete( dest );
                    }
                    throw e;
                }
            } else {
                downloadedBytes = 0;
            }
            ftpClientFactory.recycleClient( url, client );
            return downloadedBytes;
        } catch ( Exception e ) {
            ftpClientFactory.destroyClient( url, client );
            throw e;
        }
    }

    private long downloadHttp( URL url, Path dest, boolean force ) throws IOException {
        URLConnection connection = url.openConnection();
        if ( connection instanceof HttpURLConnection ) {
            ( ( HttpURLConnection ) connection ).setInstanceFollowRedirects( true );
        }
        connection.connect();
        try {
            long size = connection.getContentLengthLong();
            boolean download;
            if ( force ) {
                log.info( "Force download requested, downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( !Files.exists( dest ) ) {
                log.info( "File does not exist, downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( size != -1 && size != Files.size( dest ) ) {
                log.info( "File differ in size, re-downloading " + url + " to " + dest + "." );
                download = true;
            } else if ( connection.getLastModified() > Files.getLastModifiedTime( dest ).toMillis() ) {
                log.info( "File is newer, re-downloading " + url + " to " + dest + "." );
                download = true;
            } else {
                log.info( "Skipping download of " + url + " to " + dest + ": same size and modification time." );
                download = false;
            }
            if ( download ) {
                PathUtils.createParentDirectories( dest );
                try {
                    long downloadedBytes;
                    try ( InputStream in = new ProgressInputStream( connection.getInputStream(), url.toString(), SimpleDownloader.class.getName(), size );
                            OutputStream out = Files.newOutputStream( dest ) ) {
                        downloadedBytes = IOUtils.copyLarge( in, out );
                    }
                    checkDownloadedFile( dest, size );
                    return downloadedBytes;
                } catch ( Exception e ) {
                    if ( Files.exists( dest ) ) {
                        log.warn( "An error occurred while downloading " + url + ", deleting " + dest + ".", e );
                        Files.delete( dest );
                    }
                    throw e;
                }
            } else {
                return 0;
            }
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
    }

    /**
     * TODO: implement more checks, as well as checksums if available.
     */
    private void checkDownloadedFile( Path dest, long expectedSize ) throws IOException {
        if ( expectedSize != -1 && expectedSize != Files.size( dest ) ) {
            log.warn( "Downloaded file " + dest + " has size " + Files.size( dest ) + ", expected " + expectedSize );
            throw new IOException( "Downloaded file " + dest + " has size " + Files.size( dest ) + ", expected " + expectedSize );
        }
        if ( checkArchiveIntegrity ) {
            long decompressedSize;
            if ( dest.toString().toLowerCase().endsWith( ".tar.gz" ) ) {
                try ( TarArchiveInputStream tis = new TarArchiveInputStream( new GZIPInputStream( Files.newInputStream( dest ) ) ) ) {
                    decompressedSize = tis.skip( Long.MAX_VALUE );
                }
            } else if ( dest.toString().toLowerCase().endsWith( ".gz" ) ) {
                try ( GZIPInputStream in = new GZIPInputStream( Files.newInputStream( dest ) ) ) {
                    decompressedSize = in.skip( Long.MAX_VALUE );
                }
            } else if ( dest.toString().toLowerCase().endsWith( ".zip" ) ) {
                try ( ZipArchiveInputStream is = new ZipArchiveInputStream( Files.newInputStream( dest ) ) ) {
                    decompressedSize = is.skip( Long.MAX_VALUE );
                }
            } else {
                return;
            }
            log.info( String.format( "Archive file %s appears to be correct, it has a decompressed size of %s.",
                    dest, byteCountToDisplaySize( decompressedSize ) ) );
        }
    }
}
