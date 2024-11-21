package ubic.gemma.web.util;

import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.web.taglib.AbstractStaticAssetTag;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Base64;

/**
 * Configuration for serving static assets externally.
 * @author poirigui
 * @see AbstractStaticAssetTag
 */
@Getter
@Component
@CommonsLog
public class StaticAssetServer implements InitializingBean {

    @Value("${gemma.staticAssetServer.enabled}")
    private boolean enabled;

    @Value("${gemma.staticAssetServer.baseUrl}")
    private String baseUrl;

    /**
     * Resource directories that are allowed to be served.
     */
    @Value("${gemma.staticAssetServer.allowedDirs}")
    private String[] allowedDirs;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private Environment environment;

    @Override
    public void afterPropertiesSet() {
        for ( String allowedDir : allowedDirs ) {
            Assert.isTrue( allowedDir.startsWith( "/" ) && allowedDir.endsWith( "/" ),
                    "An allowed resource directory must start and end with '/'." );
        }
        if ( enabled ) {
            log.info( "Static assets will be served from " + baseUrl + " for the following paths:\n\t"
                    + String.join( "\n\t", allowedDirs ) );
            if ( !isAlive() ) {
                String message = "The static asset server does not appear to be running.";
                if ( environment.acceptsProfiles( EnvironmentProfiles.DEV ) ) {
                    message += " You may start it with:\n\tnpm --prefix gemma-web/src/main/webapp run serve";
                }
                log.warn( message );
            }
        } else {
            log.debug( "Static assets will be served from the webapp resources under the following paths:\n\t"
                    + String.join( "\n\t", allowedDirs ) );
        }
    }

    /**
     * Check if the static asset server is alive.
     */
    public boolean isAlive() {
        if ( !enabled ) {
            return false;
        }
        URLConnection connection = null;
        try {
            connection = new URL( baseUrl ).openConnection();
            connection.connect();
            return true;
        } catch ( ConnectException e ) {
            return false;
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to access the static asset server.", e );
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
    }

    /**
     * Resolve a URL to a static asset.
     */
    public String resolveUrl( String path ) {
        Assert.isTrue( Arrays.stream( allowedDirs ).anyMatch( path::startsWith ),
                "Path for static asset must start with one of " + String.join( ",", allowedDirs ) + "." );
        Assert.isTrue( path.startsWith( "/" ) );
        if ( enabled ) {
            return baseUrl + path;
        } else {
            return servletContext.getContextPath() + path;
        }
    }
}
