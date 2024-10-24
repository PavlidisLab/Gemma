package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

/**
 * Simple username/password authenticator.
 * @author poirigui
 */
public class UsernamePasswordFTPClientAuthenticator extends AbstractFTPClientAuthenticator {

    private final String username;
    private final String password;

    public UsernamePasswordFTPClientAuthenticator( String username, String password ) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void authenticate( FTPClient client, String host ) throws IOException {
        authenticate( client, host, username, password );
    }
}
