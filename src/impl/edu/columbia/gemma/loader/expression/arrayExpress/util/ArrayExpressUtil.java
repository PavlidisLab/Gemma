package edu.columbia.gemma.loader.expression.arrayExpress.util;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import baseCode.util.NetUtils;

import edu.columbia.gemma.loader.expression.smd.util.SmdUtil;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressUtil {
    protected static final Log log = LogFactory.getLog( SmdUtil.class );
    private static String host;
    private static String login;
    private static String password;
    public static final String ARRAYEXPRESS_DELIM = "\n";

    static {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );
            login = ( String ) config.getProperty( "arrayExpress.login" );
            password = ( String ) config.getProperty( "arrayExpress.password" );
            host = ( String ) config.getProperty( "arrayExpress.host" );
            if ( host == null || host.length() == 0 ) throw new ConfigurationException( "No host name found" );
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
        return NetUtils.connect( mode, host, login, password );
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
