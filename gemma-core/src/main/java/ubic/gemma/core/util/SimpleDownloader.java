package ubic.gemma.core.util;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * A simple downloader for FTP and HTTP(s) URLs.
 * @author poirigui
 */
@CommonsLog
public class SimpleDownloader {

    private final SimpleRetry<IOException> retry;
    private final FTPClientFactory ftpClientFactory;
    private final ExecutorService taskExecutor;

    /**
     * @param retryPolicy      policy for retrying failed downloads
     * @param ftpClientFactory factory for creating FTP clients
     */
    public SimpleDownloader( SimpleRetryPolicy retryPolicy, FTPClientFactory ftpClientFactory, ExecutorService taskExecutor ) {
        this.retry = new SimpleRetry<>( retryPolicy, IOException.class, getClass().getName() );
        this.ftpClientFactory = ftpClientFactory;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Download a file from a URL to a local destination.
     * @param url   URL to download
     * @param dest  destination for the download
     * @param force download even if the file exists, has the same size and is up to date
     * @return the number of bytes that were downloaded
     */
    public long download( URL url, Path dest, boolean force ) throws IOException, InterruptedException {
        try {
            return downloadAsync( url, dest, force ).get();
        } catch ( ExecutionException e ) {
            if ( e.getCause() instanceof IOException ) {
                throw ( IOException ) e.getCause();
            } else if ( e.getCause() instanceof RuntimeException ) {
                throw ( RuntimeException ) e.getCause();
            } else {
                throw new RuntimeException( e.getCause() );
            }
        }
    }

    @Value
    public static class URLAndDestination {
        URL url;
        Path dest;
    }

    public void downloadInParallel( List<URLAndDestination> urls2dest, boolean force ) throws IOException, InterruptedException {
        List<Future<?>> futures = urls2dest.stream()
                .map( entry -> downloadAsync( entry.getUrl(), entry.getDest(), force ) )
                .collect( Collectors.toList() );
        for ( Future<?> future : futures ) {
            try {
                future.get();
            } catch ( ExecutionException e ) {
                if ( e.getCause() instanceof IOException ) {
                    throw ( IOException ) e.getCause();
                } else if ( e.getCause() instanceof RuntimeException ) {
                    throw ( RuntimeException ) e.getCause();
                } else {
                    throw new RuntimeException( e.getCause() );
                }
            }
        }
    }

    public Future<Long> downloadAsync( URL url, Path dest, boolean force ) {
        return taskExecutor.submit( () -> retry.execute( ( ctx ) -> {
            if ( url.getProtocol().equalsIgnoreCase( "ftp" ) ) {
                return downloadFtp( url, dest, force );
            } else {
                return downloadHttp( url, dest, force );
            }
        }, "download " + url + " to " + dest ) );
    }

    private long downloadFtp( URL url, Path dest, boolean force ) throws IOException {
        FTPClient client = ftpClientFactory.getFtpClient( url );
        try {
            String remoteFile = url.getFile();
            boolean download;
            if ( !force && Files.exists( dest ) ) {
                // check if downloading is necessary
                // check size and last modified
                FTPFile ftpFile = client.mlistFile( remoteFile );
                if ( ftpFile.getSize() != -1 && ftpFile.getSize() != Files.size( dest ) ) {
                    log.info( "File differ in size, re-downloading." );
                    download = true;
                } else if ( ftpFile.getTimestampInstant().isAfter( Files.getLastModifiedTime( dest ).toInstant() ) ) {
                    log.info( "File is newer, re-downloading." );
                    download = true;
                } else {
                    log.info( "Skipping download of " + url + " to " + dest + ": same size and modification time." );
                    download = false;
                }
            } else {
                download = true;
            }
            long downloadedBytes;
            if ( download ) {
                PathUtils.createParentDirectories( dest );
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
                downloadedBytes = Files.size( dest );
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
            boolean download;
            if ( !force && Files.exists( dest ) ) {
                long size = connection.getContentLengthLong();
                long lastModified = connection.getLastModified();
                if ( size != -1 && size != Files.size( dest ) ) {
                    log.info( "File differ in size, re-downloading." );
                    download = true;
                } else if ( lastModified > Files.getLastModifiedTime( dest ).toMillis() ) {
                    log.info( "File is newer, re-downloading." );
                    download = true;
                } else {
                    log.info( "Skipping download of " + url + " to " + dest + ": same size and modification time." );
                    download = false;
                }
            } else {
                download = true;
            }
            if ( download ) {
                PathUtils.createParentDirectories( dest );
                try ( InputStream in = new ProgressInputStream( connection.getInputStream(), url.toString(), SimpleDownloader.class.getName(), connection.getContentLengthLong() );
                        OutputStream out = Files.newOutputStream( dest ) ) {
                    return IOUtils.copyLarge( in, out );
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
}
