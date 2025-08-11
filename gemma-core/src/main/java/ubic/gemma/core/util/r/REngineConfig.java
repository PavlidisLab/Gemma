package ubic.gemma.core.util.r;

import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class REngineConfig {

    @Bean
    public REngineFactory rEngineFactory(
            @Value("${r.exe}") Path rExe,
            @Value("${r.backend}") String rBackend,
            @Value("${r.rserve.host}") String host,
            @Value("${r.rserve.port}") int port
    ) {
        if ( rBackend.equalsIgnoreCase( "JRIEngine" ) ) {
            return JRIEngine::new;
        } else if ( rBackend.equalsIgnoreCase( "RConnection" ) ) {
            return () -> new RConnection( host, port );
        } else if ( rBackend.equalsIgnoreCase( "StandaloneRConnection" ) ) {
            return () -> new StandaloneRConnection( rExe );
        } else {
            throw new IllegalArgumentException( "Unsupported R backend '" + rBackend + "'. Choose one among: JRIEngine, RConnection or StandaloneRConnection." );
        }
    }
}
