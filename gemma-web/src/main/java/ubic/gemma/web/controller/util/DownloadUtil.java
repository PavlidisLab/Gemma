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
     * @param f                    the file to download from
     * @param downloadName         this string will be used as a download name for the downloaded file. If null, the filesystem name
     *                             of the file will be used.
     * @param response             the http response to download to.
     * @throws IOException if the file in the given path can not be read.
     */
    public void download( Path f, @Nullable String downloadName, String contentType, HttpServletRequest request, HttpServletResponse response, boolean downloadAsAttachment ) throws IOException {
        long fileSize = Files.size( f );
        response.setContentType( contentType );
        response.setContentLengthLong( fileSize );
        if ( downloadAsAttachment ) {
            if ( StringUtils.isBlank( downloadName ) ) {
                downloadName = f.getFileName().toString();
            }
            response.addHeader( "Content-Disposition", "attachment; filename=\"" + downloadName + "\"" );
        }
        if ( enableTomcatSendfile ) {
            if ( Boolean.TRUE.equals( request.getAttribute( "org.apache.tomcat.sendfile.support" ) ) ) {
                downloadViaSendfile( f, request, response, fileSize );
            } else {
                log.warn( "Tomcat sendfile is not supported for this request. Falling back to stream download." );
            }
        }
        downloadViaStream( f, response );
    }

    /**
     * Uses Tomcat sendfile to download the file.
     */
    private void downloadViaSendfile( Path f, HttpServletRequest request, HttpServletResponse response, long fileSize ) throws IOException {
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
