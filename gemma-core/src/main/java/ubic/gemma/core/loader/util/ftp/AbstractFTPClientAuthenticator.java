package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

public abstract class AbstractFTPClientAuthenticator implements FTPClientAuthenticator {

    protected final Log log = LogFactory.getLog( getClass() );

    protected void authenticate( FTPClient client, String host, String username, String password ) throws IOException {
        client.login( username, password );
        log.debug( client.getReplyString() );
        if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
            throw new IOException( String.format( "Failed to authenticate to %s as %s %s: %s",
                    host,
                    username,
                    StringUtils.isNotBlank( password ) ? "with password" : "without password",
                    client.getReplyString() ) );
        }
    }
}
