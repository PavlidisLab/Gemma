package ubic.gemma.web.controller.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for providing {@link Path} as downloads.
 * <p>
 * If available, Tomcat sendfile is used.
 * @author poirigui
 */
@Component
@CommonsLog
public class DownloadUtil {

    @Value("${tomcat.sendfile.enabled}")
    private boolean enableTomcatSendfile;

    /**
     * Provide a file download to the response.
     * <p>
     * If available, Tomcat sendfile capability is used.
     * <p>
     * If the file is not readable, a 404 error will be sent to the response.
     *
     * @param file                 the file to download from
     * @param contentType          content type of the file to download, e.g. "application/pdf" or "text/plain", if the
     *                             file is compressed, you may either serve it as-is using  "application/octet-stream"
     *                             or transparently decompressed by the client by setting contentEncoding appropriately.
     * @param contentEncoding      content encoding of the file to download, set this only to provide on-the-fly
     *                             decompression by the client.
     * @param downloadAsAttachment if true, the file will be downloaded as an attachment, otherwise it will be served
     *                             directly
     * @param downloadName         this string will be used as a download name for the downloaded file. If null, the
     *                             filesystem name of the file will be used. This does nothing if downloadAsAttachment
     *                             is false. It is not escaped, so you should make sure that the filename is safe to
     *                             use in an HTTP header.
     * @throws IOException if an error occurred during the file transmission
     */
    public void download( Path file, String contentType, @Nullable String contentEncoding, boolean downloadAsAttachment, @Nullable String downloadName,
            HttpServletRequest request, HttpServletResponse response ) throws IOException {
        if ( !Files.isReadable( file ) ) {
            response.sendError( HttpServletResponse.SC_NOT_FOUND, "The requested file was not found." );
            return;
        }
        long fileSize = Files.size( file );
        response.setContentType( contentType );
        response.setContentLengthLong( fileSize );
        if ( contentEncoding != null ) {
            response.addHeader( "Content-Encoding", contentEncoding );
        }
        if ( downloadAsAttachment ) {
            if ( StringUtils.isBlank( downloadName ) ) {
                downloadName = file.getFileName().toString();
            }
            response.addHeader( "Content-Disposition", "attachment; filename=\"" + downloadName + "\"" );
        }
        if ( enableTomcatSendfile ) {
            if ( Boolean.TRUE.equals( request.getAttribute( "org.apache.tomcat.sendfile.support" ) ) ) {
                downloadViaSendfile( file, request, fileSize );
            } else {
                log.warn( "Tomcat sendfile is not supported for this request. Falling back to stream download." );
            }
        }
        downloadViaStream( file, response );
    }

    /**
     * Uses Tomcat sendfile to download the file.
     */
    private void downloadViaSendfile( Path f, HttpServletRequest request, long fileSize ) throws IOException {
        request.setAttribute( "org.apache.tomcat.sendfile.filename", f.toString() );
        request.setAttribute( "org.apache.tomcat.sendfile.start", ( long ) 0 );
        request.setAttribute( "org.apache.tomcat.sendfile.end", fileSize );
    }

    private void downloadViaStream( Path f, HttpServletResponse response ) throws IOException {
        try ( InputStream in = Files.newInputStream( f ) ) {
            IOUtils.copyLarge( in, response.getOutputStream() );
            response.flushBuffer();
        }
    }
}
