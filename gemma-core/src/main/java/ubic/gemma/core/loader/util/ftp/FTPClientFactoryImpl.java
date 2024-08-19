package ubic.gemma.core.loader.util.ftp;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This implementation integrates with various configured FTP servers from the {@code project.properties} file. If the
 * requested resource is not hosted there, it will gracefully fall back to an anonymous login.
 * @author poirigui
 */
@CommonsLog
@Component
public class FTPClientFactoryImpl implements FTPClientFactory, AutoCloseable {

    /**
     * Maximum number of idle connections kept in the pool, per host.
     */
    private static final int MAX_IDLE_CONNECTIONS = 4;

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

    /**
     * Pools of FTP clients, one per authority.
     */
    private final ConcurrentMap<String, GenericObjectPool<FTPClient>> clientsPool = new ConcurrentHashMap<>();

    @Override
    public void close() {
        for ( GenericObjectPool<FTPClient> pool : clientsPool.values() ) {
            pool.close();
        }
    }

    @Override
    public FTPClient getFtpClient( URL url ) throws IOException {
        Assert.isTrue( url.getProtocol().equals( "ftp" ), "URL protocol must be FTP." );
        try {
            return getPool( url ).borrowObject();
        } catch ( IOException | RuntimeException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void destroyClient( URL url, FTPClient client ) {
        try {
            getPool( url ).invalidateObject( client );
        } catch ( Exception e ) {
            log.error( "Failed to destroy FTP client for " + url + ".", e );
        }
    }

    @Override
    public void recycleClient( URL url, FTPClient client ) {
        Assert.isTrue( url.getProtocol().equals( "ftp" ), "URL protocol must be FTP." );
        getPool( url ).returnObject( client );
    }

    @Override
    public InputStream openStream( URL url ) throws IOException {
        FTPClient client = getFtpClient( url );
        InputStream s = client.retrieveFileStream( url.getPath() );
        if ( s == null ) {
            String reply = client.getReplyString();
            destroyClient( url, client );
            throw new IOException( String.format( "Failed to retrieve %s: %s", url, reply ) );
        }
        return new FilterInputStream( s ) {

            private boolean eofReached = false;

            @Override
            public int read() throws IOException {
                int ret = super.read();
                if ( ret == -1 ) {
                    eofReached = true;
                }
                return ret;
            }

            @Override
            public int read( byte[] b ) throws IOException {
                int ret = super.read( b );
                if ( ret == -1 ) {
                    eofReached = true;
                }
                return ret;
            }

            @Override
            public int read( byte[] b, int off, int len ) throws IOException {
                int ret = super.read( b, off, len );
                if ( ret == -1 ) {
                    eofReached = true;
                }
                return ret;
            }

            @Override
            public void close() throws IOException {
                if ( !eofReached ) {
                    // If EOF is not reached, we face two options: read it until the end or close the connection
                    // immediately. We don't know how much data is left in the stream, so it's almost certainly cheaper
                    // to open a new FTP connection.
                    log.debug( "FTP data connection was not read until EOF, cannot recycle, will destroy instead." );
                    try {
                        super.close();
                    } finally {
                        destroyClient( url, client );
                    }
                    return;
                }
                client.completePendingCommand();
                if ( FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                    recycleClient( url, client );
                } else {
                    String reply = client.getReplyString();
                    destroyClient( url, client );
                    throw new IOException( String.format( "Failed to retrieve %s: %s", url, reply ) );
                }
            }
        };
    }

    private GenericObjectPool<FTPClient> getPool( URL url ) {
        return clientsPool.computeIfAbsent( url.getAuthority(), k -> {
            GenericObjectPool<FTPClient> pool = new GenericObjectPool<>( new FTPClientPooledObjectFactory( url ) );
            pool.setMaxIdle( MAX_IDLE_CONNECTIONS );
            // always check if an FTP connection is still valid when borrowing because FTP servers tend to close
            // inactive connections pretty aggressively.
            pool.setTestOnBorrow( true );
            return pool;
        } );
    }

    private static final class FTPClientPooledObject extends DefaultPooledObject<FTPClient> {

        @Nullable
        private String loggedInAs;

        public FTPClientPooledObject( FTPClient object ) {
            super( object );
        }

        @Nullable
        public String getLoggedInAs() {
            return loggedInAs;
        }

        public void setLoggedInAs( @Nullable String loggedInAs ) {
            this.loggedInAs = loggedInAs;
        }
    }

    private class FTPClientPooledObjectFactory extends BasePooledObjectFactory<FTPClient> {

        private final String authority;
        private final String host;
        private final int port;
        private final String username, password;
        private final String path;

        private FTPClientPooledObjectFactory( URL url ) {
            this.authority = url.getAuthority();
            this.host = url.getHost();
            this.port = url.getPort();
            if ( url.getUserInfo() != null ) {
                String[] userInfo = url.getUserInfo().split( ":", 2 );
                if ( userInfo.length != 2 ) {
                    throw new IllegalArgumentException( String.format( "The userinfo of %s does not have a ':' delimiter for credentials.", url ) );
                }
                username = userInfo[0];
                password = userInfo[1];
            } else if ( url.getHost().equals( ncbiHost ) ) {
                username = ncbiUser;
                password = ncbiPassword;
            } else if ( url.getHost().equals( geoHost ) ) {
                username = geoUser;
                password = geoPassword;
            } else if ( url.getHost().equals( arrayExpressHost ) ) {
                username = arrayExpressUser;
                password = arrayExpressPassword;
            } else {
                log.warn( String.format( "Supplementary file %s is not hosted on a known FTP server, using anonymous login.", url ) );
                username = "anonymous";
                password = "";
            }
            this.path = url.getPath();
        }

        @Override
        public FTPClient create() throws Exception {
            FTPClient client = new FTPClient();
            client.setReceiveBufferSize( 33554432 );
            client.setReceieveDataSocketBufferSize( 33554432 );
            return client;
        }

        @Override
        public PooledObject<FTPClient> wrap( FTPClient ftpClient ) {
            return new FTPClientPooledObject( ftpClient );
        }

        @Override
        public boolean validateObject( PooledObject<FTPClient> p ) {
            FTPClient client = p.getObject();
            if ( !client.isConnected() ) {
                return false;
            }
            try {
                int replyCode = client.noop();
                log.debug( client.getReplyString() );
                return FTPReply.isPositiveCompletion( replyCode );
            } catch ( IOException e ) {
                return false;
            }
        }

        @Override
        public void activateObject( PooledObject<FTPClient> p ) throws Exception {
            FTPClient client = p.getObject();
            if ( !client.isConnected() ) {
                if ( port != -1 ) {
                    client.connect( host, port );
                } else {
                    client.connect( host );
                }
                log.debug( client.getReplyString() );
                if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                    throw new IOException( String.format( "Failed to connect to %s: %s", authority, client.getReplyString() ) );
                }
            }
            if ( !username.equals( ( ( FTPClientPooledObject ) p ).getLoggedInAs() ) ) {
                // if already logged in as another user, logout first
                if ( ( ( FTPClientPooledObject ) p ).getLoggedInAs() != null ) {
                    client.logout();
                    log.debug( client.getReplyString() );
                    if ( FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                        ( ( FTPClientPooledObject ) p ).setLoggedInAs( null );
                    } else {
                        throw new IOException( String.format( "Failed to logout from %s as %s: %s",
                                authority,
                                ( ( FTPClientPooledObject ) p ).getLoggedInAs(),
                                client.getReplyString() ) );
                    }
                }
                client.login( username, password );
                log.debug( client.getReplyString() );
                if ( FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                    ( ( FTPClientPooledObject ) p ).setLoggedInAs( username );
                } else {
                    throw new IOException( String.format( "Failed to authenticate to %s as %s %s: %s",
                            authority,
                            username,
                            StringUtils.isNotBlank( password ) ? "with password" : "without password",
                            client.getReplyString() ) );
                }
            }
            // recommended settings as per https://ftp.ncbi.nlm.nih.gov/README.ftp
            client.enterLocalPassiveMode();
            client.setFileType( FTP.BINARY_FILE_TYPE );
            if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                throw new IOException( String.format( "Failed to set file type to BINARY for %s from %s: %s",
                        path, authority, client.getReplyString() ) );
            }
        }

        @Override
        public void destroyObject( PooledObject<FTPClient> p ) throws Exception {
            FTPClient client = p.getObject();
            if ( !client.isConnected() ) {
                return;
            }
            if ( ( ( FTPClientPooledObject ) p ).getLoggedInAs() != null ) {
                try {
                    client.logout();
                } catch ( IOException e ) {
                    // ignore, we're going to disconnect anyway
                }
            }
            client.disconnect();
        }
    }
}
