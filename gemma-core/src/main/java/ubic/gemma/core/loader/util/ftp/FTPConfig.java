package ubic.gemma.core.loader.util.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FTPConfig {

    @Value("${ncbi.host}")
    private String ncbiHost;
    @Value("${ncbi.user}")
    private String ncbiUser;
    @Value("${ncbi.password}")
    private String ncbiPassword;

    @Value("${geo.host}")
    private String geoHost;
    @Value("${geo.user}")
    private String geoUser;
    @Value("${geo.password}")
    private String geoPassword;

    @Value("${arrayExpress.host}")
    private String arrayExpressHost;
    @Value("${arrayExpress.user}")
    private String arrayExpressUser;
    @Value("${arrayExpress.password}")
    private String arrayExpressPassword;

    @Bean
    public FTPClientFactory ftpClientFactory() {
        FTPClientFactoryImpl factory = new FTPClientFactoryImpl();
        factory.setAuthenticator( new FTPClientAuthenticatorImpl() );
        return factory;
    }

    /**
     * This implementation integrates with various configured FTP servers from the {@code project.properties} file. If the
     * requested resource is not hosted there, it will gracefully fall back to an anonymous login.
     * @author poirigui
     */
    private class FTPClientAuthenticatorImpl extends AbstractFTPClientAuthenticator {

        @Override
        public void authenticate( FTPClient client, String host ) throws IOException {
            String username, password;
            if ( host.equals( ncbiHost ) ) {
                username = ncbiUser;
                password = ncbiPassword;
            } else if ( host.equals( geoHost ) ) {
                username = geoUser;
                password = geoPassword;
            } else if ( host.equals( arrayExpressHost ) ) {
                username = arrayExpressUser;
                password = arrayExpressPassword;
            } else {
                log.warn( String.format( "%s is not a known FTP server, using anonymous login.", host ) );
                username = "anonymous";
                password = "";
            }
            authenticate( client, host, username, password );
        }
    }
}
