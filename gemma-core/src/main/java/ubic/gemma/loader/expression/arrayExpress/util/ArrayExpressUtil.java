package ubic.gemma.loader.expression.arrayExpress.util;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import ubic.basecode.util.NetUtils;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressUtil {
    protected static final Log log = LogFactory.getLog( ArrayExpressUtil.class );
    private static String host;
    private static String login;
    private static String password;
    public static final String ARRAYEXPRESS_DELIM = "\n";

    static {
        try {
            login = ConfigUtils.getString( "arrayExpress.login" );
            password = ConfigUtils.getString( "arrayExpress.password" );
            host = ConfigUtils.getString( "arrayExpress.host" );
            if ( StringUtils.isBlank( host ) ) throw new ConfigurationException( "No host name found" );
        } catch ( ConfigurationException e ) {
            log.error( e, e );
        }

    }

    /**
     * Convenient method to get a FTP connection.
     * 
     * @param mode
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public static FTPClient connect( int mode ) throws SocketException, IOException {
        FTPClient result = NetUtils.connect( mode, host, login, password );
        return result;
    }

    /**
     * @param f
     * @throws IOException
     */
    public static void disconnect( FTPClient f ) throws IOException {
        f.disconnect();
    }

    /**
     * @return
     */
    public static String getHost() {
        return host;
    }
}
