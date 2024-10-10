package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

public class AnonymousFTPCLientAuthenticator extends AbstractFTPClientAuthenticator {

    @Override
    public void authenticate( FTPClient client, String host ) throws IOException {
        authenticate( client, host, "anonymous", "" );
    }
}
