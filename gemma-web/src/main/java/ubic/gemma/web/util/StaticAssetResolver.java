package ubic.gemma.web.util;

import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.web.taglib.AbstractStaticAssetTag;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.util.Arrays;

/**
 * Resolve static assets either froma {@link InternalStaticAssetServer} or from the webapp resources.
 * @author poirigui
 * @see AbstractStaticAssetTag
 */
@Getter
@Component
@CommonsLog
public class StaticAssetResolver implements InitializingBean {

    /**
     * Resource directories that are allowed to be served.
     */
    @Value("${gemma.staticAssetServer.allowedDirs}")
    private String[] allowedDirs;

    @Nullable
    @Autowired(required = false)
    private StaticAssetServer staticAssetServer;

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
        if ( staticAssetServer != null ) {
            log.info( String.format( "Static assets will be served from %s for the following paths:\n\t%s",
                    staticAssetServer.getBaseUrl(), String.join( "\n\t", allowedDirs ) ) );
            if ( !staticAssetServer.isAlive() ) {
                String message = "The static asset server does not appear to be running.";
                if ( staticAssetServer instanceof ExternalStaticAssetServer
                        && environment.acceptsProfiles( EnvironmentProfiles.DEV ) ) {
                    message += " You may start it with:\n"
                            + "\tnpm --prefix gemma-web/src/main/webapp run serve\n"
                            + "or by launching the \"Serve static assets\" run configuration in IntelliJ.";
                }
                log.warn( message );
            }
        } else {
            log.debug( "Static assets will be served from the webapp resources under the following paths:\n\t"
                    + String.join( "\n\t", allowedDirs ) );
        }
    }

    /**
     * Resolve a URL to a static asset.
     */
    public String resolveUrl( String path ) {
        Assert.isTrue( Arrays.stream( allowedDirs ).anyMatch( path::startsWith ),
                "Path for static asset must start with one of " + String.join( ",", allowedDirs ) + "." );
        Assert.isTrue( path.startsWith( "/" ) );
        if ( staticAssetServer != null ) {
            return staticAssetServer.getBaseUrl() + path;
        } else {
            return servletContext.getContextPath() + path;
        }
    }
}
