package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

public interface FTPClientAuthenticator {

    void authenticate( FTPClient client, String host ) throws IOException;
}
