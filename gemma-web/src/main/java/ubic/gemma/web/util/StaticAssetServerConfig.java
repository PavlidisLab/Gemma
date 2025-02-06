package ubic.gemma.web.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class StaticAssetServerConfig {

    @Value("${gemma.staticAssetServer.enabled}")
    private boolean enabled;
    @Value("${gemma.staticAssetServer.baseUrl}")
    private String baseUrl;

    // settings for the internal server
    @Value("${gemma.staticAssetServer.internal.enabled}")
    private boolean internal;
    @Value("${gemma.staticAssetServer.internal.prefix}")
    private Path prefix;
    @Value("${gemma.staticAssetServer.internal.logFile}")
    private Path logFile;

    @Bean
    public StaticAssetServer staticAssetServer() {
        if ( !enabled ) {
            return null;
        }
        if ( internal ) {
            return new InternalStaticAssetServer( prefix, baseUrl, true, logFile );
        } else {
            return new ExternalStaticAssetServer( baseUrl );
        }
    }
}
