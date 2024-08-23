package ubic.gemma.core.loader.util.ftp;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This implementation maintains one pool of FTP clients per URL authority (i.e {@link URL#getAuthority()}).
 * <p>
 * Anonymous authentication is used by default.
 * @author poirigui
 */
@CommonsLog
public class FTPClientFactoryImpl implements FTPClientFactory, AutoCloseable {

    /**
     * Maximum number of idle connections kept in the pool, per host.
     */
    private int maxIdleConnections = 4;
    private int maxTotalConnections = 16;
    private FTPClientAuthenticator authenticator = new AnonymousFTPCLientAuthenticator();

    /**
     * Pools of FTP clients, one per authority.
     */
    private final ConcurrentMap<String, GenericObjectPool<FTPClient>> clientsPool = new ConcurrentHashMap<>();

    @Override
    public void setMaxIdleConnections( int maxIdle ) {
        this.maxIdleConnections = maxIdle;
        clientsPool.values().forEach( pool -> pool.setMaxIdle( maxIdle ) );
    }

    @Override
    public void setMaxTotalConnections( int maxTotal ) {
        this.maxTotalConnections = maxTotal;
        clientsPool.values().forEach( pool -> pool.setMaxTotal( maxTotal ) );
    }

    @Override
    public void setAuthenticator( @Nullable FTPClientAuthenticator authenticator ) {
        this.authenticator = authenticator;
    }

    @Override
    public void close() {
        for ( GenericObjectPool<FTPClient> pool : clientsPool.values() ) {
            pool.close();
        }
    }

    @Override
    public FTPClient getFtpClient( URL url ) throws IOException {
        try {
            FTPClient client = getPool( url ).borrowObject();
            if ( url.getUserInfo() != null ) {
                log.debug( "Applying authentication from URL userinfo..." );
                try {
                    String[] userInfo = url.getUserInfo().split( ":", 2 );
                    if ( userInfo.length != 2 ) {
                        throw new IllegalArgumentException( String.format( "The userinfo of %s does not have a ':' delimiter for credentials.", url ) );
                    }
                    String username = userInfo[0];
                    String password = userInfo[1];
                    new UsernamePasswordFTPClientAuthenticator( username, password )
                            .authenticate( client, url.getHost() );
                } catch ( Exception e ) {
                    destroyClient( url, client );
                    throw e;
                }
            }
            return client;
        } catch ( IOException | RuntimeException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to borrow FTP client for " + url + ".", e );
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
    public void abandonClient( URL url, FTPClient client ) {
        try {
            getPool( url ).invalidateObject( client, DestroyMode.ABANDONED );
        } catch ( Exception e ) {
            log.error( "Failed to abandon FTP client for " + url + ".", e );
        }
    }

    @Override
    public void recycleClient( URL url, FTPClient client ) {
        getPool( url ).returnObject( client );
    }

    @Override
    public InputStream openStream( URL url ) throws IOException {
        FTPClient client = getFtpClient( url );
        InputStream s;
        try {
            s = client.retrieveFileStream( url.getPath() );
            log.debug( client.getReplyString() );
            if ( s == null ) {
                String reply = client.getReplyString();
                throw new IOException( String.format( "Failed to retrieve %s: %s", url, reply ) );
            }
            return new FTPClientInputStream( client, url, s );
        } catch ( Exception e ) {
            destroyClient( url, client );
            throw e;
        }
    }

    private class FTPClientInputStream extends FilterInputStream {

        private final FTPClient client;
        private final URL url;
        private boolean reachedEof = false;

        private FTPClientInputStream( FTPClient client, URL url, InputStream is ) {
            super( is );
            this.client = client;
            this.url = url;
        }

        @Override
        public int read() throws IOException {
            int ret = super.read();
            reachedEof = ret == -1;
            return ret;
        }

        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            int ret = super.read( b, off, len );
            reachedEof = ret == -1;
            return ret;
        }

        @Override
        public void close() throws IOException {
            try {
                if ( reachedEof ) {
                    client.completePendingCommand();
                    log.debug( client.getReplyString() );
                    if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                        throw new IOException( String.format( "Failed to retrieve %s: %s", url, client.getReplyString() ) );
                    }
                    recycleClient( url, client );
                } else {
                    // EOF hasn't been reached, the FTP client will block for completePendingCommand(), so it's just
                    // more efficient to abandon it and open a new connection
                    log.debug( "Stream was partially consumed for " + url + ", the client will be abandoned and disconnected immediately." );
                    abandonClient( url, client );
                }
            } catch ( FTPConnectionClosedException e ) {
                // no need to rethrow this exception
                destroyClient( url, client );
            } catch ( Exception e ) {
                destroyClient( url, client );
                throw e;
            } finally {
                super.close();
            }
        }
    }

    private GenericObjectPool<FTPClient> getPool( URL url ) {
        Assert.isTrue( url.getProtocol().equals( "ftp" ), "URL protocol must be FTP." );
        return clientsPool.computeIfAbsent( url.getAuthority(), k -> createPool( url ) );
    }

    private GenericObjectPool<FTPClient> createPool( URL url ) {
        GenericObjectPool<FTPClient> pool = new GenericObjectPool<>( new FTPClientPooledObjectFactory( url.getAuthority(), url.getHost(), url.getPort(), authenticator ) );
        pool.setMaxIdle( maxIdleConnections );
        pool.setMaxTotal( maxTotalConnections );
        // always check if an FTP connection is still valid when borrowing because FTP servers tend to close
        // inactive connections pretty aggressively.
        pool.setTestOnBorrow( true );
        return pool;
    }

    private static class FTPClientPooledObjectFactory extends BasePooledObjectFactory<FTPClient> {

        private final String authority;
        private final String host;
        private final int port;
        @Nullable
        private final FTPClientAuthenticator authenticator;

        private FTPClientPooledObjectFactory( String authority, String host, int port, @Nullable FTPClientAuthenticator authenticator ) {
            this.authority = authority;
            this.host = host;
            this.port = port;
            this.authenticator = authenticator;
        }

        @Override
        public FTPClient create() {
            FTPClient client = new FTPClient();
            // recommended settings as per https://ftp.ncbi.nlm.nih.gov/README.ftp
            client.setSendDataSocketBufferSize( 33554432 );
            client.setReceieveDataSocketBufferSize( 33554432 );
            return client;
        }

        @Override
        public PooledObject<FTPClient> wrap( FTPClient ftpClient ) {
            return new DefaultPooledObject<>( ftpClient );
        }

        @Override
        public boolean validateObject( PooledObject<FTPClient> p ) {
            FTPClient client = p.getObject();
            if ( !client.isAvailable() ) {
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
                // this needs to be set after each connect()
                client.enterLocalPassiveMode();
                // always treat files as binary to avoid transformation of EOLs
                client.setFileType( FTP.BINARY_FILE_TYPE );
                log.debug( client.getReplyString() );
                if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                    throw new IOException( String.format( "Failed to set file type to BINARY for %s: %s",
                            authority, client.getReplyString() ) );
                }
                if ( authenticator != null ) {
                    authenticator.authenticate( client, authority );
                }
            }
        }

        @Override
        public void destroyObject( PooledObject<FTPClient> p, DestroyMode destroyMode ) throws Exception {
            if ( destroyMode == DestroyMode.ABANDONED ) {
                FTPClient client = p.getObject();
                if ( client.isConnected() ) {
                    client.disconnect();
                }
            } else {
                destroyObject( p );
            }
        }

        @Override
        public void destroyObject( PooledObject<FTPClient> p ) throws Exception {
            FTPClient client = p.getObject();
            if ( client.isConnected() ) {
                try {
                    client.logout();
                    log.debug( client.getReplyString() );
                    if ( !FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                        throw new IOException( String.format( "Failed to logout from to %s: %s", authority, client.getReplyString() ) );
                    }
                } finally {
                    client.disconnect();
                }
            }
        }
    }
}
